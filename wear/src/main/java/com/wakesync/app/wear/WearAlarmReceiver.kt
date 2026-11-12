package com.wakesync.app.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.time.ZonedDateTime

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

        // v1.15.x: guard against firing at the wrong local time. After the watch
        // boots or the app is replaced, the system timezone may not have been
        // applied yet — ZonedDateTime.now() could report UTC while the user
        // scheduled 06:00 local. Without this guard the alarm would fire 3–5
        // hours late (e.g. 06:00 scheduled → 09:00–11:00 actual).
        val expectedHour = intent.getIntExtra(WearAlarmScheduler.EXTRA_EXPECTED_HOUR, -1)
        val expectedMinute = intent.getIntExtra(WearAlarmScheduler.EXTRA_EXPECTED_MINUTE, -1)
        val now = ZonedDateTime.now()
        if (expectedHour != -1 && expectedMinute != -1 &&
            (now.hour != expectedHour || now.minute != expectedMinute)) {
            Log.w(TAG, "Skipping fire for $syncId: expected ${String.format("%02d:%02d", expectedHour, expectedMinute)} " +
                "local=${now.hour}:${now.minute} — timezone likely not applied yet, rescheduling.")
            WearAlarmScheduler.schedule(context, entry)
            return
        }

        // Local first: the watch starts its own feedback immediately and does
        // not wait for Bluetooth/Wear Data Layer. If a peer is connected,
        // this event is mirrored to the phone in parallel.
        WearAlarmFeedbackService.start(context, syncId, entry.label)
        WakeSyncPeerController.sendMutation(context, "RINGING", syncId, null)

        val firing = Intent(context, WearAlarmFiringActivity::class.java).apply {
            putExtra(WearAlarmFiringActivity.EXTRA_SYNC_ID, syncId)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        runCatching { context.startActivity(firing) }
            .onFailure { Log.e(TAG, "Failed to open firing screen for syncId=$syncId", it) }

        // Repeating alarms get their next occurrence immediately. Snooze owns
        // its own exact alarm and must not be replaced here.
        if (!WearAlarmScheduler.isSnooze(intent)) {
            WearAlarmScheduler.rescheduleAfterDismiss(context, entry)
        }

    }

    companion object {
        private const val TAG = "WakeSyncWatch"
    }
}
