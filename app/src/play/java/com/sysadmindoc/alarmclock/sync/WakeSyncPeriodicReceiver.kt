package com.sysadmindoc.alarmclock.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.wearable.Wearable

/** Periodically requests the Wear snapshot. Phone state is published only after that snapshot is merged. */
class WakeSyncPeriodicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED, ACTION_SYNC)) return
        schedule(context)
        requestWatchSnapshot(context)
    }

    private fun requestWatchSnapshot(context: Context) {
        Wearable.getNodeClient(context.applicationContext).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context.applicationContext)
                    .sendMessage(node.id, PATH_REQUEST_WATCH_SNAPSHOT, ByteArray(0))
            }
        }
    }

    companion object {
        const val ACTION_SYNC = "com.sysadmindoc.alarmclock.WAKESYNC_PERIODIC_SYNC"
        const val PATH_REQUEST_WATCH_SNAPSHOT = "/wakesync/alarm/request_watch_snapshot"
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
