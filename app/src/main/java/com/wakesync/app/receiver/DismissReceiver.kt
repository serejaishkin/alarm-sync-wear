package com.wakesync.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.wakesync.app.data.local.entity.AlarmIncidentEvent
import com.wakesync.app.domain.AlarmScheduler
import com.wakesync.app.service.AlarmFireDismissContract

/**
 * Handles dismiss action from notification button.
 */
class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L)
        val fireId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID)
            ?: AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)

        context.sendBroadcast(Intent("com.wakesync.app.WAKESYNC_LOCAL_ACTION").apply {
            setPackage(context.packageName)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
            putExtra("operation", "DISMISS")
        })

        val serviceIntent = AlarmFireDismissContract.dismissServiceIntent(context, alarmId, scheduledAt, fireId)
        try {
            context.startForegroundService(serviceIntent)
            recordAlarmIncidentsAsync(
                context,
                listOf(
                    ReceiverAlarmIncident(
                        alarmId = alarmId,
                        fireId = fireId,
                        scheduledAt = scheduledAt,
                        type = AlarmIncidentEvent.TYPE_BROADCAST,
                        status = AlarmIncidentEvent.STATUS_RECEIVED,
                        reasonCode = "DISMISS_ACTION_RECEIVED",
                        source = "DismissReceiver"
                    )
                )
            )
        } catch (e: Exception) {
            Log.e("DismissReceiver", "startForegroundService failed for alarm $alarmId", e)
            recordAlarmIncidentsAsync(
                context,
                listOf(
                    ReceiverAlarmIncident(
                        alarmId = alarmId,
                        fireId = fireId,
                        scheduledAt = scheduledAt,
                        type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                        status = AlarmIncidentEvent.STATUS_FAILED,
                        reasonCode = "DISMISS_SERVICE_START_FAILED_${e.javaClass.simpleName}",
                        source = "DismissReceiver"
                    )
                )
            )
        }
    }
}