package com.sysadmindoc.alarmclock.wear

import android.content.ComponentName
import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import org.json.JSONObject

/** Applies phone-side WakeSync mutations to the local Wear alarm collection. */
class WakeSyncMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != PATH_MUTATION) return
        val payload = runCatching {
            JSONObject(String(messageEvent.data, Charsets.UTF_8))
        }.getOrElse { return }

        val operation = payload.optString("operation")
        val syncId = payload.optString("syncId")
        if (syncId.isBlank()) return

        when (operation) {
            "CREATE", "UPDATE", "ENABLE", "DISABLE" -> {
                val token = payload.optString("alarmToken", "")
                val current = WearAlarmListStore.load(applicationContext)
                    .firstOrNull { it.syncId == syncId }
                val enabled = when (operation) {
                    "ENABLE" -> true
                    "DISABLE" -> false
                    else -> payload.optBoolean("enabled", current?.enabled ?: true)
                }
                WearAlarmListStore.upsert(
                    applicationContext,
                    WearAlarmListStore.Entry(
                        syncId = syncId,
                        label = payload.optString("label", current?.label ?: ""),
                        hour = payload.optInt("hour", current?.hour ?: 0),
                        minute = payload.optInt("minute", current?.minute ?: 0),
                        enabled = enabled,
                        revision = payload.optLong("revision", current?.revision ?: 0L),
                        updatedAt = payload.optLong("timestamp", System.currentTimeMillis()),
                        alarmToken = if (token.isNotBlank()) token else current?.alarmToken.orEmpty()
                    )
                )
            }
            "DELETE" -> WearAlarmListStore.remove(applicationContext, syncId)
            "RINGING", "DISMISS", "SNOOZE" -> Unit
            else -> return
        }

        // Keep the legacy single-alarm presentation in step with sync state.
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_PAYLOAD, payload.toString())
            .putLong(KEY_RECEIVED_AT, System.currentTimeMillis())
            .apply()
        requestUiRefresh()
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
