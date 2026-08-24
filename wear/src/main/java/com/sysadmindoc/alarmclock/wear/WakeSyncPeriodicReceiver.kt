package com.sysadmindoc.alarmclock.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Periodically asks the phone to republish its complete alarm snapshot. */
class WakeSyncPeriodicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, ACTION_SYNC -> {
                requestAndSchedule(context)
            }
        }
    }

    companion object {
        const val ACTION_SYNC = "com.sysadmindoc.alarmclock.wear.WAKESYNC_PERIODIC_SYNC"
        private const val REQUEST_CODE = 7301
        private const val INTERVAL_MS = 15 * 60 * 1000L

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val pending = pendingIntent(context)
            alarmManager.cancel(pending)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + INTERVAL_MS,
                INTERVAL_MS,
                pending
            )
        }

        private fun requestAndSchedule(context: Context) {
            WakeSyncPeerController.requestSnapshot(context)
            schedule(context)
        }

        private fun pendingIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                Intent(context, WakeSyncPeriodicReceiver::class.java).setAction(ACTION_SYNC),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }
}
