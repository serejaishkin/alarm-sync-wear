@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.wakesync.app.ui.alarmfiring

import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateWithSpec
import androidx.compose.animation.core.linear
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakesync.app.R
import com.wakesync.app.ui.theme.AccentRed
import com.wakesync.app.ui.theme.DismissGreen
import com.wakesync.app.ui.theme.SnoozeYellow
import com.wakesync.app.ui.theme.SurfaceCard
import com.wakesync.app.ui.theme.SurfaceDark
import com.wakesync.app.ui.theme.TextMuted
import com.wakesync.app.ui.theme.TextPrimary
import com.wakesync.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val MIN_CUSTOM_SNOOZE_MINUTES = 1
private const val MAX_CUSTOM_SNOOZE_MINUTES = 120

@Composable
fun AlarmFiringScreen(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onSnoozeCustom: (Int) -> Unit = { onSnooze() },
    onSnoozeUntil: (Long) -> Unit = { onSnooze() },
    viewModel: AlarmFiringViewModel = hiltViewModel()
) {
    // Keep the signature compatible with the previous screen while ignoring
    // challenge-related parameters — challenges are no longer shown here.
    val context = LocalContext.current
    val is24Hour = DateFormat.is24HourFormat(context)

    // Live clock
    val currentTime by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1_000)
        }
    }
    val currentDate by produceState(initialValue = LocalDate.now()) {
        while (true) {
            value = LocalDate.now()
            delay(60_000)
        }
    }

    val timePattern = if (is24Hour) "HH:mm" else "h:mm"
    val timeText = currentTime.format(DateTimeFormatter.ofPattern(timePattern, Locale.getDefault()))
    val amPm = if (is24Hour) "" else currentTime.format(DateTimeFormatter.ofPattern("a", Locale.getDefault()))
    val dateText = currentDate.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.FULL))

    // Alarm label from ViewModel if available, else default.
    val alarmLabel by remember { mutableStateOf<String?>(null) }
    // We keep the label empty by default so the screen shows just time + label
    // if the ViewModel provides one; the Activity passes it via intent extras
    // and the ViewModel can surface it later without changing this screen.
    val labelText = alarmLabel?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.notif_alarm_ringing)

    // Auto-silence countdown (seconds). Default 10 minutes = 600.
    val autoSilenceSeconds by remember { mutableIntStateOf(600) }

    // Audio visualiser bars
    val barHeights by remember { mutableStateOf(listOf(0.2f, 0.5f, 0.8f, 0.4f, 0.6f, 0.3f, 0.9f, 0.5f, 0.7f, 0.4f)) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(150)
            val newHeights = barHeights.map { (it + 0.3f + (Math.random() * 0.4f)).coerceIn(0.1f, 1f) }
            barHeights.clear()
            barHeights.addAll(newHeights)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A1E),
                        Color(0xFF2A1A1A),
                        Color(0xFF1A1A1E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top: time
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeText,
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                if (amPm.isNotBlank()) {
                    Text(
                        text = amPm,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = dateText,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Label
            Text(
                text = labelText,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Auto-silence progress line
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { autoSilenceSeconds.coerceIn(0, 600).toFloat() / 600f },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(4.dp),
                    color = SnoozeYellow,
                    trackColor = Color.White.copy(alpha = 0.15f)
                )
                Text(
                    text = stringResource(
                        R.string.firing_auto_silence_in,
                        autoSilenceSeconds / 60,
                        autoSilenceSeconds % 60
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Audio visualiser
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                barHeights.forEachIndexed { index, height ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp * height)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        AccentRed.copy(alpha = 0.6f),
                                        AccentRed
                                    )
                                ),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .clickable { /* no-op */ },
                        contentDescription = null
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Bottom buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSnooze,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5f, SnoozeYellow),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SnoozeYellow
                    )
                ) {
                    Text(
                        text = stringResource(R.string.firing_snooze_10_min),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentRed,
                        disabledContainerColor = AccentRed.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.dismiss_alarm),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hint
            Text(
                text = stringResource(R.string.firing_alarm_is_ringing),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun Spacer(modifier: Modifier) = androidx.compose.foundation.Spacer(modifier = modifier)
