package com.sysadmindoc.alarmclock.ui.alarmlist

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
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
import com.sysadmindoc.alarmclock.R
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.ui.theme.SurfaceDark
import com.sysadmindoc.alarmclock.ui.theme.SurfaceMedium
import com.sysadmindoc.alarmclock.ui.theme.TextMuted
import com.sysadmindoc.alarmclock.ui.theme.TextPrimary
import com.sysadmindoc.alarmclock.ui.theme.TextSecondary
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
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                top = 24.dp,
                end = 20.dp,
                bottom = 104.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.wakesync_alarms_title),
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            if (next != null) {
                item {
                    NextAlarmCard(next = next, remaining = state.remainingTime)
                }
            } else {
                item {
                    EmptyNextAlarmCard(onAddAlarm)
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 2.dp),
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
                        Text(stringResource(R.string.wakesync_personalization))
                    }
                }
            }

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

            item {
                SleepShortcut(onClick = onOpenBedtime)
            }
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
        colors = CardDefaults.cardColors(containerColor = SurfaceMedium),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.wakesync_next_alarm),
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = formatTime(next),
                color = TextPrimary,
                fontSize = 46.sp,
                fontWeight = FontWeight.Light
            )
            Text(
                text = if (remaining.isNotBlank()) "$dayText · $remaining" else dayText,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
            if (next.label.isNotBlank()) {
                Text(next.label, color = TextSecondary, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun EmptyNextAlarmCard(onAddAlarm: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceMedium),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onAddAlarm)
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Alarm, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(stringResource(R.string.wakesync_no_next_alarm), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.wakesync_tap_to_create), color = TextSecondary)
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
    val secondary = russianRepeatLabel(alarm)

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceMedium),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatTime(alarm),
                    color = if (alarm.isEnabled) TextPrimary else TextMuted,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        secondary,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (alarm.label.isNotBlank()) {
                        Text(" · ${alarm.label}", color = TextMuted, maxLines = 1)
                    }
                }
            }

            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors()
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.wakesync_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wakesync_edit)) },
                        leadingIcon = { Icon(Icons.Default.Edit, null) },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wakesync_duplicate)) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = { menuOpen = false; onDuplicate() }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.wakesync_delete)) },
                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

private fun formatTime(alarm: Alarm): String = "%02d:%02d".format(alarm.hour, alarm.minute)

private fun russianRepeatLabel(alarm: Alarm): String {
    if (alarm.repeatDays.isEmpty()) return "Один раз"
    if (alarm.repeatDays.size == 7) return "Каждый день"
    if (alarm.repeatDays.size == 5 && alarm.repeatDays.all { it.value in 1..5 }) return "По будням"
    if (alarm.repeatDays.size == 2 && alarm.repeatDays.all { it.value in 6..7 }) return "По выходным"
    val names = mapOf(
        1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс"
    )
    return alarm.repeatDays.sortedBy { it.value }.joinToString(" ") { names[it.value] ?: "" }
}

@Composable
private fun EmptyAlarmList(onAddAlarm: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Alarm, null, tint = TextMuted, modifier = Modifier.size(40.dp))
        Text(stringResource(R.string.wakesync_no_alarms), color = TextSecondary, modifier = Modifier.padding(top = 10.dp))
        TextButton(onClick = onAddAlarm) { Text(stringResource(R.string.wakesync_create_alarm)) }
    }
}

@Composable
private fun SleepShortcut(onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceMedium),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bedtime, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(stringResource(R.string.wakesync_sleep_mode), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.wakesync_sleep_mode_subtitle), color = TextSecondary)
            }
        }
    }
}
