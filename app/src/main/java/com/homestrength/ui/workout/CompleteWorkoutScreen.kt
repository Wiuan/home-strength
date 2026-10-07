package com.homestrength.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.data.local.relation.SetWithBands
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.TraineeRewards
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.HeroTitle
import com.homestrength.ui.components.SectionLabel
import com.homestrength.ui.components.SoftCard
import kotlinx.coroutines.launch

@Composable
fun CompleteWorkoutScreen(
    repository: HomeStrengthRepository,
    sessionId: Long,
    onDone: () -> Unit,
    onError: (String) -> Unit = {},
    traineeRepository: TraineeRepository? = null
) {
    var session by remember { mutableStateOf<SessionWithLogs?>(null) }
    var feeling by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    var strengthFans by remember { mutableIntStateOf(TraineeRewards.STRENGTH_FANS) }
    var strengthCoins by remember { mutableIntStateOf(TraineeRewards.STRENGTH_COINS) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sessionId) {
        runCatching { repository.getSession(sessionId) }
            .onSuccess { session = it }
            .onFailure { onError(it.message ?: "加载训练失败") }
        traineeRepository?.getProfile()?.let {
            strengthFans = it.strengthFans
            strengthCoins = it.strengthCoins
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GoldWash(Modifier.fillMaxWidth().height(180.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HeroTitle(
                leading = "每一点进步",
                accent = "都值得鼓励",
                subtitle = "练习完成，成长被看见 · +$strengthFans 粉丝 · +$strengthCoins 币"
            )

            val data = session
            if (data == null) {
                SoftCard { Text("加载中…") }
            } else {
                SoftCard {
                    SectionLabel("本次摘要")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "全身 ${data.session.workoutType.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val activeLogs = data.logs.filter { !it.log.skipped }
                    val setCount = activeLogs.sumOf { log ->
                        log.sets.map { it.set.setNumber }.toSet().size
                    }
                    Text(
                        "${activeLogs.size} 个动作 · $setCount 组",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    data.logs.sortedBy { it.log.sortOrder }.forEach { log ->
                        if (log.log.skipped) {
                            Text(
                                "${log.exercise.name}  已跳过",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        Spacer(Modifier.height(6.dp))
                    }
                }

                SoftCard {
                    SectionLabel("本次训练感觉")
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { star ->
                            FilterChip(
                                selected = feeling == star,
                                onClick = { feeling = star },
                                label = { Text("$star") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !saving && session != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("完成", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

internal fun formatSetSummary(log: ExerciseLogWithSets): String {
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

private fun displayValue(sw: SetWithBands?, unit: TargetUnit): String {
    if (sw == null) return "—"
    return when (unit) {
        TargetUnit.REPS -> sw.set.reps?.toString() ?: "—"
        TargetUnit.SECONDS -> sw.set.durationSeconds?.let { "${it}s" } ?: "—"
    }
}
