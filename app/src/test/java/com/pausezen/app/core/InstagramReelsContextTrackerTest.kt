package com.pausezen.app.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramReelsContextTrackerTest {

    @Test
    fun dmReel_isAllowedInitially_andInterceptedOnceUserScrolls() {
        val tracker = InstagramReelsContextTracker()

        // 1. User is in an Instagram DM thread
        assertTrue(
            tracker.shouldAllowWatching(
                nowMs = 10_000L,
                allowDmReels = true,
                isDmVisible = true,
                isReelsTabSelected = false,
                isReelsViewerVisible = false,
                isScrollEvent = false
            )
        )

        // 2. User taps a Reel sent in that DM (1s later) -> allowed
        assertTrue(
            tracker.shouldAllowWatching(
                nowMs = 11_000L,
                allowDmReels = true,
                isDmVisible = false,
                isReelsTabSelected = false,
                isReelsViewerVisible = true,
                isScrollEvent = false
            )
        )

        // 3. Initial layout settle scroll event (< 900ms) -> still allowed
        assertTrue(
            tracker.shouldAllowWatching(
                nowMs = 11_300L,
                allowDmReels = true,
                isDmVisible = false,
                isReelsTabSelected = false,
                isReelsViewerVisible = true,
                isScrollEvent = true
            )
        )

        // 4. User swipes up to the next Reel (3s later) -> intercepted!
        assertFalse(
            tracker.shouldAllowWatching(
                nowMs = 14_000L,
                allowDmReels = true,
                isDmVisible = false,
                isReelsTabSelected = false,
                isReelsViewerVisible = true,
                isScrollEvent = true
            )
        )
    }

    @Test
    fun reelsTabTap_isAlwaysInterceptedEvenAfterDm() {
        val tracker = InstagramReelsContextTracker()

        tracker.shouldAllowWatching(10_000L, true, isDmVisible = true, isReelsTabSelected = false, isReelsViewerVisible = false, isScrollEvent = false)

        // User explicitly taps the bottom Reels tab -> intercepted immediately
        assertFalse(
            tracker.shouldAllowWatching(
                nowMs = 11_000L,
                allowDmReels = true,
                isDmVisible = false,
                isReelsTabSelected = true,
                isReelsViewerVisible = true,
                isScrollEvent = false
            )
        )
    }

    @Test
    fun reelOpenedWithoutDm_orWithSettingOff_isIntercepted() {
        val tracker = InstagramReelsContextTracker()

        // Opened from Explore/Feed without coming from DM -> intercepted
        assertFalse(
            tracker.shouldAllowWatching(
                nowMs = 10_000L,
                allowDmReels = true,
                isDmVisible = false,
                isReelsTabSelected = false,
                isReelsViewerVisible = true,
                isScrollEvent = false
            )
        )

        // Setting disabled -> intercepted even right after DM
        tracker.shouldAllowWatching(20_000L, true, isDmVisible = true, isReelsTabSelected = false, isReelsViewerVisible = false, isScrollEvent = false)
        assertFalse(
            tracker.shouldAllowWatching(
                nowMs = 20_500L,
                allowDmReels = false,
                isDmVisible = false,
                isReelsTabSelected = false,
                isReelsViewerVisible = true,
                isScrollEvent = false
            )
        )
    }
}
