package com.pausezen.app.core

data class ForegroundTransition(
    val isNewOpen: Boolean,
    val previousPackage: String? = null,
    val previousDurationMs: Long = 0L
)

/**
 * Tracks foreground app transitions and active session durations so each app open
 * is counted once per session and foreground screen time is measured accurately
 * (ignoring keyboards, SystemUI, in-app screen changes, and PauseZen itself).
 */
class DailyOpenTracker(
    private val selfPackageName: String = "com.pausezen.app"
) {
    private var currentForegroundPackage: String? = null
    private var sessionStartedAtMs: Long = 0L

    /**
     * Evaluates a foreground package transition.
     */
    @Synchronized
    fun onForegroundPackageChanged(
        packageName: String,
        nowMs: Long = 0L,
        ignoredPackages: Set<String> = emptySet(),
        isInterceptedByPause: Boolean = false
    ): ForegroundTransition {
        if (packageName.isBlank() ||
            packageName == selfPackageName ||
            packageName in DEFAULT_IGNORED_PACKAGES ||
            packageName in ignoredPackages
        ) {
            return ForegroundTransition(isNewOpen = false)
        }

        if (isInterceptedByPause) {
            // Flush any previous app's active time, but do not count this intercepted app yet.
            val flushed = flushCurrentSessionLocked(nowMs)
            return ForegroundTransition(
                isNewOpen = false,
                previousPackage = flushed?.first,
                previousDurationMs = flushed?.second ?: 0L
            )
        }

        if (packageName == currentForegroundPackage) {
            return ForegroundTransition(isNewOpen = false)
        }

        val flushed = flushCurrentSessionLocked(nowMs)
        currentForegroundPackage = packageName
        sessionStartedAtMs = nowMs
        return ForegroundTransition(
            isNewOpen = true,
            previousPackage = flushed?.first,
            previousDurationMs = flushed?.second ?: 0L
        )
    }

    /**
     * Called when the user finishes the pause countdown and taps "Continue".
     */
    @Synchronized
    fun markContinuedIntoApp(packageName: String, nowMs: Long = 0L) {
        if (packageName.isNotBlank() && packageName != selfPackageName) {
            currentForegroundPackage = packageName
            sessionStartedAtMs = nowMs
        }
    }

    /**
     * Flushes the current foreground session (e.g., on screen off or periodic checkpoint)
     * while optionally keeping the package active if the screen is still on.
     */
    @Synchronized
    fun checkpointSession(nowMs: Long, clearCurrent: Boolean): Pair<String, Long>? {
        val pkg = currentForegroundPackage ?: return null
        val delta = if (nowMs > sessionStartedAtMs && sessionStartedAtMs > 0L) {
            (nowMs - sessionStartedAtMs).coerceAtMost(MAX_SINGLE_SESSION_MS)
        } else {
            0L
        }
        if (clearCurrent) {
            currentForegroundPackage = null
            sessionStartedAtMs = 0L
        } else {
            sessionStartedAtMs = nowMs
        }
        return if (delta > 0L) pkg to delta else null
    }

    @Synchronized
    fun getActiveSession(nowMs: Long): Pair<String, Long>? {
        val pkg = currentForegroundPackage ?: return null
        val elapsed = if (nowMs > sessionStartedAtMs && sessionStartedAtMs > 0L) {
            (nowMs - sessionStartedAtMs).coerceAtMost(MAX_SINGLE_SESSION_MS)
        } else {
            0L
        }
        return pkg to elapsed
    }

    @Synchronized
    fun clearForeground() {
        currentForegroundPackage = null
        sessionStartedAtMs = 0L
    }

    private fun flushCurrentSessionLocked(nowMs: Long): Pair<String, Long>? {
        val prev = currentForegroundPackage ?: return null
        val delta = if (nowMs > sessionStartedAtMs && sessionStartedAtMs > 0L) {
            (nowMs - sessionStartedAtMs).coerceAtMost(MAX_SINGLE_SESSION_MS)
        } else {
            0L
        }
        currentForegroundPackage = null
        sessionStartedAtMs = 0L
        return if (delta > 0L) prev to delta else null
    }

    companion object {
        val shared = DailyOpenTracker()
        private const val MAX_SINGLE_SESSION_MS = 4 * 60 * 60 * 1000L // 4h sanity cap

        private val DEFAULT_IGNORED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller"
        )
    }
}
