package com.pausezen.app.core

/**
 * Distinguishes watching a single Reel opened from an Instagram DM thread
 * vs. opening the Reels tab or scrolling past the sent Reel into doomscrolling.
 */
class InstagramReelsContextTracker(
    private val dmTransitionWindowMs: Long = 3_500L,
    private val initialLayoutSettleMs: Long = 900L
) {
    private var lastSeenInDmAtMs: Long = 0L
    private var inDmReelSession: Boolean = false
    private var dmReelEnteredAtMs: Long = 0L

    /**
     * Evaluates the current Instagram window state.
     *
     * @return true if the user is allowed to keep watching (e.g. viewing a single DM-sent Reel
     * without having scrolled to another Reel), or false if PauseZen should intercept.
     */
    @Synchronized
    fun shouldAllowWatching(
        nowMs: Long,
        allowDmReels: Boolean,
        isDmVisible: Boolean,
        isReelsTabSelected: Boolean,
        isReelsViewerVisible: Boolean,
        isScrollEvent: Boolean
    ): Boolean {
        if (!allowDmReels) {
            inDmReelSession = false
            return false
        }

        if (isDmVisible && !isReelsViewerVisible) {
            lastSeenInDmAtMs = nowMs
            inDmReelSession = false
            dmReelEnteredAtMs = 0L
            return true
        }

        if (isReelsTabSelected) {
            inDmReelSession = false
            lastSeenInDmAtMs = 0L
            return false
        }

        if (!isReelsViewerVisible) {
            inDmReelSession = false
            dmReelEnteredAtMs = 0L
            return true
        }

        // Reels viewer is visible and bottom Reels tab is NOT selected.
        if (!inDmReelSession) {
            val cameFromDm = lastSeenInDmAtMs > 0L && (nowMs - lastSeenInDmAtMs) <= dmTransitionWindowMs
            return if (cameFromDm) {
                inDmReelSession = true
                dmReelEnteredAtMs = nowMs
                true
            } else {
                false
            }
        }

        // Already watching a DM-sent Reel: intercept as soon as the user scrolls to the next Reel.
        if (isScrollEvent && (nowMs - dmReelEnteredAtMs) >= initialLayoutSettleMs) {
            inDmReelSession = false
            lastSeenInDmAtMs = 0L
            dmReelEnteredAtMs = 0L
            return false
        }

        return true
    }

    @Synchronized
    fun reset() {
        lastSeenInDmAtMs = 0L
        inDmReelSession = false
        dmReelEnteredAtMs = 0L
    }

    companion object {
        const val INSTAGRAM_PACKAGE = "com.instagram.android"

        val DM_VIEW_IDS = listOf(
            "com.instagram.android:id/message_list",
            "com.instagram.android:id/row_thread_composer_edittext",
            "com.instagram.android:id/direct_thread_header",
            "com.instagram.android:id/direct_inbox_recycler_view",
            "com.instagram.android:id/thread_title"
        )
    }
}
