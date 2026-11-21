package com.wakesync.app.ui.clockimport

import android.content.Intent
import android.provider.AlarmClock
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakesync.app.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Manual "move alarms from Clock" wizard. Android has no API for reading
 * another clock app's alarms, so the user opens that app, reads the times
 * and types them here; everything else is created like any new alarm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockImportScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClockImportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.clock_import_title)) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) { Text(stringResource(R.string.clock_import_back)) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.clock_import_intro),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = { openOtherClock(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.clock_import_open_clock)) }
                }
            }
            items(state.drafts, key = { it.id }) { draft ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = draft.time,
                                onValueChange = { v -> viewModel.updateRow(draft.id) { it.copy(time = v.take(8)) } },
                                label = { Text(stringResource(R.string.clock_import_time)) },
                                placeholder = { Text("07:30") },
                                singleLine = true,
                                isError = draft.time.isNotBlank() && !draft.isValid,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = draft.label,
                                onValueChange = { v -> viewModel.updateRow(draft.id) { it.copy(label = v.take(60)) } },
                                label = { Text(stringResource(R.string.clock_import_label)) },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DayOfWeek.values().forEach { day ->
                                FilterChip(
                                    selected = day in draft.days,
                                    onClick = {
                                        viewModel.updateRow(draft.id) {
                                            it.copy(days = if (day in it.days) it.days - day else it.days + day)
                                        }
                                    },
                                    label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault())) }
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.removeRow(draft.id) }) {
                            Text(stringResource(R.string.clock_import_remove))
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::addRow, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.clock_import_add_row))
                    }
                    Button(
                        enabled = state.validCount > 0 && !state.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.createAlarms { created, skipped, failed ->
                                val msg = context.getString(R.string.clock_import_result, created, skipped, failed)
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (failed == 0) onNavigateBack()
                            }
                        }
                    ) { Text(stringResource(R.string.clock_import_create, state.validCount)) }
                }
            }
        }
    }
}

/** Opens a clock app other than WakeSync (our own exported SET/SHOW handlers are excluded). */
private fun openOtherClock(context: android.content.Context) {
    val own = context.packageName
    val pm = context.packageManager
    val base = Intent(AlarmClock.ACTION_SHOW_ALARMS)
    val others = pm.queryIntentActivities(base, 0)
        .filter { it.activityInfo.packageName != own }
        .map { Intent(base).setPackage(it.activityInfo.packageName) }
    val first = others.firstOrNull()
    if (first == null) {
        Toast.makeText(context, R.string.clock_import_no_clock, Toast.LENGTH_LONG).show()
        return
    }
    runCatching {
        context.startActivity(first.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.onFailure {
        Toast.makeText(context, R.string.clock_import_no_clock, Toast.LENGTH_LONG).show()
    }
}
