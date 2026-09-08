package com.wakesync.app.ui.alarmlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakesync.app.R
import com.wakesync.app.data.model.Alarm
import com.wakesync.app.ui.theme.BluePrimary
import com.wakesync.app.ui.theme.BorderSubtle
import com.wakesync.app.ui.theme.DismissGreen
import com.wakesync.app.ui.theme.SurfaceCard
import com.wakesync.app.ui.theme.SurfaceDark
import com.wakesync.app.ui.theme.SurfaceLight
import com.wakesync.app.ui.theme.SurfaceMedium
import com.wakesync.app.ui.theme.TextMuted
import com.wakesync.app.ui.theme.TextPrimary
import com.wakesync.app.ui.theme.TextSecondary
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun GoogleStyleAlarmScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenBedtime: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AlarmListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val next = state.nextAlarm

    Scaffold(
        containerColor = SurfaceDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddAlarm,
                containerColor = BluePrimary,
                contentColor = SurfaceDark,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.wakesync_add_alarm))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 104.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // WakeSync Header with Status Bar
            item {
                WakeSyncHeader()
            }

            // Next Alarm Hero Card (matches web NextAlarmCard)
            if (next != null) {
                item {
                    NextAlarmCard(next = next, remaining = state.remainingTime)
                }
            } else {
                item {
                    EmptyNextAlarmCard(onAddAlarm)
                }
            }

            // Alarms Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.wakesync_your_alarms),
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.wakesync_personalization), color = BluePrimary)
                    }
                }
            }

            // Alarms List
            if (state.alarms.isEmpty()) {
                item {
                    EmptyAlarmList(onAddAlarm)
                }
            } else {
                items(state.alarms, key = { it.id }) { alarm ->
                    GoogleAlarmRow(
                        alarm = alarm,
                        onToggle = { viewModel.toggleAlarm(alarm) },
                        onEdit = { onEditAlarm(alarm.id) },
                        onDelete = { viewModel.deleteAlarm(alarm) },
                        onDuplicate = { viewModel.duplicateAlarm(alarm) }
                    )
                }
            }

            // Sleep Mode Shortcut
            item {
                SleepShortcut(onClick = onOpenBedtime)
            }
        }
    }
}

@Composable
private fun WakeSyncHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WakeSync",
                color = TextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )

            // Live Wear OS Sync Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceMedium)
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(DismissGreen)
                )
                Text(
                    text = stringResource(R.string.wakesync_sync_active),
                    color = DismissGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.PhoneAndroid,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = stringResource(R.string.wakesync_device_pair),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun NextAlarmCard(next: Alarm, remaining: String) {
    val triggerDate = if (next.nextTriggerTime > 0) {
        Instant.ofEpochMilli(next.nextTriggerTime).atZone(ZoneId.systemDefault()).toLocalDate()
    } else null
    val today = java.time.LocalDate.now()
    val dayText = when (triggerDate) {
        today -> stringResource(R.string.wakesync_today)
        today.plusDays(1) -> stringResource(R.string.wakesync_tomorrow)
        else -> triggerDate?.format(DateTimeFormatter.ofPattern("d MMM")) ?: next.repeatLabel
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Alarm,
                        contentDescription = null,
                        tint = BluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        stringResource(R.string.wakesync_next_alarm).uppercase(),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (remaining.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SurfaceLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = remaining,
                            color = BluePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = formatTime(next),
                color = TextPrimary,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dayText,
                    color = BluePrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (next.label.isNotBlank()) {
                    Text(
                        text = "·  ${next.label}",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Watch Sync Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = DismissGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.wakesync_synced_badge),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyNextAlarmCard(onAddAlarm: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddAlarm)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Alarm,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(24.dp)
            )
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    stringResource(R.string.wakesync_no_next_alarm),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.wakesync_tap_to_create),
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun GoogleAlarmRow(
    alarm: Alarm,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Row 1: Time and Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(alarm),
                    color = if (alarm.isEnabled) TextPrimary else TextMuted,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp
                )

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BluePrimary,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SurfaceLight,
                        uncheckedBorderColor = BorderSubtle
                    )
                )
            }

            // Row 2: Days of the week chips (matches web PhoneSimulator)
            Spacer(Modifier.height(8.dp))
            WeekdayChipsRow(alarm = alarm)

            // Row 3: Label, Sync Badge, and Actions
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (alarm.isEnabled) BluePrimary else TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (alarm.label.isNotBlank()) alarm.label else "Будильник",
                        color = if (alarm.isEnabled) TextSecondary else TextMuted,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }

                // Quick Action Icons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.wakesync_edit),
                            tint = TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onDuplicate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.wakesync_duplicate),
                            tint = TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.wakesync_delete),
                            tint = TextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayChipsRow(alarm: Alarm) {
    val dayNames = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    if (alarm.repeatDays.isEmpty()) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLight.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Один раз",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            dayNames.forEachIndexed { index, dayName ->
                val dayOfWeek = DayOfWeek.of(index + 1)
                val isActive = alarm.repeatDays.contains(dayOfWeek)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isActive && alarm.isEnabled) BluePrimary
                            else if (isActive) BluePrimary.copy(alpha = 0.35f)
                            else SurfaceLight.copy(alpha = 0.4f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        color = if (isActive && alarm.isEnabled) SurfaceDark else if (isActive) TextPrimary else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

private fun formatTime(alarm: Alarm): String = "%02d:%02d".format(alarm.hour, alarm.minute)

@Composable
private fun EmptyAlarmList(onAddAlarm: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Alarm,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(44.dp)
        )
        Text(
            stringResource(R.string.wakesync_no_alarms),
            color = TextSecondary,
            modifier = Modifier.padding(top = 10.dp),
            fontSize = 15.sp
        )
        TextButton(onClick = onAddAlarm) {
            Text(stringResource(R.string.wakesync_create_alarm), color = BluePrimary)
        }
    }
}

@Composable
private fun SleepShortcut(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceMedium),
        border = BorderStroke(1.dp, BorderSubtle),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Bedtime,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                Modifier
                    .padding(start = 14.dp)
                    .weight(1f)
            ) {
                Text(
                    stringResource(R.string.wakesync_sleep_mode),
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.wakesync_sleep_mode_subtitle),
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
