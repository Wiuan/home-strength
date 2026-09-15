package com.homestrength.ui.settings

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private enum class SettingEditor { WeeklyGoal, DefaultSets, RestSeconds }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onOpenBands: () -> Unit,
    onOpenPlanA: () -> Unit,
    onOpenPlanB: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var editor by remember { mutableStateOf<SettingEditor?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SectionTitle("弹力带")
            SettingLine("管理我的弹力带", onClick = onOpenBands)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SectionTitle("训练计划")
            SettingLine("查看 Workout A", onClick = onOpenPlanA)
            SettingLine("查看 Workout B", onClick = onOpenPlanB)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SectionTitle("通知")
            SettingLine("训练提醒", "第一版占位，暂未实现")
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
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
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
            .padding(vertical = 10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        if (subtitle != null) {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
