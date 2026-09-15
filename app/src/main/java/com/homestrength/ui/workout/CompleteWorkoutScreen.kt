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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.repository.HomeStrengthRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompleteWorkoutScreen(
    repository: HomeStrengthRepository,
    sessionId: Long,
    onDone: () -> Unit,
    onError: (String) -> Unit = {}
) {
    var session by remember { mutableStateOf<SessionWithLogs?>(null) }
    var feeling by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        session = repository.getSession(sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("训练完成") })
        }
    ) { padding ->
        val data = session
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("训练完成", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)

            if (data == null) {
                Text("加载中…")
                return@Column
            }

            Text("全身 ${data.session.workoutType.name}", style = MaterialTheme.typography.headlineSmall)

            val activeLogs = data.logs.filter { !it.log.skipped }
            val setCount = activeLogs.sumOf { log ->
                log.sets.map { it.set.setNumber }.toSet().size
            }
            Text("${activeLogs.size} 个动作 · $setCount 组")

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            data.logs.sortedBy { it.log.sortOrder }.forEach { log ->
                if (log.log.skipped) {
                    Text("${log.exercise.name}  已跳过", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        "${log.exercise.name}  ${formatSetSummary(log)}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    val resistance = log.sets.map { it.set.totalResistance }.firstOrNull { it > 0 } ?: 0
                    if (resistance > 0) {
                        Text("$resistance LB", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("本次训练感觉", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    FilterChip(
                        selected = feeling == star,
                        onClick = { feeling = star },
                        label = { Text("★".repeat(star)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    scope.launch {
                        runCatching {
                            repository.completeWorkout(sessionId, feeling)
                        }.onSuccess {
                            saving = false
                            onDone()
                        }.onFailure {
                            saving = false
                            onError(it.message ?: "保存训练失败")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !saving
            ) {
                Text("完成", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun formatSetSummary(log: com.homestrength.data.local.relation.ExerciseLogWithSets): String {
    val unit = log.exercise.targetUnit
    val sets = log.sets.sortedWith(
        compareBy({ it.set.setNumber }, { it.set.side?.ordinal ?: -1 })
    )
    if (log.exercise.isUnilateral) {
        val parts = sets.groupBy { it.set.setNumber }.toSortedMap().map { (_, sideSets) ->
            val left = sideSets.firstOrNull { it.set.side == SetSide.LEFT }
            val right = sideSets.firstOrNull { it.set.side == SetSide.RIGHT }
            "${displayValue(left, unit)} / ${displayValue(right, unit)}"
        }
        return parts.joinToString(" · ")
    }
    val values = sets.map { displayValue(it, unit) }
    return values.joinToString(" / ")
}

private fun displayValue(
    sw: com.homestrength.data.local.relation.SetWithBands?,
    unit: TargetUnit
): String {
    if (sw == null) return "—"
    return when (unit) {
        TargetUnit.REPS -> sw.set.reps?.toString() ?: "—"
        TargetUnit.SECONDS -> sw.set.durationSeconds?.let { "${it}s" } ?: "—"
    }
}
