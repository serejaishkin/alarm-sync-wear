package com.sysadmindoc.alarmclock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sysadmindoc.alarmclock.data.local.entity.AlarmIncidentEvent
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmFireDismissContract

/**
 * Handles snooze action from notification button.
 */
class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        if (alarmId == -1L) return
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L)
        val fireId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID)
            ?: AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)

        context.sendBroadcast(Intent("com.sysadmindoc.alarmclock.WAKESYNC_LOCAL_ACTION").apply {
            setPackage(context.packageName)
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID, fireId)
            putExtra("operation", "SNOOZE")
        })

        val serviceIntent = AlarmFireDismissContract.snoozeServiceIntent(context, alarmId, scheduledAt, fireId)
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
                        reasonCode = "SNOOZE_ACTION_RECEIVED",
                        source = "SnoozeReceiver"
                    )
                )
            )
        } catch (e: Exception) {
            Log.e("SnoozeReceiver", "startForegroundService failed for alarm $alarmId", e)
            recordAlarmIncidentsAsync(
                context,
                listOf(
                    ReceiverAlarmIncident(
                        alarmId = alarmId,
                        fireId = fireId,
                        scheduledAt = scheduledAt,
                        type = AlarmIncidentEvent.TYPE_FOREGROUND_SERVICE,
                        status = AlarmIncidentEvent.STATUS_FAILED,
                        reasonCode = "SNOOZE_SERVICE_START_FAILED_${e.javaClass.simpleName}",
                        source = "SnoozeReceiver"
                    )
                )
            )
        }
    }
}