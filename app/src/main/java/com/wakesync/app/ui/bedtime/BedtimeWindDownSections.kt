package com.wakesync.app.ui.bedtime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.wakesync.app.R
import com.wakesync.app.ui.components.AppSectionTitle
import com.wakesync.app.ui.components.AppStatusChip
import com.wakesync.app.ui.components.AppSurfaceCard
import com.wakesync.app.ui.theme.DismissGreen
import com.wakesync.app.ui.theme.SurfaceCard
import com.wakesync.app.ui.theme.TextMuted
import com.wakesync.app.ui.theme.TextPrimary
import com.wakesync.app.ui.theme.TextSecondary

@Composable
internal fun SleepCycleOptionRow(index: Int, option: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceCard.copy(alpha = if (index == 0) 0.82f else 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppStatusChip(
                label = if (index == 0) {
                    stringResource(R.string.bedtime_cycle_best_match)
                } else {
                    "${index + 1}"
                },
                color = if (index == 0) DismissGreen else MaterialTheme.colorScheme.primary
            )
            Text(
                text = option,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
internal fun WindDownChecklistSection(
    state: BedtimeUiState,
    onToggle: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(modifier = modifier) {
        AppSectionTitle(
            title = stringResource(R.string.bedtime_checklist_title),
            description = stringResource(R.string.bedtime_checklist_desc),
            action = {
                AppStatusChip(
                    label = stringResource(
                        R.string.bedtime_checklist_done_count,
                        state.bedtimeChecklistDone.size,
                        state.bedtimeChecklist.size
                    ),
                    icon = Icons.Default.CheckCircle,
                    color = if (state.bedtimeChecklistDone.isEmpty()) TextMuted else DismissGreen
                )
            }
        )

        state.bedtimeChecklist.forEachIndexed { index, item ->
            val done = index in state.bedtimeChecklistDone
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Checkbox) { onToggle(index) },
                shape = RoundedCornerShape(12.dp),
                color = if (done) DismissGreen.copy(alpha = 0.09f) else SurfaceCard.copy(alpha = 0.72f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.Bedtime,
                        contentDescription = if (done) {
                            stringResource(R.string.bedtime_checklist_completed)
                        } else {
                            stringResource(R.string.bedtime_checklist_not_done)
                        },
                        tint = if (done) DismissGreen else TextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = item,
                        color = if (done) TextMuted else TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    AppStatusChip(
                        label = if (done) {
                            stringResource(R.string.bedtime_checklist_done)
                        } else {
                            stringResource(R.string.bedtime_checklist_up_next)
                        },
                        color = if (done) DismissGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (state.bedtimeChecklistDone.isNotEmpty()) {
            TextButton(
                onClick = onReset,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(R.string.bedtime_checklist_reset), color = TextSecondary)
            }
        }
    }
}
