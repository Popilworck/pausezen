package com.pausezen.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pausezen.app.core.DailyOpenTracker
import com.pausezen.app.core.InstalledAppsProvider
import com.pausezen.app.core.InterceptionStateMachine
import com.pausezen.app.data.BreathingPattern
import com.pausezen.app.data.BreathingStyle
import com.pausezen.app.data.DailyStatsRepository
import com.pausezen.app.data.GuardConfig
import com.pausezen.app.service.AppGuardAccessibilityService
import com.pausezen.app.ui.PauseOverlayScreen
import com.pausezen.app.ui.theme.PauseZenTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PauseOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        renderFromIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderFromIntent(intent)
    }

    private fun renderFromIntent(intent: Intent) {
        val targetKey = intent.getStringExtra(EXTRA_TARGET_KEY) ?: ""
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val featureRuleId = intent.getStringExtra(EXTRA_FEATURE_RULE_ID)
        val featureDisplayName = intent.getStringExtra(EXTRA_FEATURE_DISPLAY_NAME)
        val groupName = intent.getStringExtra(EXTRA_GROUP_NAME)
        val pauseSeconds = intent.getIntExtra(EXTRA_PAUSE_SECONDS, GuardConfig.DEFAULT_PAUSE_SECONDS)
        val customMessage = intent.getStringExtra(EXTRA_CUSTOM_MESSAGE) ?: GuardConfig.DEFAULT_MESSAGE
        val unlockGraceMs = intent.getLongExtra(EXTRA_UNLOCK_GRACE_MS, GuardConfig.DEFAULT_GRACE_MINUTES * 60_000L)
        val breathingEnabled = intent.getBooleanExtra(EXTRA_BREATHING_ENABLED, true)
        val breathingPattern = BreathingPattern.fromId(intent.getStringExtra(EXTRA_BREATHING_PATTERN))
        val breathingStyle = BreathingStyle.fromId(intent.getStringExtra(EXTRA_BREATHING_STYLE))
        val todayOpenCount = intent.getIntExtra(EXTRA_TODAY_OPEN_COUNT, 0)
        val limitBlockReason = intent.getStringExtra(EXTRA_LIMIT_BLOCK_REASON)
        val groupOpensToday = intent.getIntExtra(EXTRA_GROUP_OPENS_TODAY, 0)
        val groupOpenLimit = intent.getIntExtra(EXTRA_GROUP_OPEN_LIMIT, 0)
        val groupTimeTodayMs = intent.getLongExtra(EXTRA_GROUP_TIME_TODAY_MS, 0L)
        val groupTimeLimitMinutes = intent.getIntExtra(EXTRA_GROUP_TIME_LIMIT_MINUTES, 0)
        val isPreview = intent.getBooleanExtra(EXTRA_IS_PREVIEW, false)

        val appLabel = if (packageName.isNotBlank()) {
            InstalledAppsProvider.getAppLabel(this, packageName)
        } else {
            "App"
        }
        val targetDisplayName = featureDisplayName ?: appLabel
        val appIcon = if (packageName.isNotBlank()) {
            InstalledAppsProvider.getAppIcon(this, packageName)
        } else {
            null
        }
        val isInAppFeature = featureRuleId != null

        setContent {
            PauseZenTheme {
                PauseOverlayScreen(
                    targetDisplayName = targetDisplayName,
                    groupName = groupName,
                    featureRuleId = featureRuleId,
                    appIcon = appIcon,
                    customMessage = customMessage,
                    pauseDurationSeconds = pauseSeconds,
                    breathingCueEnabled = breathingEnabled,
                    breathingPattern = breathingPattern,
                    breathingStyle = breathingStyle,
                    isInAppFeature = isInAppFeature,
                    todayOpenCount = todayOpenCount,
                    limitBlockReason = limitBlockReason,
                    groupOpensToday = groupOpensToday,
                    groupOpenLimit = groupOpenLimit,
                    groupTimeTodayMs = groupTimeTodayMs,
                    groupTimeLimitMinutes = groupTimeLimitMinutes,
                    onGoBack = {
                        if (!isPreview && targetKey.isNotBlank()) {
                            InterceptionStateMachine.shared.recordDismissal(
                                targetKey = targetKey,
                                nowMs = SystemClock.elapsedRealtime()
                            )
                            if (isInAppFeature) {
                                // Exit the Reels/Shorts view via Back while keeping the rest of the app open.
                                AppGuardAccessibilityService.requestNavigateBack(delayMs = 220L)
                            } else {
                                DailyOpenTracker.shared.clearForeground()
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                startActivity(homeIntent)
                            }
                        }
                        finish()
                    },
                    onContinue = {
                        if (!isPreview && targetKey.isNotBlank()) {
                            InterceptionStateMachine.shared.grantUnlock(
                                targetKey = targetKey,
                                durationMs = unlockGraceMs,
                                nowMs = SystemClock.elapsedRealtime()
                            )
                            if (!isInAppFeature && packageName.isNotBlank()) {
                                DailyOpenTracker.shared.markContinuedIntoApp(
                                    packageName = packageName,
                                    nowMs = SystemClock.elapsedRealtime()
                                )
                            }
                            val statsRepo = DailyStatsRepository(applicationContext)
                            CoroutineScope(Dispatchers.IO).launch {
                                statsRepo.incrementOpen(targetKey)
                            }
                        }
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_TARGET_KEY = "extra_target_key"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_FEATURE_RULE_ID = "extra_feature_rule_id"
        const val EXTRA_FEATURE_DISPLAY_NAME = "extra_feature_display_name"
        const val EXTRA_GROUP_NAME = "extra_group_name"
        const val EXTRA_PAUSE_SECONDS = "extra_pause_seconds"
        const val EXTRA_CUSTOM_MESSAGE = "extra_custom_message"
        const val EXTRA_UNLOCK_GRACE_MS = "extra_unlock_grace_ms"
        const val EXTRA_BREATHING_ENABLED = "extra_breathing_enabled"
        const val EXTRA_BREATHING_PATTERN = "extra_breathing_pattern"
        const val EXTRA_BREATHING_STYLE = "extra_breathing_style"
        const val EXTRA_TODAY_OPEN_COUNT = "extra_today_open_count"
        const val EXTRA_LIMIT_BLOCK_REASON = "extra_limit_block_reason"
        const val EXTRA_GROUP_OPENS_TODAY = "extra_group_opens_today"
        const val EXTRA_GROUP_OPEN_LIMIT = "extra_group_open_limit"
        const val EXTRA_GROUP_TIME_TODAY_MS = "extra_group_time_today_ms"
        const val EXTRA_GROUP_TIME_LIMIT_MINUTES = "extra_group_time_limit_minutes"
        const val EXTRA_IS_PREVIEW = "extra_is_preview"
    }
}
