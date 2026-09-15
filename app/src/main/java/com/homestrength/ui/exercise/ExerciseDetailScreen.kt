package com.homestrength.ui.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    viewModel: ExerciseDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val exercise = state.exercise

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exercise?.name ?: "动作详情") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Text("加载中…", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
        if (exercise == null) {
            Text("未找到动作", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(exercise.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "当前阻力  ${if (state.currentResistance > 0) "${state.currentResistance} LB" else "—"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text("最佳次数  ${state.bestLabel}", style = MaterialTheme.typography.titleMedium)
                if (exercise.description.isNotBlank()) {
                    Text(exercise.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("训练历史", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            if (state.history.isEmpty()) {
                item {
                    Text("还没有记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(state.history) { item ->
                    val date = DateTimeFormatter.ofPattern("M/d")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(item.dateMillis))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(date, style = MaterialTheme.typography.titleMedium)
                        Text(item.summaryLabel)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
