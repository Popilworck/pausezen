package com.pausezen.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pausezen.app.data.BreathingPattern
import com.pausezen.app.data.BreathingStyle
import com.pausezen.app.ui.theme.PauseZenPalette
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BreathingGuide(
    pattern: BreathingPattern,
    style: BreathingStyle,
    modifier: Modifier = Modifier
) {
    // 0f = fully exhaled, 1f = fully inhaled
    val progress = remember { Animatable(0f) }
    var phaseLabel by remember { mutableStateOf("Breathe in") }

    LaunchedEffect(pattern) {
        progress.snapTo(0f)
        while (true) {
            phaseLabel = "Breathe in"
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = pattern.inhaleMs, easing = FastOutSlowInEasing)
            )
            if (pattern.holdAfterInhaleMs > 0) {
                phaseLabel = "Hold"
                delay(pattern.holdAfterInhaleMs.toLong())
            }
            phaseLabel = "Breathe out"
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = pattern.exhaleMs, easing = FastOutSlowInEasing)
            )
            if (pattern.holdAfterExhaleMs > 0) {
                phaseLabel = if (pattern.holdAfterExhaleMs >= 1_500) "Hold" else "Rest"
                delay(pattern.holdAfterExhaleMs.toLong())
            }
        }
    }

    val p = progress.value

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        when (style) {
            BreathingStyle.BLOOM -> BloomVisual(progress = p, phaseLabel = phaseLabel)
            BreathingStyle.LOTUS -> LotusVisual(progress = p, phaseLabel = phaseLabel)
            BreathingStyle.HORIZON -> HorizonVisual(progress = p, phaseLabel = phaseLabel)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${pattern.displayName} · ${pattern.cadenceLabel}",
            style = MaterialTheme.typography.labelSmall,
            color = PauseZenPalette.TextMuted
        )
    }
}

@Composable
private fun BloomVisual(progress: Float, phaseLabel: String) {
    val scale = 0.42f + progress * 0.58f
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(224.dp)
    ) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(PauseZenPalette.Accent.copy(alpha = 0.14f))
        )
        Box(
            modifier = Modifier
                .size(172.dp)
                .scale(0.20f + scale * 0.80f)
                .clip(CircleShape)
                .background(PauseZenPalette.Accent.copy(alpha = 0.24f))
                .border(
                    width = 1.5.dp,
                    color = PauseZenPalette.Accent.copy(alpha = 0.55f),
                    shape = CircleShape
                )
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .scale(0.36f + scale * 0.64f)
                .clip(CircleShape)
                .background(PauseZenPalette.Accent.copy(alpha = 0.44f))
        ) {
            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = PauseZenPalette.TextPrimary
            )
        }
    }
}

@Composable
private fun LotusVisual(progress: Float, phaseLabel: String) {
    val accent = PauseZenPalette.Accent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(224.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.25f
            val petalRadius = maxR * (0.62f + 0.38f * progress)
            val spreadDistance = maxR * (0.28f + 0.78f * progress)
            val baseRotationRad = (progress * 30.0) * (PI / 180.0)

            val petalCount = 6
            for (i in 0 until petalCount) {
                val angle = baseRotationRad + (i * 2.0 * PI / petalCount)
                val cx = center.x + (cos(angle) * spreadDistance).toFloat()
                val cy = center.y + (sin(angle) * spreadDistance).toFloat()
                val petalCenter = Offset(cx, cy)

                drawCircle(
                    color = accent.copy(alpha = 0.20f + 0.14f * progress),
                    radius = petalRadius,
                    center = petalCenter
                )
                drawCircle(
                    color = accent.copy(alpha = 0.52f),
                    radius = petalRadius,
                    center = petalCenter,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PauseZenPalette.TextPrimary
        )
    }
}

@Composable
private fun HorizonVisual(progress: Float, phaseLabel: String) {
    val accent = PauseZenPalette.Accent
    val surface = PauseZenPalette.Surface
    val hairline = PauseZenPalette.Hairline

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 186.dp, height = 216.dp)
            .clip(RoundedCornerShape(36.dp))
            .background(surface)
            .border(1.dp, hairline, RoundedCornerShape(36.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val fillFraction = 0.14f + 0.78f * progress
            val topY = size.height * (1f - fillFraction)

            val tideBrush = Brush.verticalGradient(
                colors = listOf(
                    accent.copy(alpha = 0.46f),
                    accent.copy(alpha = 0.16f)
                ),
                startY = topY,
                endY = size.height
            )

            drawRect(
                brush = tideBrush,
                topLeft = Offset(0f, topY),
                size = Size(size.width, size.height - topY)
            )
            drawLine(
                color = accent.copy(alpha = 0.85f),
                start = Offset(0f, topY),
                end = Offset(size.width, topY),
                strokeWidth = 2.dp.toPx()
            )
        }
        Text(
            text = phaseLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PauseZenPalette.TextPrimary
        )
    }
}
