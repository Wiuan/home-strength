package com.homestrength.ui.bands

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.homestrength.data.local.entity.BandEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BandManageScreen(
    viewModel: BandManageViewModel,
    onBack: () -> Unit
) {
    val bands by viewModel.bands.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<BandEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的弹力带") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
                actions = {
                    TextButton(onClick = { showAdd = true }) { Text("添加") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "同阻力多条请改数量。组合时不会重复使用超出数量的带子。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(bands, key = { it.id }) { band ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${band.resistance} LB", style = MaterialTheme.typography.titleLarge)
                        Text("数量 × ${band.quantity}")
                    }
                    Row {
                        TextButton(onClick = { editing = band }) { Text("修改") }
                        TextButton(onClick = { viewModel.delete(band) }) { Text("删除") }
                    }
                }
            }
        }
    }

    if (showAdd) {
        BandEditDialog(
            title = "添加弹力带",
            initialResistance = 40,
            initialQuantity = 1,
            onDismiss = { showAdd = false },
            onConfirm = { resistance, quantity ->
                viewModel.save(resistance, quantity)
                showAdd = false
            }
        )
    }

    editing?.let { band ->
        BandEditDialog(
            title = "修改弹力带",
            initialResistance = band.resistance,
            initialQuantity = band.quantity,
            onDismiss = { editing = null },
            onConfirm = { resistance, quantity ->
                viewModel.save(resistance, quantity, id = band.id)
                editing = null
            }
        )
    }
}

@Composable
private fun BandEditDialog(
    title: String,
    initialResistance: Int,
    initialQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (resistance: Int, quantity: Int) -> Unit
) {
    var resistanceText by remember { mutableStateOf(initialResistance.toString()) }
    var quantityText by remember { mutableStateOf(initialQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = resistanceText,
                    onValueChange = { resistanceText = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("阻力 (LB)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { ch -> ch.isDigit() }.take(2) },
                    label = { Text("数量") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val resistance = resistanceText.toIntOrNull() ?: return@Button
                    val quantity = quantityText.toIntOrNull() ?: return@Button
                    if (resistance > 0 && quantity > 0) onConfirm(resistance, quantity)
                }
            ) { Text("保存") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
