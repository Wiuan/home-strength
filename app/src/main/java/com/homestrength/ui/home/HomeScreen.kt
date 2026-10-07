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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartWorkout: () -> Unit,
    onContinueWorkout: (Long) -> Unit,
    onOpenLightTrack: (PracticeTrack) -> Unit,
    onOpenWelfare: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit,
    onMessage: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var draftItem by remember { mutableStateOf("") }
    var celebratingGrade by remember { mutableStateOf<TraineeGrade?>(null) }
    val scope = rememberCoroutineScope()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )

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

            SoftCard(contentPadding = 12.dp) {
                SectionLabel("今天的练习清单")
                Text(
                    "最多 5 件 · 勾选 +${state.profile.checklistCoins} 币",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (state.powerList.isEmpty()) {
                    Text(
                        "还没有安排。选轨道，或写一条生活备注。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.powerList.forEachIndexed { index, item ->
                        if (index > 0) Spacer(Modifier.height(6.dp))
                        PowerRow(
                            item = item,
                            onToggle = { viewModel.togglePowerItem(item.id) },
                            onDelete = { viewModel.deletePowerItem(item.id) }
                        )
                    }
                }
                if (state.powerList.size < 5) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = draftItem,
                            onValueChange = { draftItem = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("晾衣服") },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp)
                        )
                        TextButton(
                            onClick = {
                                if (draftItem.isNotBlank()) {
                                    viewModel.addPowerItem(draftItem, PracticeTrack.LIFE)
                                    draftItem = ""
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) { Text("添加") }
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
                            viewModel.addPowerItem(TraineeRepository.trackLabel(track), track)
                            onOpenLightTrack(track)
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
private fun PowerRow(
    item: PowerListItemEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val done = item.status == PowerItemStatus.DONE
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuietCircleCheck(checked = done, onClick = onToggle)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (done) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )
            item.track?.let {
                Text(
                    TraineeRepository.trackLabel(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        TextButton(
            onClick = onDelete,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text("去掉", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
