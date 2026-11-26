package com.pausezen.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pausezen.app.core.InstalledApp
import com.pausezen.app.core.InstalledAppsProvider
import com.pausezen.app.data.AppGroup
import com.pausezen.app.data.DailyStatsRepository
import com.pausezen.app.data.GuardConfig
import com.pausezen.app.data.PreferencesRepository
import com.pausezen.app.service.AppGuardAccessibilityService
import com.pausezen.app.ui.HomeScreen
import com.pausezen.app.ui.theme.PauseZenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = PreferencesRepository(applicationContext)
        val dailyStatsRepository = DailyStatsRepository(applicationContext)

        setContent {
            PauseZenTheme {
                val config by repository.configFlow.collectAsState(initial = GuardConfig())
                val usageHistory by dailyStatsRepository.historyFlow.collectAsState(initial = emptyMap())
                val scope = rememberCoroutineScope()
                val lifecycleOwner = LocalLifecycleOwner.current

                var isGuardActive by remember {
                    mutableStateOf(AppGuardAccessibilityService.isServiceEnabled(this))
                }
                var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
                var isLoadingApps by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    installedApps = withContext(Dispatchers.IO) {
                        InstalledAppsProvider.loadLaunchableApps(this@MainActivity)
                    }
                    isLoadingApps = false
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            isGuardActive = AppGuardAccessibilityService.isServiceEnabled(this@MainActivity)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                HomeScreen(
                    config = config,
                    usageHistory = usageHistory,
                    isGuardActive = isGuardActive,
                    installedApps = installedApps,
                    isLoadingApps = isLoadingApps,
                    onOpenAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onSaveGroup = { group ->
                        scope.launch { repository.saveGroup(group) }
                    },
                    onToggleGroupEnabled = { groupId, enabled ->
                        scope.launch { repository.toggleGroupEnabled(groupId, enabled) }
                    },
                    onDeleteGroup = { groupId ->
                        scope.launch { repository.deleteGroup(groupId) }
                    },
                    onUpdateBreathing = { enabled ->
                        scope.launch { repository.setBreathingCueEnabled(enabled) }
                    },
                    onUpdateBreathingPattern = { pattern ->
                        scope.launch { repository.setBreathingPattern(pattern) }
                    },
                    onUpdateBreathingStyle = { style ->
                        scope.launch { repository.setBreathingStyle(style) }
                    },
                    onUpdateAllowDmReels = { allow ->
                        scope.launch { repository.setAllowDmReels(allow) }
                    },
                    onPreviewOverlay = {
                        val sampleGroup = config.appGroups.firstOrNull()
                        val previewIntent = Intent(this, PauseOverlayActivity::class.java).apply {
                            putExtra(PauseOverlayActivity.EXTRA_FEATURE_RULE_ID, "preview")
                            putExtra(PauseOverlayActivity.EXTRA_FEATURE_DISPLAY_NAME, "Instagram Reels")
                            putExtra(PauseOverlayActivity.EXTRA_GROUP_NAME, sampleGroup?.name ?: "Social Media")
                            putExtra(
                                PauseOverlayActivity.EXTRA_PAUSE_SECONDS,
                                sampleGroup?.pauseDurationSeconds ?: AppGroup.DEFAULT_PAUSE_SECONDS
                            )
                            putExtra(
                                PauseOverlayActivity.EXTRA_CUSTOM_MESSAGE,
                                sampleGroup?.customMessage ?: AppGroup.DEFAULT_MESSAGE
                            )
                            putExtra(PauseOverlayActivity.EXTRA_BREATHING_ENABLED, config.breathingCueEnabled)
                            putExtra(PauseOverlayActivity.EXTRA_BREATHING_PATTERN, config.breathingPattern.id)
                            putExtra(PauseOverlayActivity.EXTRA_BREATHING_STYLE, config.breathingStyle.id)
                            putExtra(PauseOverlayActivity.EXTRA_TODAY_OPEN_COUNT, 3)
                            putExtra(PauseOverlayActivity.EXTRA_IS_PREVIEW, true)
                        }
                        startActivity(previewIntent)
                    }
                )
            }
        }
    }
}
