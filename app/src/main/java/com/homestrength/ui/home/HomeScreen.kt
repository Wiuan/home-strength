package com.homestrength.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.TraineeGrade
import com.homestrength.ui.components.FireworksOverlay
import com.homestrength.ui.components.GoldRule
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.QuietCircleCheck
import com.homestrength.ui.components.SectionLabel
import com.homestrength.ui.components.SoftCard
import com.homestrength.ui.theme.Palette
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class LightPracticeNav(
    val track: PracticeTrack,
    val itemId: Long = 0L,
    val targetSeconds: Int = 0
)

private val ChecklistTracks = listOf(
    PracticeTrack.LIFE,
    PracticeTrack.CULTIVATION,
    PracticeTrack.ALGORITHM,
    PracticeTrack.VOCAL
)

private val DurationPresetsMinutes = listOf(0, 10, 15, 25)

/** Placeholder / blank-submit default for checklist items. */
internal fun defaultChecklistTitle(track: PracticeTrack, minutes: Int): String {
    val m = minutes.coerceAtLeast(0).let { if (it == 0 && track != PracticeTrack.LIFE) 10 else it }
    return when (track) {
        PracticeTrack.LIFE -> "打扫卫生"
        PracticeTrack.CULTIVATION -> "阅读 ${if (m > 0) m else 10} 分钟"
        PracticeTrack.ALGORITHM -> "算法${if (m > 0) m else 10}分钟"
        PracticeTrack.VOCAL -> "声乐${if (m > 0) m else 10}分钟"
        else -> TraineeRepository.trackLabel(track)
    }
}

private fun resolveDraftMinutes(draftDurationMin: Int, customDurationText: String): Int {
    return when {
        customDurationText.isNotBlank() -> customDurationText.toIntOrNull() ?: 0
        draftDurationMin < 0 -> 0
        else -> draftDurationMin
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartWorkout: () -> Unit,
    onContinueWorkout: (Long) -> Unit,
    onOpenLightPractice: (LightPracticeNav) -> Unit,
    onOpenWelfare: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onMessage: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var draftItem by remember { mutableStateOf("") }
    var draftTrack by remember { mutableStateOf(PracticeTrack.LIFE) }
    var draftDurationMin by remember { mutableStateOf(0) }
    var customDurationText by remember { mutableStateOf("") }
    var celebratingGrade by remember { mutableStateOf<TraineeGrade?>(null) }
    val scope = rememberCoroutineScope()
    val draftMinutes = resolveDraftMinutes(draftDurationMin, customDurationText)
    val titleHint = defaultChecklistTitle(draftTrack, draftMinutes)
    LaunchedEffect(viewModel) {
        viewModel.gradeUpgrade.collect { celebratingGrade = it }
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                state.greeting,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ProfileCard(
                nickname = state.profile.nickname,
                gradeLetter = state.gradeLetter,
                gradeLabel = state.gradeLabel,
                fans = state.profile.fans,
                coins = state.profile.coins,
                energy = state.profile.energy,
                onClick = onOpenProfile
            )

            SoftCard(contentPadding = 0.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "今天的练习清单",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${state.powerList.size}/5 · 勾选 +${state.profile.checklistCoins}币",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                if (state.powerList.isEmpty()) {
                    Text(
                        "点选轨道后直接添加，或改标题",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                } else {
                    state.powerList.forEachIndexed { index, item ->
                        PowerRow(
                            item = item,
                            onToggle = { viewModel.togglePowerItem(item.id) },
                            onDelete = { viewModel.deletePowerItem(item.id) },
                            onStart = {
                                val track = item.track ?: return@PowerRow
                                onOpenLightPractice(
                                    LightPracticeNav(
                                        track = TraineeRepository.canonicalTrack(track),
                                        itemId = item.id,
                                        targetSeconds = item.targetDurationSeconds
                                    )
                                )
                            }
                        )
                        if (index < state.powerList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 46.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                }
                if (state.powerList.size < 5) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ChecklistTracks.forEach { track ->
                                CompactChip(
                                    selected = draftTrack == track,
                                    label = TraineeRepository.trackLabel(track),
                                    onClick = {
                                        draftTrack = track
                                        if (track == PracticeTrack.LIFE) {
                                            draftDurationMin = 0
                                            customDurationText = ""
                                        } else if (
                                            draftDurationMin == 0 && customDurationText.isBlank()
                                        ) {
                                            draftDurationMin = 10
                                        }
                                    }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DurationPresetsMinutes.forEach { min ->
                                CompactChip(
                                    selected = draftDurationMin == min &&
                                        customDurationText.isBlank(),
                                    label = when {
                                        min == 0 && draftTrack == PracticeTrack.LIFE -> "只勾选"
                                        min == 0 -> "不限时"
                                        else -> "${min}分"
                                    },
                                    onClick = {
                                        draftDurationMin = min
                                        customDurationText = ""
                                    }
                                )
                            }
                            CompactMinuteField(
                                value = customDurationText,
                                onValueChange = {
                                    customDurationText = it.filter(Char::isDigit).take(3)
                                    if (customDurationText.isNotBlank()) draftDurationMin = -1
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BasicTextField(
                                value = draftItem,
                                onValueChange = { draftItem = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (draftItem.isEmpty()) {
                                            Text(
                                                titleHint,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            Text(
                                "添加",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val minutes = draftMinutes
                                        val title = draftItem.trim()
                                            .ifBlank { defaultChecklistTitle(draftTrack, minutes) }
                                        val seconds = if (
                                            draftTrack == PracticeTrack.LIFE && minutes <= 0
                                        ) {
                                            0
                                        } else if (minutes <= 0) {
                                            0
                                        } else {
                                            minutes * 60
                                        }
                                        viewModel.addPowerItem(title, draftTrack, seconds)
                                        draftItem = ""
                                        customDurationText = ""
                                        draftDurationMin =
                                            if (draftTrack == PracticeTrack.LIFE) 0 else 10
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SectionLabel("练习轨道")
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TrackPill("力量") {
                        viewModel.addPowerItem("力量训练", PracticeTrack.STRENGTH)
                        onStartWorkout()
                    }
                    listOf(
                        PracticeTrack.ALGORITHM,
                        PracticeTrack.VOCAL,
                        PracticeTrack.CULTIVATION
                    ).forEach { track ->
                        TrackPill(TraineeRepository.trackLabel(track)) {
                            onOpenLightPractice(LightPracticeNav(track = track))
                        }
                    }
                    val sleptToday = state.profile.sleepDate == LocalDate.now().toString()
                    TrackPill(if (sleptToday) "早睡 ✓" else "早睡") {
                        scope.launch {
                            if (sleptToday) {
                                viewModel.undoEarlySleep()
                                onMessage("已取消早睡打卡")
                            } else {
                                val err = viewModel.completeEarlySleep()
                                if (err != null) {
                                    onMessage(err)
                                } else {
                                    onMessage(
                                        "早睡打卡 · 元气 +${state.profile.sleepEnergyRestore}"
                                    )
                                }
                            }
                        }
                    }
                    TrackPill("冥想") {
                        scope.launch {
                            viewModel.completeMeditation()
                            onMessage(
                                "冥想 · 元气 +${state.profile.meditationEnergyRestore}"
                            )
                        }
                    }
                }
            }

            SoftCard(
                modifier = Modifier.clickable(onClick = onOpenWelfare),
                contentPadding = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        SectionLabel("福利社")
                        Text(
                            "${state.profile.coins} 星光币 · 点开兑换",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text("›", style = MaterialTheme.typography.titleLarge)
                }
            }

            SoftCard(contentPadding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("力量 · 今天")
                        Text(
                            text = if (state.isSuggestedTrainingDay) {
                                "全身 ${state.nextWorkoutType.name}"
                            } else {
                                "也可练全身 ${state.nextWorkoutType.name}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        WeeklyDots(completed = state.weeklyCompleted, goal = state.weeklyGoal)
                        Text(
                            "本周 ${state.weeklyCompleted}/${state.weeklyGoal}" +
                                (state.lastSession?.let { last ->
                                    val dateText = DateTimeFormatter.ofPattern("M/d")
                                        .withZone(ZoneId.systemDefault())
                                        .format(Instant.ofEpochMilli(last.session.dateTime))
                                    " · 上次 $dateText"
                                } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                val incomplete = state.incompleteSession
                if (incomplete != null) {
                    Button(
                        onClick = { onContinueWorkout(incomplete.session.id) },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) { Text("继续未完成") }
                    Spacer(Modifier.height(6.dp))
                }
                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    Text("开始力量训练", style = MaterialTheme.typography.titleSmall)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoldRule(20.dp)
                Spacer(Modifier.size(8.dp))
                TextButton(
                    onClick = onOpenHistory,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) { Text("历史") }
                TextButton(
                    onClick = onOpenSettings,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) { Text("设置") }
            }
            Spacer(Modifier.height(4.dp))
        }

        celebratingGrade?.let { grade ->
            FireworksOverlay(
                grade = grade,
                onFinished = { celebratingGrade = null }
            )
        }
    }
}

@Composable
private fun ProfileCard(
    nickname: String,
    gradeLetter: String,
    gradeLabel: String,
    fans: Int,
    coins: Int,
    energy: Int,
    onClick: () -> Unit
) {
    SoftCard(modifier = Modifier.clickable(onClick = onClick), contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel("练习生档案")
                Text(nickname, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "$fans 粉丝 · $coins 币 · 元气 $energy",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "点开看计划与复盘",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    gradeLetter,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    gradeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, style = MaterialTheme.typography.labelMedium)
        },
        modifier = Modifier.height(30.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            selectedLabelColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
private fun CompactMinuteField(
    value: String,
    onValueChange: (String) -> Unit
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .width(48.dp)
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        "分",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun PowerRow(
    item: PowerListItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onStart: () -> Unit
) {
    val done = item.status == PowerItemStatus.DONE
    val canStart = !done && when (item.track) {
        PracticeTrack.LIFE -> item.targetDurationSeconds > 0
        PracticeTrack.ALGORITHM,
        PracticeTrack.VOCAL,
        PracticeTrack.CULTIVATION,
        PracticeTrack.READING,
        PracticeTrack.CALLIGRAPHY -> true
        else -> false
    }
    val meta = buildList {
        item.track?.let { add(TraineeRepository.trackLabel(it)) }
        when {
            item.targetDurationSeconds > 0 -> add("${item.targetDurationSeconds / 60}分")
            item.track != null &&
                item.track != PracticeTrack.LIFE &&
                item.track != PracticeTrack.STRENGTH &&
                item.track != PracticeTrack.SLEEP &&
                item.track != PracticeTrack.MEDITATION -> add("不限时")
        }
        if (item.practicedMinutes > 0) add("已练${item.practicedMinutes}分")
    }.joinToString(" · ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuietCircleCheck(checked = done, onClick = onToggle)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
            if (meta.isNotBlank()) {
                Text(
                    meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        if (canStart) {
            Text(
                "开始",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onStart)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        Text(
            "去掉",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onDelete)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TrackPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun WeeklyDots(completed: Int, goal: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(goal.coerceAtLeast(1)) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (index < completed) MaterialTheme.colorScheme.primary
                        else Palette.Line
                    )
            )
        }
    }
}

fun workoutTypeLabel(type: WorkoutType): String = "全身 ${type.name}"
