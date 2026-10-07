package com.homestrength.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.data.local.relation.SessionWithLogs
import com.homestrength.domain.trainee.MonthReviewBuilder
import com.homestrength.ui.components.CompactPageHeader
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.SectionLabel
import com.homestrength.ui.components.SoftCard
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onOpenDetail: (Long) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            CompactPageHeader(
                title = "力量历史",
                subtitle = "按月历看练过哪些天",
                onBack = onBack
            )

            SoftCard(contentPadding = 12.dp) {
                HistoryMonthPager(
                    month = state.viewingMonth,
                    isCurrent = state.isCurrentMonth,
                    onPrev = { viewModel.shiftMonth(-1) },
                    onNext = { viewModel.shiftMonth(1) },
                    onToday = viewModel::goToCurrentMonth,
                    onPickMonth = viewModel::goToMonth
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${state.viewingMonth.monthValue} 月练了 ${state.monthSessionCount} 次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                SectionLabel("月历")
                Spacer(Modifier.height(4.dp))
                StrengthMonthCalendar(
                    days = state.days,
                    selected = state.selectedDay,
                    onSelect = viewModel::selectDay
                )
                Spacer(Modifier.height(8.dp))
                SectionLabel(
                    "${state.selectedDay.monthValue}月${state.selectedDay.dayOfMonth}日"
                )
                Spacer(Modifier.height(4.dp))
                if (state.selectedSessions.isEmpty()) {
                    Text(
                        "这一天没有力量训练",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    state.selectedSessions.forEachIndexed { index, session ->
                        if (index > 0) Spacer(Modifier.height(6.dp))
                        DaySessionRow(
                            session = session,
                            onClick = { onOpenDetail(session.session.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HistoryMonthPager(
    month: YearMonth,
    isCurrent: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPickMonth: (YearMonth) -> Unit
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
                "${month.year} 年 ${month.monthValue} 月",
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
        HistoryMonthPickerDialog(
            initial = month,
            onDismiss = { showPicker = false },
            onConfirm = {
                onPickMonth(it)
                showPicker = false
            }
        )
    }
}

@Composable
private fun HistoryMonthPickerDialog(
    initial: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    var yearText by remember(initial) { mutableStateOf(initial.year.toString()) }
    var monthValue by remember(initial) { mutableIntStateOf(initial.monthValue) }
    var yearError by remember { mutableStateOf<String?>(null) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择年月") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = yearText,
                    onValueChange = {
                        yearText = it.filter(Char::isDigit).take(4)
                        yearError = null
                    },
                    label = { Text("年份") },
                    singleLine = true,
                    isError = yearError != null,
                    supportingText = yearError?.let { { Text(it) } },
                    colors = fieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
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
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val year = yearText.trim().toIntOrNull()
                    if (year == null || year !in 1900..2200) {
                        yearError = "请输入 1900–2200 的年份"
                        return@TextButton
                    }
                    onConfirm(YearMonth.of(year, monthValue))
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun StrengthMonthCalendar(
    days: List<HistoryDayCell>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    if (days.isEmpty()) return
    val month = YearMonth.from(days.first().day)
    val first = month.atDay(1)
    val lead = (first.dayOfWeek.value + 6) % 7
    val cells = buildList {
        repeat(lead) { add(null as HistoryDayCell?) }
        days.forEach { add(it) }
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
    Spacer(Modifier.height(4.dp))
    cells.chunked(7).forEach { week ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            week.forEach { cell ->
                if (cell == null) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                } else {
                    StrengthDayCell(
                        cell = cell,
                        selected = cell.day == selected,
                        onClick = { onSelect(cell.day) },
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
private fun StrengthDayCell(
    cell: HistoryDayCell,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val trained = cell.sessionCount > 0
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(
                if (trained) {
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
            "${cell.day.dayOfMonth}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(Modifier.height(2.dp))
        if (trained) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun DaySessionRow(session: SessionWithLogs, onClick: () -> Unit) {
    val doneCount = session.logs.count { !it.log.skipped }
    val duration = formatDuration(session.session)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "全身 ${session.session.workoutType.name}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                "$doneCount 个动作 · $duration",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            session.session.feeling?.let { feeling ->
                Text(
                    "感觉 ${"★".repeat(feeling)}${"☆".repeat((5 - feeling).coerceAtLeast(0))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HistoryDetailScreen(
    session: SessionWithLogs?,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GoldWash(Modifier.fillMaxWidth().height(88.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            CompactPageHeader(
                title = "训练详情",
                subtitle = session?.let {
                    DateTimeFormatter.ofPattern("yyyy年M月d日")
                        .withZone(ZoneId.systemDefault())
                        .format(Instant.ofEpochMilli(it.session.dateTime))
                } ?: "未找到记录",
                onBack = onBack
            )
            Spacer(Modifier.height(8.dp))

            if (session == null) {
                SoftCard(contentPadding = 12.dp) {
                    Text("未找到记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SoftCard(contentPadding = 12.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                SectionLabel("本次力量")
                                Text(
                                    "全身 ${session.session.workoutType.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "${session.logs.count { !it.log.skipped }} 个动作 · ${formatDuration(session.session)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                session.session.feeling?.let { feeling ->
                                    Text(
                                        "感觉 ${"★".repeat(feeling)}${"☆".repeat((5 - feeling).coerceAtLeast(0))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            TextButton(
                                onClick = onDelete,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) { Text("删除") }
                        }
                    }
                    session.logs.sortedBy { it.log.sortOrder }.forEach { log ->
                        SoftCard(contentPadding = 12.dp) {
                            DetailExercise(log)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailExercise(log: ExerciseLogWithSets) {
    if (log.log.skipped) {
        Text(
            log.exercise.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            "已跳过",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val resistance = log.sets.map { it.set.totalResistance }.firstOrNull { it > 0 } ?: 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                log.exercise.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                formatSets(log),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (resistance > 0) {
            Text(
                "$resistance LB",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatDuration(session: WorkoutSessionEntity): String {
    val seconds = MonthReviewBuilder.strengthDurationSeconds(session)
    val m = seconds / 60
    val s = seconds % 60
    return if (m >= 60) {
        val h = m / 60
        "${h}h${m % 60}m"
    } else if (s == 0) {
        "${m} 分"
    } else {
        "${m}分${s}秒"
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
