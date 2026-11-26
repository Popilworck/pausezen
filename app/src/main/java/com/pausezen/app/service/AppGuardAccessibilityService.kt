package com.pausezen.app.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import com.pausezen.app.PauseOverlayActivity
import com.pausezen.app.core.DailyOpenTracker
import com.pausezen.app.core.InAppFeatureCatalog
import com.pausezen.app.core.InstagramReelsContextTracker
import com.pausezen.app.core.InterceptionDecision
import com.pausezen.app.core.InterceptionStateMachine
import com.pausezen.app.core.WindowNodeInspector
import com.pausezen.app.data.AppGroup
import com.pausezen.app.data.DailyStatsRepository
import com.pausezen.app.data.GuardConfig
import com.pausezen.app.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class AppGuardAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val reelsContextTracker = InstagramReelsContextTracker()
    private lateinit var dailyStatsRepository: DailyStatsRepository

    @Volatile
    private var currentConfig: GuardConfig = GuardConfig()

    @Volatile
    private var todayOpens: Map<String, Int> = emptyMap()

    @Volatile
    private var todayScreenTimeMs: Map<String, Long> = emptyMap()

    @Volatile
    private var imePackages: Set<String> = emptySet()

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                val flushed = DailyOpenTracker.shared.checkpointSession(
                    nowMs = SystemClock.elapsedRealtime(),
                    clearCurrent = true
                )
                if (flushed != null && ::dailyStatsRepository.isInitialized) {
                    val key = InterceptionStateMachine.appTargetKey(flushed.first)
                    serviceScope.launch {
                        dailyStatsRepository.addScreenTimeMs(key, flushed.second)
                    }
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = WeakReference(this)
        imePackages = loadImePackages()

        val repository = PreferencesRepository(applicationContext)
        dailyStatsRepository = DailyStatsRepository(applicationContext)

        runCatching {
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        }

        serviceScope.launch {
            repository.configFlow.collectLatest { config ->
                currentConfig = config
            }
        }
        serviceScope.launch {
            dailyStatsRepository.todayOpensFlow.collectLatest { opens ->
                todayOpens = opens
            }
        }
        serviceScope.launch {
            dailyStatsRepository.todayScreenTimeFlow.collectLatest { times ->
                todayScreenTimeMs = times
            }
        }

        // Periodic checkpoint + live daily screen time limit enforcement while an app is open.
        serviceScope.launch {
            var tickCount = 0
            while (isActive) {
                delay(15_000L)
                tickCount++
                val nowMs = SystemClock.elapsedRealtime()
                val active = DailyOpenTracker.shared.getActiveSession(nowMs) ?: continue
                val (activePkg, activeElapsedMs) = active

                // Flush accumulated screen time every 60 seconds so long sessions persist cleanly.
                if (tickCount % 4 == 0 && activeElapsedMs > 0L) {
                    val flushed = DailyOpenTracker.shared.checkpointSession(nowMs, clearCurrent = false)
                    if (flushed != null) {
                        dailyStatsRepository.addScreenTimeMs(
                            InterceptionStateMachine.appTargetKey(flushed.first),
                            flushed.second
                        )
                    }
                }

                // Check if active app's group has exceeded its daily screen time limit.
                val config = currentConfig
                val group = config.findGroupForPackage(activePkg) ?: continue
                if (group.dailyTimeLimitMinutes > 0) {
                    val totalGroupTimeMs = computeGroupScreenTimeMs(group, activePkg, activeElapsedMs)
                    if (totalGroupTimeMs >= group.dailyTimeLimitMs) {
                        val flushed = DailyOpenTracker.shared.checkpointSession(nowMs, clearCurrent = true)
                        if (flushed != null) {
                            dailyStatsRepository.addScreenTimeMs(
                                InterceptionStateMachine.appTargetKey(flushed.first),
                                flushed.second
                            )
                        }
                        val decision = InterceptionDecision.ShowPause(
                            targetKey = InterceptionStateMachine.appTargetKey(activePkg),
                            packageName = activePkg,
                            group = group
                        )
                        launchPauseOverlay(decision, config, activeElapsedMs = 0L, countAsAttempt = false)
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        val config = currentConfig
        val nowMs = SystemClock.elapsedRealtime()

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val appDecision = InterceptionStateMachine.shared.evaluateAppLaunch(
                packageName = pkg,
                nowMs = nowMs,
                config = config,
                ignoredPackages = imePackages
            )
            if (appDecision is InterceptionDecision.ShowPause) {
                val transition = DailyOpenTracker.shared.onForegroundPackageChanged(
                    packageName = pkg,
                    nowMs = nowMs,
                    ignoredPackages = imePackages,
                    isInterceptedByPause = true
                )
                flushPreviousDuration(transition.previousPackage, transition.previousDurationMs)
                launchPauseOverlay(appDecision, config)
                return
            }

            // If this app is within its unlock grace period, still enforce daily screen time limit!
            val activeGroup = config.findGroupForPackage(pkg)
            if (activeGroup != null && activeGroup.dailyTimeLimitMinutes > 0) {
                val totalGroupTimeMs = computeGroupScreenTimeMs(activeGroup, pkg, 0L)
                if (totalGroupTimeMs >= activeGroup.dailyTimeLimitMs) {
                    val decision = InterceptionDecision.ShowPause(
                        targetKey = InterceptionStateMachine.appTargetKey(pkg),
                        packageName = pkg,
                        group = activeGroup
                    )
                    launchPauseOverlay(decision, config)
                    return
                }
            }

            // Track foreground transitions to flush previous app's screen time.
            // Daily opens are incremented exclusively when the user completes the pause timer
            // and taps "Continue" in PauseOverlayActivity.
            val transition = DailyOpenTracker.shared.onForegroundPackageChanged(
                packageName = pkg,
                nowMs = nowMs,
                ignoredPackages = imePackages,
                isInterceptedByPause = false
            )
            flushPreviousDuration(transition.previousPackage, transition.previousDurationMs)
        }

        if (pkg != InstagramReelsContextTracker.INSTAGRAM_PACKAGE &&
            pkg != packageName &&
            pkg !in imePackages &&
            pkg != "com.android.systemui" &&
            pkg != "android"
        ) {
            reelsContextTracker.reset()
        }

        // Only inspect window view IDs when a user-enabled group has an in-app rule for this package.
        val enabledRulesForPkg = InAppFeatureCatalog.rulesForPackage(pkg, config.allActiveFeatureRuleIds)
        if (enabledRulesForPkg.isNotEmpty()) {
            val rootNode = runCatching { rootInActiveWindow }.getOrNull() ?: return
            val inspector = AndroidWindowNodeInspector(rootNode)

            // If Instagram Reels is enabled and "Allow Reels sent in DMs" is on, check whether
            // the user is watching a single DM-sent Reel without having scrolled yet.
            var effectiveConfig = config
            if (pkg == InstagramReelsContextTracker.INSTAGRAM_PACKAGE &&
                InAppFeatureCatalog.INSTAGRAM_REELS.id in config.allActiveFeatureRuleIds
            ) {
                val isDmVisible = InstagramReelsContextTracker.DM_VIEW_IDS.any {
                    inspector.hasVisibleViewId(it)
                }
                val isReelsTabSelected = InAppFeatureCatalog.INSTAGRAM_REELS.selectedTabViewIds.any {
                    inspector.hasSelectedViewId(it)
                }
                val isReelsViewerVisible = InAppFeatureCatalog.INSTAGRAM_REELS.matchingViewIds.any {
                    inspector.hasVisibleViewId(it)
                }
                val isScrollEvent = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED

                val allowWatching = reelsContextTracker.shouldAllowWatching(
                    nowMs = nowMs,
                    allowDmReels = config.allowDmReels,
                    isDmVisible = isDmVisible,
                    isReelsTabSelected = isReelsTabSelected,
                    isReelsViewerVisible = isReelsViewerVisible,
                    isScrollEvent = isScrollEvent
                )
                if (allowWatching && isReelsViewerVisible) {
                    // Temporarily suppress only the INSTAGRAM_REELS rule while watching this single DM Reel.
                    effectiveConfig = config.copy(
                        appGroups = config.appGroups.map { g ->
                            g.copy(featureRuleIds = g.featureRuleIds - InAppFeatureCatalog.INSTAGRAM_REELS.id)
                        }
                    )
                }
            }

            val featureDecision = InterceptionStateMachine.shared.evaluateInAppFeature(
                packageName = pkg,
                nowMs = nowMs,
                config = effectiveConfig,
                inspector = inspector,
                ignoredPackages = imePackages
            )
            if (featureDecision is InterceptionDecision.ShowPause) {
                launchPauseOverlay(featureDecision, config)
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOffReceiver) }
        activeInstance = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun flushPreviousDuration(previousPackage: String?, durationMs: Long) {
        if (previousPackage.isNullOrBlank() || durationMs <= 0L || !::dailyStatsRepository.isInitialized) return
        val key = InterceptionStateMachine.appTargetKey(previousPackage)
        serviceScope.launch {
            dailyStatsRepository.addScreenTimeMs(key, durationMs)
        }
    }

    private fun computeGroupOpensToday(group: AppGroup): Int {
        val pkgOpens = group.packages.sumOf {
            todayOpens[InterceptionStateMachine.appTargetKey(it)] ?: 0
        }
        val feedOpens = group.featureRuleIds.sumOf {
            todayOpens[InterceptionStateMachine.featureTargetKey(it)] ?: 0
        }
        return pkgOpens + feedOpens
    }

    private fun computeGroupScreenTimeMs(
        group: AppGroup,
        activePkg: String? = null,
        activeElapsedMs: Long = 0L
    ): Long {
        val trackedPackages = if (group.packages.isNotEmpty()) {
            group.packages
        } else {
            group.featureRuleIds
                .mapNotNull { InAppFeatureCatalog.findById(it) }
                .flatMap { it.hostPackages }
                .toSet()
        }
        val storedMs = trackedPackages.sumOf {
            todayScreenTimeMs[InterceptionStateMachine.appTargetKey(it)] ?: 0L
        }
        val extra = if (activePkg != null && activePkg in trackedPackages) activeElapsedMs else 0L
        return storedMs + extra
    }

    private fun launchPauseOverlay(
        decision: InterceptionDecision.ShowPause,
        config: GuardConfig,
        activeElapsedMs: Long = 0L,
        countAsAttempt: Boolean = true
    ) {
        if (countAsAttempt && ::dailyStatsRepository.isInitialized) {
            serviceScope.launch {
                dailyStatsRepository.incrementAttempt(decision.targetKey)
            }
        }

        val group = decision.group
        val groupOpens = computeGroupOpensToday(group)
        val groupTimeMs = computeGroupScreenTimeMs(group, decision.packageName, activeElapsedMs)

        val limitBlockReason = when {
            group.dailyTimeLimitMinutes > 0 && groupTimeMs >= group.dailyTimeLimitMs -> {
                "Daily screen time limit reached (${group.dailyTimeLimitMinutes}m today)"
            }
            group.dailyOpenLimit > 0 && groupOpens >= group.dailyOpenLimit -> {
                "Daily open limit reached ($groupOpens / ${group.dailyOpenLimit} opens today)"
            }
            else -> null
        }

        val intent = Intent(this, PauseOverlayActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra(PauseOverlayActivity.EXTRA_TARGET_KEY, decision.targetKey)
            putExtra(PauseOverlayActivity.EXTRA_PACKAGE_NAME, decision.packageName)
            putExtra(PauseOverlayActivity.EXTRA_FEATURE_RULE_ID, decision.featureRuleId)
            putExtra(PauseOverlayActivity.EXTRA_FEATURE_DISPLAY_NAME, decision.featureDisplayName)
            putExtra(PauseOverlayActivity.EXTRA_GROUP_NAME, group.name)
            putExtra(PauseOverlayActivity.EXTRA_PAUSE_SECONDS, group.pauseDurationSeconds)
            putExtra(PauseOverlayActivity.EXTRA_CUSTOM_MESSAGE, group.customMessage)
            putExtra(PauseOverlayActivity.EXTRA_UNLOCK_GRACE_MS, group.unlockGraceMs)
            putExtra(PauseOverlayActivity.EXTRA_BREATHING_ENABLED, config.breathingCueEnabled)
            putExtra(PauseOverlayActivity.EXTRA_BREATHING_PATTERN, config.breathingPattern.id)
            putExtra(PauseOverlayActivity.EXTRA_BREATHING_STYLE, config.breathingStyle.id)
            putExtra(PauseOverlayActivity.EXTRA_TODAY_OPEN_COUNT, todayOpens[decision.targetKey] ?: 0)
            putExtra(PauseOverlayActivity.EXTRA_LIMIT_BLOCK_REASON, limitBlockReason)
            putExtra(PauseOverlayActivity.EXTRA_GROUP_OPENS_TODAY, groupOpens)
            putExtra(PauseOverlayActivity.EXTRA_GROUP_OPEN_LIMIT, group.dailyOpenLimit)
            putExtra(PauseOverlayActivity.EXTRA_GROUP_TIME_TODAY_MS, groupTimeMs)
            putExtra(PauseOverlayActivity.EXTRA_GROUP_TIME_LIMIT_MINUTES, group.dailyTimeLimitMinutes)
        }
        startActivity(intent)
    }

    private fun loadImePackages(): Set<String> {
        return runCatching {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.enabledInputMethodList?.mapNotNull { it.packageName }?.toSet() ?: emptySet()
        }.getOrDefault(emptySet())
    }

    private class AndroidWindowNodeInspector(
        private val root: AccessibilityNodeInfo
    ) : WindowNodeInspector {
        override fun hasVisibleViewId(viewId: String): Boolean {
            val nodes = runCatching { root.findAccessibilityNodeInfosByViewId(viewId) }.getOrNull()
                ?: return false
            return nodes.any { it.isVisibleToUser }
        }

        override fun hasSelectedViewId(viewId: String): Boolean {
            val nodes = runCatching { root.findAccessibilityNodeInfosByViewId(viewId) }.getOrNull()
                ?: return false
            return nodes.any { it.isVisibleToUser && it.isSelected }
        }
    }

    companion object {
        @Volatile
        private var activeInstance: WeakReference<AppGuardAccessibilityService>? = null

        fun isServiceEnabled(context: Context): Boolean {
            if (activeInstance?.get() != null) return true
            val enabledServices = runCatching {
                Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                )
            }.getOrNull() ?: return false
            val expectedComponent = "${context.packageName}/${AppGuardAccessibilityService::class.java.name}"
            val shortComponent = "${context.packageName}/.service.AppGuardAccessibilityService"
            return enabledServices.split(':').any {
                it.equals(expectedComponent, ignoreCase = true) ||
                    it.equals(shortComponent, ignoreCase = true)
            }
        }

        fun requestNavigateBack(delayMs: Long = 220L) {
            val service = activeInstance?.get() ?: return
            service.mainHandler.postDelayed({
                service.performGlobalAction(GLOBAL_ACTION_BACK)
            }, delayMs)
        }
    }
}
