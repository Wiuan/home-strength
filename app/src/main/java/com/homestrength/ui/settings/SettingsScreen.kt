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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.track.LightTrackDef
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
    val room by viewModel.room.collectAsStateWithLifecycle()
    val update by viewModel.update.collectAsStateWithLifecycle()
    val lightTracks by viewModel.lightTracks.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<SettingEditor?>(null) }
    var showRewardEditor by remember { mutableStateOf(false) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var showRoomEditor by remember { mutableStateOf(false) }
    var showInstallPermission by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showNicknameEditor by remember { mutableStateOf(false) }
    var showLightTrackEditor by remember { mutableStateOf(false) }

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
    LaunchedEffect(viewModel) {
        viewModel.needInstallPermission.collect { showInstallPermission = true }
    }
    LaunchedEffect(update.available) {
        if (update.available != null) showUpdateDialog = true
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
            subtitle = "训练 · 练习室 · 备份",
            onBack = onBack
        )

        WeChatGroupLabel("档案")
        WeChatGroup {
            WeChatCell(
                title = "昵称",
                value = profile.nickname.ifBlank { "W" },
                subtitle = "主页标题 · 同步后对方也能看到",
                onClick = { showNicknameEditor = true }
            )
        }

        WeChatGroupLabel("轻轨轨道")
        WeChatGroup {
            WeChatCell(
                title = "管理轨道",
                subtitle = "改名称、加阅读/书法等 · 首页与清单同步",
                onClick = {
                    viewModel.reloadLightTracks()
                    showLightTrackEditor = true
                }
            )
        }

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

        WeChatGroupLabel("双人练习室")
        WeChatGroup {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("开启同步", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (room.enabled) "已开启 · 进度会推到 Gitee" else "关闭时不同步",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Switch(
                    checked = room.enabled,
                    onCheckedChange = viewModel::setRoomEnabled
                )
            }
            WeChatInsetDivider()
            WeChatCell(
                title = "填写令牌与成员",
                subtitle = when {
                    room.accessToken.isNotBlank() && room.enabled ->
                        "${room.owner}/${room.repo} · ${room.myMemberId}"
                    room.accessToken.isNotBlank() -> "令牌已填 · 打开上方开关即可"
                    else -> "粘贴 Gitee 私人令牌（只存本机）"
                },
                showDivider = true,
                onClick = { showRoomEditor = true }
            )
            WeChatCell(
                title = if (room.syncing) "同步中…" else "立即同步并拉取对方",
                subtitle = "档案页可看日 / 周 / 月对比",
                onClick = { if (!room.syncing) viewModel.testRoomSync() }
            )
        }

        WeChatGroupLabel("关于与更新")
        WeChatGroup {
            WeChatCell(
                title = "当前版本",
                value = update.localLabel,
                showChevron = false,
                showDivider = true
            )
            WeChatCell(
                title = when {
                    update.checking -> "检查中…"
                    update.downloading -> "下载中 ${(update.progress * 100).toInt()}%"
                    update.available != null -> "发现 ${update.available!!.versionName} · 点此安装"
                    else -> "检查更新"
                },
                subtitle = "从 Gitee 练习室仓库拉取 APK",
                onClick = {
                    when {
                        update.checking || update.downloading -> Unit
                        update.available != null -> showUpdateDialog = true
                        else -> viewModel.checkForUpdate()
                    }
                }
            )
            if (update.downloading) {
                LinearProgressIndicator(
                    progress = { update.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
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

    if (showLightTrackEditor) {
        LightTrackEditorDialog(
            tracks = lightTracks,
            defaultLightFans = profile.lightFans,
            defaultLightCoins = profile.lightCoins,
            defaultChecklistFans = profile.checklistFans,
            defaultChecklistCoins = profile.checklistCoins,
            onDismiss = { showLightTrackEditor = false },
            onAdd = { label, timed, minutes, fans, coins ->
                viewModel.addLightTrack(label, timed, minutes, fans, coins)
            },
            onUpdate = { id, label, minutes, fans, coins ->
                viewModel.updateLightTrack(id, label, minutes, fans, coins)
            },
            onDelete = viewModel::deleteLightTrack
        )
    }

    if (showNicknameEditor) {
        NicknameEditorDialog(
            current = profile.nickname,
            onDismiss = { showNicknameEditor = false },
            onSave = { name ->
                viewModel.updateNickname(name)
                showNicknameEditor = false
            }
        )
    }

    if (showRoomEditor) {
        RoomSettingsDialog(
            room = room,
            onDismiss = { showRoomEditor = false },
            onChange = { enabled, owner, repo, branch, token, myId, peerId ->
                viewModel.updateRoomDraft(enabled, owner, repo, branch, token, myId, peerId)
            },
            onSave = {
                viewModel.saveRoomSettings()
                showRoomEditor = false
            }
        )
    }

    val pending = update.available
    if (showUpdateDialog && pending != null) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("新版本 ${pending.versionName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("当前 ${update.localLabel}")
                    Text(
                        pending.notes.ifBlank { "有可用更新，下载后按系统提示安装。" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpdateDialog = false
                        viewModel.downloadAndInstallUpdate()
                    },
                    enabled = !update.downloading
                ) { Text("下载并安装") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("稍后") }
            }
        )
    }

    if (showInstallPermission) {
        AlertDialog(
            onDismissRequest = { showInstallPermission = false },
            title = { Text("需要安装权限") },
            text = { Text("请允许「安装未知应用」，返回后再点一次安装。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInstallPermission = false
                        viewModel.openUnknownAppSettings(context)
                    }
                ) { Text("去开启") }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermission = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun LightTrackEditorDialog(
    tracks: List<LightTrackDef>,
    defaultLightFans: Int,
    defaultLightCoins: Int,
    defaultChecklistFans: Int,
    defaultChecklistCoins: Int,
    onDismiss: () -> Unit,
    onAdd: (label: String, timed: Boolean, defaultMinutes: Int, fans: Int, coins: Int) -> Unit,
    onUpdate: (id: String, label: String, defaultMinutes: Int, fans: Int, coins: Int) -> Unit,
    onDelete: (id: String) -> Unit
) {
    var newLabel by remember { mutableStateOf("") }
    var newTimed by remember { mutableStateOf(true) }
    var newMinutes by remember { mutableStateOf("10") }
    var newFans by remember { mutableStateOf(defaultLightFans.toString()) }
    var newCoins by remember { mutableStateOf(defaultLightCoins.toString()) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var editLabel by remember { mutableStateOf("") }
    var editMinutes by remember { mutableStateOf("10") }
    var editFans by remember { mutableStateOf("") }
    var editCoins by remember { mutableStateOf("") }

    fun effectiveFans(def: LightTrackDef): Int =
        if (def.rewardFans >= 0) def.rewardFans
        else if (def.isTimed()) defaultLightFans else defaultChecklistFans

    fun effectiveCoins(def: LightTrackDef): Int =
        if (def.rewardCoins >= 0) def.rewardCoins
        else if (def.isTimed()) defaultLightCoins else defaultChecklistCoins

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("管理练习轨道", style = MaterialTheme.typography.titleMedium)
                Text(
                    "与算法 / 声乐同级；每条可单独设粉丝和币。内置项不能删。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                WeChatGroupLabel("现有轨道")
                WeChatGroup {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp, max = 200.dp)
                    ) {
                        itemsIndexed(tracks, key = { _, def -> def.id }) { index, def ->
                            if (editingId == def.id) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    RoomTextRow("名称", editLabel) { editLabel = it.take(12) }
                                    if (def.backing != PracticeTrack.LIFE) {
                                        RoomTextRow("默认分钟", editMinutes) {
                                            editMinutes = it.filter(Char::isDigit).take(3)
                                        }
                                    }
                                    RoomTextRow("粉丝奖励", editFans) {
                                        editFans = it.filter(Char::isDigit).take(3)
                                    }
                                    RoomTextRow("星光币", editCoins) {
                                        editCoins = it.filter(Char::isDigit).take(2)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(
                                            onClick = {
                                                onUpdate(
                                                    def.id,
                                                    editLabel,
                                                    editMinutes.toIntOrNull() ?: def.defaultMinutes,
                                                    editFans.toIntOrNull() ?: effectiveFans(def),
                                                    editCoins.toIntOrNull() ?: effectiveCoins(def)
                                                )
                                                editingId = null
                                            }
                                        ) { Text("保存") }
                                        TextButton(onClick = { editingId = null }) { Text("取消") }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(def.label, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            trackModeHint(def, effectiveFans(def), effectiveCoins(def)) +
                                                if (def.locked) " · 内置" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            editingId = def.id
                                            editLabel = def.label
                                            editMinutes = def.defaultMinutes.toString()
                                            editFans = effectiveFans(def).toString()
                                            editCoins = effectiveCoins(def).toString()
                                        }
                                    ) { Text("改") }
                                    if (!def.locked) {
                                        TextButton(onClick = { onDelete(def.id) }) { Text("删") }
                                    }
                                }
                            }
                            if (index < tracks.lastIndex) WeChatInsetDivider()
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )

                WeChatGroupLabel("新建同级轨道")
                WeChatGroup {
                    RoomTextRow("名称", newLabel) { newLabel = it.take(12) }
                    WeChatInsetDivider()
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            "怎么练",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = newTimed,
                                onClick = {
                                    newTimed = true
                                    newFans = defaultLightFans.toString()
                                    newCoins = defaultLightCoins.toString()
                                },
                                label = { Text("计时练习") }
                            )
                            FilterChip(
                                selected = !newTimed,
                                onClick = {
                                    newTimed = false
                                    newFans = defaultChecklistFans.toString()
                                    newCoins = defaultChecklistCoins.toString()
                                },
                                label = { Text("只勾选") }
                            )
                        }
                        if (newTimed) {
                            Spacer(Modifier.height(6.dp))
                            RoomTextRow("默认分钟", newMinutes) {
                                newMinutes = it.filter(Char::isDigit).take(3)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        RoomTextRow("粉丝奖励", newFans) {
                            newFans = it.filter(Char::isDigit).take(3)
                        }
                        RoomTextRow("星光币", newCoins) {
                            newCoins = it.filter(Char::isDigit).take(2)
                        }
                    }
                    WeChatInsetDivider()
                    WeChatCell(
                        title = "添加",
                        subtitle = when {
                            newLabel.isBlank() -> "先写名称，如：钢琴、英语"
                            else -> "+${newFans.ifBlank { "0" }}粉 · +${newCoins.ifBlank { "0" }}币"
                        },
                        showChevron = false,
                        onClick = {
                            if (newLabel.isNotBlank()) {
                                onAdd(
                                    newLabel,
                                    newTimed,
                                    newMinutes.toIntOrNull() ?: 10,
                                    newFans.toIntOrNull()
                                        ?: if (newTimed) defaultLightFans else defaultChecklistFans,
                                    newCoins.toIntOrNull()
                                        ?: if (newTimed) defaultLightCoins else defaultChecklistCoins
                                )
                                newLabel = ""
                                newMinutes = "10"
                                newTimed = true
                                newFans = defaultLightFans.toString()
                                newCoins = defaultLightCoins.toString()
                            }
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("完成") }
                }
            }
        }
    }
}

private fun trackModeHint(def: LightTrackDef, fans: Int, coins: Int): String {
    val mode = if (def.backing == PracticeTrack.LIFE) {
        "只勾选"
    } else {
        "计时 · ${def.defaultMinutes}分"
    }
    return "$mode · +${fans}粉 +${coins}币"
}

@Composable
private fun NicknameEditorDialog(
    current: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var draft by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改昵称", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "最多 12 字 · 留空则恢复为 W",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it.take(12) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (draft.isEmpty()) {
                                Text(
                                    "例如：太原 / 阿忻",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun RoomSettingsDialog(
    room: RoomSettingsUi,
    onDismiss: () -> Unit,
    onChange: (
        enabled: Boolean?,
        owner: String?,
        repo: String?,
        branch: String?,
        accessToken: String?,
        myMemberId: String?,
        peerMemberId: String?
    ) -> Unit,
    onSave: () -> Unit
) {
    // Local draft: typing must not push ViewModel every keystroke (that scrolled the dialog to top).
    var showToken by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf(room.accessToken) }
    var myId by remember { mutableStateOf(room.myMemberId) }
    var peerId by remember { mutableStateOf(room.peerMemberId) }
    var owner by remember { mutableStateOf(room.owner) }
    var repo by remember { mutableStateOf(room.repo) }
    var branch by remember { mutableStateOf(room.branch) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text("双人练习室", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (room.enabled) "保存后会自动保持开启" else "填入令牌并保存后会自动开启同步",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                // Token fixed near top so keyboard doesn't bury it after a scroll jump.
                WeChatGroupLabel("私人令牌 · 只存本机")
                WeChatGroup {
                    RoomTextRow(
                        label = if (showToken) "Token" else "Token ·••",
                        value = token,
                        password = !showToken,
                        onValueChange = { token = it }
                    )
                    WeChatInsetDivider()
                    WeChatCell(
                        title = if (showToken) "隐藏令牌" else "显示令牌",
                        onClick = { showToken = !showToken }
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 260.dp)
                ) {
                    item {
                        WeChatGroupLabel("成员 ID")
                        WeChatGroup {
                            RoomTextRow("我的 ID", myId) { myId = it }
                            WeChatInsetDivider()
                            RoomTextRow("对方 ID", peerId) { peerId = it }
                        }
                        WeChatGroupLabel("仓库（一般不用改）")
                        WeChatGroup {
                            RoomTextRow("Owner", owner) { owner = it }
                            WeChatInsetDivider()
                            RoomTextRow("Repo", repo) { repo = it }
                            WeChatInsetDivider()
                            RoomTextRow("Branch", branch) { branch = it }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("取消") }
                    TextButton(
                        onClick = {
                            onChange(null, owner, repo, branch, token, myId, peerId)
                            onSave()
                        }
                    ) { Text("保存") }
                }
            }
        }
    }
}

@Composable
private fun RoomTextRow(
    label: String,
    value: String,
    password: Boolean = false,
    onValueChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = if (password) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .border(
                    0.5.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
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
