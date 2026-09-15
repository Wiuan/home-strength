package com.homestrength.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectScreen(
    viewModel: ModeSelectViewModel,
    onStarted: (Long) -> Unit,
    onContinueIncomplete: (Long) -> Unit,
    onBack: () -> Unit,
    onError: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pendingMode by remember { mutableStateOf<TrainingMode?>(null) }

    LaunchedEffect(state.error) {
        state.error?.let(onError)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("今天状态") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "全身 ${state.nextWorkoutType.name}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "选择今天的状态后开始。保持习惯比完美更重要。",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (state.hasIncomplete) {
                Text(
                    "有未完成训练。开始新训练将丢弃未完成进度。",
                    color = StatusColors.caution
                )
                state.incompleteSessionId?.let { id ->
                    OutlinedButton(
                        onClick = { onContinueIncomplete(id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("继续未完成训练")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            fun requestStart(mode: TrainingMode) {
                if (state.hasIncomplete) {
                    pendingMode = mode
                } else {
                    viewModel.start(mode, onStarted)
                }
            }

            ModeButton(
                title = "正常",
                subtitle = "完整动作 × 默认组数",
                enabled = !state.starting,
                onClick = { requestStart(TrainingMode.NORMAL) }
            )
            ModeButton(
                title = "有点累",
                subtitle = "3 个主要动作 × 默认组数",
                enabled = !state.starting,
                onClick = { requestStart(TrainingMode.TIRED) }
            )
            ModeButton(
                title = "极度疲惫",
                subtitle = "Push / Pull / Squat 各 1 组",
                enabled = !state.starting,
                outlined = true,
                onClick = { requestStart(TrainingMode.EXHAUSTED) }
            )

            state.error?.let {
                Text(it, color = StatusColors.fatigue)
            }
        }
    }

    pendingMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { pendingMode = null },
            title = { Text("丢弃未完成训练？") },
            text = { Text("开始新的训练会删除当前未完成记录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingMode = null
                        viewModel.start(mode, onStarted)
                    }
                ) { Text("开始新训练") }
            },
            dismissButton = {
                TextButton(onClick = { pendingMode = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ModeButton(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    outlined: Boolean = false
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(72.dp)
    when {
        outlined -> OutlinedButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
        else -> Button(onClick = onClick, enabled = enabled, modifier = modifier) {
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
