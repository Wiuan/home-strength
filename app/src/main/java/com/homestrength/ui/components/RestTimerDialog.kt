package com.homestrength.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.os.SystemClock
import kotlinx.coroutines.delay

/**
 * Rest countdown based on [SystemClock.elapsedRealtime] so backgrounding stays accurate.
 */
@Composable
fun RestTimerDialog(
    initialSeconds: Int,
    onDismiss: () -> Unit
) {
    var selectedSeconds by remember { mutableIntStateOf(initialSeconds.coerceIn(60, 120)) }
    var endAtElapsed by remember {
        mutableLongStateOf(SystemClock.elapsedRealtime() + selectedSeconds * 1000L)
    }
    var remaining by remember { mutableIntStateOf(selectedSeconds) }
    var finished by remember { mutableStateOf(false) }

    fun restart(seconds: Int) {
        selectedSeconds = seconds
        finished = false
        endAtElapsed = SystemClock.elapsedRealtime() + seconds * 1000L
        remaining = seconds
    }

    LaunchedEffect(endAtElapsed) {
        while (true) {
            val leftMs = endAtElapsed - SystemClock.elapsedRealtime()
            val left = ((leftMs + 999) / 1000).toInt().coerceAtLeast(0)
            remaining = left
            if (leftMs <= 0L) {
                finished = true
                remaining = 0
                break
            }
            delay(200)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (finished) "休息结束" else "休息")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AnimatedContent(
                    targetState = finished,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.92f)).togetherWith(fadeOut())
                    },
                    label = "restFinished"
                ) { isFinished ->
                    Text(
                        text = if (isFinished) "可以开始下一组了" else "%d".format(remaining),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isFinished) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                if (!finished) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 90, 120).forEach { sec ->
                            FilterChip(
                                selected = selectedSeconds == sec,
                                onClick = { restart(sec) },
                                label = { Text("${sec}s") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (finished) "继续" else "跳过")
            }
        }
    )
}
