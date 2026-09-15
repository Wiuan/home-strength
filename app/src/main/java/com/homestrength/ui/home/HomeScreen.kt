package com.homestrength.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.WorkoutType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartWorkout: () -> Unit,
    onContinueWorkout: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(state.greeting, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        Text("今天", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = if (state.isSuggestedTrainingDay) {
                "全身 ${state.nextWorkoutType.name}"
            } else {
                "今天休息 · 也可练全身 ${state.nextWorkoutType.name}"
            },
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )
        if (!state.isSuggestedTrainingDay) {
            Text(
                "散步 / 自由活动",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text("本周训练", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        WeeklyDots(completed = state.weeklyCompleted, goal = state.weeklyGoal)
        Text("目标 ${state.weeklyGoal} 次 / 周", style = MaterialTheme.typography.bodyMedium)

        AnimatedVisibility(
            visible = state.incompleteSession != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val incomplete = state.incompleteSession
            if (incomplete != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Text("有未完成训练", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("全身 ${incomplete.session.workoutType.name}")
                    Button(
                        onClick = { onContinueWorkout(incomplete.session.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("继续未完成训练")
                    }
                }
            }
        }

        HorizontalDivider()
        Text("上次训练", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        val last = state.lastSession
        if (last == null) {
            Text("还没有训练记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val dateText = DateTimeFormatter.ofPattern("M月d日")
                .withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(last.session.dateTime))
            Text("全身 ${last.session.workoutType.name}")
            Text(dateText)
            last.session.feeling?.let { feeling ->
                Text("★".repeat(feeling) + "☆".repeat(5 - feeling))
            }
            last.logs.sortedBy { it.log.sortOrder }.forEach { log ->
                if (log.log.skipped) return@forEach
                val values = log.sets
                    .sortedWith(compareBy({ it.set.setNumber }, { it.set.side?.name.orEmpty() }))
                    .mapNotNull { sw ->
                        sw.set.reps?.toString() ?: sw.set.durationSeconds?.let { "${it}s" }
                    }
                if (values.isNotEmpty()) {
                    val resistance = log.sets.map { it.set.totalResistance }.firstOrNull { it > 0 }
                    val suffix = if (resistance != null && resistance > 0) " · $resistance LB" else ""
                    Text("${log.exercise.name} ${values.joinToString(" / ")}$suffix")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onStartWorkout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("开始训练", style = MaterialTheme.typography.titleMedium)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onOpenHistory) { Text("历史") }
            TextButton(onClick = onOpenSettings) { Text("设置") }
        }
    }
}

@Composable
private fun WeeklyDots(completed: Int, goal: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(goal.coerceAtLeast(1)) { index ->
            Text(
                text = if (index < completed) "●" else "○",
                style = MaterialTheme.typography.headlineSmall,
                color = if (index < completed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

fun workoutTypeLabel(type: WorkoutType): String = "全身 ${type.name}"
