package com.wakesync.app.ui.alarmlist

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakesync.app.R
import com.wakesync.app.data.model.Alarm
import com.wakesync.app.data.model.ShiftPattern
import com.wakesync.app.data.share.AlarmShareCodec
import com.wakesync.app.ui.adaptive.shouldUseTwoPaneLayout
import com.wakesync.app.ui.alarmlist.components.SwipeableAlarmCard
import com.wakesync.app.ui.components.AlarmClockHeroHeader
import com.wakesync.app.ui.components.AppEmptyState
import com.wakesync.app.ui.components.AppFilterChip
import com.wakesync.app.ui.components.AppInlineNotice
import com.wakesync.app.ui.components.AppSectionTitle
import com.wakesync.app.ui.components.AppStatusChip
import com.wakesync.app.ui.components.AppSurfaceCard
import com.wakesync.app.ui.components.AppInputShape
import com.wakesync.app.ui.components.appOutlinedTextFieldColors
import com.wakesync.app.ui.components.appSwitchColors
import com.wakesync.app.ui.templates.TemplatePickerSheet
import com.wakesync.app.ui.theme.AccentRed
import com.wakesync.app.ui.theme.BorderSubtle
import com.wakesync.app.ui.theme.ClockTimeSmall
import com.wakesync.app.ui.theme.DismissGreen
import com.wakesync.app.ui.theme.LocalAppShapeTokens
import com.wakesync.app.ui.theme.SnoozeYellow
import com.wakesync.app.ui.theme.SurfaceCard
import com.wakesync.app.ui.theme.SurfaceDark
import com.wakesync.app.ui.theme.SurfaceMedium
import com.wakesync.app.ui.theme.TextMuted
import com.wakesync.app.ui.theme.TextPrimary
import com.wakesync.app.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    onAddAlarm: () -> Unit,
    onEditAlarm: (Long) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: AlarmListViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showTemplates by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showBulkDeleteConfirmation by remember { mutableStateOf(false) }

    var statsAlarmLabel by remember { mutableStateOf<String?>(null) }
    val alarmStats by viewModel.alarmStats.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.syncWithWatch()
            delay(2_000L)
        }
    }
    if (statsAlarmLabel != null && alarmStats != null) {
        val stats = alarmStats!!
        AlertDialog(
            onDismissRequest = { statsAlarmLabel = null; viewModel.clearAlarmStats() },
            confirmButton = {
                TextButton(onClick = { statsAlarmLabel = null; viewModel.clearAlarmStats() }) {
                    Text(stringResource(R.string.alarm_stats_close))
                }
            },
            title = { Text(statsAlarmLabel ?: stringResource(R.string.alarm_history)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.alarm_stats_last_days), color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    if (stats.fireCount == 0) {
                        // A brand-new (or recently-cleared) alarm has nothing to
                        // report yet — frame it rather than dumping all-zero stats.
                        Text(
                            stringResource(R.string.alarm_stats_never_fired),
                            color = TextSecondary
                        )
                    } else {
                        Text(stringResource(R.plurals.alarm_stats_fired, stats.fireCount, stats.fireCount))
                        Text(stringResource(R.string.alarm_stats_avg_snoozes, String.format("%.1f", stats.avgSnoozesPerFire)))
                        Text(stringResource(R.string.alarm_stats_avg_dismiss, stats.avgDismissTimeSec.toString()))
                        if (stats.missedCount > 0) {
                            Text(stringResource(R.plurals.alarm_stats_missed, stats.missedCount, stats.missedCount), color = AccentRed)
                        }
                    }
                }
            }
        )
    }

    if (showTemplates) {
        TemplatePickerSheet(
            onSelect = { template ->
                viewModel.createFromTemplate(template)
                showTemplates = false
            },
            onDismiss = { showTemplates = false }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = androidx.compose.runtime.rememberCoroutineScope()
    val listState = rememberLazyListState()
    var draggingAlarmId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var selectedAlarmId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        viewModel.feedbackEvents.collect { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(state.undoAlarm) {
        state.undoAlarm?.let {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.alarm_deleted),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            } else {
                viewModel.confirmDelete()
            }
        }
    }

    val filteredAlarms = remember(state.alarms, searchQuery, state.selectedGroup) {
        state.alarms
            .filter { alarm ->
                state.selectedGroup == null || alarm.group == state.selectedGroup
            }
            .filter { alarm ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    alarm.label.contains(searchQuery, ignoreCase = true) ||
                        alarm.repeatLabel.contains(searchQuery, ignoreCase = true) ||
                        alarm.group.contains(searchQuery, ignoreCase = true)
                }
            }
    }
    val visibleAlarmIds = filteredAlarms.map { it.id }
    val currentVisibleAlarmIds by rememberUpdatedState(visibleAlarmIds)
    val canReorderAlarms = !state.isSelectionMode && filteredAlarms.size > 1

    if (showBulkDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = AccentRed
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBulkDeleteConfirmation = false
                        viewModel.deleteSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        stringResource(R.plurals.alarm_bulk_delete_confirm, state.selectedIds.size, state.selectedIds.size)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            },
            title = {
                Text(
                    text = stringResource(R.plurals.alarm_bulk_delete_title, state.selectedIds.size, state.selectedIds.size),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = if (state.selectedIds.size == filteredAlarms.size && filteredAlarms.isNotEmpty()) {
                        stringResource(R.string.alarm_bulk_delete_all_body)
                    } else {
                        stringResource(R.string.alarm_bulk_delete_selected_body)
                    },
                    color = TextSecondary
                )
            },
            containerColor = SurfaceMedium,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        containerColor = SurfaceDark,
        // v1.7.1: Skip the inner Scaffold's default system insets — the outer
        // AppNavigation Scaffold already paddings NavHost with the bottom-nav
        // inset. Without this, both scaffolds compete for the same insets and
        // the alarm list stops well short of the floating nav.
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedVisibility(visible = state.isSelectionMode) {
                SelectionActionBar(
                    selectedCount = state.selectedIds.size,
                    totalCount = filteredAlarms.size,
                    onSelectAll = { viewModel.selectMany(filteredAlarms.map { it.id }.toSet()) },
                    onClearSelection = viewModel::clearSelection,
                    onDeleteSelected = { showBulkDeleteConfirmation = true },
                    onEnableSelected = viewModel::enableSelected,
                    onDisableSelected = viewModel::disableSelected
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val useTwoPane = !state.isSelectionMode && shouldUseTwoPaneLayout(maxWidth.value)
                val selectedAlarm = filteredAlarms.firstOrNull { it.id == selectedAlarmId }

                LaunchedEffect(useTwoPane, filteredAlarms) {
                    if (!useTwoPane || filteredAlarms.isEmpty()) {
                        selectedAlarmId = null
                    } else if (filteredAlarms.none { it.id == selectedAlarmId }) {
                        selectedAlarmId = filteredAlarms.first().id
                    }
                }

                val alarmListContent: LazyListScope.() -> Unit = {
                item {
                    AlarmHeader(
                        remainingTime = state.remainingTime,
                        hasAlarms = state.nextAlarm != null,
                        alarmCount = state.alarms.size,
                        vacationActive = state.vacationActive,
                        sortLabel = when (state.sortOrder) {
                            AlarmSortOrder.TIME -> stringResource(R.string.alarm_sort_time)
                            AlarmSortOrder.MANUAL -> stringResource(R.string.alarm_sort_manual)
                            AlarmSortOrder.CREATED -> stringResource(R.string.alarm_sort_newest)
                            AlarmSortOrder.ENABLED_FIRST -> stringResource(R.string.alarm_sort_enabled)
                        },
                        onCycleSort = viewModel::cycleSortOrder,
                        onSync = viewModel::syncWithWatch,
                    )
                }

                if (state.groups.any { it.isNotBlank() } || state.alarms.size > 3) {
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (state.groups.any { it.isNotBlank() }) {
                                GroupFilterRow(
                                    title = stringResource(R.string.groups),
                                    groups = state.groups.filter { it.isNotBlank() },
                                    selectedGroup = state.selectedGroup,
                                    onSelectGroup = viewModel::selectGroup
                                )
                            }

                            if (state.profiles.any { it.isNotBlank() }) {
                                GroupFilterRow(
                                    title = stringResource(R.string.profiles),
                                    groups = state.profiles.filter { it.isNotBlank() },
                                    selectedGroup = state.selectedProfile,
                                    onSelectGroup = viewModel::selectProfile
                                )
                            }

                            if (state.alarms.size > 3) {
                                AppSurfaceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text(stringResource(R.string.alarm_search_hint)) },
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                                        trailingIcon = {
                                            if (searchQuery.isNotBlank()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(Icons.Default.Clear, stringResource(R.string.clear_search), tint = TextMuted)
                                                }
                                            }
                                        },
                                        colors = appOutlinedTextFieldColors(),
                                        shape = AppInputShape,
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                when {
                    state.alarms.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                    AppEmptyState(
                                        icon = Icons.Default.AlarmAdd,
                                        title = stringResource(R.string.no_alarms_title),
                                        description = stringResource(R.string.no_alarms_desc),
                                        footer = {
                                            AlarmListEmptyActions(
                                                onAddAlarm = onAddAlarm,
                                                onBrowseTemplates = { showTemplates = true }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    filteredAlarms.isEmpty() -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(320.dp)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AppSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                                    AppEmptyState(
                                        icon = Icons.Default.Search,
                                        title = stringResource(R.string.search_no_match_title),
                                        description = stringResource(R.string.search_no_match_desc),
                                        footer = {
                                            TextButton(
                                                onClick = {
                                                    searchQuery = ""
                                                    viewModel.selectGroup(null)
                                                    viewModel.selectProfile(null)
                                                }
                                            ) {
                                                Text(stringResource(R.string.clear_filters), color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    else -> {
                        val duplicateExtras = state.duplicateExtras
                        if (duplicateExtras.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AppInlineNotice(
                                        title = stringResource(R.plurals.duplicate_alarms, duplicateExtras.size, duplicateExtras.size),
                                        message = stringResource(R.string.duplicate_alarms_msg),
                                        icon = Icons.Default.ContentCopy,
                                        color = AccentRed
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = viewModel::removeDuplicateExtras) {
                                            Text(
                                                stringResource(R.plurals.remove_duplicates, duplicateExtras.size, duplicateExtras.size),
                                                color = AccentRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        val conflictTimes = filteredAlarms
                            .filter { it.isEnabled }
                            .groupBy { it.hour * 60 + it.minute }
                            .filterValues { it.size > 1 }
                            .keys
                        if (conflictTimes.isNotEmpty()) {
                            item {
                                val timeLabels = conflictTimes.joinToString(", ") { totalMin ->
                                    val h = totalMin / 60
                                    val m = totalMin % 60
                                    if (state.is24HourFormat) "%02d:%02d".format(h, m)
                                    else "%d:%02d %s".format(if (h % 12 == 0) 12 else h % 12, m, if (h < 12) "AM" else "PM")
                                }
                                AppInlineNotice(
                                    title = stringResource(R.string.conflict_time_title),
                                    message = stringResource(R.string.conflict_time_msg, timeLabels),
                                    icon = Icons.Default.Warning,
                                    color = SnoozeYellow,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }
                        }
                        items(filteredAlarms, key = { it.id }) { alarm ->
                            val isDragging = draggingAlarmId == alarm.id
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .graphicsLayer {
                                        translationY = if (isDragging) dragOffsetPx else 0f
                                        alpha = if (isDragging) 0.94f else 1f
                                        scaleX = if (isDragging) 1.01f else 1f
                                        scaleY = if (isDragging) 1.01f else 1f
                                    }
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .animateItem(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canReorderAlarms) {
                                    AlarmReorderHandle(
                                        enabled = canReorderAlarms,
                                        alarmLabel = alarm.label.ifBlank { formatAlarmTime(alarm, state.is24HourFormat) },
                                        // v1.13.15: TalkBack-reachable reorder — same persistence path as drag.
                                        onMoveUp = {
                                            val ids = currentVisibleAlarmIds
                                            val index = ids.indexOf(alarm.id)
                                            if (index > 0) {
                                                viewModel.moveAlarm(
                                                    movedAlarmId = alarm.id,
                                                    targetAlarmId = ids[index - 1],
                                                    visibleAlarmIds = ids
                                                )
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                        onMoveDown = {
                                            val ids = currentVisibleAlarmIds
                                            val index = ids.indexOf(alarm.id)
                                            if (index in 0 until ids.lastIndex) {
                                                viewModel.moveAlarm(
                                                    movedAlarmId = alarm.id,
                                                    targetAlarmId = ids[index + 1],
                                                    visibleAlarmIds = ids
                                                )
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                        modifier = Modifier.pointerInput(canReorderAlarms, alarm.id) {
                                            if (canReorderAlarms) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggingAlarmId = alarm.id
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggingAlarmId = null
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggingAlarmId = null
                                                        dragOffsetPx = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetPx += dragAmount.y
                                                        val draggedInfo = listState.layoutInfo.visibleItemsInfo
                                                            .firstOrNull { it.key == alarm.id }
                                                        val targetInfo = draggedInfo?.let { dragged ->
                                                            val draggedCenter = dragged.offset + (dragged.size / 2) + dragOffsetPx
                                                            listState.layoutInfo.visibleItemsInfo.firstOrNull { candidate ->
                                                                candidate.key is Long &&
                                                                    candidate.key != alarm.id &&
                                                                    draggedCenter >= candidate.offset &&
                                                                    draggedCenter <= candidate.offset + candidate.size
                                                            }
                                                        }
                                                        val targetAlarmId = targetInfo?.key as? Long
                                                        if (targetAlarmId != null) {
                                                            viewModel.moveAlarm(
                                                                movedAlarmId = alarm.id,
                                                                targetAlarmId = targetAlarmId,
                                                                visibleAlarmIds = currentVisibleAlarmIds
                                                            )
                                                            dragOffsetPx = 0f
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                val isSelected = alarm.id in state.selectedIds
                                if (state.isSelectionMode) {
                                    SelectableAlarmCard(
                                        alarm = alarm,
                                        is24Hour = state.is24HourFormat,
                                        isSelected = isSelected,
                                        onToggleSelect = { viewModel.toggleSelection(alarm.id) }
                                    )
                                } else {
                                    SwipeableAlarmCard(
                                        onDelete = { viewModel.deleteAlarm(alarm) }
                                    ) {
                                        // v1.5.2: Surface vacation suppression per-card.
                                        val suppressedByVacation = alarm.isEnabled &&
                                            state.vacationStartMillis > 0L &&
                                            state.vacationEndMillis > state.vacationStartMillis &&
                                            alarm.nextTriggerTime in
                                                state.vacationStartMillis..state.vacationEndMillis
                                        AlarmCard(
                                            alarm = alarm,
                                            is24Hour = state.is24HourFormat,
                                            suppressedByVacation = suppressedByVacation,
                                            isActivePaneSelection = useTwoPane && selectedAlarmId == alarm.id,
                                            lastChanged = state.syncChanges[alarm.id],
                                            onToggle = { viewModel.toggleAlarm(alarm) },
                                            onForceToggle = { viewModel.forceDisableAlarm(alarm) },
                                            onClick = {
                                                if (useTwoPane) {
                                                    selectedAlarmId = alarm.id
                                                } else {
                                                    onEditAlarm(alarm.id)
                                                }
                                            },
                                            onDelete = { viewModel.deleteAlarm(alarm) },
                                            onSkipNext = { viewModel.skipNextOccurrence(alarm) },
                                            onDuplicate = { viewModel.duplicateAlarm(alarm) },
                                            onShare = { shareAlarm(context, alarm, state.is24HourFormat) },
                                            onShowHistory = {
                                                statsAlarmLabel = alarm.label.ifBlank { "%d:%02d".format(alarm.hour, alarm.minute) }
                                                viewModel.loadAlarmStats(alarm.id)
                                            },
                                            onLongClick = { viewModel.toggleSelection(alarm.id) }
                                        )
                                    }
                                }
                                }
                            }
                        }
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showTemplates = true },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.templates))
                                }
                                Button(
                                    onClick = onAddAlarm,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.new_alarm))
                                }
                            }
                        }
                    }
                }

                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        QuickAlarmRow(
                            onQuickAlarm = viewModel::createQuickAlarm,
                            napDefaultMinutes = state.napDefaultMinutes
                        )
                    }
                }
                }

                val alarmListPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                if (useTwoPane) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .widthIn(min = 360.dp, max = 520.dp)
                                .fillMaxHeight(),
                            contentPadding = alarmListPadding,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            content = alarmListContent
                        )
                        AlarmDetailPane(
                            alarm = selectedAlarm,
                            is24Hour = state.is24HourFormat,
                            suppressedByVacation = selectedAlarm?.let { alarm ->
                                alarm.isEnabled &&
                                    state.vacationStartMillis > 0L &&
                                    state.vacationEndMillis > state.vacationStartMillis &&
                                    alarm.nextTriggerTime in state.vacationStartMillis..state.vacationEndMillis
                            } == true,
                            onEdit = { alarm -> onEditAlarm(alarm.id) },
                            onToggle = { alarm -> viewModel.toggleAlarm(alarm) },
                            onForceToggle = { alarm -> viewModel.forceDisableAlarm(alarm) },
                            onDelete = { alarm -> viewModel.deleteAlarm(alarm) },
                            onSkipNext = { alarm -> viewModel.skipNextOccurrence(alarm) },
                            onDuplicate = { alarm -> viewModel.duplicateAlarm(alarm) },
                            onShare = { alarm -> shareAlarm(context, alarm, state.is24HourFormat) },
                            onShowHistory = { alarm ->
                                statsAlarmLabel = alarm.label.ifBlank { "%d:%02d".format(alarm.hour, alarm.minute) }
                                viewModel.loadAlarmStats(alarm.id)
                            },
                            onAddAlarm = onAddAlarm,
                            onBrowseTemplates = { showTemplates = true },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(end = 18.dp, top = 12.dp, bottom = 24.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = alarmListPadding,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = alarmListContent
                    )
                }
        }
    }
}
}

@Composable
private fun AlarmListEmptyActions(
    onAddAlarm: () -> Unit,
    onBrowseTemplates: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 360.dp
        if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onAddAlarm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.create_alarm))
                }
                OutlinedButton(
                    onClick = onBrowseTemplates,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.browse_templates))
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onAddAlarm,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.create_alarm))
                }
                OutlinedButton(
                    onClick = onBrowseTemplates,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.browse_templates))
                }
            }
        }
    }
}

@Composable
private fun AlarmDetailPane(
    alarm: Alarm?,
    is24Hour: Boolean,
    suppressedByVacation: Boolean,
    onEdit: (Alarm) -> Unit,
    onToggle: (Alarm) -> Unit,
    onForceToggle: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onSkipNext: (Alarm) -> Unit,
    onDuplicate: (Alarm) -> Unit,
    onShare: (Alarm) -> Unit,
    onShowHistory: (Alarm) -> Unit,
    onAddAlarm: () -> Unit,
    onBrowseTemplates: () -> Unit,
    modifier: Modifier = Modifier
) {
    val detailPaneCd = if (alarm == null) {
        stringResource(R.string.cd_alarm_detail_pane)
    } else {
        stringResource(R.string.cd_alarm_detail_pane_for, alarm.label.ifBlank { formatAlarmTime(alarm, is24Hour) })
    }
    AppSurfaceCard(
        modifier = modifier.semantics {
            contentDescription = detailPaneCd
        },
        highlighted = alarm?.isEnabled == true
    ) {
        if (alarm == null) {
            AppEmptyState(
                icon = Icons.Default.AlarmAdd,
                title = stringResource(R.string.select_alarm_title),
                description = stringResource(R.string.select_alarm_desc),
                footer = {
                    AlarmListEmptyActions(
                        onAddAlarm = onAddAlarm,
                        onBrowseTemplates = onBrowseTemplates
                    )
                }
            )
            return@AppSurfaceCard
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AppSectionTitle(
                title = alarm.label.ifBlank { stringResource(R.string.alarm_details) },
                description = if (suppressedByVacation) {
                    stringResource(R.string.vacation_paused_detail)
                } else {
                    nextOccurrenceLabel(alarm, is24Hour, LocalContext.current)
                },
                action = {
                    AppStatusChip(
                        label = if (alarm.isEnabled) stringResource(R.string.status_enabled) else stringResource(R.string.stopwatch_status_paused),
                        icon = if (alarm.isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        color = if (alarm.isEnabled) DismissGreen else TextMuted
                    )
                }
            )

            Text(
                text = formatAlarmTime(alarm, is24Hour),
                color = if (alarm.isEnabled) TextPrimary else TextMuted,
                style = ClockTimeSmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (suppressedByVacation) {
                    AppStatusChip(
                        label = stringResource(R.string.vacation_paused_chip),
                        icon = Icons.Default.BeachAccess,
                        color = SnoozeYellow
                    )
                }
                if (alarm.repeatLabel.isNotBlank()) {
                    AppStatusChip(
                        label = alarm.repeatLabel,
                        icon = Icons.Default.CheckCircle,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                alarm.shiftPatternChipLabel()?.let { label ->
                    AppStatusChip(
                        label = label,
                        color = SnoozeYellow
                    )
                }
                if (alarm.usesFixedTimezone) {
                    AppStatusChip(label = alarm.fixedTimezoneId, color = SnoozeYellow)
                }
                if (alarm.group.isNotBlank()) {
                    AppStatusChip(label = alarm.group)
                }
                if (alarm.challengeType != "NONE") {
                    AppStatusChip(
                        label = challengeTypeLabel(alarm.challengeType)?.let { stringResource(it) }
                            ?: alarm.challengeType.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() },
                        color = SnoozeYellow
                    )
                }
                if (alarm.ringtoneUri == "silent") {
                    AppStatusChip(label = stringResource(R.string.silent), color = TextMuted)
                }
            }

            val toggleSemanticsCd = stringResource(
        R.string.alarm_toggle_cd,
        alarm.label.ifBlank { formatAlarmTime(alarm, is24Hour) }
    )
    val toggleSemanticsState = stringResource(
        if (alarm.isEnabled) R.string.status_enabled else R.string.status_disabled
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(
                onClick = { onToggle(alarm) },
                onLongClick = { if (alarm.isEnabled) onForceToggle(alarm) }
            )
            .semantics {
                contentDescription = toggleSemanticsCd
                stateDescription = toggleSemanticsState
                role = Role.Switch
            },
                shape = RoundedCornerShape(10.dp),
                color = SurfaceMedium,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = com.wakesync.app.ui.theme.BorderSubtle
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.alarm_state), color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(
                                if (alarm.isEnabled) R.string.alarm_state_toggle_running else R.string.alarm_state_toggle_off
                            ),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = null,
                        colors = appSwitchColors(),
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onEdit(alarm) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.edit_alarm))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onDuplicate(alarm) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.duplicate))
                    }
                    OutlinedButton(
                        onClick = { onShare(alarm) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.share))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onShowHistory(alarm) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.history))
                    }
                    OutlinedButton(
                        onClick = { onDelete(alarm) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.delete))
                    }
                }
                if (alarm.isEnabled && alarm.isRecurringSchedule) {
                    OutlinedButton(
                        onClick = { onSkipNext(alarm) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.skip_next))
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmHeader(
    remainingTime: String,
    hasAlarms: Boolean,
    alarmCount: Int,
    vacationActive: Boolean,
    sortLabel: String,
    onCycleSort: () -> Unit,
    onSync: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "WakeSync",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.header_phone_watch),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(DismissGreen)
                )
                Text(
                    text = stringResource(R.string.synced),
                    color = DismissGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            IconButton(onClick = onCycleSort) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = sortLabel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = stringResource(
                    if (vacationActive) R.string.vacation_active else R.string.autosync_enabled
                ),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onSync,
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text(stringResource(R.string.sync_watch), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GroupFilterRow(
    title: String,
    groups: List<String>,
    selectedGroup: String?,
    onSelectGroup: (String?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppSectionTitle(title = title)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppFilterChip(
                label = stringResource(R.string.all),
                selected = selectedGroup == null,
                onClick = { onSelectGroup(null) },
                selectionSemantics = true,
            )
            groups.forEach { group ->
                AppFilterChip(
                    label = group,
                    selected = selectedGroup == group,
                    onClick = { onSelectGroup(if (selectedGroup == group) null else group) },
                    selectionSemantics = true,
                )
            }
        }
    }
}

@Composable
private fun AlarmReorderHandle(
    enabled: Boolean,
    alarmLabel: String,
    onMoveUp: () -> Boolean,
    onMoveDown: () -> Boolean,
    modifier: Modifier = Modifier
) {
    // v1.13.15: WCAG 2.5.7 — expose drag-equivalent moves as accessibility actions.
    val moveUpLabel = stringResource(R.string.alarm_list_move_up)
    val moveDownLabel = stringResource(R.string.alarm_list_move_down)
    val handleCd = stringResource(
        if (enabled) R.string.cd_drag_handle_for else R.string.cd_drag_handle_unavailable,
        if (enabled) alarmLabel else ""
    )
    val handleState = stringResource(
        if (enabled) R.string.stopwatch_status_ready else R.string.status_disabled
    )
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (enabled) {
                    SurfaceMedium
                } else {
                    SurfaceMedium.copy(alpha = 0.42f)
                }
            )
            .semantics {
                contentDescription = handleCd
                stateDescription = handleState
                if (enabled) {
                    customActions = listOf(
                        CustomAccessibilityAction(moveUpLabel) { onMoveUp() },
                        CustomAccessibilityAction(moveDownLabel) { onMoveDown() }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DragIndicator,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.primary else TextMuted
        )
    }
}

@Composable
private fun QuickAlarmRow(
    onQuickAlarm: (Int) -> Unit,
    napDefaultMinutes: Int = 20
) {
    AppSurfaceCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        AppSectionTitle(
            title = stringResource(R.string.quick_alarms)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickOptions = listOf(
                10 to R.string.quick_10min,
                30 to R.string.quick_30min,
                60 to R.string.quick_1hour,
                120 to R.string.quick_2hours
            )
            quickOptions.forEach { (minutes, labelRes) ->
                val label = stringResource(labelRes)
                AppFilterChip(
                    label = label,
                    selected = false,
                    accessibilityLabel = stringResource(R.string.quick_alarm_cd, label),
                    onClick = { onQuickAlarm(minutes) },
                )
            }
        }
        // v1.4.0 nap row, v1.5.0 pre-selects the user's default.
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )
        Text(
            text = stringResource(R.string.power_nap),
            color = TextSecondary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Always include the user's default nap length, even if it's not
            // one of the standard chip values, so the setting is honored here.
            val napOptions = (listOf(15, 20, 25, 45, 90) + napDefaultMinutes)
                .filter { it > 0 }
                .distinct()
                .sorted()
            napOptions.forEach { minutes ->
                val isDefault = minutes == napDefaultMinutes
                AppFilterChip(
                    label = stringResource(R.string.timer_preset_minutes, minutes),
                    selected = isDefault,
                    leadingIcon = if (isDefault) Icons.Default.CheckCircle else null,
                    selectionSemantics = false,
                    accessibilityLabel = stringResource(R.string.quick_nap_cd, minutes) +
                        if (isDefault) stringResource(R.string.quick_nap_default) else "",
                    onClick = { onQuickAlarm(minutes) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlarmCard(
    alarm: Alarm,
    is24Hour: Boolean,
    suppressedByVacation: Boolean = false,
    isActivePaneSelection: Boolean = false,
    lastChanged: com.wakesync.app.sync.AlarmLastChange? = null,
    onToggle: () -> Unit,
    onForceToggle: () -> Unit = {},
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onSkipNext: () -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onShowHistory: () -> Unit = {},
    onLongClick: () -> Unit
) {
    val shapeTokens = LocalAppShapeTokens.current
    val cardAlpha = if (alarm.isEnabled) 1f else 0.55f
    val (timeStr, periodStr) = splitAlarmTime(alarm, is24Hour)
    val selectedSemantics = stringResource(R.string.selected_state)
    val toggleStateSemantics = stringResource(
        if (alarm.isEnabled) R.string.status_enabled else R.string.status_disabled
    )
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics {
                if (isActivePaneSelection) {
                    selected = true
                    stateDescription = selectedSemantics
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isActivePaneSelection -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                alarm.isEnabled -> SurfaceCard
                else -> SurfaceCard.copy(alpha = 0.55f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            // ── Time + label + toggle ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = timeStr,
                            color = if (alarm.isEnabled) TextPrimary else TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            letterSpacing = 0.sp
                        )
                        if (periodStr.isNotBlank()) {
                            Text(
                                text = periodStr,
                                color = if (alarm.isEnabled) TextMuted else TextMuted.copy(alpha = 0.6f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                    if (alarm.label.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = alarm.label,
                            color = if (alarm.isEnabled) TextSecondary else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Toggle pill
                val alarmToggleLabel = alarm.label.ifBlank { timeStr }
                val toggleCd = stringResource(R.string.alarm_toggle_cd, alarmToggleLabel)
                Box(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { onToggle() },
                            onLongClick = { if (alarm.isEnabled) onForceToggle() }
                        )
                        .semantics {
                            contentDescription = toggleCd
                            stateDescription = toggleStateSemantics
                            role = Role.Switch
                        }
                ) {
                    Switch(
                        checked = alarm.isEnabled,
                        onCheckedChange = null,
                        colors = appSwitchColors(),
                        modifier = Modifier.clearAndSetSemantics {}
                    )
                }
            }

            // ── Divider ──
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = BorderSubtle,
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // ── Footer: repeat + remaining · action icons ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val repeatText = buildList {
                    if (alarm.repeatLabel.isNotBlank()) add(alarm.repeatLabel)
                    alarm.shiftPatternChipLabel()?.let(::add)
                }.joinToString(" · ")

                val remainingText = if (alarm.isEnabled) nextRemainingLabel(alarm, context) else ""

                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (repeatText.isNotBlank()) {
                        Text(
                            text = repeatText,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    if (remainingText.isNotBlank() && repeatText.isNotBlank()) {
                        Text("·", color = TextMuted.copy(alpha = 0.5f), fontSize = 11.sp)
                    }
                    if (remainingText.isNotBlank()) {
                        Text(
                            text = remainingText,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (alarm.isEnabled) FontWeight.Medium else FontWeight.Normal,
                            maxLines = 1
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (alarm.isEnabled) {
                        IconButton(onClick = { onShowHistory() }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.test_ring),
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_edit),
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ── Sync info tag ──
            lastChanged?.let { change ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (change.fromWatch) Icons.Filled.Watch else Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = TextMuted.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(
                            if (change.fromWatch) R.string.change_from_watch else R.string.change_from_phone
                        ),
                        color = TextMuted.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onEnableSelected: () -> Unit,
    onDisableSelected: () -> Unit
) {
    AppSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        highlighted = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Default.Close, stringResource(R.string.cd_clear_selection), tint = TextPrimary)
                    }
                    Column {
                        Text(stringResource(R.plurals.selection_count, selectedCount, selectedCount), color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(
                                if (selectedCount == totalCount) R.string.bulk_all_note else R.string.bulk_selected_note
                            ),
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (selectedCount < totalCount) {
                    TextButton(onClick = onSelectAll) {
                        Text(stringResource(R.string.select_visible), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEnableSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DismissGreen)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.enable))
                }
                OutlinedButton(
                    onClick = onDisableSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                ) {
                    Icon(Icons.Default.NotificationsOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.pause))
                }
                Button(
                    onClick = onDeleteSelected,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SelectableAlarmCard(
    alarm: Alarm,
    is24Hour: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    val shapeTokens = LocalAppShapeTokens.current
    val selectStateSemantics = stringResource(
        if (isSelected) R.string.selected_state else R.string.not_selected
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggleSelect)
            .semantics {
                selected = isSelected
                stateDescription = selectStateSemantics
            },
        shape = shapeTokens.card,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            } else {
                SurfaceMedium
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.34f)
            else com.wakesync.app.ui.theme.BorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = TextMuted
                )
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatAlarmTime(alarm, is24Hour),
                    color = if (alarm.isEnabled) TextPrimary else TextMuted,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light
                )
                Text(
                    text = alarm.label.ifBlank { alarm.repeatLabel },
                    color = if (alarm.isEnabled) TextSecondary else TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                imageVector = if (alarm.isEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                contentDescription = null,
                tint = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else TextMuted
            )
        }
    }
}

private fun shareAlarm(context: Context, alarm: Alarm, is24Hour: Boolean) {
    val deepLink = AlarmShareCodec.createDeepLink(alarm)
    val title = alarm.label.ifBlank { context.getString(R.string.share_alarm_title, formatAlarmTime(alarm, is24Hour)) }
    val shareText = buildString {
        appendLine(context.getString(R.string.share_subject, title))
        appendLine(context.getString(R.string.share_time, formatAlarmTime(alarm, is24Hour)))
        append(context.getString(R.string.share_import, deepLink))
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject, title))
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_alarm_chooser)))
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.share_no_app), Toast.LENGTH_SHORT).show()
    }
}

private fun formatAlarmTime(alarm: Alarm, is24Hour: Boolean): String {
    return if (is24Hour) {
        String.format("%02d:%02d", alarm.hour, alarm.minute)
    } else {
        val hour12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
        val amPm = if (alarm.hour < 12) "AM" else "PM"
        "$hour12:${String.format("%02d", alarm.minute)} $amPm"
    }
}

/** Splits time into (timeStr, periodStr). 24h returns ("07:00", ""). 12h returns ("7:00", "AM"). */
private fun splitAlarmTime(alarm: Alarm, is24Hour: Boolean): Pair<String, String> {
    return if (is24Hour) {
        String.format("%02d:%02d", alarm.hour, alarm.minute) to ""
    } else {
        val hour12 = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
        val amPm = if (alarm.hour < 12) "AM" else "PM"
        "$hour12:${String.format("%02d", alarm.minute)}" to amPm
    }
}

/** Short relative remaining time like "in 23h 20m" or "in 5m". */
private fun nextRemainingLabel(alarm: Alarm, context: Context): String {
    val now = java.lang.System.currentTimeMillis()
    val diff = alarm.nextTriggerTime - now
    if (diff <= 0) return ""
    val totalMinutes = diff / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> context.getString(R.string.alarm_in_hours_mins, hours.toString(), minutes.toString())
        minutes > 0 -> context.getString(R.string.alarm_in_minutes, minutes.toString())
        else -> context.getString(R.string.alarm_in_less_minute)
    }
}

private fun challengeTypeLabel(type: String): Int? = when (type) {
    "MATH_EASY"      -> R.string.challenge_math_easy
    "MATH_MEDIUM"    -> R.string.challenge_math_medium
    "MATH_HARD"      -> R.string.challenge_math_hard
    "SHAKE"          -> R.string.challenge_shake
    "SEQUENCE"       -> R.string.challenge_sequence
    "MEMORY_PATTERN" -> R.string.challenge_memory_pattern
    "TYPING"         -> R.string.challenge_typing
    "VOICE_PHRASE"   -> R.string.challenge_voice_phrase
    "HANDWRITING"    -> R.string.challenge_handwriting
    "WALK_STEPS"     -> R.string.challenge_walk_steps
    "NFC_SCAN"       -> R.string.challenge_nfc_scan
    "BARCODE_SCAN"   -> R.string.challenge_barcode_scan
    "PHOTO_MATCH"    -> R.string.challenge_photo_match
    "SQUAT"          -> R.string.challenge_squat
    "WIFI_CONNECT"   -> R.string.challenge_wifi_connect
    "MAZE"           -> R.string.challenge_maze
    "COUNT_SHEEP"    -> R.string.challenge_count_sheep
    "SIMON_SAYS"     -> R.string.challenge_simon_says
    "DATE_BACKWARDS" -> R.string.challenge_date_backwards
    "STROOP"         -> R.string.challenge_stroop
    "ROCK_PAPER_SCISSORS" -> R.string.challenge_rps
    "EMOJI_MEMORY"   -> R.string.challenge_emoji_memory
    "TYPING_SPEED"   -> R.string.challenge_typing_speed
    "WORDLE"         -> R.string.challenge_wordle
    "PVT"            -> R.string.challenge_pvt
    "PUSH_UP"        -> R.string.challenge_push_up
    "PLANK_HOLD"     -> R.string.challenge_plank_hold
    else             -> null
}

private fun Alarm.shiftPatternChipLabel(): String? {
    val pattern = ShiftPattern.fromKey(shiftPattern) ?: return null
    if (shiftPatternStartDate.isBlank()) return null
    return pattern.shortLabel
}

private fun nextOccurrenceLabel(alarm: Alarm, is24Hour: Boolean, context: Context): String {
    if (!alarm.isEnabled || alarm.nextTriggerTime <= 0) {
        return context.getString(R.string.alarm_paused_until_enable)
    }
    val pattern = if (is24Hour) "EEE, MMM d • HH:mm" else "EEE, MMM d • h:mm a"
    val formatted = Instant.ofEpochMilli(alarm.nextTriggerTime)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern, java.util.Locale.getDefault()))
    return context.getString(R.string.next_occurrence, formatted)
}
