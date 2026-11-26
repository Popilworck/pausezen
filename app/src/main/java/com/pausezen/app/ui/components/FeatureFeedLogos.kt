package com.pausezen.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pausezen.app.core.InAppFeatureCatalog

/**
 * Custom crisp vector logos for in-app feeds (Instagram Reels, YouTube Shorts,
 * Snapchat Spotlight, Snapchat Stories, Instagram Explore, Facebook Reels).
 */
@Composable
fun FeatureFeedLogo(
    ruleId: String,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    when (ruleId) {
        InAppFeatureCatalog.INSTAGRAM_REELS.id -> InstagramReelsLogo(size, modifier)
        InAppFeatureCatalog.YOUTUBE_SHORTS.id -> YouTubeShortsLogo(size, modifier)
        InAppFeatureCatalog.SNAPCHAT_SPOTLIGHT.id -> SnapchatSpotlightLogo(size, modifier)
        InAppFeatureCatalog.SNAPCHAT_STORIES.id -> SnapchatStoriesLogo(size, modifier)
        InAppFeatureCatalog.INSTAGRAM_EXPLORE.id -> InstagramExploreLogo(size, modifier)
        InAppFeatureCatalog.FACEBOOK_REELS.id -> FacebookReelsLogo(size, modifier)
        else -> InstagramReelsLogo(size, modifier)
    }
}

@Composable
private fun InstagramReelsLogo(size: Dp, modifier: Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = w * 0.26f

        val gradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF833AB4),
                Color(0xFFE1306C),
                Color(0xFFF77737),
                Color(0xFFFCAF45)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // Clapperboard rounded tile
        drawRoundRect(
            brush = gradient,
            topLeft = Offset(w * 0.06f, h * 0.06f),
            size = Size(w * 0.88f, h * 0.88f),
            cornerRadius = CornerRadius(radius, radius)
        )

        // Top clapper bar separator
        val barY = h * 0.34f
        drawLine(
            color = Color.White,
            start = Offset(w * 0.06f, barY),
            end = Offset(w * 0.94f, barY),
            strokeWidth = w * 0.065f
        )

        // Diagonal clapper stripes
        drawLine(
            color = Color.White,
            start = Offset(w * 0.33f, h * 0.06f),
            end = Offset(w * 0.44f, barY),
            strokeWidth = w * 0.065f
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.62f, h * 0.06f),
            end = Offset(w * 0.73f, barY),
            strokeWidth = w * 0.065f
        )

        // Center play triangle
        val playPath = Path().apply {
            moveTo(w * 0.42f, h * 0.47f)
            lineTo(w * 0.67f, h * 0.62f)
            lineTo(w * 0.42f, h * 0.77f)
            close()
        }
        drawPath(playPath, color = Color.White)
    }
}

@Composable
private fun YouTubeShortsLogo(size: Dp, modifier: Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val shortsRed = Color(0xFFFF0033)

        // Iconic overlapping angled red capsules forming the Shorts mark
        rotate(degrees = -26f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            drawRoundRect(
                color = shortsRed,
                topLeft = Offset(w * 0.14f, h * 0.18f),
                size = Size(w * 0.72f, h * 0.32f),
                cornerRadius = CornerRadius(h * 0.16f, h * 0.16f)
            )
            drawRoundRect(
                color = shortsRed,
                topLeft = Offset(w * 0.14f, h * 0.50f),
                size = Size(w * 0.72f, h * 0.32f),
                cornerRadius = CornerRadius(h * 0.16f, h * 0.16f)
            )
            drawRect(
                color = shortsRed,
                topLeft = Offset(w * 0.24f, h * 0.34f),
                size = Size(w * 0.52f, h * 0.32f)
            )
        }

        // White play triangle in center
        val playPath = Path().apply {
            moveTo(w * 0.42f, h * 0.36f)
            lineTo(w * 0.64f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.64f)
            close()
        }
        drawPath(playPath, color = Color.White)
    }
}

@Composable
private fun SnapchatSpotlightLogo(size: Dp, modifier: Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        drawCircle(
            color = Color(0xFFFFFC00),
            radius = w * 0.46f,
            center = Offset(w * 0.5f, h * 0.5f)
        )

        val playPath = Path().apply {
            moveTo(w * 0.40f, h * 0.32f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.40f, h * 0.68f)
            close()
        }
        drawPath(
            path = playPath,
            color = Color(0xFF111110),
            style = Stroke(width = w * 0.05f, join = StrokeJoin.Round)
        )
        drawPath(playPath, color = Color(0xFF111110))
    }
}

@Composable
private fun SnapchatStoriesLogo(size: Dp, modifier: Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val snapYellow = Color(0xFFFFFC00)

        // Dashed outer story ring
        drawCircle(
            color = snapYellow,
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(
                width = w * 0.075f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.34f, w * 0.16f), 0f)
            )
        )

        // Minimal ghost silhouette in center
        val ghost = Path().apply {
            moveTo(w * 0.50f, h * 0.28f)
            cubicTo(w * 0.37f, h * 0.28f, w * 0.34f, h * 0.39f, w * 0.35f, h * 0.50f)
            cubicTo(w * 0.35f, h * 0.57f, w * 0.29f, h * 0.61f, w * 0.28f, h * 0.64f)
            cubicTo(w * 0.34f, h * 0.67f, w * 0.40f, h * 0.71f, w * 0.50f, h * 0.71f)
            cubicTo(w * 0.60f, h * 0.71f, w * 0.66f, h * 0.67f, w * 0.72f, h * 0.64f)
            cubicTo(w * 0.71f, h * 0.61f, w * 0.65f, h * 0.57f, w * 0.65f, h * 0.50f)
            cubicTo(w * 0.66f, h * 0.39f, w * 0.63f, h * 0.28f, w * 0.50f, h * 0.28f)
            close()
        }
        drawPath(ghost, color = snapYellow)
    }
}

@Composable
private fun InstagramExploreLogo(size: Dp, modifier: Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = w * 0.26f

        val gradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF5851DB),
                Color(0xFFC13584),
                Color(0xFFF56040)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        drawRoundRect(
            brush = gradient,
            topLeft = Offset(w * 0.06f, h * 0.06f),
            size = Size(w * 0.88f, h * 0.88f),
            cornerRadius = CornerRadius(radius, radius)
        )

        // Magnifying glass
        drawCircle(
            color = Color.White,
            radius = w * 0.20f,
            center = Offset(w * 0.44f, h * 0.44f),
            style = Stroke(width = w * 0.08f)
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.59f, h * 0.59f),
            end = Offset(w * 0.75f, h * 0.75f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun FacebookReelsLogo(size: Dp, modifier: Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val radius = w * 0.26f

        drawRoundRect(
            color = Color(0xFF1877F2),
            topLeft = Offset(w * 0.06f, h * 0.06f),
            size = Size(w * 0.88f, h * 0.88f),
            cornerRadius = CornerRadius(radius, radius)
        )

        val barY = h * 0.34f
        drawLine(
            color = Color.White,
            start = Offset(w * 0.06f, barY),
            end = Offset(w * 0.94f, barY),
            strokeWidth = w * 0.065f
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.35f, h * 0.06f),
            end = Offset(w * 0.46f, barY),
            strokeWidth = w * 0.065f
        )
        drawLine(
            color = Color.White,
            start = Offset(w * 0.64f, h * 0.06f),
            end = Offset(w * 0.75f, barY),
            strokeWidth = w * 0.065f
        )

        val playPath = Path().apply {
            moveTo(w * 0.42f, h * 0.47f)
            lineTo(w * 0.67f, h * 0.62f)
            lineTo(w * 0.42f, h * 0.77f)
            close()
        }
        drawPath(playPath, color = Color.White)
    }
}
