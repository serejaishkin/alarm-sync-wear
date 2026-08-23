package com.sysadmindoc.alarmclock.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Wakes the Wear firing UI and restores the independent schedule after reboot/update. */
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

        val firing = Intent(context, WearAlarmFiringActivity::class.java).apply {
            putExtra(WearAlarmFiringActivity.EXTRA_SYNC_ID, syncId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { context.startActivity(firing) }

        // Repeating alarms get their next occurrence immediately. A one-shot
        // alarm is intentionally left configured but has no next trigger; it
        // can be edited/enabled again without losing the synchronized entry.
        if (!WearAlarmScheduler.isSnooze(intent)) {
            WearAlarmScheduler.rescheduleAfterDismiss(context, entry)
        }
    }
}
