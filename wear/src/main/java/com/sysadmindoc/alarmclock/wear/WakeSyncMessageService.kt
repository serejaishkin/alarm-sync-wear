package com.sysadmindoc.alarmclock.wear

import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import android.content.ComponentName
import org.json.JSONObject

/** Applies phone-side WakeSync events to the local Wear presentation state. */
class WakeSyncMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != PATH_MUTATION) return
        val payload = String(messageEvent.data, Charsets.UTF_8)
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAST_PAYLOAD, payload)
            .putLong(KEY_RECEIVED_AT, System.currentTimeMillis())
            .apply()

        val operation = runCatching { JSONObject(payload).optString("operation") }.getOrDefault("")
        val current = WearAlarmStore.load(applicationContext)
        val updated = when (operation) {
            "RINGING" -> current.copy(isFiring = true, updatedAt = System.currentTimeMillis())
            "DISMISS", "SNOOZE" -> current.copy(isFiring = false, updatedAt = System.currentTimeMillis())
            "DELETE" -> WearAlarmSnapshot(updatedAt = System.currentTimeMillis())
            else -> current.copy(updatedAt = System.currentTimeMillis())
        }
        WearAlarmStore.save(applicationContext, updated)
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
