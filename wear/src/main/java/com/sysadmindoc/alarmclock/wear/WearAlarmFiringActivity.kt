package com.sysadmindoc.alarmclock.wear

import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Alarm UI. Audio/haptics are owned by WearAlarmFeedbackService. */
class WearAlarmFiringActivity : ComponentActivity() {
    private var syncId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        syncId = intent.getStringExtra(EXTRA_SYNC_ID).orEmpty()
        val entry = WearAlarmListStore.load(this).firstOrNull { it.syncId == syncId } ?: run {
            finish(); return
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 24, 24, 24)
        }
        WearUi.styleRoot(this, root)
        root.addView(TextView(this).apply {
            text = String.format("%02d:%02d", entry.hour, entry.minute)
            textSize = 46f
            gravity = Gravity.CENTER
            WearUi.styleHeader(this@WearAlarmFiringActivity, this)
        })
        if (entry.label.isNotBlank()) root.addView(TextView(this).apply {
            text = entry.label
            textSize = 18f
            gravity = Gravity.CENTER
            WearUi.styleSectionLabel(this@WearAlarmFiringActivity, this)
        })

        root.addView(Button(this).apply {
            text = "Отложить ${entry.snoozeDurationMinutes} мин"
            WearUi.styleActionButton(this@WearAlarmFiringActivity, this, R.color.snooze_yellow)
            setOnClickListener { snooze(entry) }
        })
        root.addView(Button(this).apply {
            text = "Выключить"
            WearUi.styleActionButton(this@WearAlarmFiringActivity, this, R.color.dismiss_green)
            setOnClickListener { dismiss(entry) }
        })
        setContentView(root)

        activeSyncId = syncId
        if (intent.action == ACTION_REMOTE_SNOOZE || intent.action == ACTION_REMOTE_DISMISS) finish()
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        setIntent(intent)
        if (intent.action == ACTION_REMOTE_SNOOZE || intent.action == ACTION_REMOTE_DISMISS) finish()
    }

    private fun stopFeedback() = WearAlarmFeedbackService.stop(this)

    private fun snooze(entry: WearAlarmListStore.Entry) {
        stopFeedback()
        WearAlarmScheduler.scheduleSnooze(this, entry, entry.snoozeDurationMinutes)
        WakeSyncPeerController.sendMutation(this, "SNOOZE", entry.syncId, entry.alarmToken)
        finish()
    }

    private fun dismiss(entry: WearAlarmListStore.Entry) {
        stopFeedback()
        WearAlarmScheduler.rescheduleAfterDismiss(this, entry)
        WakeSyncPeerController.sendMutation(this, "DISMISS", entry.syncId, entry.alarmToken)
        finish()
    }

    override fun onDestroy() {
        if (activeSyncId == syncId) activeSyncId = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SYNC_ID = "syncId"
        const val ACTION_REMOTE_SNOOZE = "com.sysadmindoc.alarmclock.wear.REMOTE_SNOOZE"
        const val ACTION_REMOTE_DISMISS = "com.sysadmindoc.alarmclock.wear.REMOTE_DISMISS"
        @Volatile var activeSyncId: String? = null
    }
}
