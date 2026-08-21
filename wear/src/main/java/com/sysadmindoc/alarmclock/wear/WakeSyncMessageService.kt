package com.sysadmindoc.alarmclock.wear

import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * First Wear-side endpoint for the WakeSync protocol.
 *
 * Phase one only persists the received envelope. Applying it to the local alarm
 * scheduler comes after the transport and storage contracts are verified.
 */
class WakeSyncMessageService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != "/wakesync/alarm/mutation") return
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_PAYLOAD, String(messageEvent.data, Charsets.UTF_8))
            .putLong(KEY_RECEIVED_AT, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val PREFS = "wakesync_transport"
        private const val KEY_LAST_PAYLOAD = "last_payload"
        private const val KEY_RECEIVED_AT = "received_at"
    }
}
