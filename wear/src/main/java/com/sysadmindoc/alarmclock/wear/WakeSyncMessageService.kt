package com.sysadmindoc.alarmclock.wear

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

/** Receives persistent phone-side alarm state through DataClient. */
class WakeSyncMessageService : WearableListenerService() {

    override fun onCreate() {
        super.onCreate()
        // Log connected nodes on watch side too
        Wearable.getNodeClient(this).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w(TAG, "Watch: No connected phone nodes — Data Layer will not receive")
                } else {
                    nodes.forEach { node ->
                        Log.i(TAG, "Watch: Connected phone: ${node.displayName} id=${node.id}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Watch: Failed to list nodes: ${e.message}", e)
            }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(TAG, "onDataChanged: received ${dataEvents.count} events")
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val path = event.dataItem.uri.path.orEmpty()
            Log.d(TAG, "Data event path: $path")
            if (!path.startsWith(PATH_ALARM_STATE_PREFIX)) continue
            val dataMap = runCatching { DataMapItem.fromDataItem(event.dataItem).dataMap }.getOrNull() ?: continue
            val encoded = dataMap.getString(KEY_MUTATION).orEmpty()
            if (encoded.isBlank()) continue
            Log.d(TAG, "Applying mutation: ${encoded.take(200)}")
            applyPersistentMutation(encoded)
        }
    }

    /** MessageClient remains the low-latency path for alarm mutations. */
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}")
        if (messageEvent.path != PATH_MUTATION) return
        applyPersistentMutation(String(messageEvent.data, Charsets.UTF_8))
    }

    private fun applyPersistentMutation(raw: String) {
        val payload = runCatching { JSONObject(raw) }.getOrElse { return }
        val operation = payload.optString("operation")
        val syncId = payload.optString("syncId")
        if (syncId.isBlank()) return

        val incomingRevision = payload.optLong("revision", 0L)
        val incomingTimestamp = payload.optLong("timestamp", 0L)
        val currentRevision = WearAlarmListStore.revisionFor(applicationContext, syncId)
        val currentTimestamp = WearAlarmListStore.timestampFor(applicationContext, syncId)
        if (incomingRevision < currentRevision ||
            incomingRevision == currentRevision && incomingTimestamp <= currentTimestamp) return

        when (operation) {
            "CREATE", "UPDATE", "ENABLE", "DISABLE" -> {
                val current = WearAlarmListStore.load(applicationContext).firstOrNull { it.syncId == syncId }
                val enabled = when (operation) {
                    "ENABLE" -> true
                    "DISABLE" -> false
                    else -> payload.optBoolean("enabled", current?.enabled ?: true)
                }
                val repeatDays = if (payload.has("repeatDays")) {
                    buildSet {
                        val array = payload.optJSONArray("repeatDays")
                        if (array != null) for (i in 0 until array.length()) add(array.optInt(i))
                    }
                } else current?.repeatDays.orEmpty()
                val entry = WearAlarmListStore.Entry(
                    syncId = syncId,
                    label = payload.optString("label", current?.label ?: ""),
                    hour = payload.optInt("hour", current?.hour ?: 0),
                    minute = payload.optInt("minute", current?.minute ?: 0),
                    enabled = enabled,
                    repeatDays = repeatDays,
                    snoozeDurationMinutes = payload.optInt("snoozeDurationMinutes", current?.snoozeDurationMinutes ?: 10),
                    vibrationEnabled = payload.optBoolean("vibrationEnabled", current?.vibrationEnabled ?: true),
                    volume = payload.optInt("volume", current?.volume ?: 100),
                    revision = incomingRevision,
                    updatedAt = incomingTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    alarmToken = payload.optString("alarmToken").ifBlank { current?.alarmToken.orEmpty() },
                    source = payload.optString("source", "PHONE"),
                    originDeviceId = payload.optString("originDeviceId")
                )
                WearAlarmListStore.upsert(applicationContext, entry)
                if (operation == "DISABLE") {
                    notifyActiveFiringActivity(syncId, WearAlarmFiringActivity.ACTION_REMOTE_DISMISS)
                }
            }
            "DELETE" -> {
                WearAlarmListStore.removeWithTombstone(
                    applicationContext,
                    syncId,
                    incomingRevision,
                    incomingTimestamp.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    payload.optString("source", "PHONE"),
                    payload.optString("originDeviceId")
                )
            }
            "SNOOZE" -> {
                val current = WearAlarmListStore.load(applicationContext).firstOrNull { it.syncId == syncId }
                if (current != null) {
                    WearAlarmScheduler.scheduleSnooze(applicationContext, current, current.snoozeDurationMinutes)
                    notifyActiveFiringActivity(syncId, WearAlarmFiringActivity.ACTION_REMOTE_SNOOZE)
                }
            }
            "DISMISS" -> {
                val current = WearAlarmListStore.load(applicationContext).firstOrNull { it.syncId == syncId }
                if (current != null) {
                    WearAlarmScheduler.rescheduleAfterDismiss(applicationContext, current)
                    notifyActiveFiringActivity(syncId, WearAlarmFiringActivity.ACTION_REMOTE_DISMISS)
                }
            }
            "RINGING" -> Unit
            else -> return
        }

        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_PAYLOAD, payload.toString())
            .putLong(KEY_RECEIVED_AT, System.currentTimeMillis())
            .apply()
        requestUiRefresh()
    }

    private fun notifyActiveFiringActivity(syncId: String, action: String) {
        if (WearAlarmFiringActivity.activeSyncId != syncId) return
        val intent = Intent(applicationContext, WearAlarmFiringActivity::class.java).apply {
            this.action = action
            putExtra(WearAlarmFiringActivity.EXTRA_SYNC_ID, syncId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { applicationContext.startActivity(intent) }
    }

    private fun requestUiRefresh() {
        runCatching {
            TileService.getUpdater(applicationContext).requestUpdate(NextAlarmTileService::class.java)
            ComplicationDataSourceUpdateRequester.create(
                applicationContext,
                ComponentName(applicationContext, NextAlarmComplicationDataSourceService::class.java)
            ).requestUpdateAll()
        }
    }

    companion object {
        private const val TAG = "WakeSyncWatch"
        const val PATH_MUTATION = "/wakesync/alarm/mutation"
        private const val PATH_ALARM_STATE_PREFIX = "/wakesync/alarm/state/"
        private const val KEY_MUTATION = "mutation"
        private const val PREFS = "wakesync_transport"
        private const val KEY_LAST_PAYLOAD = "last_payload"
        private const val KEY_RECEIVED_AT = "received_at"
    }
}
