package com.wakesync.app.wear

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.wakesync.app.domain.AlarmScheduler
import com.wakesync.app.receiver.DismissReceiver
import com.wakesync.app.receiver.SkipNextReceiver
import com.wakesync.app.receiver.SnoozeReceiver
import com.wakesync.app.service.AlarmService

class WearAlarmActionListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val alarmId = runCatching {
            DataMap.fromByteArray(messageEvent.data)
                .getLong(WearAlarmData.KEY_ALARM_ID, -1L)
        }.getOrDefault(-1L)
        if (alarmId <= 0L) return

        when (messageEvent.path) {
            WearAlarmData.PATH_ACTION_SKIP, WearAlarmData.PATH_WAKESYNC_ACTION_SKIP -> {
                sendBroadcast(Intent(applicationContext, SkipNextReceiver::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                })
            }
            WearAlarmData.PATH_ACTION_SNOOZE, WearAlarmData.PATH_WAKESYNC_ACTION_SNOOZE -> forwardFiringOnly(
                alarmId = alarmId,
                receiver = SnoozeReceiver::class.java,
                actionName = "snooze"
            )
            WearAlarmData.PATH_ACTION_DISMISS, WearAlarmData.PATH_WAKESYNC_ACTION_DISMISS -> forwardFiringOnly(
                alarmId = alarmId,
                receiver = DismissReceiver::class.java,
                actionName = "dismiss"
            )
        }
    }

    private fun forwardFiringOnly(
        alarmId: Long,
        receiver: Class<*>,
        actionName: String,
    ) {
        if (AlarmService.activeAlarmId != alarmId) {
            Log.i(TAG, "Ignored Wear $actionName for non-firing alarm $alarmId")
            return
        }
        sendBroadcast(Intent(applicationContext, receiver).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        })
    }

    companion object {
        private const val TAG = "WearAlarmAction"
    }
}
