package com.wakesync.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakesync.app.R
import com.wakesync.app.data.preferences.PreferencesManager
import com.wakesync.app.data.repository.CalendarEvent
import com.wakesync.app.data.repository.CalendarRepository
import com.wakesync.app.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val todayDate: String = "",
    val nextAlarmTime: String = "",
    val nextAlarmLabel: String = "",
    val nextAlarmSchedule: String = "",
    val showCalendar: Boolean = true,
    // Calendar
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val calendarError: String? = null,
    val calendarPermissionNeeded: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val calendarRepository: CalendarRepository,
    private val alarmRepository: AlarmRepository,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        _uiState.update { it.copy(
            todayDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
        ) }
        viewModelScope.launch {
            combine(
                alarmRepository.observeNextAlarm(),
                preferencesManager.settings
            ) { alarm, settings ->
                if (alarm == null) {
                    Triple("", "", "")
                } else {
                    val pattern = if (settings.is24HourFormat) "HH:mm" else "h:mm a"
                    Triple(
                        alarm.time.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault())),
                        alarm.label.ifBlank {
                            getApplication<Application>().getString(R.string.stats_alarm_default)
                        },
                        alarm.repeatLabel
                    )
                }
            }.collect { (time, label, schedule) ->
                _uiState.update {
                    it.copy(
                        nextAlarmTime = time,
                        nextAlarmLabel = label,
                        nextAlarmSchedule = schedule
                    )
                }
            }
        }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val settings = preferencesManager.getCurrentSettings()
            _uiState.update { it.copy(
                showCalendar = settings.showCalendarOnDashboard
            ) }

            if (settings.showCalendarOnDashboard) {
                loadCalendar()
            } else {
                _uiState.update { it.copy(
                    calendarEvents = emptyList(),
                    calendarError = null,
                    calendarPermissionNeeded = false
                ) }
            }
        }
    }

    fun loadCalendar() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = calendarRepository.getTodayEvents()
            result.onSuccess { events ->
                _uiState.update { it.copy(
                    calendarEvents = events,
                    calendarError = null,
                    calendarPermissionNeeded = false
                ) }
            }.onFailure { e ->
                if (e is SecurityException) {
                    _uiState.update { it.copy(
                        calendarEvents = emptyList(),
                        calendarPermissionNeeded = true,
                        calendarError = getApplication<Application>().getString(R.string.dashboard_calendar_permission_error)
                    ) }
                } else {
                    _uiState.update { it.copy(
                        calendarEvents = emptyList(),
                        calendarError = getApplication<Application>().getString(R.string.dashboard_calendar_load_error)
                    ) }
                }
            }
        }
    }
}