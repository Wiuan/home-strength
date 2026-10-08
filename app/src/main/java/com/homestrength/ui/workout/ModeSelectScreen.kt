package com.homestrength.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.homestrength.ui.components.CompactPageHeader
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.WeChatCell
import com.homestrength.ui.components.WeChatGroup
import com.homestrength.ui.components.WeChatGroupLabel
import com.homestrength.ui.theme.StatusColors

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

    fun requestStart(mode: TrainingMode) {
        if (state.starting) return
        if (state.hasIncomplete) {
            pendingMode = mode
        } else {
            viewModel.start(mode, onStarted)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GoldWash(Modifier.fillMaxWidth().height(88.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            CompactPageHeader(
                title = "开始力量训练",
                subtitle = "全身 ${state.nextWorkoutType.name} · 按今天状态安排",
                onBack = onBack
            )

            if (state.hasIncomplete) {
                WeChatGroupLabel("未完成")
                WeChatGroup {
                    WeChatCell(
                        title = "继续未完成训练",
                        subtitle = "开始新训练将丢弃当前进度",
                        showDivider = false,
                        onClick = {
                            state.incompleteSessionId?.let(onContinueIncomplete)
                        }
                    )
                }
            }

            WeChatGroupLabel("今天状态")
            WeChatGroup {
                WeChatCell(
                    title = "正常",
                    subtitle = "完整动作 · 默认组数",
                    value = if (state.starting) "…" else null,
                    showDivider = true,
                    onClick = { requestStart(TrainingMode.NORMAL) }
                )
                WeChatCell(
                    title = "有点累",
                    subtitle = "3 个主要动作 · 默认组数",
                    showDivider = true,
                    onClick = { requestStart(TrainingMode.TIRED) }
                )
                WeChatCell(
                    title = "极度疲惫",
                    subtitle = "Push / Pull / Squat 各 1 组",
                    onClick = { requestStart(TrainingMode.EXHAUSTED) }
                )
            }

            Text(
                "保持习惯比完美更重要",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )

            state.error?.let {
                Text(
                    it,
                    color = StatusColors.fatigue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
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
