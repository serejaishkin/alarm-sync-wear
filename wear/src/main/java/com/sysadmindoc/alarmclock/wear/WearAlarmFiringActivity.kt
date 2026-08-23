package com.sysadmindoc.alarmclock.wear

import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity

/** Alarm UI that is independent from MediaSession/media controls. */
class WearAlarmFiringActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
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
        root.addView(TextView(this).apply {
            text = String.format("%02d:%02d", entry.hour, entry.minute)
            textSize = 44f
            gravity = Gravity.CENTER
        })
        if (entry.label.isNotBlank()) root.addView(TextView(this).apply {
            text = entry.label
            textSize = 18f
            gravity = Gravity.CENTER
        })

        val snooze = Button(this).apply {
            text = "Отложить ${entry.snoozeDurationMinutes} мин"
            setOnClickListener { snooze(entry) }
        }
        val dismiss = Button(this).apply {
            text = "Выключить"
            setOnClickListener { dismiss(entry) }
        }
        root.addView(snooze)
        root.addView(dismiss)
        setContentView(root)

        startAlarmFeedback(entry)
    }

    private fun startAlarmFeedback(entry: WearAlarmListStore.Entry) {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, uri)?.also {
            it.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            it.play()
        }
        if (entry.vibrationEnabled) {
            vibrator = getSystemService(Vibrator::class.java)
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 400), 0))
        }
    }

    private fun stopFeedback() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
    }

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
        stopFeedback()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_SYNC_ID = "syncId"
    }
}
