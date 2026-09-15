package com.homestrength.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.data.local.relation.SetWithBands
import com.homestrength.domain.progression.ProgressionSuggestion
import com.homestrength.domain.progression.ProgressionType
import com.homestrength.domain.workout.PreviousPerformance
import com.homestrength.ui.components.NumberStepper
import com.homestrength.ui.components.ResistancePickerDialog
import com.homestrength.ui.components.RestTimerDialog
import com.homestrength.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel,
    onFinish: () -> Unit,
    onBack: () -> Unit,
    onOpenExercise: (Long) -> Unit
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val previous by viewModel.previousByExerciseId.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestionsByExerciseId.collectAsStateWithLifecycle()
    val combinations by viewModel.combinations.collectAsStateWithLifecycle()
    val defaultRest by viewModel.defaultRestSeconds.collectAsStateWithLifecycle()
    var pickingLogId by remember { mutableStateOf<Long?>(null) }
    var showRestTimer by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val s = session?.session
                    Text(if (s == null) "训练中" else "全身 ${s.workoutType.name}")
                },
                navigationIcon = {
                    TextButton(onClick = { confirmLeave = true }) { Text("离开") }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = session != null
            ) {
                Text("完成训练", style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { padding ->
        val data = session
        if (data == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) { Text("加载中…") }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "今天状态：${modeLabel(data.session.trainingMode)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            data.logs.sortedBy { it.log.sortOrder }.forEach { log ->
                ExerciseBlock(
                    logWithSets = log,
                    previous = previous[log.exercise.id],
                    suggestion = suggestions[log.exercise.id],
                    onOpenDetail = { onOpenExercise(log.exercise.id) },
                    onValueChange = { setId, value, unit, filledSet ->
                        viewModel.updateSetValue(setId, value, unit)
                        if (filledSet) showRestTimer = true
                    },
                    onPickResistance = { pickingLogId = log.log.id },
                    onApplySuggestedResistance = {
                        viewModel.applySuggestedResistance(log.log.id, log.exercise.id)
                    },
                    onSkip = { viewModel.skipExercise(log.log.id) },
                    onUnskip = { viewModel.unskipExercise(log.log.id) }
                )
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }

    val picking = pickingLogId
    if (picking != null) {
        val currentResistance = session?.logs
            ?.firstOrNull { it.log.id == picking }
            ?.sets
            ?.firstOrNull()
            ?.set
            ?.totalResistance
            ?: 0
        ResistancePickerDialog(
            combinations = combinations,
            selectedTotal = currentResistance,
            onSelect = { combo ->
                viewModel.applyResistance(picking, combo)
                pickingLogId = null
            },
            onDismiss = { pickingLogId = null }
        )
    }

    if (showRestTimer) {
        RestTimerDialog(
            initialSeconds = defaultRest,
            onDismiss = { showRestTimer = false }
        )
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("离开训练？") },
            text = { Text("已记录的组数会自动保存，可稍后继续未完成训练。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLeave = false
                        onBack()
                    }
                ) { Text("离开") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text("留下") }
            }
        )
    }
}

@Composable
private fun ExerciseBlock(
    logWithSets: ExerciseLogWithSets,
    previous: PreviousPerformance?,
    suggestion: ProgressionSuggestion?,
    onOpenDetail: () -> Unit,
    onValueChange: (Long, Int?, TargetUnit, filledSet: Boolean) -> Unit,
    onPickResistance: () -> Unit,
    onApplySuggestedResistance: () -> Unit,
    onSkip: () -> Unit,
    onUnskip: () -> Unit
) {
    val exercise = logWithSets.exercise
    val skipped = logWithSets.log.skipped
    val unit = exercise.targetUnit
    val unitLabel = when (unit) {
        TargetUnit.REPS -> "次"
        TargetUnit.SECONDS -> "秒"
    }
    val currentResistance = logWithSets.sets.firstOrNull()?.set?.totalResistance ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenDetail)
            ) {
                Text(exercise.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "目标 ${exercise.targetMin}–${exercise.targetMax} $unitLabel" +
                        if (exercise.isUnilateral) " / 每侧" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { if (skipped) onUnskip() else onSkip() }) {
                Text(if (skipped) "取消跳过" else "跳过")
            }
        }

        if (previous != null) {
            Text("上次  ${previous.summaryLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (suggestion != null) {
            SuggestionBlock(
                suggestion = suggestion,
                currentResistance = currentResistance,
                onApplyResistance = onApplySuggestedResistance
            )
        }

        OutlinedButton(onClick = onPickResistance, modifier = Modifier.fillMaxWidth()) {
            Text(if (currentResistance > 0) "阻力  $currentResistance LB" else "阻力  0 LB（自重/未选）")
        }

        if (skipped) {
            Text("已跳过", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }

        val sets = logWithSets.sets.sortedWith(
            compareBy<SetWithBands> { it.set.setNumber }
                .thenBy { it.set.side?.ordinal ?: -1 }
        )

        if (exercise.isUnilateral) {
            val byNumber = sets.groupBy { it.set.setNumber }
            byNumber.keys.sorted().forEach { setNumber ->
                val sides = byNumber[setNumber].orEmpty()
                Text("第 $setNumber 组", style = MaterialTheme.typography.titleMedium)
                sides.forEach { sw ->
                    val sideLabel = when (sw.set.side) {
                        SetSide.LEFT -> "左侧"
                        SetSide.RIGHT -> "右侧"
                        null -> ""
                    }
                    SetRow(
                        label = sideLabel,
                        value = valueOf(sw, unit),
                        unit = unit,
                        onChange = { newValue ->
                            val old = valueOf(sw, unit)
                            val filled = old == null && newValue != null && newValue > 0
                            onValueChange(sw.set.id, newValue, unit, filled)
                        }
                    )
                }
            }
        } else {
            sets.forEach { sw ->
                SetRow(
                    label = "第 ${sw.set.setNumber} 组",
                    value = valueOf(sw, unit),
                    unit = unit,
                    onChange = { newValue ->
                        val old = valueOf(sw, unit)
                        val filled = old == null && newValue != null && newValue > 0
                        onValueChange(sw.set.id, newValue, unit, filled)
                    }
                )
            }
        }
    }
}

@Composable
private fun SuggestionBlock(
    suggestion: ProgressionSuggestion,
    currentResistance: Int,
    onApplyResistance: () -> Unit
) {
    val color = when (suggestion.type) {
        ProgressionType.ADD_RESISTANCE -> MaterialTheme.colorScheme.primary
        ProgressionType.CONSIDER_LOWER -> StatusColors.caution
        ProgressionType.FIRST_TIME -> MaterialTheme.colorScheme.onSurfaceVariant
        ProgressionType.ADD_REPS -> MaterialTheme.colorScheme.onSurface
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = when (suggestion.type) {
                ProgressionType.FIRST_TIME -> suggestion.message
                ProgressionType.ADD_RESISTANCE -> "建议增加阻力"
                else -> "建议"
            },
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )

        when (suggestion.type) {
            ProgressionType.FIRST_TIME -> {
                Text(suggestion.suggestedValuesLabel, color = color)
                suggestion.detailMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            ProgressionType.ADD_RESISTANCE -> {
                Text(
                    "上次已满组 · 下一档 ${suggestion.suggestedResistance} LB",
                    color = color
                )
                Text(
                    "建议目标 ${suggestion.suggestedValuesLabel}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentResistance != suggestion.suggestedResistance) {
                    TextButton(onClick = onApplyResistance) {
                        Text("应用 ${suggestion.suggestedResistance} LB")
                    }
                }
            }
            ProgressionType.ADD_REPS -> {
                Text(
                    "${suggestion.suggestedResistance} LB · ${suggestion.suggestedValuesLabel}",
                    color = color
                )
                suggestion.detailMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            ProgressionType.CONSIDER_LOWER -> {
                Text(
                    "${suggestion.suggestedResistance} LB · ${suggestion.suggestedValuesLabel}",
                    color = color
                )
                suggestion.detailMessage?.let { Text(it, color = color) }
            }
        }
    }
}

@Composable
private fun SetRow(
    label: String,
    value: Int?,
    unit: TargetUnit,
    onChange: (Int?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        NumberStepper(
            value = value,
            onValueChange = onChange,
            min = 0,
            max = if (unit == TargetUnit.SECONDS) 600 else 100
        )
    }
}

private fun valueOf(sw: SetWithBands, unit: TargetUnit): Int? =
    when (unit) {
        TargetUnit.REPS -> sw.set.reps
        TargetUnit.SECONDS -> sw.set.durationSeconds
    }

private fun modeLabel(mode: TrainingMode): String = when (mode) {
    TrainingMode.NORMAL -> "正常"
    TrainingMode.TIRED -> "有点累"
    TrainingMode.EXHAUSTED -> "极度疲惫"
}
