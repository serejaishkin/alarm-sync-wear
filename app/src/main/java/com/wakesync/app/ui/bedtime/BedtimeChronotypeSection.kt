package com.wakesync.app.ui.bedtime

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wakesync.app.R
import com.wakesync.app.domain.ChronotypeEstimator
import com.wakesync.app.ui.components.AppFilterChip
import com.wakesync.app.ui.components.AppSectionTitle
import com.wakesync.app.ui.components.AppStatusChip
import com.wakesync.app.ui.components.AppSurfaceCard
import com.wakesync.app.ui.theme.DismissGreen
import com.wakesync.app.ui.theme.SnoozeYellow
import com.wakesync.app.ui.theme.TextMuted
import com.wakesync.app.ui.theme.TextPrimary
import com.wakesync.app.ui.theme.TextSecondary

private data class LocalizedChronotypeQuestion(
    val prompt: String,
    val options: List<String>
)

@Composable
private fun localizedChronotypeQuestions(): List<LocalizedChronotypeQuestion> = listOf(
    LocalizedChronotypeQuestion(
        stringResource(R.string.chronotype_prompt_0),
        stringArrayResource(R.array.chronotype_options_0).toList()
    ),
    LocalizedChronotypeQuestion(
        stringResource(R.string.chronotype_prompt_1),
        stringArrayResource(R.array.chronotype_options_1).toList()
    ),
    LocalizedChronotypeQuestion(
        stringResource(R.string.chronotype_prompt_2),
        stringArrayResource(R.array.chronotype_options_2).toList()
    ),
    LocalizedChronotypeQuestion(
        stringResource(R.string.chronotype_prompt_3),
        stringArrayResource(R.array.chronotype_options_3).toList()
    ),
    LocalizedChronotypeQuestion(
        stringResource(R.string.chronotype_prompt_4),
        stringArrayResource(R.array.chronotype_options_4).toList()
    )
)

@Composable
internal fun ChronotypeSection(
    state: BedtimeUiState,
    onAnswer: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AppSurfaceCard(
        modifier = modifier,
        highlighted = state.chronotypeComplete
    ) {
        AppSectionTitle(
            title = stringResource(R.string.bedtime_chronotype_title),
            description = if (state.chronotypeComplete) {
                stringResource(R.string.bedtime_chronotype_desc_complete)
            } else {
                stringResource(R.string.bedtime_chronotype_desc_idle)
            }
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppStatusChip(
                label = state.chronotypeCategoryLabel,
                icon = Icons.Default.Lightbulb,
                color = if (state.chronotypeComplete) MaterialTheme.colorScheme.primary else TextMuted
            )
            AppStatusChip(
                label = state.chronotypeTimingLabel,
                icon = Icons.Default.Schedule,
                color = if (state.chronotypeComplete) DismissGreen else SnoozeYellow
            )
            AppStatusChip(
                label = "${state.chronotypeAnsweredCount}/${ChronotypeEstimator.QUESTION_COUNT}",
                icon = Icons.Default.CheckCircle,
                color = if (state.chronotypeComplete) DismissGreen else TextMuted
            )
        }

        Text(
            text = state.chronotypeHelper,
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        localizedChronotypeQuestions().forEachIndexed { questionIndex, question ->
            if (questionIndex > 0) {
                HorizontalDivider(color = TextMuted.copy(alpha = 0.12f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = question.prompt,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.options.forEachIndexed { answerIndex, label ->
                        AppFilterChip(
                            label = label,
                            selected = state.chronotypeAnswers.getOrNull(questionIndex) == answerIndex,
                            onClick = { onAnswer(questionIndex, answerIndex) },
                            selectionSemantics = true
                        )
                    }
                }
            }
        }
    }
}
