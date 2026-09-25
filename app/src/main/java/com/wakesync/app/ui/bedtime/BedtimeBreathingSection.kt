package com.wakesync.app.ui.bedtime

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wakesync.app.R
import com.wakesync.app.domain.BreathingPattern
import com.wakesync.app.domain.BreathingPhaseKind
import com.wakesync.app.ui.components.AppFilterChip
import com.wakesync.app.ui.components.AppSectionTitle
import com.wakesync.app.ui.components.AppStatusChip
import com.wakesync.app.ui.components.AppSurfaceCard
import com.wakesync.app.ui.theme.DismissGreen
import com.wakesync.app.ui.theme.SnoozeYellow
import com.wakesync.app.ui.theme.SurfaceCard
import com.wakesync.app.ui.theme.TextMuted
import com.wakesync.app.ui.theme.TextPrimary
import com.wakesync.app.ui.theme.TextSecondary

@StringRes
private fun breathingPhaseLabelRes(kind: BreathingPhaseKind): Int = when (kind) {
    BreathingPhaseKind.INHALE -> R.string.bedtime_breathing_inhale
    BreathingPhaseKind.HOLD -> R.string.bedtime_breathing_hold
    BreathingPhaseKind.SETTLE -> R.string.bedtime_breathing_hold
    BreathingPhaseKind.EXHALE -> R.string.bedtime_breathing_exhale
    BreathingPhaseKind.COMPLETE -> R.string.bedtime_breathing_complete
}

@StringRes
private fun breathingPhaseCueRes(kind: BreathingPhaseKind): Int = when (kind) {
    BreathingPhaseKind.INHALE -> R.string.bedtime_breathing_cue_inhale
    BreathingPhaseKind.HOLD -> R.string.bedtime_breathing_cue_hold
    BreathingPhaseKind.SETTLE -> R.string.bedtime_breathing_cue_hold_soft
    BreathingPhaseKind.EXHALE -> R.string.bedtime_breathing_cue_exhale
    BreathingPhaseKind.COMPLETE -> R.string.bedtime_breathing_cue_done
}

@Composable
private fun breathingPatternLabel(option: BreathingPattern): String = when (option) {
    BreathingPattern.FOUR_SEVEN_EIGHT -> "4-7-8"
    BreathingPattern.BOX -> stringResource(R.string.bedtime_breathing_pattern_box)
}

@Composable
internal fun BreathingExerciseSection(
    pattern: BreathingPattern,
    elapsedSeconds: Int,
    running: Boolean,
    onPatternSelected: (BreathingPattern) -> Unit,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = pattern.phaseAt(elapsedSeconds)
    val remainingSessionSeconds = (pattern.totalSeconds - elapsedSeconds).coerceAtLeast(0)

    AppSurfaceCard(
        modifier = modifier,
        highlighted = running
    ) {
        AppSectionTitle(
            title = stringResource(R.string.bedtime_breathing_title),
            description = if (running) {
                stringResource(R.string.bedtime_breathing_desc_running)
            } else {
                stringResource(R.string.bedtime_breathing_desc_idle)
            }
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BreathingPattern.entries.forEach { option ->
                AppFilterChip(
                    label = breathingPatternLabel(option),
                    selected = option == pattern,
                    onClick = { onPatternSelected(option) },
                    selectionSemantics = true
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = SurfaceCard.copy(alpha = 0.72f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppStatusChip(
                    label = stringResource(R.string.bedtime_breathing_cycle, phase.cycleNumber, phase.cycleCount),
                    icon = Icons.Default.Schedule,
                    color = if (phase.completed) DismissGreen else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(breathingPhaseLabelRes(phase.kind)),
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = if (phase.completed) {
                        stringResource(R.string.bedtime_breathing_done)
                    } else {
                        "${phase.remainingSeconds}"
                    },
                    color = if (phase.completed) DismissGreen else SnoozeYellow,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(breathingPhaseCueRes(phase.kind)),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                val remainingLabel = if (remainingSessionSeconds >= 60) {
                    val m = remainingSessionSeconds / 60
                    val s = remainingSessionSeconds % 60
                    "${m}:${s.toString().padStart(2, '0')}"
                } else {
                    stringResource(R.string.bedtime_seconds_short, remainingSessionSeconds)
                }
                Text(
                    text = stringResource(R.string.bedtime_breathing_left, remainingLabel),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppFilterChip(
                label = when {
                    running -> stringResource(R.string.bedtime_breathing_pause)
                    phase.completed -> stringResource(R.string.bedtime_breathing_restart)
                    else -> stringResource(R.string.bedtime_breathing_start)
                },
                selected = running,
                onClick = onToggleRunning,
                selectionSemantics = false,
                accessibilityLabel = if (running) {
                    stringResource(R.string.bedtime_breathing_accessibility_pause)
                } else {
                    stringResource(R.string.bedtime_breathing_accessibility_start)
                }
            )
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.bedtime_breathing_reset), color = TextSecondary)
            }
        }
    }
}
