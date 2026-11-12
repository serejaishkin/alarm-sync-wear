package com.wakesync.app.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Alarm UI on Wear OS. Audio/haptics are owned by WearAlarmFeedbackService. */
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
            setPadding(16, 16, 16, 16)
        }
        WearUi.styleRoot(this, root)

        // Large time in centre
        root.addView(TextView(this).apply {
            text = String.format("%02d:%02d", entry.hour, entry.minute)
            textSize = 56f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.text_primary, theme))
            WearUi.styleHeader(this@WearAlarmFiringActivity, this)
        })

        // Label below time
        if (entry.label.isNotBlank()) root.addView(TextView(this).apply {
            text = entry.label
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.text_secondary, theme))
            WearUi.styleSectionLabel(this@WearAlarmFiringActivity, this)
        })

        Spacer(root, 24)

        // Auto-silence progress + countdown
        root.addView(TextView(this).apply {
            text = String.format("%02d:%02d", entry.snoozeDurationMinutes / 60, entry.snoozeDurationMinutes % 60)
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(resources.getColor(R.color.text_muted, theme))
        })

        Spacer(root, 32)

        // Bottom buttons row
        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        btnRow.addView(Button(this).apply {
            text = getString(R.string.wear_snooze_button, entry.snoozeDurationMinutes)
            WearUi.styleActionButton(this@WearAlarmFiringActivity, this, R.color.snooze_yellow)
            setOnClickListener { snooze(entry) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { weight = 1f }
        })

        btnRow.addView(Button(this).apply {
            text = getString(R.string.wear_dismiss)
            WearUi.styleActionButton(this@WearAlarmFiringActivity, this, R.color.dismiss_green)
            setOnClickListener { dismiss(entry) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { weight = 1f }
        })

        root.addView(btnRow)
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
        const val ACTION_REMOTE_SNOOZE = "com.wakesync.app.wear.REMOTE_SNOOZE"
        const val ACTION_REMOTE_DISMISS = "com.wakesync.app.wear.REMOTE_DISMISS"
        @Volatile var activeSyncId: String? = null
    }
}

private fun Spacer(parent: LinearLayout, dp: Int) {
    val v = View(parent.context)
    val px = (dp * parent.context.resources.displayMetrics.density).toInt()
    v.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px)
    parent.addView(v)
}
