package com.homestrength.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun NumberStepper(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 999,
    emptyLabel: String = "—"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = {
                val current = value ?: 0
                val next = (current - 1).coerceAtLeast(min)
                onValueChange(if (value == null && next == 0) null else next)
            }
        ) {
            Text("−", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            text = value?.toString() ?: emptyLabel,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 56.dp)
        )
        FilledTonalButton(
            onClick = {
                val current = value ?: (min - 1).coerceAtLeast(0)
                onValueChange((current + 1).coerceAtMost(max).takeIf { it >= min } ?: min)
            }
        ) {
            Text("+", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
