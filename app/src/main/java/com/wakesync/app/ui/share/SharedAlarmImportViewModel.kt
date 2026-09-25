package com.wakesync.app.ui.share

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wakesync.app.R
import com.wakesync.app.data.model.Alarm
import com.wakesync.app.data.repository.AlarmRepository
import com.wakesync.app.data.share.AlarmShareCodec
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedAlarmImportUiState(
    val isSaving: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SharedAlarmImportViewModel @Inject constructor(
    application: Application,
    private val repository: AlarmRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SharedAlarmImportUiState())
    val uiState: StateFlow<SharedAlarmImportUiState> = _uiState.asStateFlow()

    fun saveDraft(
        alarm: Alarm,
        stripRiskyFields: Boolean,
        onSaved: (Long) -> Unit
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.value = SharedAlarmImportUiState(isSaving = true)
            val candidate = if (stripRiskyFields) {
                AlarmShareCodec.stripRiskyImportedFields(alarm)
            } else {
                alarm
            }
            val imported = AlarmShareCodec.prepareImportedAlarm(candidate)
            try {
                val id = repository.save(imported)
                _uiState.value = SharedAlarmImportUiState()
                onSaved(id)
            } catch (_: Exception) {
                _uiState.value = SharedAlarmImportUiState(
                    error = getApplication<Application>().getString(R.string.share_import_save_error)
                )
            }
        }
    }
}
