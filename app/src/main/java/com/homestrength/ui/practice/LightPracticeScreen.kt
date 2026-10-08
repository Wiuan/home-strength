package com.homestrength.ui.practice

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.secondsToPracticedMinutes
import com.homestrength.ui.components.CompactPageHeader
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.WeChatCell
import com.homestrength.ui.components.WeChatGroup
import com.homestrength.ui.components.WeChatGroupLabel
import com.homestrength.ui.components.WeChatInsetDivider
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun LightPracticeScreen(
    track: PracticeTrack,
    traineeRepository: TraineeRepository,
    powerListItemId: Long = 0L,
    targetSeconds: Int = 0,
    titleHint: String = "",
    onDone: (fans: Int, coins: Int) -> Unit,
    onBack: () -> Unit,
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by traineeRepository.observeProfile().collectAsStateWithLifecycle(
        initialValue = TraineeProfileEntity()
    )
    var practiceId by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(true) }
    var countdownMode by remember { mutableStateOf(targetSeconds > 0) }
    var remaining by remember { mutableIntStateOf(targetSeconds.coerceAtLeast(0)) }
    var endAtElapsed by remember {
        mutableLongStateOf(
            if (targetSeconds > 0) {
                SystemClock.elapsedRealtime() + targetSeconds * 1000L
            } else {
                0L
            }
        )
    }
    var elapsed by remember { mutableIntStateOf(0) }
    var pausedAccumulatedMs by remember { mutableLongStateOf(0L) }
    var pauseStartedAt by remember { mutableLongStateOf(0L) }
    var sessionStartedAt by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var note by remember { mutableStateOf("") }
    var noteTitle by remember {
        mutableStateOf(titleHint.takeIf { it.isNotBlank() } ?: "")
    }
    var saving by remember { mutableStateOf(false) }
    var finishedMessage by remember { mutableStateOf<String?>(null) }
    var alarmVisible by remember { mutableStateOf(false) }
    var alarmPlayer by remember { mutableStateOf<PracticeAlarmPlayer?>(null) }

    LaunchedEffect(track, powerListItemId) {
        runCatching { traineeRepository.startLightPractice(track, powerListItemId) }
            .onSuccess { practiceId = it }
            .onFailure { onError(it.message ?: "无法开始练习") }
        if (powerListItemId > 0L && titleHint.isBlank()) {
            traineeRepository.getPowerItem(powerListItemId)?.let { item ->
                if (noteTitle.isBlank()) noteTitle = item.title
            }
        }
    }

    LaunchedEffect(running, practiceId, countdownMode, endAtElapsed) {
        if (!running || practiceId == 0L) return@LaunchedEffect
        while (isActive && running) {
            if (countdownMode) {
                val leftMs = endAtElapsed - SystemClock.elapsedRealtime()
                val left = ((leftMs + 999) / 1000).toInt().coerceAtLeast(0)
                remaining = left
                val now = SystemClock.elapsedRealtime()
                val pauseExtra = if (pauseStartedAt > 0L) now - pauseStartedAt else 0L
                elapsed = ((now - sessionStartedAt - pausedAccumulatedMs - pauseExtra) / 1000)
                    .toInt()
                    .coerceAtLeast(0)
                if (leftMs <= 0L) {
                    remaining = 0
                    running = false
                    alarmVisible = true
                    break
                }
            } else {
                delay(200)
                val now = SystemClock.elapsedRealtime()
                val pauseExtra = if (pauseStartedAt > 0L) now - pauseStartedAt else 0L
                elapsed = ((now - sessionStartedAt - pausedAccumulatedMs - pauseExtra) / 1000)
                    .toInt()
                    .coerceAtLeast(0)
            }
            delay(200)
        }
    }

    DisposableEffect(alarmVisible) {
        if (alarmVisible) {
            val player = PracticeAlarmPlayer(context).also { it.start() }
            alarmPlayer = player
            onDispose {
                player.stop()
                alarmPlayer = null
            }
        } else {
            onDispose { }
        }
    }

    fun stopAlarm() {
        alarmPlayer?.stop()
        alarmPlayer = null
        alarmVisible = false
    }

    fun extendCountdown(extraSeconds: Int) {
        stopAlarm()
        countdownMode = true
        endAtElapsed = SystemClock.elapsedRealtime() + extraSeconds * 1000L
        remaining = extraSeconds
        running = true
        pauseStartedAt = 0L
    }

    fun switchToUnlimited() {
        stopAlarm()
        countdownMode = false
        remaining = 0
        running = true
        pauseStartedAt = 0L
    }

    fun togglePause() {
        if (running) {
            running = false
            pauseStartedAt = SystemClock.elapsedRealtime()
            if (countdownMode) {
                remaining = ((endAtElapsed - SystemClock.elapsedRealtime() + 999) / 1000)
                    .toInt()
                    .coerceAtLeast(0)
            }
        } else {
            val pausedFor = SystemClock.elapsedRealtime() - pauseStartedAt
            pausedAccumulatedMs += pausedFor
            if (countdownMode) {
                endAtElapsed = SystemClock.elapsedRealtime() + remaining * 1000L
            }
            pauseStartedAt = 0L
            running = true
        }
    }

    fun completePractice() {
        if (saving || practiceId == 0L || finishedMessage != null) return
        stopAlarm()
        saving = true
        running = false
        scope.launch {
            runCatching {
                traineeRepository.completeLightPractice(
                    id = practiceId,
                    durationSeconds = elapsed,
                    note = note,
                    noteTitle = noteTitle,
                    powerListItemId = powerListItemId
                )
            }.onSuccess { result ->
                saving = false
                if (result != null) {
                    finishedMessage =
                        "已记录 · +${result.fansEarned} 粉 · +${result.coinsEarned} 币" +
                            " · ${secondsToPracticedMinutes(result.durationSeconds)} 分钟"
                    onDone(result.fansEarned, result.coinsEarned)
                }
            }.onFailure {
                saving = false
                onError(it.message ?: "保存失败")
            }
        }
    }

    val trackLabel = TraineeRepository.trackLabel(track)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )
    val displaySeconds = if (countdownMode) remaining else elapsed
    val minutesLabel = secondsToPracticedMinutes(elapsed)
    val subtitle = buildString {
        append(trackLabel)
        if (noteTitle.isNotBlank()) append(" · ").append(noteTitle)
        if (targetSeconds > 0) append(" · 目标 ${targetSeconds / 60} 分")
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
                title = "轻轨道练习",
                subtitle = subtitle,
                onBack = onBack
            )

            WeChatGroupLabel(if (countdownMode) "倒计时" else "正计时")
            WeChatGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        formatDuration(displaySeconds),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "本次累计 $minutesLabel 分钟",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                WeChatInsetDivider()
                WeChatCell(
                    title = if (running) "暂停" else "继续",
                    subtitle = if (running) "计时进行中" else "已暂停",
                    showChevron = false,
                    onClick = {
                        if (!alarmVisible && finishedMessage == null) togglePause()
                    }
                )
            }

            WeChatGroupLabel("备注")
            WeChatGroup {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { noteTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("笔记名（可选）") },
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text("一句备注") },
                        minLines = 2,
                        colors = fieldColors,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            WeChatGroupLabel("完成")
            WeChatGroup {
                WeChatCell(
                    title = when {
                        finishedMessage != null -> "已完成"
                        saving -> "保存中…"
                        else -> "完成练习"
                    },
                    subtitle = "+${profile.lightFans} 粉 · +${profile.lightCoins} 币",
                    showChevron = finishedMessage == null && !saving,
                    onClick = if (finishedMessage == null && !saving) {
                        { completePractice() }
                    } else {
                        null
                    }
                )
            }

            finishedMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (alarmVisible) {
        AlertDialog(
            onDismissRequest = { /* must tap a button */ },
            title = { Text("时间到") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("到点了。选一下怎么继续，或直接完成。")
                    Text(
                        "本次已累计 $minutesLabel 分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { extendCountdown(5 * 60) }) { Text("再学 5 分钟") }
                    TextButton(onClick = { extendCountdown(10 * 60) }) { Text("再学 10 分钟") }
                    TextButton(onClick = { switchToUnlimited() }) { Text("继续不限时") }
                    TextButton(onClick = { completePractice() }) { Text("完成练习") }
                }
            },
            dismissButton = {}
        )
    }
}

private fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%d:%02d".format(m, s)
}

/** Alarm-style ringtone + vibration until [stop]. */
private class PracticeAlarmPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    fun start() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(appContext, uri)?.also { tone ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                tone.isLooping = true
                tone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            tone.play()
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator?.let { vib ->
            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(pattern, 0)
            }
        }
    }

    fun stop() {
        ringtone?.stop()
        ringtone = null
        vibrator?.cancel()
        vibrator = null
    }
}
