package com.sysadmindoc.alarmclock.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

/**
 * The watch schedules its own alarms. Wear Data Layer is only used to keep the
 * schedule in sync; a Bluetooth connection is never required at firing time.
 */
object WearAlarmScheduler {
    private const val ACTION_FIRE = "com.sysadmindoc.alarmclock.wear.FIRE_ALARM"
    private const val EXTRA_SYNC_ID = "syncId"
    private const val EXTRA_ALARM_TOKEN = "alarmToken"
    private const val EXTRA_SNOOZE = "snooze"
    private const val REQUEST_BASE = 710000

    fun schedule(context: Context, entry: WearAlarmListStore.Entry) {
        cancel(context, entry.syncId)
        if (!entry.enabled) return
        val trigger = nextTrigger(entry) ?: return
        val intent = Intent(context, WearAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_SYNC_ID, entry.syncId)
            putExtra(EXTRA_ALARM_TOKEN, entry.alarmToken)
            putExtra(EXTRA_SNOOZE, false)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(entry.syncId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            trigger.toInstant().toEpochMilli(),
            pi
        )
    }

    fun scheduleSnooze(context: Context, entry: WearAlarmListStore.Entry, minutes: Int) {
        cancel(context, entry.syncId)
        val triggerAt = System.currentTimeMillis() + minutes.coerceIn(1, 120) * 60_000L
        val intent = Intent(context, WearAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_SYNC_ID, entry.syncId)
            putExtra(EXTRA_ALARM_TOKEN, entry.alarmToken)
            putExtra(EXTRA_SNOOZE, true)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(entry.syncId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        context.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pi
        )
    }

    fun cancel(context: Context, syncId: String) {
        val intent = Intent(context, WearAlarmReceiver::class.java).apply { action = ACTION_FIRE }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode(syncId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            context.getSystemService(AlarmManager::class.java).cancel(pi)
            pi.cancel()
        }
    }

    fun nextTrigger(entry: WearAlarmListStore.Entry, now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? {
        val time = now.withHour(entry.hour.coerceIn(0, 23))
            .withMinute(entry.minute.coerceIn(0, 59))
            .withSecond(0).withNano(0)
        if (entry.repeatDays.isEmpty()) {
            return if (time.isAfter(now)) time else time.plusDays(1)
        }
        for (offset in 0..7) {
            val candidate = time.plusDays(offset.toLong())
            val day = candidate.dayOfWeek.value
            if (day in entry.repeatDays && (candidate.isAfter(now) || offset > 0)) return candidate
        }
        return null
    }

    fun rescheduleAfterDismiss(context: Context, entry: WearAlarmListStore.Entry) {
        if (entry.enabled && entry.repeatDays.isNotEmpty()) schedule(context, entry)
    }

    private fun requestCode(syncId: String): Int =
        REQUEST_BASE + (syncId.hashCode() and 0x0007FFFF)

    internal fun syncId(intent: Intent) = intent.getStringExtra(EXTRA_SYNC_ID).orEmpty()
    internal fun alarmToken(intent: Intent) = intent.getStringExtra(EXTRA_ALARM_TOKEN).orEmpty()
    internal fun isSnooze(intent: Intent) = intent.getBooleanExtra(EXTRA_SNOOZE, false)
    internal const val ACTION = ACTION_FIRE
}
