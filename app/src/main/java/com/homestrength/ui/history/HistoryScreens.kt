package com.homestrength.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.data.local.relation.SessionWithLogs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenDetail: (Long) -> Unit,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp)
            ) {
                Text("还没有完成的训练", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sessions, key = { it.session.id }) { session ->
                HistoryListItem(
                    session = session,
                    onClick = { onOpenDetail(session.session.id) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun HistoryListItem(
    session: SessionWithLogs,
    onClick: () -> Unit
) {
    val dateText = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(session.session.dateTime))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(dateText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("全身 ${session.session.workoutType.name}")
        session.session.feeling?.let { Text("★".repeat(it)) }
        Text(
            "${session.logs.count { !it.log.skipped }} 个动作",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    session: SessionWithLogs?,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("训练详情") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
                actions = {
                    if (session != null) {
                        TextButton(onClick = onDelete) { Text("删除") }
                    }
                }
            )
        }
    ) { padding ->
        if (session == null) {
            Text("未找到记录", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        val dateText = DateTimeFormatter.ofPattern("yyyy/MM/dd")
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(session.session.dateTime))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(dateText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("全身 ${session.session.workoutType.name}")
                session.session.feeling?.let { Text("★".repeat(it)) }
            }
            items(session.logs.sortedBy { it.log.sortOrder }, key = { it.log.id }) { log ->
                DetailExercise(log)
            }
        }
    }
}

@Composable
private fun DetailExercise(log: ExerciseLogWithSets) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(log.exercise.name, style = MaterialTheme.typography.titleLarge)
        if (log.log.skipped) {
            Text("已跳过", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        val resistance = log.sets.map { it.set.totalResistance }.firstOrNull { it > 0 } ?: 0
        if (resistance > 0) {
            Text("$resistance LB")
        }
        Text(formatSets(log), style = MaterialTheme.typography.titleMedium)
    }
}

private fun formatSets(log: ExerciseLogWithSets): String {
    val unit = log.exercise.targetUnit
    val sets = log.sets.sortedWith(compareBy({ it.set.setNumber }, { it.set.side?.ordinal ?: -1 }))
    return if (log.exercise.isUnilateral) {
        sets.groupBy { it.set.setNumber }.toSortedMap().map { (_, sides) ->
            val left = sides.firstOrNull { it.set.side == SetSide.LEFT }
            val right = sides.firstOrNull { it.set.side == SetSide.RIGHT }
            "${valueOf(left, unit)} / ${valueOf(right, unit)}"
        }.joinToString(" · ")
    } else {
        sets.joinToString(" / ") { valueOf(it, unit) }
    }
}

private fun valueOf(
    sw: com.homestrength.data.local.relation.SetWithBands?,
    unit: TargetUnit
): String {
    if (sw == null) return "—"
    return when (unit) {
        TargetUnit.REPS -> sw.set.reps?.toString() ?: "—"
        TargetUnit.SECONDS -> sw.set.durationSeconds?.let { "${it}s" } ?: "—"
    }
}
