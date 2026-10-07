package com.homestrength.ui.practice

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.HeroTitle
import com.homestrength.ui.components.SoftCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun LightPracticeScreen(
    track: PracticeTrack,
    traineeRepository: TraineeRepository,
    onDone: (fans: Int, coins: Int) -> Unit,
    onBack: () -> Unit,
    onError: (String) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val profile by traineeRepository.observeProfile().collectAsStateWithLifecycle(
        initialValue = TraineeProfileEntity()
    )
    var practiceId by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(true) }
    var elapsed by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    var noteTitle by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var finishedMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(track) {
        runCatching { traineeRepository.startLightPractice(track) }
            .onSuccess { practiceId = it }
            .onFailure { onError(it.message ?: "无法开始练习") }
    }

    LaunchedEffect(running, practiceId) {
        if (!running || practiceId == 0L) return@LaunchedEffect
        while (isActive && running) {
            delay(1000)
            elapsed += 1
        }
    }

    val title = TraineeRepository.trackLabel(track)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )

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
                leading = "每一点练习",
                accent = "都值得留下",
                subtitle = "$title · 深度仍在电脑笔记里"
            )

            SoftCard {
                Text(
                    formatDuration(elapsed),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { running = !running }) {
                    Text(if (running) "暂停" else "继续")
                }
            }

            SoftCard {
                OutlinedTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("笔记名（可选）") },
                    supportingText = { Text("方便回电脑找，不会打开文件") },
                    colors = fieldColors,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("一句备注") },
                    minLines = 2,
                    colors = fieldColors,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            finishedMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    if (saving || practiceId == 0L) return@Button
                    saving = true
                    running = false
                    scope.launch {
                        runCatching {
                            traineeRepository.completeLightPractice(
                                id = practiceId,
                                durationSeconds = elapsed,
                                note = note,
                                noteTitle = noteTitle
                            )
                        }.onSuccess { result ->
                            saving = false
                            if (result != null) {
                                finishedMessage =
                                    "已记录 · +${result.fansEarned} 粉丝 · +${result.coinsEarned} 星光币"
                                onDone(result.fansEarned, result.coinsEarned)
                            }
                        }.onFailure {
                            saving = false
                            onError(it.message ?: "保存失败")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !saving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background
                )
            ) {
                Text("完成练习", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "完成约 +${profile.lightFans} 粉丝 · +${profile.lightCoins} 币",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}
