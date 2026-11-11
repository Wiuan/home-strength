package com.homestrength.ui.room

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.room.RoomFeedItem
import com.homestrength.ui.components.CompactPageHeader
import com.homestrength.ui.components.WeChatGroup
import com.homestrength.ui.components.WeChatGroupLabel
import com.homestrength.ui.components.WeChatInsetDivider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RoomFeedScreen(
    viewModel: RoomFeedViewModel,
    onBack: () -> Unit,
    onMessage: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { onMessage(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            CompactPageHeader(
                title = "练习室动态",
                subtitle = when {
                    state.peerName != null -> "${state.myName ?: "我"} · ${state.peerName}"
                    else -> "按完成时间 · 近两周"
                },
                onBack = onBack
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp),
                        strokeWidth = 2.dp
                    )
                }
                TextButton(
                    onClick = { viewModel.refresh(silent = false) },
                    enabled = !state.loading
                ) {
                    Text(if (state.loading) "同步中" else "刷新同步")
                }
            }
        }

        if (state.days.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    state.emptyHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                state.days.forEach { day ->
                    item(key = "label-${day.day}") {
                        WeChatGroupLabel(formatDayLabel(day.day))
                    }
                    item(key = "group-${day.day}") {
                        WeChatGroup {
                            day.items.forEachIndexed { index, item ->
                                FeedRow(item)
                                if (index < day.items.lastIndex) WeChatInsetDivider()
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun FeedRow(item: RoomFeedItem) {
    val zone = ZoneId.systemDefault()
    val time = if (item.activity.preciseTime) {
        Instant.ofEpochMilli(item.activity.at)
            .atZone(zone)
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        "—"
    }
    val kindLabel = kindLabel(item.activity.kind)
    val duration = formatDuration(item.activity.durationSec)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (item.isMe) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.nickname.take(1).ifBlank { if (item.isMe) "我" else "友" },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (item.isMe) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    item.nickname,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    kindLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
            Text(
                item.activity.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (duration != null) {
                Text(
                    duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Text(
            time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun kindLabel(kind: String): String = when (kind) {
    "strength" -> "力量"
    "sleep" -> "早睡"
    "meditation" -> "冥想"
    "light" -> "轻轨"
    "checklist" -> "清单"
    else -> "练习"
}

private fun formatDuration(sec: Int): String? {
    if (sec <= 0) return null
    val m = sec / 60
    val s = sec % 60
    return when {
        m >= 60 -> "${m / 60} 小时 ${m % 60} 分"
        m > 0 && s > 0 -> "${m} 分 ${s} 秒"
        m > 0 -> "${m} 分"
        else -> "${s} 秒"
    }
}

private fun formatDayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    val base = when (day) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> day.format(DateTimeFormatter.ofPattern("M月d日"))
    }
    return base
}
