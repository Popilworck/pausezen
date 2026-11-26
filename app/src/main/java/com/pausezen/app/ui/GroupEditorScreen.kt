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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.pausezen.app.core.InAppFeatureCatalog
import com.pausezen.app.core.InAppFeatureRule
import com.pausezen.app.core.InstalledApp
import com.pausezen.app.data.AppGroup
import com.pausezen.app.ui.components.FeatureFeedLogo
import com.pausezen.app.ui.theme.PauseZenPalette

private sealed class BlocklistEntry {
    abstract val key: String
    abstract val label: String
    abstract val isSelected: Boolean

    data class Feed(
        val rule: InAppFeatureRule,
        override val isSelected: Boolean
    ) : BlocklistEntry() {
        override val key: String = "feed:${rule.id}"
        override val label: String = rule.displayName
    }

    data class App(
        val app: InstalledApp,
        override val isSelected: Boolean
    ) : BlocklistEntry() {
        override val key: String = "app:${app.packageName}"
        override val label: String = app.label
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GroupEditorScreen(
    initialGroup: AppGroup?,
    presetName: String = "",
    installedApps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onSave: (AppGroup) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var name by remember(initialGroup, presetName) {
        mutableStateOf(initialGroup?.name ?: presetName.ifBlank { "Social Media" })
    }
    var selectedPackages by remember(initialGroup) {
        mutableStateOf(initialGroup?.packages ?: emptySet())
    }
    var selectedFeatureIds by remember(initialGroup) {
        mutableStateOf(initialGroup?.featureRuleIds ?: emptySet())
    }
    var pauseSeconds by remember(initialGroup) {
        mutableIntStateOf(initialGroup?.pauseDurationSeconds ?: AppGroup.DEFAULT_PAUSE_SECONDS)
    }
    var graceMinutes by remember(initialGroup) {
        mutableIntStateOf(initialGroup?.unlockGraceMinutes ?: AppGroup.DEFAULT_GRACE_MINUTES)
    }
    var customMessage by remember(initialGroup) {
        mutableStateOf(initialGroup?.customMessage ?: AppGroup.DEFAULT_MESSAGE)
    }
    var dailyOpenLimit by remember(initialGroup) {
        mutableIntStateOf(initialGroup?.dailyOpenLimit ?: 0)
    }
    var dailyTimeLimitMinutes by remember(initialGroup) {
        mutableIntStateOf(initialGroup?.dailyTimeLimitMinutes ?: 0)
    }

    // 0 = Blocklist, 1 = Timer & Prompt
    var activeSection by remember { mutableIntStateOf(0) }
    // 0 = All, 1 = Reels & Feeds, 2 = Apps
    var blocklistFilter by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val entries = remember(installedApps, selectedPackages, selectedFeatureIds, blocklistFilter, searchQuery) {
        val feeds = if (blocklistFilter == 0 || blocklistFilter == 1) {
            InAppFeatureCatalog.allRules.map { rule ->
                BlocklistEntry.Feed(rule = rule, isSelected = rule.id in selectedFeatureIds)
            }
        } else {
            emptyList()
        }
        val apps = if (blocklistFilter == 0 || blocklistFilter == 2) {
            installedApps.map { app ->
                BlocklistEntry.App(app = app, isSelected = app.packageName in selectedPackages)
            }
        } else {
            emptyList()
        }
        (feeds + apps)
            .filter { entry ->
                searchQuery.isBlank() || entry.label.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<BlocklistEntry> { it.isSelected }
                    .thenBy { if (it is BlocklistEntry.Feed) 0 else 1 }
                    .thenBy { it.label.lowercase() }
            )
    }

    val totalSelected = selectedPackages.size + selectedFeatureIds.size
    val canSave = name.isNotBlank() && totalSelected > 0

    Scaffold(
        containerColor = PauseZenPalette.Canvas,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialGroup == null) "Blocklist" else name.ifBlank { "Blocklist" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = PauseZenPalette.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    if (initialGroup != null && onDelete != null) {
                        IconButton(onClick = { onDelete(initialGroup.id) }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete group",
                                tint = PauseZenPalette.Danger
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = PauseZenPalette.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PauseZenPalette.Canvas
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PauseZenPalette.Canvas)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = {
                        val updated = AppGroup(
                            id = initialGroup?.id ?: java.util.UUID.randomUUID().toString(),
                            name = name.trim(),
                            enabled = initialGroup?.enabled ?: true,
                            packages = selectedPackages,
                            featureRuleIds = selectedFeatureIds,
                            pauseDurationSeconds = pauseSeconds,
                            customMessage = customMessage.ifBlank { AppGroup.DEFAULT_MESSAGE },
                            unlockGraceMinutes = graceMinutes,
                            dailyOpenLimit = dailyOpenLimit,
                            dailyTimeLimitMinutes = dailyTimeLimitMinutes
                        )
                        onSave(updated)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PauseZenPalette.Accent,
                        contentColor = PauseZenPalette.OnAccent,
                        disabledContainerColor = PauseZenPalette.SurfaceControl,
                        disabledContentColor = PauseZenPalette.TextMuted
                    )
                ) {
                    Text(
                        text = if (totalSelected > 0) "Done ($totalSelected selected)" else "Select at least 1 item",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
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
            // Top segmented pills (Blocklist | Timer & Prompt)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PillTab(
                    label = "Blocklist",
                    selected = activeSection == 0,
                    onClick = { activeSection = 0 }
                )
                PillTab(
                    label = "Limits & Timer",
                    selected = activeSection == 1,
                    onClick = { activeSection = 1 }
                )
            }

            if (activeSection == 0) {
                // Filter pills (All / Reels & Feeds / Apps)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubFilterPill("All", blocklistFilter == 0) { blocklistFilter = 0 }
                    SubFilterPill("Reels & Feeds", blocklistFilter == 1) { blocklistFilter = 1 }
                    SubFilterPill("Apps", blocklistFilter == 2) { blocklistFilter = 2 }
                }

                // Search bar + Clear button (matching ScreenZen's layout)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Search", color = PauseZenPalette.TextMuted)
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = PauseZenPalette.TextSecondary
                            )
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
                        onClick = {
                            if (totalSelected > 0) {
                                selectedPackages = emptySet()
                                selectedFeatureIds = emptySet()
                            } else {
                                selectedFeatureIds = InAppFeatureCatalog.allRules.map { it.id }.toSet()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = PauseZenPalette.Surface,
                        border = BorderStroke(1.dp, PauseZenPalette.Hairline)
                    ) {
                        Text(
                            text = if (totalSelected > 0) "Clear" else "All Feeds",
                            style = MaterialTheme.typography.labelLarge,
                            color = PauseZenPalette.TextPrimary,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = PauseZenPalette.Hairline, thickness = 1.dp)

                // Hairline-divided Blocklist rows with crisp logos on left and checkbox on right
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(entries, key = { it.key }) { entry ->
                        when (entry) {
                            is BlocklistEntry.Feed -> {
                                BlocklistRow(
                                    label = entry.rule.displayName,
                                    subtitle = entry.rule.subtitle,
                                    checked = entry.isSelected,
                                    leadingLogo = {
                                        FeatureFeedLogo(ruleId = entry.rule.id, size = 38.dp)
                                    },
                                    onToggle = {
                                        selectedFeatureIds = if (entry.isSelected) {
                                            selectedFeatureIds - entry.rule.id
                                        } else {
                                            selectedFeatureIds + entry.rule.id
                                        }
                                    }
                                )
                            }

                            is BlocklistEntry.App -> {
                                val iconBitmap = remember(entry.app.packageName) {
                                    entry.app.icon?.let {
                                        runCatching { it.toBitmap(80, 80).asImageBitmap() }.getOrNull()
                                    }
                                }
                                BlocklistRow(
                                    label = entry.app.label,
                                    subtitle = null,
                                    checked = entry.isSelected,
                                    leadingLogo = {
                                        if (iconBitmap != null) {
                                            Image(
                                                bitmap = iconBitmap,
                                                contentDescription = entry.app.label,
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(PauseZenPalette.SurfaceControl)
                                            )
                                        }
                                    },
                                    onToggle = {
                                        selectedPackages = if (entry.isSelected) {
                                            selectedPackages - entry.app.packageName
                                        } else {
                                            selectedPackages + entry.app.packageName
                                        }
                                    }
                                )
                            }
                        }
                        HorizontalDivider(
                            color = PauseZenPalette.Hairline,
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                // Limits & Timer section
                val pauseOptions = listOf(5, 10, 15, 20, 30)
                val graceOptions = listOf(1, 5, 10, 15)
                val openLimitOptions = listOf(0, 3, 5, 10, 15, 20)
                val timeLimitOptions = listOf(0, 15, 30, 45, 60, 90)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Group name",
                            style = MaterialTheme.typography.titleSmall,
                            color = PauseZenPalette.TextSecondary
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Social Media") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PauseZenPalette.Surface,
                                unfocusedContainerColor = PauseZenPalette.Surface,
                                focusedBorderColor = PauseZenPalette.Accent,
                                unfocusedBorderColor = PauseZenPalette.Hairline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Daily open limit",
                            style = MaterialTheme.typography.titleSmall,
                            color = PauseZenPalette.TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            openLimitOptions.forEach { limit ->
                                PillTab(
                                    label = if (limit == 0) "Off" else "$limit / day",
                                    selected = dailyOpenLimit == limit,
                                    onClick = { dailyOpenLimit = limit }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Daily screen time limit",
                            style = MaterialTheme.typography.titleSmall,
                            color = PauseZenPalette.TextSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timeLimitOptions.forEach { mins ->
                                PillTab(
                                    label = if (mins == 0) "Off" else "${mins}m / day",
                                    selected = dailyTimeLimitMinutes == mins,
                                    onClick = { dailyTimeLimitMinutes = mins }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Pause countdown",
                            style = MaterialTheme.typography.titleSmall,
                            color = PauseZenPalette.TextSecondary
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pauseOptions.forEach { sec ->
                                PillTab(
                                    label = "${sec}s",
                                    selected = pauseSeconds == sec,
                                    onClick = { pauseSeconds = sec }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Unlock grace period",
                            style = MaterialTheme.typography.titleSmall,
                            color = PauseZenPalette.TextSecondary
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            graceOptions.forEach { mins ->
                                PillTab(
                                    label = "$mins min",
                                    selected = graceMinutes == mins,
                                    onClick = { graceMinutes = mins }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Mindful prompt",
                            style = MaterialTheme.typography.titleSmall,
                            color = PauseZenPalette.TextSecondary
                        )
                        OutlinedTextField(
                            value = customMessage,
                            onValueChange = { customMessage = it },
                            placeholder = { Text(AppGroup.DEFAULT_MESSAGE) },
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PauseZenPalette.Surface,
                                unfocusedContainerColor = PauseZenPalette.Surface,
                                focusedBorderColor = PauseZenPalette.Accent,
                                unfocusedBorderColor = PauseZenPalette.Hairline
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlocklistRow(
    label: String,
    subtitle: String?,
    checked: Boolean,
    leadingLogo: @Composable () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingLogo()
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = PauseZenPalette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PauseZenPalette.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = PauseZenPalette.Accent,
                checkmarkColor = PauseZenPalette.OnAccent,
                uncheckedColor = PauseZenPalette.TextSecondary
            )
        )
    }
}

@Composable
private fun PillTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) PauseZenPalette.Accent else PauseZenPalette.Surface,
        border = if (selected) null else BorderStroke(1.dp, PauseZenPalette.Hairline)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) PauseZenPalette.OnAccent else PauseZenPalette.TextPrimary,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SubFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) PauseZenPalette.AccentSoft else PauseZenPalette.Canvas,
        border = BorderStroke(
            1.dp,
            if (selected) PauseZenPalette.Accent else PauseZenPalette.Hairline
        )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) PauseZenPalette.Accent else PauseZenPalette.TextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
