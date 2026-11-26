package com.pausezen.app.core

import com.pausezen.app.data.AppGroup
import com.pausezen.app.data.GuardConfig

sealed class InterceptionDecision {
    data object Ignore : InterceptionDecision()

    data class ShowPause(
        val targetKey: String,
        val packageName: String,
        val group: AppGroup,
        val featureRuleId: String? = null,
        val featureDisplayName: String? = null
    ) : InterceptionDecision()
}

class InterceptionStateMachine(
    private val selfPackageName: String = "com.pausezen.app",
    private val debounceWindowMs: Long = 1_500L,
    private val dismissalCooldownMs: Long = 1_200L
) {
    private val unlockedUntilByKey = mutableMapOf<String, Long>()
    private val lastTriggeredAtByKey = mutableMapOf<String, Long>()
    private val dismissedUntilByKey = mutableMapOf<String, Long>()

    @Synchronized
    fun evaluateAppLaunch(
        packageName: String,
        nowMs: Long,
        config: GuardConfig,
        ignoredPackages: Set<String> = emptySet()
    ): InterceptionDecision {
        if (isIgnoredPackage(packageName, ignoredPackages)) {
            return InterceptionDecision.Ignore
        }

        clearExpired(nowMs)

        val matchedGroup = config.findGroupForPackage(packageName)
            ?: return InterceptionDecision.Ignore

        val targetKey = appTargetKey(packageName)
        if (isSuppressed(targetKey, nowMs)) {
            return InterceptionDecision.Ignore
        }

        lastTriggeredAtByKey[targetKey] = nowMs
        return InterceptionDecision.ShowPause(
            targetKey = targetKey,
            packageName = packageName,
            group = matchedGroup
        )
    }

    @Synchronized
    fun evaluateInAppFeature(
        packageName: String,
        nowMs: Long,
        config: GuardConfig,
        inspector: WindowNodeInspector,
        ignoredPackages: Set<String> = emptySet()
    ): InterceptionDecision {
        if (isIgnoredPackage(packageName, ignoredPackages)) {
            return InterceptionDecision.Ignore
        }

        clearExpired(nowMs)

        val matchedRule = InAppFeatureDetector.detectMatchingRule(
            packageName = packageName,
            enabledRuleIds = config.allActiveFeatureRuleIds,
            inspector = inspector
        ) ?: return InterceptionDecision.Ignore

        val matchedGroup = config.findGroupForFeatureRule(matchedRule.id)
            ?: return InterceptionDecision.Ignore

        val targetKey = featureTargetKey(matchedRule.id)
        if (isSuppressed(targetKey, nowMs)) {
            return InterceptionDecision.Ignore
        }

        lastTriggeredAtByKey[targetKey] = nowMs
        return InterceptionDecision.ShowPause(
            targetKey = targetKey,
            packageName = packageName,
            group = matchedGroup,
            featureRuleId = matchedRule.id,
            featureDisplayName = matchedRule.displayName
        )
    }

    @Synchronized
    fun grantUnlock(targetKey: String, durationMs: Long, nowMs: Long) {
        unlockedUntilByKey[targetKey] = nowMs + durationMs.coerceAtLeast(0L)
    }

    @Synchronized
    fun recordDismissal(targetKey: String, nowMs: Long) {
        dismissedUntilByKey[targetKey] = nowMs + dismissalCooldownMs
    }

    @Synchronized
    fun isUnlocked(targetKey: String, nowMs: Long): Boolean {
        val expiry = unlockedUntilByKey[targetKey] ?: return false
        return nowMs < expiry
    }

    @Synchronized
    fun reset() {
        unlockedUntilByKey.clear()
        lastTriggeredAtByKey.clear()
        dismissedUntilByKey.clear()
    }

    private fun isSuppressed(targetKey: String, nowMs: Long): Boolean {
        val unlockExpiry = unlockedUntilByKey[targetKey]
        if (unlockExpiry != null && nowMs < unlockExpiry) {
            return true
        }
        val dismissCooldown = dismissedUntilByKey[targetKey]
        if (dismissCooldown != null && nowMs < dismissCooldown) {
            return true
        }
        val lastTrigger = lastTriggeredAtByKey[targetKey]
        if (lastTrigger != null && (nowMs - lastTrigger) < debounceWindowMs) {
            return true
        }
        return false
    }

    private fun isIgnoredPackage(packageName: String, ignoredPackages: Set<String>): Boolean {
        return packageName.isBlank() ||
            packageName == selfPackageName ||
            packageName in DEFAULT_IGNORED_PACKAGES ||
            packageName in ignoredPackages
    }

    private fun clearExpired(nowMs: Long) {
        unlockedUntilByKey.entries.removeAll { (_, expiry) -> nowMs >= expiry }
        dismissedUntilByKey.entries.removeAll { (_, expiry) -> nowMs >= expiry }
    }

    companion object {
        val shared = InterceptionStateMachine()

        private val DEFAULT_IGNORED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )

        fun appTargetKey(packageName: String): String = "app:$packageName"
        fun featureTargetKey(ruleId: String): String = "feature:$ruleId"
    }
}
