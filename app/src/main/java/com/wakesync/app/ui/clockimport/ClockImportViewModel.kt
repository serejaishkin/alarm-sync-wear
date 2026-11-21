package com.wakesync.app.ui.clockimport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wakesync.app.data.model.Alarm
import com.wakesync.app.data.repository.AlarmRepository
import com.wakesync.app.domain.AlarmScheduler
import com.wakesync.app.domain.NextAlarmCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/** One row the user is typing in, kept as raw text until it is saved. */
data class ClockImportDraft(
    val id: Int,
    val time: String = "",
    val days: Set<DayOfWeek> = emptySet(),
    val label: String = ""
) {
    val parsedTime: Pair<Int, Int>? get() = ClockImportParser.parseTime(time)
    val isValid: Boolean get() = parsedTime != null
}

data class ClockImportUiState(
    val drafts: List<ClockImportDraft> = listOf(ClockImportDraft(id = 0)),
    val isSaving: Boolean = false
) {
    val validCount: Int get() = drafts.count { it.isValid }
}

@HiltViewModel
class ClockImportViewModel @Inject constructor(
    private val repository: AlarmRepository,
    private val scheduler: AlarmScheduler,
    private val calculator: NextAlarmCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClockImportUiState())
    val uiState: StateFlow<ClockImportUiState> = _uiState.asStateFlow()

    fun addRow() = _uiState.update { state ->
        state.copy(drafts = state.drafts + ClockImportDraft(id = (state.drafts.maxOfOrNull { it.id } ?: -1) + 1))
    }

    fun removeRow(id: Int) = _uiState.update { state ->
        val remaining = state.drafts.filterNot { it.id == id }
        state.copy(drafts = remaining.ifEmpty { listOf(ClockImportDraft(id = id + 1)) })
    }

    fun updateRow(id: Int, transform: (ClockImportDraft) -> ClockImportDraft) = _uiState.update { state ->
        state.copy(drafts = state.drafts.map { if (it.id == id) transform(it) else it })
    }

    /**
     * Creates the valid rows as enabled alarms through the same steps the
     * system "set alarm" handler uses (save, compute next trigger, schedule),
     * so phone/watch sync and scheduling behave exactly as for any new alarm.
     * Rows identical to an existing enabled alarm are skipped, not duplicated.
     */
    fun createAlarms(onDone: (created: Int, skipped: Int, failed: Int) -> Unit) {
        val current = _uiState.value
        if (current.isSaving) return
        val rows = current.drafts.filter { it.isValid }
        if (rows.isEmpty()) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            var created = 0
            var skipped = 0
            var failed = 0
            val seen = repository.getEnabled().toMutableList()
            for (row in rows) {
                val (hour, minute) = row.parsedTime ?: continue
                val draft = Alarm(
                    hour = hour,
                    minute = minute,
                    label = row.label.trim(),
                    repeatDays = row.days,
                    isEnabled = true
                ).sanitized()
                val duplicate = seen.any {
                    it.hour == draft.hour && it.minute == draft.minute &&
                        it.label == draft.label && it.repeatDays == draft.repeatDays
                }
                if (duplicate) {
                    skipped++
                    continue
                }
                try {
                    val id = repository.save(draft)
                    val withId = draft.copy(id = id)
                    val trigger = calculator.calculate(withId)
                    repository.updateNextTrigger(id, trigger)
                    val scheduled = withId.copy(nextTriggerTime = trigger)
                    scheduler.schedule(scheduled)
                    seen += scheduled
                    created++
                } catch (_: Exception) {
                    failed++
                }
            }
            _uiState.update { it.copy(isSaving = false) }
            onDone(created, skipped, failed)
        }
    }
}
