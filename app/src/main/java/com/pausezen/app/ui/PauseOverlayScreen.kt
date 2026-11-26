package com.pausezen.app.ui

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pausezen.app.data.BreathingPattern
import com.pausezen.app.data.BreathingStyle
import com.pausezen.app.ui.components.BreathingGuide
import com.pausezen.app.ui.components.FeatureFeedLogo
import com.pausezen.app.ui.theme.PauseZenPalette
import kotlinx.coroutines.delay

@Composable
fun PauseOverlayScreen(
    targetDisplayName: String,
    groupName: String? = null,
    featureRuleId: String? = null,
    appIcon: Drawable?,
    customMessage: String,
    pauseDurationSeconds: Int,
    breathingCueEnabled: Boolean,
    breathingPattern: BreathingPattern = BreathingPattern.CALM,
    breathingStyle: BreathingStyle = BreathingStyle.BLOOM,
    isInAppFeature: Boolean,
    todayOpenCount: Int = 0,
    limitBlockReason: String? = null,
    groupOpensToday: Int = 0,
    groupOpenLimit: Int = 0,
    groupTimeTodayMs: Long = 0L,
    groupTimeLimitMinutes: Int = 0,
    onGoBack: () -> Unit,
    onContinue: () -> Unit
) {
    val isBlockedByLimit = !limitBlockReason.isNullOrBlank()
    BackHandler(enabled = true) {
        onGoBack()
    }

    val totalSeconds = pauseDurationSeconds.coerceAtLeast(1)
    var remainingSeconds by remember(totalSeconds) { mutableIntStateOf(totalSeconds) }

    LaunchedEffect(totalSeconds) {
        remainingSeconds = totalSeconds
        while (remainingSeconds > 0) {
            delay(1_000L)
            remainingSeconds--
        }
    }

    val timerProgress by animateFloatAsState(
        targetValue = (totalSeconds - remainingSeconds).toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "timerProgress"
    )

    val iconBitmap = remember(appIcon) {
        appIcon?.let { runCatching { it.toBitmap(width = 96, height = 96).asImageBitmap() }.getOrNull() }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = PauseZenPalette.Canvas
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top target & group header
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (featureRuleId != null && featureRuleId != "preview") {
                    FeatureFeedLogo(ruleId = featureRuleId, size = 54.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (featureRuleId == "preview") {
                    FeatureFeedLogo(ruleId = "instagram_reels", size = 54.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                } else if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = targetDisplayName,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "Pausing $targetDisplayName",
                    style = MaterialTheme.typography.titleMedium,
                    color = PauseZenPalette.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!groupName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = PauseZenPalette.Surface,
                            border = BorderStroke(1.dp, PauseZenPalette.Hairline)
                        ) {
                            Text(
                                text = groupName,
                                style = MaterialTheme.typography.labelSmall,
                                color = PauseZenPalette.TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = buildString {
                            append(
                                when {
                                    groupOpenLimit > 0 -> "$groupOpensToday / $groupOpenLimit opens"
                                    todayOpenCount == 0 -> "First open today"
                                    todayOpenCount == 1 -> "1 open today"
                                    else -> "$todayOpenCount opens today"
                                }
                            )
                            if (groupTimeLimitMinutes > 0) {
                                val usedMins = (groupTimeTodayMs / 60_000L).toInt()
                                append(" · ${usedMins}m / ${groupTimeLimitMinutes}m")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isBlockedByLimit) PauseZenPalette.Danger else PauseZenPalette.Accent
                    )
                }
            }

            // Centerpiece: Dedicated breathing animation + editorial serif prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (breathingCueEnabled) {
                    BreathingGuide(
                        pattern = breathingPattern,
                        style = breathingStyle
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }

                Text(
                    text = if (isBlockedByLimit) limitBlockReason!! else customMessage,
                    style = MaterialTheme.typography.headlineMedium,
                    color = PauseZenPalette.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            // Bottom: Separate linear countdown timer + pill action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isBlockedByLimit) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (remainingSeconds > 0) "Mindful pause" else "Ready",
                                style = MaterialTheme.typography.labelMedium,
                                color = PauseZenPalette.TextSecondary
                            )
                            Text(
                                text = if (remainingSeconds > 0) "${remainingSeconds}s left" else "0s",
                                style = MaterialTheme.typography.labelLarge,
                                color = PauseZenPalette.Accent
                            )
                        }
                        LinearProgressIndicator(
                            progress = { timerProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = PauseZenPalette.Accent,
                            trackColor = PauseZenPalette.SurfaceControl,
                            strokeCap = StrokeCap.Round
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    Text(
                        text = "Limit resets automatically at midnight",
                        style = MaterialTheme.typography.bodySmall,
                        color = PauseZenPalette.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = onGoBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PauseZenPalette.Accent,
                        contentColor = PauseZenPalette.OnAccent
                    )
                ) {
                    Text(
                        text = if (isInAppFeature) "Leave $targetDisplayName" else "Never mind, go back",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!isBlockedByLimit) {
                    OutlinedButton(
                        onClick = onContinue,
                        enabled = remainingSeconds == 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, PauseZenPalette.Hairline)
                    ) {
                        Text(
                            text = if (remainingSeconds > 0) {
                                "Continue in ${remainingSeconds}s"
                            } else {
                                "Continue to $targetDisplayName"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (remainingSeconds == 0) PauseZenPalette.TextPrimary else PauseZenPalette.TextMuted
                        )
                    }
                }
            }
        }
    }
}
