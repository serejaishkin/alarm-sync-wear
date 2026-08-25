package com.sysadmindoc.alarmclock.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.text.DateFormat
import java.util.Date

/**
 * Low-priority foreground service whose only job is to keep the WakeSync
 * process alive while an alarm is armed. `AlarmManager` itself survives
 * process death on stock/certified Wear OS, but some OEM battery managers
 * (observed on OPLUS, reported by a third-party project) force-stop the app
 * shortly after screen-off AND actively cancel that app's pending alarms
 * (`removeAlarmsForPackage`) — an aggressive behaviour outside normal
 * AlarmManager semantics. A silent, minimal-priority foreground service is
 * the standard defence: it's a much less attractive force-stop target than a
 * backgrounded process with no visible foreground state.
 *
 * Not verified as an issue on Galaxy Watch 4 / stock Wear OS specifically —
 * this is a cheap, harmless precaution ported from that project, not a
 * confirmed local bug fix.
 *
 * [refresh] is the only entry point that matters: call it after any change
 * to the alarm set (schedule/cancel/dismiss/snooze). It looks at the full
 * list of armed alarms and starts/stops the service as needed, rather than
 * tying service lifetime to any single alarm's schedule/cancel call — with
 * multiple alarms, cancelling one must not stop protection for the others.
 */
class WearAlarmKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // intent can be null if the system restarts the service after a kill
        // (START_STICKY) — the next trigger time is unknown then, but
        // startForeground must still be called immediately.
        val triggerMs = intent?.getLongExtra(EXTRA_TRIGGER_MS, -1L) ?: -1L
        try {
            startForeground(NOTIFICATION_ID, buildNotification(triggerMs))
        } catch (e: Exception) {
            stopSelf()
        }
        return START_STICKY
    }

    private fun buildNotification(triggerMs: Long): Notification {
        ensureChannel(this)
        val whenStr = if (triggerMs > 0L) {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(triggerMs))
        } else {
            "—"
        }
        val openIntent = Intent(this, WakeSyncAlarmListActivity::class.java)
        val pi = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(whenStr)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "wakesync_keepalive"
        private const val NOTIFICATION_ID = 710099
        private const val EXTRA_TRIGGER_MS = "trigger_ms"

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return
            val ch = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            nm.createNotificationChannel(ch)
        }

        private fun start(context: Context, triggerMs: Long) {
            val app = context.applicationContext
            val intent = Intent(app, WearAlarmKeepAliveService::class.java).apply {
                putExtra(EXTRA_TRIGGER_MS, triggerMs)
            }
            try {
                if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(intent) else app.startService(intent)
            } catch (_: Exception) {
                // Best-effort: if the OS refuses the FGS start here, alarms still
                // fire via AlarmManager as before — this is a defence-in-depth
                // addition, not a hard dependency.
            }
        }

        private fun stop(context: Context) {
            val app = context.applicationContext
            try {
                app.stopService(Intent(app, WearAlarmKeepAliveService::class.java))
            } catch (_: Exception) {
            }
        }

        /**
         * Recomputes whether ANY enabled alarm currently has a future trigger
         * and starts/stops the keep-alive service accordingly. Safe to call
         * after every schedule/cancel/dismiss/snooze — cheap, idempotent.
         */
        fun refresh(context: Context) {
            val soonest = WearAlarmListStore.load(context)
                .filter { it.enabled }
                .mapNotNull { WearAlarmScheduler.nextTrigger(it) }
                .minOrNull()
            if (soonest != null) {
                start(context, soonest.toInstant().toEpochMilli())
            } else {
                stop(context)
            }
        }
    }
}
