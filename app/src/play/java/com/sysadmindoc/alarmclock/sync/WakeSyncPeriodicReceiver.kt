package com.sysadmindoc.alarmclock.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Periodically republishes the complete phone alarm snapshot for reconnecting watches. */
@AndroidEntryPoint
class WakeSyncPeriodicReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: AlarmSyncCoordinator

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, ACTION_SYNC)) return
        schedule(context)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try { coordinator.syncNow() } finally { pendingResult.finish() }
        }
    }

    companion object {
        const val ACTION_SYNC = "com.sysadmindoc.alarmclock.WAKESYNC_PERIODIC_SYNC"
        private const val REQUEST_CODE = 7302
        private const val INTERVAL_MS = 15 * 60 * 1000L

        fun schedule(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val pending = PendingIntent.getBroadcast(
                context, REQUEST_CODE,
                Intent(context, WakeSyncPeriodicReceiver::class.java).setAction(ACTION_SYNC),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pending)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + INTERVAL_MS,
                INTERVAL_MS,
                pending
            )
        }
    }
}
