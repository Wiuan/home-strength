package com.homestrength.ui.settings

import android.app.Activity
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.ui.components.CompactPageHeader
import com.homestrength.ui.components.WeChatCell
import com.homestrength.ui.components.WeChatGroup
import com.homestrength.ui.components.WeChatGroupLabel
import com.homestrength.ui.components.WeChatInsetDivider
import kotlinx.coroutines.delay

private enum class SettingEditor { WeeklyGoal, DefaultSets, RestSeconds }

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenBands: () -> Unit,
    onOpenPlanA: () -> Unit,
    onOpenPlanB: () -> Unit,
    onMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<SettingEditor?>(null) }
    var showRewardEditor by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(context, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(context, uri)
    }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { onMessage(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.importFinished.collect {
            delay(900)
            (context as? Activity)?.finishAffinity()
            Process.killProcess(Process.myPid())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        CompactPageHeader(
            title = "设置",
            subtitle = "训练 · 奖励 · 备份",
            onBack = onBack
        )

        WeChatGroupLabel("训练")
        WeChatGroup {
            WeChatCell(
                title = "每周目标",
                value = "${settings.weeklyGoal} 次",
                showDivider = true,
                onClick = { editor = SettingEditor.WeeklyGoal }
            )
            WeChatCell(
                title = "默认组数",
                value = "${settings.defaultSets} 组",
                showDivider = true,
                onClick = { editor = SettingEditor.DefaultSets }
            )
            WeChatCell(
                title = "默认休息",
                value = "${settings.defaultRestSeconds} 秒",
                onClick = { editor = SettingEditor.RestSeconds }
            )
        }

        WeChatGroupLabel("奖励规则")
        WeChatGroup {
            WeChatCell(
                title = "清单勾选",
                subtitle = "+${profile.checklistFans} 粉 · +${profile.checklistCoins} 币",
                showDivider = true,
                onClick = { showRewardEditor = true }
            )
            WeChatCell(
                title = "轻轨道",
                subtitle = "+${profile.lightFans} 粉 · +${profile.lightCoins} 币",
                showDivider = true,
                onClick = { showRewardEditor = true }
            )
            WeChatCell(
                title = "力量训练",
                subtitle = "+${profile.strengthFans} 粉 · +${profile.strengthCoins} 币",
                showDivider = true,
                onClick = { showRewardEditor = true }
            )
            WeChatCell(
                title = "早睡",
                subtitle = "元气 +${profile.sleepEnergyRestore} · 一天一次",
                showDivider = true,
                onClick = { showRewardEditor = true }
            )
            WeChatCell(
                title = "冥想",
                subtitle = "元气 +${profile.meditationEnergyRestore} · 不限次",
                onClick = { showRewardEditor = true }
            )
        }

        WeChatGroupLabel("力量模块")
        WeChatGroup {
            WeChatCell(title = "我的弹力带", showDivider = true, onClick = onOpenBands)
            WeChatCell(title = "Workout A", showDivider = true, onClick = onOpenPlanA)
            WeChatCell(title = "Workout B", onClick = onOpenPlanB)
        }

        WeChatGroupLabel("数据")
        WeChatGroup {
            WeChatCell(
                title = "导出备份",
                subtitle = "JSON · 可放网盘",
                showDivider = true,
                onClick = { exportLauncher.launch(viewModel.suggestedBackupFileName()) }
            )
            WeChatCell(
                title = "导入备份",
                subtitle = "覆盖本机全部数据",
                onClick = { showImportConfirm = true }
            )
        }
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

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { Text("导入备份？") },
            text = {
                Text("会清空并覆盖本机全部练习记录、清单、档案与设置。导入后 App 会重启。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                ) { Text("选择文件") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) { Text("取消") }
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
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("奖励规则", style = MaterialTheme.typography.titleMedium)
                Text(
                    "粉丝 / 币",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                WeChatGroupLabel("清单勾选")
                WeChatGroup {
                    RewardValueRow("粉丝", checklistFans, maxLen = 3) { checklistFans = it }
                    WeChatInsetDivider()
                    RewardValueRow("星光币", checklistCoins, maxLen = 2) { checklistCoins = it }
                }

                WeChatGroupLabel("轻轨道")
                WeChatGroup {
                    RewardValueRow("粉丝", lightFans, maxLen = 3) { lightFans = it }
                    WeChatInsetDivider()
                    RewardValueRow("星光币", lightCoins, maxLen = 2) { lightCoins = it }
                }

                WeChatGroupLabel("力量训练")
                WeChatGroup {
                    RewardValueRow("粉丝", strengthFans, maxLen = 3) { strengthFans = it }
                    WeChatInsetDivider()
                    RewardValueRow("星光币", strengthCoins, maxLen = 2) { strengthCoins = it }
                }

                WeChatGroupLabel("早睡")
                WeChatGroup {
                    RewardValueRow("恢复元气", sleepEnergy, maxLen = 3) { sleepEnergy = it }
                    WeChatInsetDivider()
                    RewardValueRow("粉丝", sleepFans, maxLen = 3) { sleepFans = it }
                    WeChatInsetDivider()
                    RewardValueRow("星光币", sleepCoins, maxLen = 2) { sleepCoins = it }
                }

                WeChatGroupLabel("冥想 · 不限次")
                WeChatGroup {
                    RewardValueRow("恢复元气", meditationEnergy, maxLen = 2) { meditationEnergy = it }
                    WeChatInsetDivider()
                    RewardValueRow("粉丝", meditationFans, maxLen = 3) { meditationFans = it }
                    WeChatInsetDivider()
                    RewardValueRow("星光币", meditationCoins, maxLen = 2) { meditationCoins = it }
                }
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

/** WeChat form cell: label left, compact number field right. */
@Composable
private fun RewardValueRow(
    label: String,
    value: String,
    maxLen: Int,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(maxLen)) },
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                textAlign = TextAlign.End
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(72.dp)
                .height(32.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text(
                            "0",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    inner()
                }
            }
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
