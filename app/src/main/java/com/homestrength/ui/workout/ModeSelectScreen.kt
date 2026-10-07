package com.homestrength.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.HeroTitle
import com.homestrength.ui.components.SoftCard
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
        GoldWash(Modifier.fillMaxWidth().height(180.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextButton(onClick = onBack) { Text("返回") }
            HeroTitle(
                leading = "按今天的状态",
                accent = "安排有余量",
                subtitle = "全身 ${state.nextWorkoutType.name} · 保持习惯比完美更重要"
            )

            if (state.hasIncomplete) {
                SoftCard {
                    Text(
                        "有未完成训练。开始新训练将丢弃未完成进度。",
                        color = StatusColors.caution
                    )
                    Spacer(Modifier.height(10.dp))
                    state.incompleteSessionId?.let { id ->
                        Button(
                            onClick = { onContinueIncomplete(id) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text("继续未完成训练") }
                    }
                }
            }

            ModeCard(
                title = "正常",
                subtitle = "完整动作 × 默认组数",
                enabled = !state.starting,
                emphasized = true,
                onClick = { requestStart(TrainingMode.NORMAL) }
            )
            ModeCard(
                title = "有点累",
                subtitle = "3 个主要动作 × 默认组数",
                enabled = !state.starting,
                onClick = { requestStart(TrainingMode.TIRED) }
            )
            ModeCard(
                title = "极度疲惫",
                subtitle = "Push / Pull / Squat 各 1 组",
                enabled = !state.starting,
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
private fun ModeCard(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
    emphasized: Boolean = false
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (emphasized) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.surface
            )
            .border(
                1.dp,
                if (emphasized) MaterialTheme.colorScheme.onBackground
                else MaterialTheme.colorScheme.outline,
                shape
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (emphasized) {
                    MaterialTheme.colorScheme.background
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (emphasized) {
                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
