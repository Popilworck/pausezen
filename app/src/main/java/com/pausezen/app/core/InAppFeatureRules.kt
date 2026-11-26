package com.pausezen.app.core

data class InAppFeatureRule(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val hostPackages: Set<String>,
    val matchingViewIds: List<String>,
    val selectedTabViewIds: List<String> = emptyList()
) {
    val primaryHostPackage: String
        get() = hostPackages.first()
}

interface WindowNodeInspector {
    fun hasVisibleViewId(viewId: String): Boolean
    fun hasSelectedViewId(viewId: String): Boolean
}

object InAppFeatureCatalog {
    val INSTAGRAM_REELS = InAppFeatureRule(
        id = "instagram_reels",
        displayName = "Instagram Reels",
        subtitle = "Pause Reels while keeping DMs & Feed open",
        hostPackages = setOf("com.instagram.android"),
        matchingViewIds = listOf(
            "com.instagram.android:id/clips_viewer_view_pager",
            "com.instagram.android:id/clips_video_container",
            "com.instagram.android:id/clips_viewer_media_info"
        ),
        selectedTabViewIds = listOf(
            "com.instagram.android:id/clips_tab"
        )
    )

    val YOUTUBE_SHORTS = InAppFeatureRule(
        id = "youtube_shorts",
        displayName = "YouTube Shorts",
        subtitle = "Pause Shorts in YouTube & YouTube ReVanced",
        hostPackages = setOf(
            "com.google.android.youtube",
            "app.revanced.android.youtube"
        ),
        matchingViewIds = listOf(
            "com.google.android.youtube:id/reel_recycler",
            "com.google.android.youtube:id/reel_watch_fragment_root",
            "com.google.android.youtube:id/reel_player_page_container",
            "com.google.android.youtube:id/reel_progress_bar",
            "app.revanced.android.youtube:id/reel_recycler",
            "app.revanced.android.youtube:id/reel_watch_fragment_root",
            "app.revanced.android.youtube:id/reel_player_page_container"
        )
    )

    val SNAPCHAT_SPOTLIGHT = InAppFeatureRule(
        id = "snapchat_spotlight",
        displayName = "Snapchat Spotlight",
        subtitle = "Pause Spotlight short-form videos",
        hostPackages = setOf("com.snapchat.android"),
        matchingViewIds = listOf(
            "com.snapchat.android:id/spotlight_feed_container"
        ),
        selectedTabViewIds = listOf(
            "com.snapchat.android:id/ngs_spotlight_icon_container"
        )
    )

    val SNAPCHAT_STORIES = InAppFeatureRule(
        id = "snapchat_stories",
        displayName = "Snapchat Stories",
        subtitle = "Pause Discover & Stories feed",
        hostPackages = setOf("com.snapchat.android"),
        matchingViewIds = listOf(
            "com.snapchat.android:id/discover_feed_recyclerview",
            "com.snapchat.android:id/stories_recycler_view"
        ),
        selectedTabViewIds = listOf(
            "com.snapchat.android:id/ngs_discover_icon_container"
        )
    )

    val INSTAGRAM_EXPLORE = InAppFeatureRule(
        id = "instagram_explore",
        displayName = "Instagram Explore",
        subtitle = "Pause the Search / Explore grid",
        hostPackages = setOf("com.instagram.android"),
        matchingViewIds = listOf(
            "com.instagram.android:id/explore_grid"
        ),
        selectedTabViewIds = listOf(
            "com.instagram.android:id/search_tab"
        )
    )

    val FACEBOOK_REELS = InAppFeatureRule(
        id = "facebook_reels",
        displayName = "Facebook Reels",
        subtitle = "Pause Facebook Reels short videos",
        hostPackages = setOf("com.facebook.katana"),
        matchingViewIds = listOf(
            "com.facebook.katana:id/reels_viewer_container",
            "com.facebook.katana:id/fb_shorts_viewer_container"
        )
    )

    val allRules: List<InAppFeatureRule> = listOf(
        INSTAGRAM_REELS,
        YOUTUBE_SHORTS,
        SNAPCHAT_SPOTLIGHT,
        SNAPCHAT_STORIES,
        INSTAGRAM_EXPLORE,
        FACEBOOK_REELS
    )

    fun rulesForPackage(packageName: String, enabledRuleIds: Set<String>): List<InAppFeatureRule> {
        return allRules.filter { packageName in it.hostPackages && it.id in enabledRuleIds }
    }

    fun findById(ruleId: String): InAppFeatureRule? = allRules.firstOrNull { it.id == ruleId }
}

object InAppFeatureDetector {
    fun detectMatchingRule(
        packageName: String,
        enabledRuleIds: Set<String>,
        inspector: WindowNodeInspector
    ): InAppFeatureRule? {
        val candidateRules = InAppFeatureCatalog.rulesForPackage(packageName, enabledRuleIds)
        if (candidateRules.isEmpty()) return null

        for (rule in candidateRules) {
            if (rule.matchingViewIds.any { inspector.hasVisibleViewId(it) }) {
                return rule
            }
            if (rule.selectedTabViewIds.any { inspector.hasSelectedViewId(it) }) {
                return rule
            }
        }
        return null
    }
}
