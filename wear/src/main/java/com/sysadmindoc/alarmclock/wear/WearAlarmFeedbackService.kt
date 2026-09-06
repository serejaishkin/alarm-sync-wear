package com.sysadmindoc.alarmclock.wear

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Owns alarm audio/haptics independently from the firing Activity.
 * The Activity is only the UI; killing/swiping it away must not stop an alarm.
 */
class WearAlarmFeedbackService : Service() {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var syncId: String = ""

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        vibrator = getSystemService(Vibrator::class.java)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopFeedback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                syncId = intent.getStringExtra(EXTRA_SYNC_ID).orEmpty()
                val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
                startForeground(NOTIFICATION_ID, buildNotification(label))
                startFeedback()
                // Samsung Wear OS may keep a full-screen notification behind
                // the lock screen. Launch the same alarm UI directly from
                // this foreground service as a second path.
                val firingIntent = Intent(this, WearAlarmFiringActivity::class.java).apply {
                    putExtra(WearAlarmFiringActivity.EXTRA_SYNC_ID, syncId)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                }
                runCatching { startActivity(firingIntent) }
                    .onFailure { Log.w(TAG, "Unable to open firing activity from feedback service", it) }
            }
        }
        return START_NOT_STICKY
    }

    private fun startFeedback() {
        ringtone?.stop()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(this, uri)?.also {
            it.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            it.play()
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 400), 0))
    }

    private fun stopFeedback() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
    }

    private fun buildNotification(label: String): Notification {
        val fullScreenIntent = Intent(this, WearAlarmFiringActivity::class.java).apply {
            putExtra(WearAlarmFiringActivity.EXTRA_SYNC_ID, syncId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            syncId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Будильник")
            .setContentText(label.ifBlank { "Время вставать" })
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Будильник", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(null, null)
                    enableVibration(false)
                    description = "Служебное уведомление активного будильника"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
    }

    override fun onDestroy() {
        stopFeedback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "alarm_firing"
        private const val NOTIFICATION_ID = 710001
        const val ACTION_START = "com.sysadmindoc.alarmclock.wear.START_FEEDBACK"
        const val ACTION_STOP = "com.sysadmindoc.alarmclock.wear.STOP_FEEDBACK"
        const val EXTRA_SYNC_ID = "syncId"
        const val EXTRA_LABEL = "label"
        private const val TAG = "WakeSyncWatch"

        fun start(context: android.content.Context, syncId: String, label: String) {
            val intent = Intent(context, WearAlarmFeedbackService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SYNC_ID, syncId)
                putExtra(EXTRA_LABEL, label)
            }
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: android.content.Context) {
            context.startService(Intent(context, WearAlarmFeedbackService::class.java).apply { action = ACTION_STOP })
        }
    }
}
