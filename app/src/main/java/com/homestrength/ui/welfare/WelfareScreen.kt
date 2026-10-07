package com.homestrength.ui.welfare

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.RewardEntity
import com.homestrength.data.local.entity.RewardRedemptionEntity
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.ui.components.CompactPageHeader
import com.homestrength.ui.components.GoldWash
import com.homestrength.ui.components.SectionLabel
import com.homestrength.ui.components.SoftCard
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private enum class RewardSortMode { Custom, CostAsc, CostDesc }

@Composable
fun WelfareScreen(
    traineeRepository: TraineeRepository,
    onBack: () -> Unit,
    onMessage: (String) -> Unit = {}
) {
    val profile by traineeRepository.observeProfile().collectAsStateWithLifecycle(
        initialValue = com.homestrength.data.local.entity.TraineeProfileEntity()
    )
    val rewards by traineeRepository.observeRewards().collectAsStateWithLifecycle(initialValue = emptyList())
    val redemptions by traineeRepository.observeRedemptions(6)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RewardEntity?>(null) }
    var draftTitle by remember { mutableStateOf("") }
    var draftCost by remember { mutableStateOf("20") }
    var sortMode by remember { mutableStateOf(RewardSortMode.Custom) }
    var customOrder by remember { mutableStateOf<List<RewardEntity>>(emptyList()) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    var fundLabel by remember { mutableStateOf("服装基金") }
    var fundItem by remember { mutableStateOf("") }
    var fundYuan by remember { mutableStateOf("") }
    var showRateEditor by remember { mutableStateOf(false) }
    var draftRate by remember { mutableStateOf("10") }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.secondary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
    )

    LaunchedEffect(rewards, sortMode) {
        if (sortMode == RewardSortMode.Custom && draggingId == null) {
            customOrder = rewards
        }
    }

    val displayed = when (sortMode) {
        RewardSortMode.Custom -> customOrder.ifEmpty { rewards }
        RewardSortMode.CostAsc -> rewards.sortedBy { it.cost }
        RewardSortMode.CostDesc -> rewards.sortedByDescending { it.cost }
    }

    fun openEditor(reward: RewardEntity?) {
        editing = reward
        draftTitle = reward?.title.orEmpty()
        draftCost = (reward?.cost ?: 20).toString()
        showAdd = true
    }

    fun moveDragged(from: Int, to: Int) {
        if (from == to || from < 0 || to < 0) return
        customOrder = customOrder.toMutableList().apply {
            add(to, removeAt(from))
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactPageHeader(
                title = "福利社 · ${profile.coins} 币",
                subtitle = "基金花销与即时奖励",
                onBack = onBack
            )

            SoftCard(contentPadding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("基金花销")
                    TextButton(
                        onClick = {
                            draftRate = profile.coinsPerYuan.toString()
                            showRateEditor = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("${profile.coinsPerYuan}币/元")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("服装基金", "旅游基金", "其他").forEach { label ->
                        FilterChip(
                            selected = fundLabel == label,
                            onClick = { fundLabel = label },
                            label = { Text(label.removeSuffix("基金").ifBlank { label }) },
                            colors = chipColors,
                            modifier = Modifier.height(32.dp)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fundItem,
                        onValueChange = { fundItem = it },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        placeholder = { Text("卫衣") },
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = fundYuan,
                        onValueChange = { fundYuan = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.weight(0.8f),
                        singleLine = true,
                        placeholder = { Text("元") },
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                val yuan = fundYuan.toIntOrNull() ?: 0
                val cost = yuan * profile.coinsPerYuan.coerceAtLeast(1)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (yuan > 0) "扣 $cost 币" else "填品名与金额",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            scope.launch {
                                val err = traineeRepository.redeemFundPurchase(
                                    fundLabel = fundLabel,
                                    itemName = fundItem,
                                    yuan = yuan
                                )
                                if (err == null) {
                                    fundItem = ""
                                    fundYuan = ""
                                }
                                onMessage(err ?: "已记账并扣币")
                            }
                        },
                        enabled = yuan > 0 && fundItem.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) { Text("确认") }
                }
            }

            SoftCard(contentPadding = 12.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("即时奖励")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = sortMode == RewardSortMode.Custom,
                            onClick = { sortMode = RewardSortMode.Custom },
                            label = { Text("排") },
                            colors = chipColors,
                            modifier = Modifier.height(28.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = sortMode == RewardSortMode.CostAsc,
                            onClick = { sortMode = RewardSortMode.CostAsc },
                            label = { Text("↑") },
                            colors = chipColors,
                            modifier = Modifier.height(28.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        FilterChip(
                            selected = sortMode == RewardSortMode.CostDesc,
                            onClick = { sortMode = RewardSortMode.CostDesc },
                            label = { Text("↓") },
                            colors = chipColors,
                            modifier = Modifier.height(28.dp)
                        )
                        TextButton(
                            onClick = { openEditor(null) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) { Text("+") }
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (displayed.isEmpty()) {
                    Text(
                        "还没有奖励",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val latestOrder = rememberUpdatedState(customOrder)
                    val latestRewards = rememberUpdatedState(rewards)
                    displayed.forEachIndexed { index, reward ->
                        key(reward.id) {
                            if (index > 0) Spacer(Modifier.height(6.dp))
                            val isDragging = draggingId == reward.id
                            val rowModifier = if (sortMode == RewardSortMode.Custom) {
                                Modifier
                                    .zIndex(if (isDragging) 1f else 0f)
                                    .offset {
                                        IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0)
                                    }
                                    .shadow(
                                        if (isDragging) 6.dp else 0.dp,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .pointerInput(reward.id) {
                                        val rowHeightPx = 72f
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggingId = reward.id
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggingId = null
                                                dragOffsetY = 0f
                                                customOrder = latestRewards.value
                                            },
                                            onDragEnd = {
                                                val ids = latestOrder.value.map { it.id }
                                                draggingId = null
                                                dragOffsetY = 0f
                                                scope.launch {
                                                    traineeRepository.reorderRewards(ids)
                                                    onMessage("顺序已保存")
                                                }
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount.y
                                                val order = latestOrder.value
                                                val from = order.indexOfFirst { it.id == reward.id }
                                                if (from < 0) return@detectDragGesturesAfterLongPress
                                                val shift = (dragOffsetY / rowHeightPx).toInt()
                                                val to = (from + shift).coerceIn(0, order.lastIndex)
                                                if (to != from) {
                                                    moveDragged(from, to)
                                                    dragOffsetY -= (to - from) * rowHeightPx
                                                }
                                            }
                                        )
                                    }
                            } else {
                                Modifier
                            }
                            Box(modifier = rowModifier) {
                                RewardRow(
                                    reward = reward,
                                    coins = profile.coins,
                                    showDragHint = sortMode == RewardSortMode.Custom,
                                    onRedeem = {
                                        scope.launch {
                                            val err = traineeRepository.redeemReward(reward.id)
                                            onMessage(err ?: "已兑换：${reward.title}")
                                        }
                                    },
                                    onEdit = { openEditor(reward) },
                                    onDelete = {
                                        scope.launch { traineeRepository.deleteReward(reward.id) }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (redemptions.isNotEmpty()) {
                SoftCard(contentPadding = 12.dp) {
                    SectionLabel("最近兑现")
                    Spacer(Modifier.height(6.dp))
                    redemptions.forEachIndexed { index, item ->
                        if (index > 0) Spacer(Modifier.height(4.dp))
                        RedemptionRow(item)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showRateEditor) {
        AlertDialog(
            onDismissRequest = { showRateEditor = false },
            title = { Text("币 / 元 汇率") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "多少星光币等于 1 元。填 10：100 币 = 10 元；填 100：100 币 = 1 元。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = draftRate,
                        onValueChange = { draftRate = it.filter(Char::isDigit).take(4) },
                        label = { Text("星光币 / 元") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val rate = draftRate.toIntOrNull() ?: 10
                        scope.launch {
                            traineeRepository.updateCoinsPerYuan(rate)
                            showRateEditor = false
                            onMessage("已更新：${rate.coerceIn(1, 1000)} 币 = 1 元")
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showRateEditor = false }) { Text("取消") }
            }
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(if (editing == null) "添加奖励" else "编辑奖励") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text("奖励内容") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = draftCost,
                        onValueChange = { draftCost = it.filter(Char::isDigit).take(3) },
                        label = { Text("星光币价格") },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cost = draftCost.toIntOrNull() ?: 20
                        scope.launch {
                            val target = editing
                            if (target == null) {
                                traineeRepository.addReward(draftTitle, cost)
                            } else {
                                traineeRepository.updateReward(target.id, draftTitle, cost)
                            }
                            showAdd = false
                        }
                    }
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun RewardRow(
    reward: RewardEntity,
    coins: Int,
    showDragHint: Boolean,
    onRedeem: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val canAfford = coins >= reward.cost
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (showDragHint) {
                Text(
                    "⋮",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    reward.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${reward.cost} 币",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onEdit,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) { Text("改") }
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(28.dp)
            ) { Text("删") }
            Button(
                onClick = onRedeem,
                enabled = canAfford,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(30.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(if (canAfford) "兑" else "差")
            }
        }
    }
}

@Composable
private fun RedemptionRow(item: RewardRedemptionEntity) {
    val date = DateTimeFormatter.ofPattern("M/d HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(item.redeemedAt))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            "-${item.cost}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}
