package com.sysadmindoc.alarmclock.wear

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import org.json.JSONObject

/** Applies phone-side WakeSync mutations to the local Wear alarm collection. */
class WakeSyncMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != PATH_MUTATION) return
        val payload = runCatching { JSONObject(String(messageEvent.data, Charsets.UTF_8)) }.getOrElse { return }
        val operation = payload.optString("operation")
        val syncId = payload.optString("syncId")
        if (syncId.isBlank()) return

        val current = WearAlarmListStore.load(applicationContext).firstOrNull { it.syncId == syncId }

        when (operation) {
            "CREATE", "UPDATE", "ENABLE", "DISABLE" -> {
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
                WearAlarmListStore.upsert(
                    applicationContext,
                    WearAlarmListStore.Entry(
                        syncId = syncId,
                        label = payload.optString("label", current?.label ?: ""),
                        hour = payload.optInt("hour", current?.hour ?: 0),
                        minute = payload.optInt("minute", current?.minute ?: 0),
                        enabled = enabled,
                        repeatDays = repeatDays,
                        snoozeDurationMinutes = payload.optInt("snoozeDurationMinutes", current?.snoozeDurationMinutes ?: 10),
                        vibrationEnabled = payload.optBoolean("vibrationEnabled", current?.vibrationEnabled ?: true),
                        volume = payload.optInt("volume", current?.volume ?: 100),
                        revision = payload.optLong("revision", current?.revision ?: 0L),
                        updatedAt = payload.optLong("timestamp", System.currentTimeMillis()),
                        alarmToken = payload.optString("alarmToken").ifBlank { current?.alarmToken.orEmpty() }
                    )
                )
            }
            "DELETE" -> WearAlarmListStore.remove(applicationContext, syncId)
            "SNOOZE" -> {
                if (current != null) {
                    WearAlarmScheduler.scheduleSnooze(applicationContext, current, current.snoozeDurationMinutes)
                    notifyActiveFiringActivity(syncId, WearAlarmFiringActivity.ACTION_REMOTE_SNOOZE)
                }
            }
            "DISMISS" -> {
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
        const val PATH_MUTATION = "/wakesync/alarm/mutation"
        private const val PREFS = "wakesync_transport"
        private const val KEY_LAST_PAYLOAD = "last_payload"
        private const val KEY_RECEIVED_AT = "received_at"
    }
}
