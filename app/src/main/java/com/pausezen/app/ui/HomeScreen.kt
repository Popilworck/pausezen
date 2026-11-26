package com.pausezen.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pausezen.app.core.InAppFeatureCatalog
import com.pausezen.app.core.InstalledApp
import com.pausezen.app.core.InterceptionStateMachine
import com.pausezen.app.data.AppGroup
import com.pausezen.app.data.BreathingPattern
import com.pausezen.app.data.BreathingStyle
import com.pausezen.app.data.DayUsageSnapshot
import com.pausezen.app.data.GuardConfig
import com.pausezen.app.ui.components.FeatureFeedLogo
import com.pausezen.app.ui.theme.PauseZenPalette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    config: GuardConfig,
    usageHistory: Map<String, DayUsageSnapshot>,
    isGuardActive: Boolean,
    installedApps: List<InstalledApp>,
    isLoadingApps: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
    onSaveGroup: (AppGroup) -> Unit,
    onToggleGroupEnabled: (String, Boolean) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onUpdateBreathing: (Boolean) -> Unit,
    onUpdateBreathingPattern: (BreathingPattern) -> Unit,
    onUpdateBreathingStyle: (BreathingStyle) -> Unit,
    onPreviewOverlay: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isEditingGroup by remember { mutableStateOf(false) }
    var groupBeingEdited by remember { mutableStateOf<AppGroup?>(null) }
    var presetGroupName by remember { mutableStateOf("") }

    if (isEditingGroup) {
        GroupEditorScreen(
            initialGroup = groupBeingEdited,
            presetName = presetGroupName,
            installedApps = installedApps,
            onDismiss = {
                isEditingGroup = false
                groupBeingEdited = null
                presetGroupName = ""
            },
            onSave = { group ->
                onSaveGroup(group)
                isEditingGroup = false
                groupBeingEdited = null
                presetGroupName = ""
            },
            onDelete = if (groupBeingEdited != null) {
                { groupId ->
                    onDeleteGroup(groupId)
                    isEditingGroup = false
                    groupBeingEdited = null
                }
            } else {
                null
            }
        )
        return
    }

    val appsByPackage = remember(installedApps) { installedApps.associateBy { it.packageName } }
    val todayKey = remember { LocalDate.now().toString() }
    val todaySnapshot = usageHistory[todayKey] ?: DayUsageSnapshot(date = todayKey)

    Scaffold(
        containerColor = PauseZenPalette.Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PauseZen",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PauseZenPalette.TextPrimary
                    )
                },
                actions = {
                    Surface(
                        onClick = onOpenAccessibilitySettings,
                        shape = RoundedCornerShape(999.dp),
                        color = PauseZenPalette.Surface,
                        border = BorderStroke(1.dp, PauseZenPalette.Hairline),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isGuardActive) PauseZenPalette.ActiveGreen
                                        else PauseZenPalette.WarningAmber
                                    )
                            )
                            Text(
                                text = if (isGuardActive) "Active" else "Enable Guard",
                                style = MaterialTheme.typography.labelMedium,
                                color = PauseZenPalette.TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PauseZenPalette.Canvas
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        groupBeingEdited = null
                        presetGroupName = ""
                        isEditingGroup = true
                    },
                    shape = RoundedCornerShape(999.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Group", fontWeight = FontWeight.SemiBold) },
                    containerColor = PauseZenPalette.Accent,
                    contentColor = PauseZenPalette.OnAccent
                )
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = PauseZenPalette.Hairline, thickness = 1.dp)
                NavigationBar(
                    containerColor = PauseZenPalette.Surface,
                    tonalElevation = 0.dp
                ) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PauseZenPalette.Accent,
                        selectedTextColor = PauseZenPalette.TextPrimary,
                        unselectedIconColor = PauseZenPalette.TextMuted,
                        unselectedTextColor = PauseZenPalette.TextMuted,
                        indicatorColor = PauseZenPalette.AccentSoft
                    )
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = "Groups") },
                        label = { Text("Groups") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "History") },
                        label = { Text("History") },
                        colors = itemColors
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = itemColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isGuardActive) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = PauseZenPalette.Surface,
                    border = BorderStroke(1.dp, PauseZenPalette.WarningAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Accessibility guard is off",
                                style = MaterialTheme.typography.titleSmall,
                                color = PauseZenPalette.TextPrimary
                            )
                            Text(
                                text = "Turn on PauseZen in Accessibility settings to pause apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = PauseZenPalette.TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onOpenAccessibilitySettings,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PauseZenPalette.Accent,
                                contentColor = PauseZenPalette.OnAccent
                            )
                        ) {
                            Text("Enable")
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> AppGroupsTab(
                    groups = config.appGroups,
                    appsByPackage = appsByPackage,
                    todaySnapshot = todaySnapshot,
                    onToggleGroup = onToggleGroupEnabled,
                    onEditGroup = { group ->
                        groupBeingEdited = group
                        presetGroupName = ""
                        isEditingGroup = true
                    },
                    onQuickCreate = { templateName ->
                        groupBeingEdited = null
                        presetGroupName = templateName
                        isEditingGroup = true
                    }
                )

                1 -> HistoryActivityTab(
                    installedApps = installedApps,
                    usageHistory = usageHistory,
                    isLoading = isLoadingApps
                )

                2 -> SettingsTab(
                    config = config,
                    onUpdateBreathing = onUpdateBreathing,
                    onUpdateBreathingPattern = onUpdateBreathingPattern,
                    onUpdateBreathingStyle = onUpdateBreathingStyle,
                    onPreviewOverlay = onPreviewOverlay
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppGroupsTab(
    groups: List<AppGroup>,
    appsByPackage: Map<String, InstalledApp>,
    todaySnapshot: DayUsageSnapshot,
    onToggleGroup: (String, Boolean) -> Unit,
    onEditGroup: (AppGroup) -> Unit,
    onQuickCreate: (String) -> Unit
) {
    if (groups.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Build your first blocklist",
                style = MaterialTheme.typography.headlineMedium,
                color = PauseZenPalette.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Group apps and short-form feeds like Instagram Reels or YouTube Shorts with their own pause timer, daily open limit, and screen time limit.",
                style = MaterialTheme.typography.bodyMedium,
                color = PauseZenPalette.TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Social Media", "Reels & Shorts", "Games", "Video").forEach { preset ->
                    Surface(
                        onClick = { onQuickCreate(preset) },
                        shape = RoundedCornerShape(999.dp),
                        color = PauseZenPalette.Surface,
                        border = BorderStroke(1.dp, PauseZenPalette.Hairline)
                    ) {
                        Text(
                            text = "+ $preset",
                            style = MaterialTheme.typography.labelLarge,
                            color = PauseZenPalette.TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(groups, key = { it.id }) { group ->
            AppGroupCard(
                group = group,
                appsByPackage = appsByPackage,
                todaySnapshot = todaySnapshot,
                onToggle = { onToggleGroup(group.id, it) },
                onClick = { onEditGroup(group) }
            )
        }
    }
}

@Composable
private fun AppGroupCard(
    group: AppGroup,
    appsByPackage: Map<String, InstalledApp>,
    todaySnapshot: DayUsageSnapshot,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val groupOpensToday = remember(group, todaySnapshot) {
        val pkgOpens = group.packages.sumOf {
            todaySnapshot.opensByTarget[InterceptionStateMachine.appTargetKey(it)] ?: 0
        }
        val feedOpens = group.featureRuleIds.sumOf {
            todaySnapshot.opensByTarget[InterceptionStateMachine.featureTargetKey(it)] ?: 0
        }
        pkgOpens + feedOpens
    }

    val groupTimeMsToday = remember(group, todaySnapshot) {
        val pkgs = if (group.packages.isNotEmpty()) {
            group.packages
        } else {
            group.featureRuleIds
                .mapNotNull { InAppFeatureCatalog.findById(it) }
                .flatMap { it.hostPackages }
                .toSet()
        }
        pkgs.sumOf { todaySnapshot.screenTimeMsByTarget[InterceptionStateMachine.appTargetKey(it)] ?: 0L }
    }

    val matchedApps = remember(group.packages, appsByPackage) {
        group.packages.mapNotNull { appsByPackage[it] }
    }
    val matchedFeeds = remember(group.featureRuleIds) {
        group.featureRuleIds.mapNotNull { InAppFeatureCatalog.findById(it) }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = PauseZenPalette.Surface,
        border = BorderStroke(1.dp, PauseZenPalette.Hairline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = PauseZenPalette.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildString {
                            append("${group.pauseDurationSeconds}s pause")
                            if (group.dailyOpenLimit > 0) {
                                append("  ·  $groupOpensToday/${group.dailyOpenLimit} opens")
                            } else {
                                append("  ·  ${formatOpensToday(groupOpensToday)}")
                            }
                            if (group.dailyTimeLimitMinutes > 0) {
                                append("  ·  ${formatDurationShort(groupTimeMsToday)}/${group.dailyTimeLimitMinutes}m")
                            } else if (groupTimeMsToday >= 60_000L) {
                                append("  ·  ${formatDurationShort(groupTimeMsToday)}")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = PauseZenPalette.TextSecondary
                    )
                }
                Switch(
                    checked = group.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PauseZenPalette.OnAccent,
                        checkedTrackColor = PauseZenPalette.Accent,
                        uncheckedThumbColor = PauseZenPalette.TextSecondary,
                        uncheckedTrackColor = PauseZenPalette.SurfaceControl,
                        uncheckedBorderColor = PauseZenPalette.Hairline
                    )
                )
            }

            if (matchedFeeds.isNotEmpty() || matchedApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    matchedFeeds.take(4).forEach { feed ->
                        FeatureFeedLogo(ruleId = feed.id, size = 30.dp)
                    }
                    val remainingSlots = (6 - matchedFeeds.take(4).size).coerceAtLeast(1)
                    matchedApps.take(remainingSlots).forEach { app ->
                        val iconBitmap = remember(app.packageName) {
                            app.icon?.let { runCatching { it.toBitmap(64, 64).asImageBitmap() }.getOrNull() }
                        }
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    val hiddenCount = (matchedFeeds.size + matchedApps.size) -
                        (matchedFeeds.take(4).size + matchedApps.take(remainingSlots).size)
                    if (hiddenCount > 0) {
                        Text(
                            text = "+$hiddenCount",
                            style = MaterialTheme.typography.labelMedium,
                            color = PauseZenPalette.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryActivityTab(
    installedApps: List<InstalledApp>,
    usageHistory: Map<String, DayUsageSnapshot>,
    isLoading: Boolean
) {
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyActive by remember { mutableStateOf(true) }
    // true = Tries (attempts to open), false = Unlocked (opens after waiting out timer)
    var showAttemptsMetric by remember { mutableStateOf(true) }
    var expandedTargetKey by remember { mutableStateOf<String?>(null) }

    val selectedKey = selectedDate.toString()
    val daySnapshot = usageHistory[selectedKey] ?: DayUsageSnapshot(date = selectedKey)

    val totalAttemptsForDay = remember(installedApps, daySnapshot) {
        val appTries = installedApps.sumOf {
            daySnapshot.attemptsByTarget[InterceptionStateMachine.appTargetKey(it.packageName)] ?: 0
        }
        val feedTries = InAppFeatureCatalog.allRules.sumOf {
            daySnapshot.attemptsByTarget[InterceptionStateMachine.featureTargetKey(it.id)] ?: 0
        }
        appTries + feedTries
    }

    val totalUnlockedForDay = remember(installedApps, daySnapshot) {
        val appOpens = installedApps.sumOf {
            daySnapshot.opensByTarget[InterceptionStateMachine.appTargetKey(it.packageName)] ?: 0
        }
        val feedOpens = InAppFeatureCatalog.allRules.sumOf {
            daySnapshot.opensByTarget[InterceptionStateMachine.featureTargetKey(it.id)] ?: 0
        }
        appOpens + feedOpens
    }

    val resistedForDay = (totalAttemptsForDay - totalUnlockedForDay).coerceAtLeast(0)

    val totalTimeMsForDay = remember(installedApps, daySnapshot) {
        installedApps.sumOf {
            daySnapshot.screenTimeMsByTarget[InterceptionStateMachine.appTargetKey(it.packageName)] ?: 0L
        }
    }

    val activeFeeds = remember(daySnapshot, showAttemptsMetric) {
        InAppFeatureCatalog.allRules
            .map { rule ->
                val key = InterceptionStateMachine.featureTargetKey(rule.id)
                val attempts = daySnapshot.attemptsByTarget[key] ?: 0
                val unlocked = daySnapshot.opensByTarget[key] ?: 0
                Triple(rule, attempts, unlocked)
            }
            .filter { it.second > 0 || it.third > 0 }
            .sortedByDescending { if (showAttemptsMetric) it.second else it.third }
    }

    val sortedApps = remember(installedApps, daySnapshot, searchQuery, showOnlyActive, showAttemptsMetric) {
        installedApps
            .map { app ->
                val targetKey = InterceptionStateMachine.appTargetKey(app.packageName)
                val attempts = daySnapshot.attemptsByTarget[targetKey] ?: 0
                val unlocked = daySnapshot.opensByTarget[targetKey] ?: 0
                val timeMs = daySnapshot.screenTimeMsByTarget[targetKey] ?: 0L
                listOf(app, attempts, unlocked, timeMs)
            }
            .filter { item ->
                val app = item[0] as InstalledApp
                val attempts = item[1] as Int
                val unlocked = item[2] as Int
                val timeMs = item[3] as Long
                (!showOnlyActive || attempts > 0 || unlocked > 0 || timeMs >= 60_000L) &&
                    (searchQuery.isBlank() || app.label.contains(searchQuery, ignoreCase = true))
            }
            .sortedWith(
                compareByDescending<List<Any>> {
                    if (showAttemptsMetric) it[1] as Int else it[2] as Int
                }.thenByDescending {
                    if (showAttemptsMetric) it[2] as Int else it[1] as Int
                }.thenByDescending { it[3] as Long }
                    .thenBy { (it[0] as InstalledApp).label.lowercase() }
            )
    }

    val maxCount = remember(activeFeeds, sortedApps, showAttemptsMetric) {
        val maxFeed = activeFeeds.maxOfOrNull { if (showAttemptsMetric) it.second else it.third } ?: 0
        val maxApp = sortedApps.maxOfOrNull {
            if (showAttemptsMetric) it[1] as Int else it[2] as Int
        } ?: 0
        maxOf(maxFeed, maxApp, 1)
    }

    val dateLabel = remember(selectedDate, today) {
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        when (selectedDate) {
            today -> "Today · ${selectedDate.format(formatter)}"
            today.minusDays(1) -> "Yesterday · ${selectedDate.format(formatter)}"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Date switcher bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { selectedDate = selectedDate.minusDays(1) },
                enabled = selectedDate.isAfter(today.minusDays(29))
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous day",
                    tint = if (selectedDate.isAfter(today.minusDays(29))) {
                        PauseZenPalette.TextPrimary
                    } else {
                        PauseZenPalette.TextMuted
                    }
                )
            }
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = PauseZenPalette.TextPrimary
            )
            IconButton(
                onClick = { selectedDate = selectedDate.plusDays(1) },
                enabled = selectedDate.isBefore(today)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next day",
                    tint = if (selectedDate.isBefore(today)) {
                        PauseZenPalette.TextPrimary
                    } else {
                        PauseZenPalette.TextMuted
                    }
                )
            }
        }

        // Day hero summary + Tries / Unlocked metric switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (showAttemptsMetric) totalAttemptsForDay.toString() else totalUnlockedForDay.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    color = PauseZenPalette.TextPrimary
                )
                Text(
                    text = buildString {
                        append(if (showAttemptsMetric) "tries" else "unlocked opens")
                        append(" · $totalUnlockedForDay unlocked")
                        if (resistedForDay > 0) append(" · $resistedForDay resisted")
                        if (totalTimeMsForDay >= 60_000L) append(" · ${formatDurationShort(totalTimeMsForDay)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = PauseZenPalette.TextSecondary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = { showAttemptsMetric = true },
                    shape = RoundedCornerShape(999.dp),
                    color = if (showAttemptsMetric) PauseZenPalette.AccentSoft else PauseZenPalette.Surface,
                    border = BorderStroke(
                        1.dp,
                        if (showAttemptsMetric) PauseZenPalette.Accent else PauseZenPalette.Hairline
                    )
                ) {
                    Text(
                        text = "Tries",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (showAttemptsMetric) PauseZenPalette.Accent else PauseZenPalette.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
                Surface(
                    onClick = { showAttemptsMetric = false },
                    shape = RoundedCornerShape(999.dp),
                    color = if (!showAttemptsMetric) PauseZenPalette.AccentSoft else PauseZenPalette.Surface,
                    border = BorderStroke(
                        1.dp,
                        if (!showAttemptsMetric) PauseZenPalette.Accent else PauseZenPalette.Hairline
                    )
                ) {
                    Text(
                        text = "Unlocked",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (!showAttemptsMetric) PauseZenPalette.Accent else PauseZenPalette.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search apps", color = PauseZenPalette.TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = PauseZenPalette.TextSecondary)
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = PauseZenPalette.Surface,
                    unfocusedContainerColor = PauseZenPalette.Surface,
                    focusedBorderColor = PauseZenPalette.Accent,
                    unfocusedBorderColor = PauseZenPalette.Hairline
                ),
                modifier = Modifier.weight(1f)
            )
            Surface(
                onClick = { showOnlyActive = !showOnlyActive },
                shape = RoundedCornerShape(12.dp),
                color = PauseZenPalette.Surface,
                border = BorderStroke(1.dp, PauseZenPalette.Hairline)
            ) {
                Text(
                    text = if (showOnlyActive) "Active" else "All",
                    style = MaterialTheme.typography.labelMedium,
                    color = PauseZenPalette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = PauseZenPalette.Hairline, thickness = 1.dp)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PauseZenPalette.Accent)
            }
        } else if (activeFeeds.isEmpty() && sortedApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No activity recorded for this day",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PauseZenPalette.TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (activeFeeds.isNotEmpty() && searchQuery.isBlank()) {
                    items(activeFeeds, key = { "feed_${it.first.id}" }) { (rule, attempts, unlocked) ->
                        val targetKey = InterceptionStateMachine.featureTargetKey(rule.id)
                        val isExpanded = expandedTargetKey == targetKey
                        val primaryCount = if (showAttemptsMetric) attempts else unlocked
                        HistoryUsageRow(
                            label = rule.displayName,
                            attempts = attempts,
                            unlocked = unlocked,
                            showAttemptsMetric = showAttemptsMetric,
                            screenTimeMs = 0L,
                            ratio = if (primaryCount > 0) primaryCount.toFloat() / maxCount.toFloat() else 0f,
                            isExpanded = isExpanded,
                            targetKey = targetKey,
                            anchorDate = selectedDate,
                            usageHistory = usageHistory,
                            logo = { FeatureFeedLogo(ruleId = rule.id, size = 34.dp) },
                            onClick = {
                                expandedTargetKey = if (isExpanded) null else targetKey
                            }
                        )
                        HorizontalDivider(
                            color = PauseZenPalette.Hairline,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                items(sortedApps, key = { (it[0] as InstalledApp).packageName }) { item ->
                    val app = item[0] as InstalledApp
                    val attempts = item[1] as Int
                    val unlocked = item[2] as Int
                    val timeMs = item[3] as Long
                    val targetKey = InterceptionStateMachine.appTargetKey(app.packageName)
                    val isExpanded = expandedTargetKey == targetKey
                    val primaryCount = if (showAttemptsMetric) attempts else unlocked
                    val iconBitmap = remember(app.packageName) {
                        app.icon?.let { runCatching { it.toBitmap(64, 64).asImageBitmap() }.getOrNull() }
                    }
                    HistoryUsageRow(
                        label = app.label,
                        attempts = attempts,
                        unlocked = unlocked,
                        showAttemptsMetric = showAttemptsMetric,
                        screenTimeMs = timeMs,
                        ratio = if (primaryCount > 0) primaryCount.toFloat() / maxCount.toFloat() else 0f,
                        isExpanded = isExpanded,
                        targetKey = targetKey,
                        anchorDate = selectedDate,
                        usageHistory = usageHistory,
                        logo = {
                            if (iconBitmap != null) {
                                Image(
                                    bitmap = iconBitmap,
                                    contentDescription = app.label,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PauseZenPalette.SurfaceControl)
                                )
                            }
                        },
                        onClick = {
                            expandedTargetKey = if (isExpanded) null else targetKey
                        }
                    )
                    HorizontalDivider(
                        color = PauseZenPalette.Hairline,
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryUsageRow(
    label: String,
    attempts: Int,
    unlocked: Int,
    showAttemptsMetric: Boolean,
    screenTimeMs: Long,
    ratio: Float,
    isExpanded: Boolean,
    targetKey: String,
    anchorDate: LocalDate,
    usageHistory: Map<String, DayUsageSnapshot>,
    logo: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val primaryCount = if (showAttemptsMetric) attempts else unlocked
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            logo()
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = PauseZenPalette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = buildString {
                                append("$attempts ${if (attempts == 1) "try" else "tries"}")
                                append(" · $unlocked unlocked")
                                if (screenTimeMs >= 60_000L) {
                                    append(" · ${formatDurationShort(screenTimeMs)}")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = PauseZenPalette.TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = primaryCount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (primaryCount > 0) PauseZenPalette.Accent else PauseZenPalette.TextMuted
                    )
                }
                if (ratio > 0f) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(PauseZenPalette.SurfaceControl)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceIn(0.06f, 1f))
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(PauseZenPalette.Accent)
                        )
                    }
                }
            }
        }

        if (isExpanded) {
            Spacer(modifier = Modifier.height(14.dp))
            SevenDayOpensChart(
                targetKey = targetKey,
                anchorDate = anchorDate,
                usageHistory = usageHistory,
                showAttemptsMetric = showAttemptsMetric
            )
        }
    }
}

@Composable
private fun SevenDayOpensChart(
    targetKey: String,
    anchorDate: LocalDate,
    usageHistory: Map<String, DayUsageSnapshot>,
    showAttemptsMetric: Boolean
) {
    val days = remember(anchorDate, usageHistory, targetKey, showAttemptsMetric) {
        (6 downTo 0).map { offset ->
            val date = anchorDate.minusDays(offset.toLong())
            val snap = usageHistory[date.toString()]
            val count = if (showAttemptsMetric) {
                snap?.attemptsByTarget?.get(targetKey) ?: 0
            } else {
                snap?.opensByTarget?.get(targetKey) ?: 0
            }
            val dayInitial = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            Triple(date, dayInitial, count)
        }
    }
    val maxDayCount = remember(days) { maxOf(days.maxOfOrNull { it.third } ?: 0, 1) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PauseZenPalette.Surface,
        border = BorderStroke(1.dp, PauseZenPalette.Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = if (showAttemptsMetric) "7-day tries" else "7-day unlocked opens",
                style = MaterialTheme.typography.labelSmall,
                color = PauseZenPalette.TextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { (date, dayInitial, count) ->
                    val barHeight = if (count == 0) 4.dp else (8 + (44 * count / maxDayCount)).dp
                    val isAnchor = date == anchorDate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (count > 0) PauseZenPalette.TextPrimary else PauseZenPalette.TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        count == 0 -> PauseZenPalette.SurfaceControl
                                        isAnchor -> PauseZenPalette.Accent
                                        else -> PauseZenPalette.Accent.copy(alpha = 0.55f)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = dayInitial,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAnchor) PauseZenPalette.Accent else PauseZenPalette.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    config: GuardConfig,
    onUpdateBreathing: (Boolean) -> Unit,
    onUpdateBreathingPattern: (BreathingPattern) -> Unit,
    onUpdateBreathingStyle: (BreathingStyle) -> Unit,
    onPreviewOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = PauseZenPalette.Surface,
            border = BorderStroke(1.dp, PauseZenPalette.Hairline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Breathing guide",
                        style = MaterialTheme.typography.titleMedium,
                        color = PauseZenPalette.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Show a dedicated breathing animation on the pause screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = PauseZenPalette.TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = config.breathingCueEnabled,
                    onCheckedChange = onUpdateBreathing,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PauseZenPalette.OnAccent,
                        checkedTrackColor = PauseZenPalette.Accent,
                        uncheckedThumbColor = PauseZenPalette.TextSecondary,
                        uncheckedTrackColor = PauseZenPalette.SurfaceControl,
                        uncheckedBorderColor = PauseZenPalette.Hairline
                    )
                )
            }
        }

        if (config.breathingCueEnabled) {
            // Breathing visual style picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Breathing visual",
                    style = MaterialTheme.typography.titleSmall,
                    color = PauseZenPalette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BreathingStyle.entries.forEach { style ->
                        val selected = config.breathingStyle == style
                        Surface(
                            onClick = { onUpdateBreathingStyle(style) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) PauseZenPalette.AccentSoft else PauseZenPalette.Surface,
                            border = BorderStroke(
                                1.dp,
                                if (selected) PauseZenPalette.Accent else PauseZenPalette.Hairline
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = style.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) PauseZenPalette.Accent else PauseZenPalette.TextPrimary
                                )
                                Text(
                                    text = style.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PauseZenPalette.TextSecondary,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Breathing rhythm picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Breathing rhythm",
                    style = MaterialTheme.typography.titleSmall,
                    color = PauseZenPalette.TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PauseZenPalette.Surface,
                    border = BorderStroke(1.dp, PauseZenPalette.Hairline)
                ) {
                    Column {
                        BreathingPattern.entries.forEachIndexed { index, pattern ->
                            val selected = config.breathingPattern == pattern
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateBreathingPattern(pattern) }
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pattern.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (selected) PauseZenPalette.Accent else PauseZenPalette.TextPrimary
                                    )
                                    Text(
                                        text = pattern.cadenceLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PauseZenPalette.TextSecondary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) PauseZenPalette.Accent
                                            else PauseZenPalette.SurfaceControl
                                        )
                                )
                            }
                            if (index < BreathingPattern.entries.lastIndex) {
                                HorizontalDivider(
                                    color = PauseZenPalette.Hairline,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(horizontal = 18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onPreviewOverlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, PauseZenPalette.Hairline)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PauseZenPalette.Accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Preview Pause Overlay",
                color = PauseZenPalette.TextPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun formatOpensToday(count: Int): String = when (count) {
    0 -> "0 opens today"
    1 -> "1 open today"
    else -> "$count opens today"
}

private fun formatDurationShort(durationMs: Long): String {
    val totalMinutes = (durationMs / 60_000L).toInt()
    if (totalMinutes < 60) return "${totalMinutes}m"
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return if (mins == 0) "${hours}h" else "${hours}h ${mins}m"
}
