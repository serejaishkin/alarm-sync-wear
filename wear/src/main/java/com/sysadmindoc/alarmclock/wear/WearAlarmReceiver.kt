package com.sysadmindoc.alarmclock.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Wakes the Wear alarm engine and UI without depending on the app process. */
class WearAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                WearAlarmListStore.load(context).forEach { WearAlarmScheduler.schedule(context, it) }
                return
            }
            WearAlarmScheduler.ACTION -> Unit
            else -> return
        }

        val syncId = WearAlarmScheduler.syncId(intent)
        if (syncId.isBlank()) return
        val entry = WearAlarmListStore.load(context).firstOrNull { it.syncId == syncId } ?: return
        if (!entry.enabled) return

        // Local first: the watch starts its own feedback immediately and does
        // not wait for Bluetooth/Wear Data Layer. If a peer is connected,
        // this event is mirrored to the phone in parallel.
        WearAlarmFeedbackService.start(context, syncId, entry.label)
        WakeSyncPeerController.sendMutation(context, "RINGING", syncId, entry.alarmToken)

        val firing = Intent(context, WearAlarmFiringActivity::class.java).apply {
            putExtra(WearAlarmFiringActivity.EXTRA_SYNC_ID, syncId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { context.startActivity(firing) }

        // Repeating alarms get their next occurrence immediately. Snooze owns
        // its own exact alarm and must not be replaced here.
        if (!WearAlarmScheduler.isSnooze(intent)) {
            WearAlarmScheduler.rescheduleAfterDismiss(context, entry)
        }
    }
}
