package com.wakesync.app.ui.alarmfiring

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.wakesync.app.R
import com.wakesync.app.data.local.entity.AlarmIncidentEvent
import com.wakesync.app.domain.AlarmScheduler
import com.wakesync.app.service.AlarmFireDismissContract
import com.wakesync.app.sync.AlarmSyncCoordinator
import com.wakesync.app.sync.AlarmSyncOperation
import com.wakesync.app.ui.theme.WakeSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Full-screen Activity shown when an alarm fires.
 * Shows on lock screen, turns screen on, handles dismiss/snooze.
 * No challenges — plain ring until dismissed or snoozed.
 */
@AndroidEntryPoint
class AlarmFiringActivity : ComponentActivity() {

    @Inject lateinit var alarmIncidentRepository: com.wakesync.app.data.repository.AlarmIncidentRepository
    @Inject lateinit var alarmSyncCoordinator: AlarmSyncCoordinator

    private val viewModel: com.wakesync.app.ui.alarmfiring.AlarmFiringViewModel by androidx.lifecycle.viewModels()
    private var alarmId: Long = -1
    private var scheduledAt: Long = 0L
    private var fireId: String = ""
    private var flashWakeJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1) ?: -1
        scheduledAt = intent?.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, 0L) ?: 0L
        fireId = intent?.getStringExtra(AlarmScheduler.EXTRA_ALARM_FIRE_ID).orEmpty()
        if (alarmId == -1L) {
            finish()
            return
        }
        if (fireId.isBlank()) {
            fireId = com.wakesync.app.data.local.entity.AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt)
        }
        recordIncidentAsync(
            type = AlarmIncidentEvent.TYPE_ACTIVITY_LAUNCH,
            status = AlarmIncidentEvent.STATUS_RECEIVED,
            reasonCode = "FIRING_ACTIVITY_CREATED"
        )

        // Show on lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        val km = getSystemService(KeyguardManager::class.java)
        km?.requestDismissKeyguard(this, null)

        // Prevent accidental dismiss via back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* Block back press during alarm */ }
        })

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        // Flash-wake if the alarm requests it and gradual volume is configured
        val reduceMotionAndFlashing by viewModel.reduceMotionAndFlashing.collectAsStateWithLifecycle()
        viewModel.uiState.collectAsStateWithLifecycle().collect { state ->
            val alarm = state.alarm
            if (alarm != null && alarm.flashWake && alarm.gradualVolumeSeconds > 0 && flashWakeJob == null) {
                startFlashWake(alarm.gradualVolumeSeconds)
            }
        }

        setContent {
            WakeSyncTheme(reduceMotionAndFlashing = reduceMotionAndFlashing) {
                AlarmFiringScreen(
                    onDismiss = { dismiss() },
                    onSnooze = { snooze() },
                    onSnoozeCustom = { minutes -> snooze(minutes) },
                    onSnoozeUntil = { snoozeAtMillis -> snooze(snoozeAtMillis = snoozeAtMillis) },
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // An overlapping alarm preempted the one on screen: this activity is
        // singleInstance, so the second alarm's launch lands here instead of
        // creating a new instance.
        val incomingAlarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (incomingAlarmId != -1L && incomingAlarmId != alarmId) {
            finish()
            startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    // v1.4.0: Per-alarm hardware-button action. Volume keys intercepted so the
    // user can snooze or dismiss without looking at the screen.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val action = viewModel.uiState.value.alarm?.hardwareButtonAction ?: "NONE"
        if (action == "NONE" || (event?.repeatCount ?: 0) > 0) {
            return super.onKeyDown(keyCode, event)
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_CAMERA -> {
                when (action) {
                    "SNOOZE" -> snooze()
                    "DISMISS" -> {
                        val state = viewModel.uiState.value
                        if (state.canDismiss && state.alarm?.holdToDismissEnabled != true) {
                            dismiss()
                        } else {
                            snooze()
                        }
                    }
                }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun snooze(customMinutes: Int? = null, snoozeAtMillis: Long? = null) {
        lifecycleScope.launch(Dispatchers.IO) {
            alarmSyncCoordinator.notifyWearAction(alarmId, AlarmSyncOperation.SNOOZE)
                .onFailure { android.util.Log.w("AlarmFiringActivity", "Failed to mirror snooze to Wear", it) }
        }
        val intent = AlarmFireDismissContract.snoozeServiceIntent(
            context = this,
            alarmId = alarmId,
            scheduledAt = scheduledAt,
            fireId = fireId,
            customMinutes = customMinutes,
            snoozeAtMillis = snoozeAtMillis
        )
        try {
            startForegroundService(intent)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = when {
                    snoozeAtMillis != null -> "UI_SNOOZE_UNTIL_REQUESTED"
                    customMinutes != null -> "UI_CUSTOM_SNOOZE_REQUESTED"
                    else -> "UI_SNOOZE_REQUESTED"
                }
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmFiringActivity", "startForegroundService(snooze) failed", e)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "UI_SNOOZE_START_FAILED_${e.javaClass.simpleName}"
            )
        }
        finish()
    }

    private fun dismiss() {
        lifecycleScope.launch(Dispatchers.IO) {
            alarmSyncCoordinator.notifyWearAction(alarmId, AlarmSyncOperation.DISMISS)
                .onFailure { android.util.Log.w("AlarmFiringActivity", "Failed to mirror dismiss to Wear", it) }
        }
        val intent = AlarmFireDismissContract.dismissServiceIntent(
            context = this,
            alarmId = alarmId,
            scheduledAt = scheduledAt,
            fireId = fireId,
            challengeRetryCount = 0,
            challengeSolveTimeMs = 0L
        )
        try {
            startForegroundService(intent)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_REQUESTED,
                reasonCode = "UI_DISMISS_REQUESTED"
            )
        } catch (e: Exception) {
            android.util.Log.e("AlarmFiringActivity", "startForegroundService(dismiss) failed", e)
            recordIncidentAsync(
                type = AlarmIncidentEvent.TYPE_USER_ACTION,
                status = AlarmIncidentEvent.STATUS_FAILED,
                reasonCode = "UI_DISMISS_START_FAILED_${e.javaClass.simpleName}"
            )
        }
        finish()
    }

    private fun startFlashWake(durationSeconds: Int) {
        if (flashWakeJob != null) return
        flashWakeJob = lifecycleScope.launch {
            val steps = 50
            val stepDelay = (durationSeconds * 1000L) / steps
            window.attributes = window.attributes.also {
                it.screenBrightness = 0.01f
            }
            for (i in 1..steps) {
                delay(stepDelay)
                val brightness = i.toFloat() / steps
                window.attributes = window.attributes.also {
                    it.screenBrightness = brightness
                }
            }
        }
    }

    override fun onDestroy() {
        flashWakeJob?.cancel()
        super.onDestroy()
    }

    private fun recordIncidentAsync(
        type: String,
        status: String,
        reasonCode: String
    ) {
        if (alarmId <= 0L) return
        alarmIncidentRepository.recordAsync(
            alarmId = alarmId,
            fireId = fireId.ifBlank { com.wakesync.app.data.local.entity.AlarmIncidentEvent.fireIdFor(alarmId, scheduledAt) },
            scheduledAt = scheduledAt,
            type = type,
            status = status,
            reasonCode = reasonCode,
            source = "AlarmFiringActivity"
        )
    }
}
