package com.homestrength.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.homestrength.ui.theme.Palette

val TraineeCardShape = RoundedCornerShape(22.dp)

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .shadow(10.dp, TraineeCardShape, ambientColor = Color(0x1A1B2A41), spotColor = Color(0x1A1B2A41))
            .clip(TraineeCardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), TraineeCardShape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun GoldRule(width: Dp = 36.dp) {
    Box(
        modifier = Modifier
            .width(width)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.secondary)
    )
}

@Composable
fun HeroTitle(
    leading: String,
    accent: String,
    subtitle: String
) {
    Column {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)) {
                    append(leading)
                }
                append("\n")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append(accent)
                }
            },
            style = MaterialTheme.typography.displaySmall
        )
        Spacer(Modifier.height(10.dp))
        GoldRule()
        Spacer(Modifier.height(10.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Tighter header for secondary pages (档案 / 福利社). */
@Composable
fun CompactPageHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TextButton(
            onClick = onBack,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.height(32.dp)
        ) { Text("返回") }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.6.sp
    )
}

@Composable
fun QuietCircleCheck(
    checked: Boolean,
    onClick: () -> Unit
) {
    val border = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .border(1.5.dp, border, CircleShape)
            .background(if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GoldWash(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    Palette.GoldSoft.copy(alpha = 0.55f),
                    Color.Transparent
                )
            )
        )
    )
}
