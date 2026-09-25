package com.wakesync.app.ui.share

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wakesync.app.R
import com.wakesync.app.data.model.Alarm
import com.wakesync.app.ui.alarmedit.alarmChallengeLabelRes
import com.wakesync.app.ui.components.AppChipShape
import com.wakesync.app.ui.components.AppFeedbackCard
import com.wakesync.app.ui.components.AppStatusChip
import com.wakesync.app.ui.components.AppSurfaceCard
import com.wakesync.app.ui.theme.AccentRed
import com.wakesync.app.ui.theme.BorderSubtle
import com.wakesync.app.ui.theme.DismissGreen
import com.wakesync.app.ui.theme.SnoozeYellow
import com.wakesync.app.ui.theme.SurfaceDark
import com.wakesync.app.ui.theme.SurfaceLight
import com.wakesync.app.ui.theme.TextMuted
import com.wakesync.app.ui.theme.TextPrimary
import com.wakesync.app.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun SharedAlarmImportScreen(
    alarm: Alarm,
    onCancel: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: SharedAlarmImportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val riskyFields = remember(alarm) { alarm.sharedImportRiskLabelRes() }
    var stripRiskyFields by remember(alarm) { mutableStateOf(riskyFields.isNotEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.share_import_back),
                    tint = TextPrimary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.share_import_title_review),
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.share_import_subtitle),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        AppSurfaceCard {
            Text(
                text = alarm.label.ifBlank { stringResource(R.string.share_import_default_label) },
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            SharedImportDetailRow(label = stringResource(R.string.share_import_row_time), value = alarm.formatSharedImportTime())
            SharedImportDetailRow(label = stringResource(R.string.share_import_row_repeat), value = alarm.repeatLabel)
            SharedImportDetailRow(label = stringResource(R.string.share_import_row_challenge), value = alarm.challengeSummary())
            SharedImportDetailRow(label = stringResource(R.string.share_import_row_sound), value = stringResource(alarm.soundSummaryRes()))
            SharedImportDetailRow(label = stringResource(R.string.share_import_row_status), value = stringResource(R.string.share_import_status_off))
        }

        AppSurfaceCard(highlighted = riskyFields.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.share_import_private_title),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                AppStatusChip(
                    label = stringResource(
                        if (riskyFields.isEmpty()) R.string.share_import_private_clean else R.string.share_import_private_review
                    ),
                    icon = if (riskyFields.isEmpty()) Icons.Default.CheckCircle else Icons.Default.Security,
                    color = if (riskyFields.isEmpty()) DismissGreen else SnoozeYellow
                )
            }
            if (riskyFields.isEmpty()) {
                Text(
                    text = stringResource(R.string.share_import_private_none_desc),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    riskyFields.forEach { labelRes ->
                        AppStatusChip(
                            label = stringResource(labelRes),
                            color = SnoozeYellow
                        )
                    }
                }
                PrivateReferenceToggle(
                    checked = stripRiskyFields,
                    onCheckedChange = { stripRiskyFields = it }
                )
                Text(
                    text = stringResource(R.string.share_import_private_strip_desc),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        uiState.error?.let { error ->
            AppFeedbackCard(
                title = stringResource(R.string.share_import_error_title),
                message = error,
                icon = Icons.Default.Warning,
                color = AccentRed
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving,
                shape = AppChipShape
            ) {
                Text(stringResource(R.string.share_import_discard))
            }
            Button(
                onClick = {
                    viewModel.saveDraft(
                        alarm = alarm,
                        stripRiskyFields = stripRiskyFields,
                        onSaved = onSaved
                    )
                },
                modifier = Modifier.weight(1f),
                enabled = !uiState.isSaving,
                shape = AppChipShape
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.share_import_saving))
                } else {
                    Text(stringResource(R.string.share_import_save_off))
                }
            }
        }

        Spacer(modifier = Modifier.padding(bottom = 4.dp))
    }
}

@Composable
private fun PrivateReferenceToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        shape = RoundedCornerShape(10.dp),
        color = if (checked) {
            DismissGreen.copy(alpha = 0.10f)
        } else {
            SurfaceLight.copy(alpha = 0.58f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (checked) DismissGreen.copy(alpha = 0.28f) else BorderSubtle
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = DismissGreen,
                    uncheckedColor = TextMuted,
                    checkmarkColor = SurfaceDark
                )
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        color = DismissGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = DismissGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
Text(
                text = stringResource(R.string.share_import_strip_toggle),
                color = TextPrimary,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(
                    if (checked) R.string.share_import_strip_toggle_recommended else R.string.share_import_strip_toggle_optional
                ),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            }
        }
    }
}

@Composable
private fun SharedImportDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = TextMuted,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(0.34f)
        )
        Text(
            text = value,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.66f)
        )
    }
}

private fun Alarm.formatSharedImportTime(): String {
    return String.format(Locale.US, "%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
}

@Composable
private fun Alarm.challengeSummary(): String {
    val chainResourceIds = challengeChain
        .split(",")
        .mapNotNull { it.trim().takeIf(String::isNotBlank)?.alarmChallengeLabelRes() }
    if (challengeChain.isNotBlank() && chainResourceIds.isNotEmpty()) {
        return buildString {
            chainResourceIds.forEachIndexed { index, resource ->
                if (index > 0) append(" + ")
                append(stringResource(resource))
            }
        }
    }
    if (challengeType.isNotBlank() && challengeType != "NONE") {
        val resource = challengeType.alarmChallengeLabelRes()
        if (resource != null) return stringResource(resource)
        return challengeType.toSharedImportLabel()
    }
    return stringResource(R.string.share_import_challenge_none)
}

private fun Alarm.soundSummaryRes(): Int = when {
    ringtonePool.isNotBlank() -> R.string.share_import_sound_pool
    ringtoneUri == "silent" -> R.string.share_import_sound_silent
    ringtoneUri.isNotBlank() -> R.string.share_import_sound_custom
    else -> R.string.share_import_sound_default
}

private fun String.toSharedImportLabel(): String {
    val normalized = trim()
        .replace('_', ' ')
        .lowercase(Locale.US)
    return normalized.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
    }
}

private fun Alarm.sharedImportRiskLabelRes(): List<Int> = buildList {
    if (ringtoneUri.isNotBlank() || ringtonePool.isNotBlank()) add(R.string.share_import_risk_ringtone)
    if (guardianEnabled || guardianPhone.isNotBlank()) add(R.string.share_import_risk_guardian)
    if (hueEnabled) add(R.string.share_import_risk_hue)
    if (nfcTagId.isNotBlank()) add(R.string.share_import_risk_nfc)
    if (barcodeValue.isNotBlank()) add(R.string.share_import_risk_barcode)
    if (photoMatchUri.isNotBlank()) add(R.string.share_import_risk_photo)
    if (wifiDismissSsid.isNotBlank()) add(R.string.share_import_risk_wifi)
    if (locationDismissEnabled) add(R.string.share_import_risk_location)
    if (morningRoutine.isNotBlank()) add(R.string.share_import_risk_routine)
    if (challengeType.uppercase(Locale.US) in referenceBackedChallenges) {
        add(R.string.share_import_risk_challenge)
    }
    if (challengeChain.split(",").any { it.trim().uppercase(Locale.US) in referenceBackedChallenges }) {
        add(R.string.share_import_risk_chain)
    }
}

private val referenceBackedChallenges = setOf(
    "NFC_SCAN",
    "BARCODE_SCAN",
    "PHOTO_MATCH",
    "WIFI_CONNECT"
)
