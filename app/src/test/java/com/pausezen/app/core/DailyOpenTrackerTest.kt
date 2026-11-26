package com.pausezen.app.core

import com.pausezen.app.data.DailyStatsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyOpenTrackerTest {

    @Test
    fun countsOncePerForegroundSession_andMeasuresScreenTime() {
        val tracker = DailyOpenTracker()
        val ime = setOf("com.google.android.inputmethod.latin")

        // Entering Instagram at t=10s counts as 1 open
        val t1 = tracker.onForegroundPackageChanged("com.instagram.android", nowMs = 10_000L, ignoredPackages = ime)
        assertTrue(t1.isNewOpen)

        // Another screen inside Instagram does NOT double-count
        val t2 = tracker.onForegroundPackageChanged("com.instagram.android", nowMs = 20_000L, ignoredPackages = ime)
        assertFalse(t2.isNewOpen)

        // Keyboard / SystemUI popups are ignored and do NOT reset the current app
        assertFalse(tracker.onForegroundPackageChanged("com.google.android.inputmethod.latin", 25_000L, ime).isNewOpen)
        assertFalse(tracker.onForegroundPackageChanged("com.android.systemui", 26_000L, ime).isNewOpen)
        assertFalse(tracker.onForegroundPackageChanged("com.instagram.android", 30_000L, ime).isNewOpen)

        // Switching to Discord at t=70s flushes 60s of Instagram screen time
        val t3 = tracker.onForegroundPackageChanged("com.discord", nowMs = 70_000L, ignoredPackages = ime)
        assertTrue(t3.isNewOpen)
        assertEquals("com.instagram.android", t3.previousPackage)
        assertEquals(60_000L, t3.previousDurationMs)

        // Screen off at t=100s flushes 30s of Discord screen time
        val flushed = tracker.checkpointSession(nowMs = 100_000L, clearCurrent = true)
        assertEquals("com.discord" to 30_000L, flushed)
    }

    @Test
    fun interceptedGuardedApp_isNotCountedUntilUserContinues() {
        val tracker = DailyOpenTracker()

        // Intercepted launch -> not counted yet
        val intercepted = tracker.onForegroundPackageChanged(
            packageName = "com.instagram.android",
            nowMs = 10_000L,
            isInterceptedByPause = true
        )
        assertFalse(intercepted.isNewOpen)

        // User taps Continue -> marked as foreground so returning to Instagram doesn't double-count
        tracker.markContinuedIntoApp("com.instagram.android", nowMs = 20_000L)
        assertFalse(tracker.onForegroundPackageChanged("com.instagram.android", nowMs = 21_000L).isNewOpen)
    }

    @Test
    fun dailyStatsSerialization_roundTripsOpensAndScreenTimeCleanly() {
        val opens = mapOf(
            "app:com.instagram.android" to 5,
            "feature:instagram_reels" to 2
        )
        assertEquals(opens, DailyStatsRepository.parseEntries(DailyStatsRepository.serializeEntries(opens)))

        val times = mapOf(
            "app:com.instagram.android" to 1_800_000L,
            "app:com.discord" to 420_000L
        )
        assertEquals(times, DailyStatsRepository.parseLongEntries(DailyStatsRepository.serializeLongEntries(times)))
    }
}
