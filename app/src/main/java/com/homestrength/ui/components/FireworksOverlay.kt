package com.homestrength.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.homestrength.domain.trainee.TraineeGrade
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun FireworksOverlay(
    grade: TraineeGrade,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val bursts = remember {
        List(5) { index ->
            FireworkBurst(
                cx = 0.18f + index * 0.16f + Random.nextFloat() * 0.06f,
                cy = 0.22f + Random.nextFloat() * 0.28f,
                color = fireworkPalette[index % fireworkPalette.size],
                particleCount = 28 + Random.nextInt(12),
                seed = Random.nextInt()
            )
        }
    }

    LaunchedEffect(grade) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 2200, easing = LinearEasing))
        delay(400)
        onFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC1B2A41)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = progress.value
            bursts.forEach { burst ->
                val localT = ((t - burst.delay) / 0.7f).coerceIn(0f, 1f)
                if (localT <= 0f) return@forEach
                val origin = Offset(size.width * burst.cx, size.height * burst.cy)
                val rnd = Random(burst.seed)
                repeat(burst.particleCount) { i ->
                    val angle = (i.toFloat() / burst.particleCount) * Math.PI * 2 + rnd.nextDouble() * 0.4
                    val speed = 0.35f + rnd.nextFloat() * 0.55f
                    val dist = size.minDimension * 0.22f * speed * localT
                    val fade = (1f - localT).coerceIn(0f, 1f)
                    val x = origin.x + (cos(angle) * dist).toFloat()
                    val y = origin.y + (sin(angle) * dist).toFloat() + localT * localT * 80f
                    drawCircle(
                        color = burst.color.copy(alpha = fade),
                        radius = 4.5f * fade + 1.5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
        Text(
            text = "升级！\n${TraineeGrade.letter(grade)} · ${grade.label}",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

private data class FireworkBurst(
    val cx: Float,
    val cy: Float,
    val color: Color,
    val particleCount: Int,
    val seed: Int,
    val delay: Float = Random.nextFloat() * 0.15f
)

private val fireworkPalette = listOf(
    Color(0xFFD4A017),
    Color(0xFFFFE08A),
    Color(0xFFFF6B6B),
    Color(0xFF7EC8E3),
    Color(0xFFB8E986)
)
