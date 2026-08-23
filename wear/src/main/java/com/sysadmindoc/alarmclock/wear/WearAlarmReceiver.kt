package com.sysadmindoc.alarmclock.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Wakes the Wear firing UI at the scheduled time. */
class WearAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WearAlarmScheduler.ACTION) return
        val syncId = WearAlarmScheduler.syncId(intent)
        if (syncId.isBlank()) return
        val entry = WearAlarmListStore.load(context).firstOrNull { it.syncId == syncId } ?: return
        if (!entry.enabled) return

        if (!WearAlarmScheduler.isSnooze(intent) && entry.repeatDays.isEmpty()) {
            // One-shot alarms stay disabled after firing; the firing screen still
            // knows which alarm woke the user and can dismiss it normally.
            WearAlarmListStore.upsert(context, entry.copy(enabled = false))
        }

        val firing = Intent(context, WearAlarmFiringActivity::class.java).apply {
            putExtra(WearAlarmFiringActivity.EXTRA_SYNC_ID, syncId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        runCatching { context.startActivity(firing) }

        if (!WearAlarmScheduler.isSnooze(intent)) {
            WearAlarmScheduler.rescheduleAfterDismiss(context, entry)
        }
    }
}
