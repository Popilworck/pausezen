package com.pausezen.app.core

import com.pausezen.app.data.AppGroup
import com.pausezen.app.data.AppGroupCodec
import com.pausezen.app.data.GuardConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InterceptionStateMachineTest {

    private lateinit var stateMachine: InterceptionStateMachine

    @Before
    fun setUp() {
        stateMachine = InterceptionStateMachine(
            selfPackageName = "com.pausezen.app",
            debounceWindowMs = 1_500L,
            dismissalCooldownMs = 1_200L
        )
    }

    @Test
    fun unguardedApp_isIgnored() {
        val group = AppGroup(name = "Social", packages = setOf("com.instagram.android"))
        val config = GuardConfig(appGroups = listOf(group))
        val decision = stateMachine.evaluateAppLaunch(
            packageName = "com.spotify.music",
            nowMs = 10_000L,
            config = config
        )
        assertEquals(InterceptionDecision.Ignore, decision)
    }

    @Test
    fun disabledGroup_isIgnored() {
        val disabledGroup = AppGroup(
            name = "Social",
            enabled = false,
            packages = setOf("com.instagram.android")
        )
        val config = GuardConfig(appGroups = listOf(disabledGroup))
        val decision = stateMachine.evaluateAppLaunch("com.instagram.android", 10_000L, config)
        assertEquals(InterceptionDecision.Ignore, decision)
    }

    @Test
    fun guardedApp_triggersPauseWithGroupSettingsAndDebouncesRapidDuplicates() {
        val group = AppGroup(
            name = "Social Media",
            packages = setOf("com.instagram.android"),
            pauseDurationSeconds = 15,
            customMessage = "Take a breath"
        )
        val config = GuardConfig(appGroups = listOf(group))

        val first = stateMachine.evaluateAppLaunch("com.instagram.android", 10_000L, config)
        assertTrue(first is InterceptionDecision.ShowPause)
        val pause = first as InterceptionDecision.ShowPause
        assertEquals("app:com.instagram.android", pause.targetKey)
        assertEquals("Social Media", pause.group.name)
        assertEquals(15, pause.group.pauseDurationSeconds)

        // Rapid duplicate event within 1500ms is debounced
        val duplicate = stateMachine.evaluateAppLaunch("com.instagram.android", 10_400L, config)
        assertEquals(InterceptionDecision.Ignore, duplicate)

        // After debounce window, it triggers again if not unlocked
        val afterDebounce = stateMachine.evaluateAppLaunch("com.instagram.android", 12_000L, config)
        assertTrue(afterDebounce is InterceptionDecision.ShowPause)
    }

    @Test
    fun unlockGracePeriod_allowsAppUntilExpiry() {
        val group = AppGroup(name = "Chat", packages = setOf("com.discord"))
        val config = GuardConfig(appGroups = listOf(group))
        val key = InterceptionStateMachine.appTargetKey("com.discord")

        stateMachine.grantUnlock(targetKey = key, durationMs = 60_000L, nowMs = 10_000L)

        // Within grace period -> allowed
        val during = stateMachine.evaluateAppLaunch("com.discord", 50_000L, config)
        assertEquals(InterceptionDecision.Ignore, during)

        // At/after expiry -> intercepted again
        val after = stateMachine.evaluateAppLaunch("com.discord", 70_000L, config)
        assertTrue(after is InterceptionDecision.ShowPause)
    }

    @Test
    fun systemUiAndKeyboardPackages_areAlwaysIgnored() {
        val group = AppGroup(
            name = "Test",
            packages = setOf("com.android.systemui", "com.google.android.inputmethod.latin")
        )
        val config = GuardConfig(appGroups = listOf(group))
        val imePackages = setOf("com.google.android.inputmethod.latin")

        assertEquals(
            InterceptionDecision.Ignore,
            stateMachine.evaluateAppLaunch("com.android.systemui", 10_000L, config, imePackages)
        )
        assertEquals(
            InterceptionDecision.Ignore,
            stateMachine.evaluateAppLaunch("com.google.android.inputmethod.latin", 10_000L, config, imePackages)
        )
    }

    @Test
    fun instagramReelsDetection_triggersOnlyWhenReelsViewIsVisibleOrSelected() {
        val reelsGroup = AppGroup(
            name = "Short-Form Video",
            featureRuleIds = setOf(InAppFeatureCatalog.INSTAGRAM_REELS.id),
            pauseDurationSeconds = 20
        )
        val config = GuardConfig(appGroups = listOf(reelsGroup))

        // Regular Instagram feed / DMs -> no Reels views present
        val feedInspector = FakeInspector()
        assertEquals(
            InterceptionDecision.Ignore,
            stateMachine.evaluateInAppFeature("com.instagram.android", 10_000L, config, feedInspector)
        )

        // Opening Reels -> clips_viewer_view_pager is visible
        val reelsInspector = FakeInspector(
            visibleIds = setOf("com.instagram.android:id/clips_viewer_view_pager")
        )
        val decision = stateMachine.evaluateInAppFeature("com.instagram.android", 10_500L, config, reelsInspector)
        assertTrue(decision is InterceptionDecision.ShowPause)
        val pause = decision as InterceptionDecision.ShowPause
        assertEquals("feature:instagram_reels", pause.targetKey)
        assertEquals("Instagram Reels", pause.featureDisplayName)
        assertEquals("Short-Form Video", pause.group.name)
        assertEquals(20, pause.group.pauseDurationSeconds)
    }

    @Test
    fun dismissalCooldown_preventsFlickerWhileExitingReels() {
        val group = AppGroup(
            name = "Shorts",
            featureRuleIds = setOf(InAppFeatureCatalog.YOUTUBE_SHORTS.id)
        )
        val config = GuardConfig(appGroups = listOf(group))
        val shortsInspector = FakeInspector(
            visibleIds = setOf("com.google.android.youtube:id/reel_recycler")
        )

        val first = stateMachine.evaluateInAppFeature("com.google.android.youtube", 10_000L, config, shortsInspector)
        assertTrue(first is InterceptionDecision.ShowPause)

        stateMachine.recordDismissal((first as InterceptionDecision.ShowPause).targetKey, nowMs = 10_100L)

        // 500ms later while back-navigation is settling -> suppressed
        val duringDismiss = stateMachine.evaluateInAppFeature("com.google.android.youtube", 10_600L, config, shortsInspector)
        assertEquals(InterceptionDecision.Ignore, duringDismiss)

        // After cooldown + debounce -> triggers again if user re-enters Shorts
        val later = stateMachine.evaluateInAppFeature("com.google.android.youtube", 12_000L, config, shortsInspector)
        assertTrue(later is InterceptionDecision.ShowPause)
    }

    @Test
    fun appGroupCodec_roundTripsSpecialCharactersAndOrdering() {
        val groups = listOf(
            AppGroup(
                id = "g1",
                name = "Social, Media",
                enabled = true,
                packages = setOf("com.instagram.android", "com.discord"),
                featureRuleIds = setOf("instagram_reels"),
                pauseDurationSeconds = 15,
                customMessage = "Wait, is this worth it?",
                unlockGraceMinutes = 10,
                dailyOpenLimit = 5,
                dailyTimeLimitMinutes = 30
            ),
            AppGroup(
                id = "g2",
                name = "Games",
                enabled = false,
                packages = setOf("com.supercell.clashroyale"),
                featureRuleIds = emptySet(),
                pauseDurationSeconds = 5,
                customMessage = "Play intentionally",
                unlockGraceMinutes = 5
            )
        )

        val encoded = AppGroupCodec.encodeAll(groups)
        val decoded = AppGroupCodec.decodeAll(encoded)
        assertEquals(groups, decoded)
    }

    private class FakeInspector(
        private val visibleIds: Set<String> = emptySet(),
        private val selectedIds: Set<String> = emptySet()
    ) : WindowNodeInspector {
        override fun hasVisibleViewId(viewId: String): Boolean = viewId in visibleIds
        override fun hasSelectedViewId(viewId: String): Boolean = viewId in selectedIds
    }
}
