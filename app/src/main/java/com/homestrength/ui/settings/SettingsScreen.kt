package com.homestrength.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.ui.components.CompactPageHeader

private enum class SettingEditor { WeeklyGoal, DefaultSets, RestSeconds }

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenBands: () -> Unit,
    onOpenPlanA: () -> Unit,
    onOpenPlanB: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<SettingEditor?>(null) }
    var showRewardEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        CompactPageHeader(
            title = "设置",
            subtitle = "训练默认值与奖励规则",
            onBack = onBack
        )

        SectionTitle("训练")
        SettingLine(
            title = "每周目标训练次数",
            subtitle = "${settings.weeklyGoal} 次",
            onClick = { editor = SettingEditor.WeeklyGoal }
        )
        SettingLine(
            title = "默认组数",
            subtitle = "${settings.defaultSets} 组",
            onClick = { editor = SettingEditor.DefaultSets }
        )
        SettingLine(
            title = "默认休息时间",
            subtitle = "${settings.defaultRestSeconds} 秒",
            onClick = { editor = SettingEditor.RestSeconds }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("练习生 · 奖励规则")
        Text(
            "粉丝、星光币、早睡/冥想回元气都可以自己改。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        SettingLine(
            title = "清单勾选（生活）",
            subtitle = "+${profile.checklistFans} 粉丝 · +${profile.checklistCoins} 币 · 不扣元气",
            onClick = { showRewardEditor = true }
        )
        SettingLine(
            title = "轻轨道完成",
            subtitle = "+${profile.lightFans} 粉丝 · +${profile.lightCoins} 币",
            onClick = { showRewardEditor = true }
        )
        SettingLine(
            title = "力量训练完成",
            subtitle = "+${profile.strengthFans} 粉丝 · +${profile.strengthCoins} 币",
            onClick = { showRewardEditor = true }
        )
        SettingLine(
            title = "早睡打卡",
            subtitle = "恢复 ${profile.sleepEnergyRestore} 元气 · 一天一次",
            onClick = { showRewardEditor = true }
        )
        SettingLine(
            title = "冥想",
            subtitle = "恢复 ${profile.meditationEnergyRestore} 元气 · 不限次数",
            onClick = { showRewardEditor = true }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("弹力带")
        SettingLine("管理我的弹力带", onClick = onOpenBands)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SectionTitle("训练计划")
        SettingLine("查看 Workout A", onClick = onOpenPlanA)
        SettingLine("查看 Workout B", onClick = onOpenPlanB)
    }

    when (editor) {
        SettingEditor.WeeklyGoal -> OptionDialog(
            title = "每周目标",
            options = (1..7).map { it to "$it 次" },
            selected = settings.weeklyGoal,
            onSelect = {
                viewModel.updateWeeklyGoal(it)
                editor = null
            },
            onDismiss = { editor = null }
        )
        SettingEditor.DefaultSets -> OptionDialog(
            title = "默认组数",
            options = (1..5).map { it to "$it 组" },
            selected = settings.defaultSets,
            onSelect = {
                viewModel.updateDefaultSets(it)
                editor = null
            },
            onDismiss = { editor = null }
        )
        SettingEditor.RestSeconds -> OptionDialog(
            title = "默认休息时间",
            options = listOf(60, 90, 120, 150, 180).map { it to "$it 秒" },
            selected = settings.defaultRestSeconds,
            onSelect = {
                viewModel.updateDefaultRestSeconds(it)
                editor = null
            },
            onDismiss = { editor = null }
        )
        null -> Unit
    }

    if (showRewardEditor) {
        RewardRulesDialog(
            profile = profile,
            onDismiss = { showRewardEditor = false },
            onSave = { rules ->
                viewModel.updateRewardRules(
                    checklistFans = rules.checklistFans,
                    checklistCoins = rules.checklistCoins,
                    lightFans = rules.lightFans,
                    lightCoins = rules.lightCoins,
                    strengthFans = rules.strengthFans,
                    strengthCoins = rules.strengthCoins,
                    sleepEnergyRestore = rules.sleepEnergyRestore,
                    sleepFans = rules.sleepFans,
                    sleepCoins = rules.sleepCoins,
                    meditationEnergyRestore = rules.meditationEnergyRestore,
                    meditationFans = rules.meditationFans,
                    meditationCoins = rules.meditationCoins
                )
                showRewardEditor = false
            }
        )
    }
}

private data class RewardDraft(
    val checklistFans: Int,
    val checklistCoins: Int,
    val lightFans: Int,
    val lightCoins: Int,
    val strengthFans: Int,
    val strengthCoins: Int,
    val sleepEnergyRestore: Int,
    val sleepFans: Int,
    val sleepCoins: Int,
    val meditationEnergyRestore: Int,
    val meditationFans: Int,
    val meditationCoins: Int
)

@Composable
private fun RewardRulesDialog(
    profile: TraineeProfileEntity,
    onDismiss: () -> Unit,
    onSave: (RewardDraft) -> Unit
) {
    var checklistFans by remember(profile) { mutableStateOf(profile.checklistFans.toString()) }
    var checklistCoins by remember(profile) { mutableStateOf(profile.checklistCoins.toString()) }
    var lightFans by remember(profile) { mutableStateOf(profile.lightFans.toString()) }
    var lightCoins by remember(profile) { mutableStateOf(profile.lightCoins.toString()) }
    var strengthFans by remember(profile) { mutableStateOf(profile.strengthFans.toString()) }
    var strengthCoins by remember(profile) { mutableStateOf(profile.strengthCoins.toString()) }
    var sleepEnergy by remember(profile) { mutableStateOf(profile.sleepEnergyRestore.toString()) }
    var sleepFans by remember(profile) { mutableStateOf(profile.sleepFans.toString()) }
    var sleepCoins by remember(profile) { mutableStateOf(profile.sleepCoins.toString()) }
    var meditationEnergy by remember(profile) { mutableStateOf(profile.meditationEnergyRestore.toString()) }
    var meditationFans by remember(profile) { mutableStateOf(profile.meditationFans.toString()) }
    var meditationCoins by remember(profile) { mutableStateOf(profile.meditationCoins.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑奖励规则") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("清单勾选", style = MaterialTheme.typography.labelLarge)
                PairFields("粉丝", checklistFans, { checklistFans = it }, "币", checklistCoins, { checklistCoins = it })
                Text("轻轨道", style = MaterialTheme.typography.labelLarge)
                PairFields("粉丝", lightFans, { lightFans = it }, "币", lightCoins, { lightCoins = it })
                Text("力量训练", style = MaterialTheme.typography.labelLarge)
                PairFields("粉丝", strengthFans, { strengthFans = it }, "币", strengthCoins, { strengthCoins = it })
                Text("早睡", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = sleepEnergy,
                    onValueChange = { sleepEnergy = it.filter(Char::isDigit).take(3) },
                    label = { Text("恢复元气") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PairFields("粉丝", sleepFans, { sleepFans = it }, "币", sleepCoins, { sleepCoins = it })
                Text("冥想（不限次）", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = meditationEnergy,
                    onValueChange = { meditationEnergy = it.filter(Char::isDigit).take(2) },
                    label = { Text("恢复元气") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PairFields(
                    "粉丝",
                    meditationFans,
                    { meditationFans = it },
                    "币",
                    meditationCoins,
                    { meditationCoins = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        RewardDraft(
                            checklistFans = checklistFans.toIntOrNull() ?: profile.checklistFans,
                            checklistCoins = checklistCoins.toIntOrNull() ?: profile.checklistCoins,
                            lightFans = lightFans.toIntOrNull() ?: profile.lightFans,
                            lightCoins = lightCoins.toIntOrNull() ?: profile.lightCoins,
                            strengthFans = strengthFans.toIntOrNull() ?: profile.strengthFans,
                            strengthCoins = strengthCoins.toIntOrNull() ?: profile.strengthCoins,
                            sleepEnergyRestore = sleepEnergy.toIntOrNull() ?: profile.sleepEnergyRestore,
                            sleepFans = sleepFans.toIntOrNull() ?: profile.sleepFans,
                            sleepCoins = sleepCoins.toIntOrNull() ?: profile.sleepCoins,
                            meditationEnergyRestore = meditationEnergy.toIntOrNull()
                                ?: profile.meditationEnergyRestore,
                            meditationFans = meditationFans.toIntOrNull() ?: profile.meditationFans,
                            meditationCoins = meditationCoins.toIntOrNull() ?: profile.meditationCoins
                        )
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun PairFields(
    leftLabel: String,
    left: String,
    onLeft: (String) -> Unit,
    rightLabel: String,
    right: String,
    onRight: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = left,
            onValueChange = { onLeft(it.filter(Char::isDigit).take(3)) },
            label = { Text(leftLabel) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = right,
            onValueChange = { onRight(it.filter(Char::isDigit).take(2)) },
            label = { Text(rightLabel) },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OptionDialog(
    title: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (value, label) ->
                            FilterChip(
                                selected = selected == value,
                                onClick = { onSelect(value) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingLine(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 6.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
