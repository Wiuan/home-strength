package com.homestrength.ui.profile

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.PeriodGoalEntity
import com.homestrength.data.local.entity.PeriodGoalStatus
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.DayActivity
import com.homestrength.domain.trainee.MonthBucket
import com.homestrength.domain.trainee.MonthReview
import com.homestrength.domain.trainee.PeriodReview
import com.homestrength.domain.trainee.PlanPeriod
import com.homestrength.domain.trainee.PlanPeriodKind
import com.homestrength.domain.trainee.TrackShare
import com.homestrength.domain.trainee.TraineeGrade
import com.homestrength.ui.components.CompactPageHeader
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.SectionLabel
import com.homestrength.ui.components.WeChatGroup
import com.homestrength.ui.components.WeChatGroupLabel
import com.homestrength.ui.components.WeChatInsetDivider
import com.homestrength.ui.theme.Palette
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onMessage: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val grade = TraineeGrade.fromFans(state.profile.fans)
    val nextMin = TraineeGrade.progressInLevel(state.profile.fans).second
    val fraction = TraineeGrade.progressFraction(state.profile.fans)
    var showGradeHelp by remember { mutableStateOf(false) }
    var draftGoal by remember { mutableStateOf("") }
    var draftParts by remember { mutableStateOf("1") }
    var draftTrack by remember { mutableStateOf<PracticeTrack?>(PracticeTrack.LIFE) }
    var placeTodayGoalId by remember { mutableStateOf<Long?>(null) }
    var placeDurationMin by remember { mutableIntStateOf(0) }
    var placeCustomDuration by remember { mutableStateOf("") }
    val period = state.viewingPeriod
    val supportsParts = true
    val goalTracks = listOf(
        PracticeTrack.LIFE,
        PracticeTrack.CULTIVATION,
        PracticeTrack.ALGORITHM,
        PracticeTrack.VOCAL,
        PracticeTrack.STRENGTH
    )

    LaunchedEffect(Unit) { viewModel.refreshPeriodReview() }
    LaunchedEffect(state.message) {
        state.message?.let {
            onMessage(it)
            viewModel.consumeMessage()
        }
    }
    LaunchedEffect(period.key, period.kind) {
        draftGoal = ""
        draftParts = when (period.kind) {
            PlanPeriodKind.YEAR -> "12"
            PlanPeriodKind.QUARTER -> "3"
            PlanPeriodKind.MONTH -> "3"
        }
        draftTrack = PracticeTrack.LIFE
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
                title = "${state.profile.nickname} · 档案",
                subtitle = "计划与复盘",
                onBack = onBack
            )

            WeChatGroupLabel("当前年级")
            WeChatGroup {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(grade.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${state.profile.fans} 粉丝 · ${state.profile.coins} 币",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            TraineeGrade.letter(grade),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                .clickable { showGradeHelp = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
                Text(
                    text = if (nextMin == null) {
                        "已到最高阶 · 出道预备"
                    } else {
                        "距 ${TraineeGrade.fromFans(nextMin).label} 还差 ${nextMin - state.profile.fans} 粉丝"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }

            WeChatGroupLabel(period.planTitle())
            WeChatGroup {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    PeriodKindRow(
                        kind = period.kind,
                        onSelect = viewModel::setPeriodKind
                    )
                    Spacer(Modifier.height(4.dp))
                    PeriodPager(
                        period = period,
                        isCurrent = state.isCurrentPeriod,
                        onPrev = { viewModel.shiftPeriod(-1) },
                        onNext = { viewModel.shiftPeriod(1) },
                        onToday = viewModel::goToCurrentPeriod,
                        onPick = viewModel::goToPeriod
                    )
                    Text(
                        planHint(period, state.isCurrentPeriod),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    )
                }
                WeChatInsetDivider()
                if (state.periodGoals.isEmpty()) {
                    Text(
                        emptyPlanText(period.kind),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                } else {
                    state.periodGoals.forEachIndexed { index, goal ->
                        PeriodGoalRow(
                            goal = goal,
                            showParts = supportsParts || goal.targetParts > 1,
                            onPlaceToday = {
                                placeTodayGoalId = goal.id
                                placeDurationMin = 0
                                placeCustomDuration = ""
                            },
                            onBump = { viewModel.bumpGoalProgress(goal.id, it) },
                            onDone = { viewModel.markGoalDone(goal.id) },
                            onDrop = { viewModel.dropGoal(goal.id) },
                            onDelete = { viewModel.deleteGoal(goal.id) }
                        )
                        if (index < state.periodGoals.lastIndex) WeChatInsetDivider()
                    }
                }
                if (state.periodGoals.size < state.goalLimit) {
                    WeChatInsetDivider()
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BasicTextField(
                                value = draftGoal,
                                onValueChange = { draftGoal = it },
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
                                        if (draftGoal.isEmpty()) {
                                            Text(
                                                "写一条计划",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        inner()
                                    }
                                }
                            )
                            BasicTextField(
                                value = draftParts,
                                onValueChange = {
                                    draftParts = it.filter(Char::isDigit).take(3)
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .width(48.dp)
                                    .heightIn(min = 36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 8.dp),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (draftParts.isEmpty()) {
                                            Text(
                                                "份",
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
                                        if (draftGoal.isNotBlank()) {
                                            val parts = draftParts.toIntOrNull() ?: 1
                                            viewModel.addPeriodGoal(
                                                title = draftGoal,
                                                targetParts = parts,
                                                track = draftTrack
                                            )
                                            draftGoal = ""
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            goalTracks.forEach { track ->
                                FilterChip(
                                    selected = draftTrack == track,
                                    onClick = { draftTrack = track },
                                    label = {
                                        Text(
                                            TraineeRepository.trackLabel(track),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }
                    }
                }
            }

            placeTodayGoalId?.let { goalId ->
                PlaceTodayDurationDialog(
                    onDismiss = { placeTodayGoalId = null },
                    durationMin = placeDurationMin,
                    customDuration = placeCustomDuration,
                    onDurationMin = {
                        placeDurationMin = it
                        placeCustomDuration = ""
                    },
                    onCustomDuration = {
                        placeCustomDuration = it.filter(Char::isDigit).take(3)
                        if (placeCustomDuration.isNotBlank()) placeDurationMin = -1
                    },
                    onConfirm = {
                        val minutes = when {
                            placeCustomDuration.isNotBlank() ->
                                placeCustomDuration.toIntOrNull() ?: 0
                            placeDurationMin < 0 -> 0
                            else -> placeDurationMin
                        }
                        viewModel.placeGoalIntoToday(goalId, minutes.coerceAtLeast(0) * 60)
                        placeTodayGoalId = null
                    }
                )
            }

            WeChatGroupLabel(period.rhythmTitle())
            WeChatGroup {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    val review = state.periodReview
                    Text(
                        buildString {
                            append(period.label())
                            append(" · 有练 ${review.activeDays} 天")
                            if (review.strengthDays != review.activeDays) {
                                append("（力量 ${review.strengthDays}）")
                            }
                            append(" · ${review.totalDone} 件")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    val progressDenom = when (period.kind) {
                        PlanPeriodKind.MONTH ->
                            review.monthReview?.month?.lengthOfMonth()?.coerceAtLeast(1) ?: 30
                        PlanPeriodKind.QUARTER -> 90
                        PlanPeriodKind.YEAR -> 365
                    }
                    LinearProgressIndicator(
                        progress = { (review.activeDays.toFloat() / progressDenom).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                    if (state.isCurrentPeriod && period.kind == PlanPeriodKind.MONTH) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "本周 力量${state.weekStrengthCount} · 轻轨${state.weekLightCount} · 清单${state.weekChecklistDone}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    when (period.kind) {
                        PlanPeriodKind.MONTH -> {
                            val monthReview = review.monthReview
                            if (monthReview != null) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "每日完成",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                MonthBars(
                                    review = monthReview,
                                    selected = state.selectedDay,
                                    onSelect = viewModel::selectDay
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "月历",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                MonthCalendar(
                                    review = monthReview,
                                    selected = state.selectedDay,
                                    onSelect = viewModel::selectDay
                                )
                                Spacer(Modifier.height(6.dp))
                                SelectedDayDetail(activity = monthReview.activityOn(state.selectedDay))
                            }
                        }
                        PlanPeriodKind.QUARTER,
                        PlanPeriodKind.YEAR -> {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "各月力量天数",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "柱高=该月力量练过几天 · 点月进入日节奏",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            MonthBucketBars(
                                buckets = review.monthBuckets,
                                valueOf = { it.strengthDays },
                                onSelectMonth = viewModel::goToMonth
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "时长占比",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    TrackSharePie(shares = review.trackShares, emptyHint = emptyShareText(period.kind))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showGradeHelp) {
        AlertDialog(
            onDismissRequest = { showGradeHelp = false },
            title = { Text("年级门槛") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TraineeGrade.entries.forEach { g ->
                        val reached = state.profile.fans >= g.minFans
                        Text(
                            "${TraineeGrade.letter(g)}  ${g.label}  ·  ${g.minFans}+ 粉丝",
                            color = if (reached) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGradeHelp = false }) { Text("知道了") }
            }
        )
    }
}

private fun planHint(period: PlanPeriod, isCurrent: Boolean): String = when {
    !isCurrent -> "在看 ${period.label()} · 可回看计划与节奏。"
    period.kind == PlanPeriodKind.MONTH -> "写 3～5 条，填份数与轨道，再拆进今天清单。"
    period.kind == PlanPeriodKind.QUARTER -> "写本季重点，填份数后用 +/− 记进度（如 3/3）。"
    else -> "写年度方向，填份数后用 +/− 记进度（如读 12 本 → 1/12）。"
}

private fun emptyPlanText(kind: PlanPeriodKind): String = when (kind) {
    PlanPeriodKind.MONTH -> "这个月还没有计划"
    PlanPeriodKind.QUARTER -> "这个季度还没有计划"
    PlanPeriodKind.YEAR -> "这一年还没有计划"
}

private fun emptyShareText(kind: PlanPeriodKind): String = when (kind) {
    PlanPeriodKind.MONTH -> "这个月还没有可统计的时长"
    PlanPeriodKind.QUARTER -> "这个季度还没有可统计的时长"
    PlanPeriodKind.YEAR -> "这一年还没有可统计的时长"
}

@Composable
private fun PeriodKindRow(
    kind: PlanPeriodKind,
    onSelect: (PlanPeriodKind) -> Unit
) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
    )
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(
            PlanPeriodKind.MONTH to "月",
            PlanPeriodKind.QUARTER to "季",
            PlanPeriodKind.YEAR to "年"
        ).forEach { (k, label) ->
            FilterChip(
                selected = kind == k,
                onClick = { onSelect(k) },
                label = { Text(label) },
                colors = chipColors,
                modifier = Modifier.height(30.dp)
            )
        }
    }
}

@Composable
private fun PeriodPager(
    period: PlanPeriod,
    isCurrent: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPick: (PlanPeriod) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onPrev,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(28.dp)
        ) { Text("‹") }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                period.label(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { showPicker = true }
            )
            TextButton(
                onClick = { showPicker = true },
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) { Text("选") }
            if (!isCurrent) {
                TextButton(
                    onClick = onToday,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) { Text("今天") }
            }
        }
        TextButton(
            onClick = onNext,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(28.dp)
        ) { Text("›") }
    }
    if (showPicker) {
        PeriodPickerDialog(
            initial = period,
            onDismiss = { showPicker = false },
            onConfirm = {
                onPick(it)
                showPicker = false
            }
        )
    }
}

@Composable
private fun PeriodPickerDialog(
    initial: PlanPeriod,
    onDismiss: () -> Unit,
    onConfirm: (PlanPeriod) -> Unit
) {
    var yearText by remember(initial) { mutableStateOf(initial.year.toString()) }
    var monthValue by remember(initial) { mutableIntStateOf(initial.month.coerceIn(1, 12)) }
    var quarter by remember(initial) { mutableIntStateOf(initial.quarter.coerceIn(1, 4)) }
    var yearError by remember { mutableStateOf<String?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )

    fun parsedYear(): Int? {
        val y = yearText.trim().toIntOrNull() ?: return null
        return y.takeIf { it in 1900..2200 }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (initial.kind) {
                    PlanPeriodKind.MONTH -> "选择年月"
                    PlanPeriodKind.QUARTER -> "选择季度"
                    PlanPeriodKind.YEAR -> "选择年份"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = {
                        yearText = it.filter(Char::isDigit).take(4)
                        yearError = null
                    },
                    label = { Text("年份") },
                    placeholder = { Text("例如 2026") },
                    singleLine = true,
                    isError = yearError != null,
                    supportingText = yearError?.let { { Text(it) } },
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                when (initial.kind) {
                    PlanPeriodKind.MONTH -> {
                        Text("月份", style = MaterialTheme.typography.labelLarge)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..12).chunked(3).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    row.forEach { m ->
                                        FilterChip(
                                            selected = monthValue == m,
                                            onClick = { monthValue = m },
                                            modifier = Modifier.weight(1f),
                                            label = {
                                                Text(
                                                    "${m}月",
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        )
                                    }
                                    repeat(3 - row.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    PlanPeriodKind.QUARTER -> {
                        Text("季度", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..4).forEach { q ->
                                FilterChip(
                                    selected = quarter == q,
                                    onClick = { quarter = q },
                                    label = { Text("Q$q") }
                                )
                            }
                        }
                    }
                    PlanPeriodKind.YEAR -> Unit
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val year = parsedYear()
                    if (year == null) {
                        yearError = "请输入 1900–2200 的年份"
                        return@TextButton
                    }
                    onConfirm(
                        when (initial.kind) {
                            PlanPeriodKind.MONTH -> PlanPeriod.month(YearMonth.of(year, monthValue))
                            PlanPeriodKind.QUARTER -> PlanPeriod.quarter(year, quarter)
                            PlanPeriodKind.YEAR -> PlanPeriod.year(year)
                        }
                    )
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PlaceTodayDurationDialog(
    onDismiss: () -> Unit,
    durationMin: Int,
    customDuration: String,
    onDurationMin: (Int) -> Unit,
    onCustomDuration: (String) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("放进今天清单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "选本次时长。像「阅读 20 页」可不设时长（只勾选或不限时）。",
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 10, 15, 25).forEach { min ->
                        FilterChip(
                            selected = durationMin == min && customDuration.isBlank(),
                            onClick = { onDurationMin(min) },
                            label = {
                                Text(if (min == 0) "不限时/只勾选" else "${min}分")
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = customDuration,
                    onValueChange = onCustomDuration,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("自定义分钟") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("放进今天") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PeriodGoalRow(
    goal: PeriodGoalEntity,
    showParts: Boolean,
    onPlaceToday: () -> Unit,
    onBump: (Int) -> Unit,
    onDone: () -> Unit,
    onDrop: () -> Unit,
    onDelete: () -> Unit
) {
    val done = goal.status == PeriodGoalStatus.DONE
    val dropped = goal.status == PeriodGoalStatus.DROPPED
    val target = goal.targetParts.coerceAtLeast(1)
    val progress = goal.doneParts.coerceIn(0, target)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    textDecoration = when {
                        done || dropped -> TextDecoration.LineThrough
                        else -> null
                    },
                    color = if (done || dropped) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                goal.track?.let {
                    Text(
                        TraineeRepository.trackLabel(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showParts) {
                Text(
                    "$progress/$target",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (showParts && target > 1 && !dropped) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress.toFloat() / target },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        }
        if (goal.status == PeriodGoalStatus.ACTIVE || (done && showParts && target > 1)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showParts && target > 1) {
                    TextButton(
                        onClick = { onBump(-1) },
                        enabled = progress > 0,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("−") }
                    TextButton(
                        onClick = { onBump(1) },
                        enabled = progress < target,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("+") }
                }
                if (goal.status == PeriodGoalStatus.ACTIVE) {
                    TextButton(
                        onClick = onPlaceToday,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("今天") }
                    if (!showParts || target <= 1) {
                        TextButton(
                            onClick = onDone,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) { Text("完成") }
                    }
                    TextButton(
                        onClick = onDrop,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("放下") }
                } else {
                    Text(
                        "已完成",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) { Text("删") }
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    if (done) "已完成" else "已放下",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onDelete,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) { Text("删") }
            }
        }
    }
}

@Composable
private fun MonthBucketBars(
    buckets: List<MonthBucket>,
    valueOf: (MonthBucket) -> Int = { it.totalDone },
    onSelectMonth: (YearMonth) -> Unit
) {
    if (buckets.isEmpty()) {
        Text("还没有可统计的月份", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val max = (buckets.maxOfOrNull { valueOf(it) } ?: 1).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val mute = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = buckets.size.coerceAtLeast(1)
                val gap = 6.dp.toPx()
                val barWidth = (size.width - gap * (n - 1)) / n
                buckets.forEachIndexed { index, bucket ->
                    val value = valueOf(bucket)
                    val h = if (value == 0) {
                        4.dp.toPx()
                    } else {
                        (size.height * value / max).coerceAtLeast(10.dp.toPx())
                    }
                    val x = index * (barWidth + gap)
                    drawRoundRect(
                        color = if (value > 0) barColor else mute,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                buckets.forEach { bucket ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { onSelectMonth(bucket.month) }
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            buckets.forEach { bucket ->
                Text(
                    "${bucket.month.monthValue}月·${valueOf(bucket)}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MonthBars(
    review: MonthReview,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val max = (review.days.maxOfOrNull { it.doneCount } ?: 1).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val mute = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val selectedColor = MaterialTheme.colorScheme.secondary

    Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val n = review.days.size.coerceAtLeast(1)
            val gap = 2.dp.toPx()
            val barWidth = (size.width - gap * (n - 1)) / n
            review.days.forEachIndexed { index, day ->
                val h = if (day.doneCount == 0) {
                    3.dp.toPx()
                } else {
                    (size.height * day.doneCount / max).coerceAtLeast(6.dp.toPx())
                }
                val x = index * (barWidth + gap)
                val color = when {
                    day.day == selected -> selectedColor
                    day.doneCount > 0 -> barColor
                    else -> mute
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }
        Row(modifier = Modifier.fillMaxSize()) {
            review.days.forEach { day ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSelect(day.day) }
                )
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    review: MonthReview,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val first = review.month.atDay(1)
    val lead = (first.dayOfWeek.value + 6) % 7
    val cells = buildList {
        repeat(lead) { add(null as LocalDate?) }
        review.days.forEach { add(it.day) }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { w ->
            Text(
                w,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    Spacer(Modifier.height(6.dp))
    cells.chunked(7).forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            week.forEach { day ->
                if (day == null) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                } else {
                    val activity = review.activityOn(day)
                    DayCell(
                        day = day,
                        activity = activity,
                        selected = day == selected,
                        onClick = { onSelect(day) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            repeat(7 - week.size) {
                Spacer(Modifier.weight(1f).aspectRatio(1f))
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    activity: DayActivity,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                if (activity.doneCount > 0) {
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
            )
            .then(
                if (selected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${day.dayOfMonth}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            activity.tracks.take(3).forEach { track ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(trackColor(track))
                )
            }
        }
    }
}

@Composable
private fun SelectedDayDetail(activity: DayActivity) {
    SectionLabel("${activity.day.monthValue}月${activity.day.dayOfMonth}日")
    Spacer(Modifier.height(6.dp))
    if (activity.entries.isEmpty()) {
        Text("这一天还没有完成记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        activity.entries.forEach { entry ->
            Text("· ${entry.title}", style = MaterialTheme.typography.bodyMedium)
            if (entry.note.isNotBlank()) {
                Text(
                    entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, top = 1.dp, bottom = 2.dp)
                )
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun TrackSharePie(shares: List<TrackShare>, emptyHint: String) {
    if (shares.isEmpty()) {
        Text(emptyHint, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val total = shares.sumOf { it.seconds }.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(108.dp)) {
            val diameter = min(size.width, size.height)
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var start = -90f
            shares.forEach { share ->
                val sweep = 360f * share.seconds / total
                drawArc(
                    color = trackColor(share.track),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = arcSize
                )
                start += sweep
            }
            drawArc(
                color = Color.White.copy(alpha = 0.15f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 2.dp.toPx())
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            shares.forEach { share ->
                val pct = (100f * share.seconds / total).roundToIntSafe()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(trackColor(share.track))
                    )
                    Text(
                        "${share.label}  ${formatShareDuration(share.seconds)}  $pct%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun Float.roundToIntSafe(): Int = (this + 0.5f).toInt()

private fun formatShareDuration(totalSeconds: Int): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    return when {
        h > 0 -> "${h}h${m}m"
        m > 0 -> "${m} 分"
        else -> "${totalSeconds} 秒"
    }
}

private fun trackColor(track: PracticeTrack): Color = when (track) {
    PracticeTrack.STRENGTH -> Palette.Fatigue
    PracticeTrack.ALGORITHM -> Palette.Ink
    PracticeTrack.VOCAL -> Color(0xFF6B8FBF)
    PracticeTrack.CULTIVATION,
    PracticeTrack.READING,
    PracticeTrack.CALLIGRAPHY -> Palette.GoldDeep
    PracticeTrack.LIFE -> Palette.InkSoft
    PracticeTrack.SLEEP -> Color(0xFF7A6B9A)
    PracticeTrack.MEDITATION -> Color(0xFF5FA88A)
}
