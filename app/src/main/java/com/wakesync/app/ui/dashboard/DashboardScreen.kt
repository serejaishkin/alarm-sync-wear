package com.wakesync.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakesync.app.R
import com.wakesync.app.data.repository.CalendarEvent
import com.wakesync.app.ui.components.AlarmClockHeroHeader
import com.wakesync.app.ui.components.AppEmptyState
import com.wakesync.app.ui.components.AppStatusChip
import com.wakesync.app.ui.components.AppSurfaceCard
import com.wakesync.app.ui.theme.DismissGreen
import com.wakesync.app.ui.theme.SnoozeYellow
import com.wakesync.app.ui.theme.TextMuted
import com.wakesync.app.ui.theme.TextPrimary
import com.wakesync.app.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    onOpenAlarms: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        AlarmClockHeroHeader(
            transparent = false,
            title = stringResource(R.string.dashboard_today),
            subtitle = state.todayDate
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.showCalendar) {
                CalendarSection(state)
            } else {
                AppSurfaceCard {
                    AppEmptyState(
                        icon = Icons.Default.Schedule,
                        title = stringResource(R.string.dashboard_today_quiet),
                        description = stringResource(R.string.dashboard_day_clear)
                    )
                }
            }

            if (state.nextAlarmTime.isNotBlank()) {
                NextAlarmSection(
                    state = state,
                    onOpenAlarms = onOpenAlarms
                )
            }
        }
    }
}

@Composable
private fun NextAlarmSection(
    state: DashboardUiState,
    onOpenAlarms: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionTitle(title = stringResource(R.string.dashboard_next_alarm))
        AppSurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAlarms),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = state.nextAlarmTime,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = listOf(
                            state.nextAlarmLabel,
                            state.nextAlarmSchedule
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1
                    )
                }
                Text(
                    text = stringResource(R.string.dashboard_view),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun AppSectionTitle(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun CalendarSection(state: DashboardUiState) {
    AppSurfaceCard {
        Text(
            stringResource(R.string.dashboard_schedule),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        when {
            state.calendarPermissionNeeded -> {
                CompactDashboardRow(
                    icon = Icons.Default.CalendarMonth,
                    title = stringResource(R.string.dashboard_calendar_access),
                    description = stringResource(R.string.dashboard_calendar_access_desc),
                    accent = SnoozeYellow
                )
            }

            state.calendarEvents.isEmpty() -> {
                CompactDashboardRow(
                    icon = Icons.Default.EventAvailable,
                    title = stringResource(R.string.dashboard_nothing_scheduled),
                    description = stringResource(R.string.dashboard_day_clear),
                    accent = DismissGreen
                )
            }

            else -> {
                state.calendarEvents.forEachIndexed { index, event ->
                    EventRow(event)
                    if (index != state.calendarEvents.lastIndex) {
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.16f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDashboardRow(
    icon: ImageVector,
    title: String,
    description: String,
    accent: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(24.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(description, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 52.dp)
                .background(
                    color = if (event.calendarColor != 0) Color(event.calendarColor) else MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp)
                )
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = event.timeRange,
                icon = Icons.Default.Schedule,
                color = if (event.calendarColor != 0) Color(event.calendarColor) else MaterialTheme.colorScheme.primary
            )
            Text(
                text = event.title,
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            if (event.location.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = event.location,
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}