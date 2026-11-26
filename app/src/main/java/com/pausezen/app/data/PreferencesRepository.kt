package com.pausezen.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pausezen_prefs")

data class GuardConfig(
    val appGroups: List<AppGroup> = emptyList(),
    val breathingCueEnabled: Boolean = true,
    val breathingPattern: BreathingPattern = BreathingPattern.CALM,
    val breathingStyle: BreathingStyle = BreathingStyle.BLOOM
) {
    val activeGroups: List<AppGroup>
        get() = appGroups.filter { it.enabled }

    fun findGroupForPackage(packageName: String): AppGroup? {
        return activeGroups.firstOrNull { packageName in it.packages }
    }

    fun findGroupForFeatureRule(ruleId: String): AppGroup? {
        return activeGroups.firstOrNull { ruleId in it.featureRuleIds }
    }

    val allActiveFeatureRuleIds: Set<String>
        get() = activeGroups.flatMap { it.featureRuleIds }.toSet()

    companion object {
        const val DEFAULT_PAUSE_SECONDS = AppGroup.DEFAULT_PAUSE_SECONDS
        const val DEFAULT_MESSAGE = AppGroup.DEFAULT_MESSAGE
        const val DEFAULT_GRACE_MINUTES = AppGroup.DEFAULT_GRACE_MINUTES
    }
}

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val APP_GROUPS = stringSetPreferencesKey("app_groups_v1")
        val BREATHING_CUE_ENABLED = booleanPreferencesKey("breathing_cue_enabled")
        val BREATHING_PATTERN = stringPreferencesKey("breathing_pattern")
        val BREATHING_STYLE = stringPreferencesKey("breathing_style")

        // Legacy v0 keys (auto-migrated into a starter group on read if present)
        val LEGACY_GUARDED_PACKAGES = stringSetPreferencesKey("guarded_packages")
        val LEGACY_ENABLED_FEATURE_RULES = stringSetPreferencesKey("enabled_feature_rules")
        val LEGACY_PAUSE_DURATION_SECONDS = intPreferencesKey("pause_duration_seconds")
        val LEGACY_CUSTOM_MESSAGE = stringPreferencesKey("custom_message")
        val LEGACY_UNLOCK_GRACE_MINUTES = intPreferencesKey("unlock_grace_minutes")
    }

    val configFlow: Flow<GuardConfig> = context.dataStore.data.map { prefs ->
        val storedGroups = prefs[Keys.APP_GROUPS]
        val groups = if (storedGroups != null) {
            AppGroupCodec.decodeAll(storedGroups)
        } else {
            migrateLegacyToGroups(prefs)
        }

        GuardConfig(
            appGroups = groups,
            breathingCueEnabled = prefs[Keys.BREATHING_CUE_ENABLED] ?: true,
            breathingPattern = BreathingPattern.fromId(prefs[Keys.BREATHING_PATTERN]),
            breathingStyle = BreathingStyle.fromId(prefs[Keys.BREATHING_STYLE])
        )
    }

    suspend fun saveGroup(group: AppGroup) {
        context.dataStore.edit { prefs ->
            val current = readGroups(prefs).toMutableList()
            val idx = current.indexOfFirst { it.id == group.id }
            if (idx >= 0) {
                current[idx] = group
            } else {
                current.add(group)
            }
            prefs[Keys.APP_GROUPS] = AppGroupCodec.encodeAll(current)
        }
    }

    suspend fun toggleGroupEnabled(groupId: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = readGroups(prefs).map { group ->
                if (group.id == groupId) group.copy(enabled = enabled) else group
            }
            prefs[Keys.APP_GROUPS] = AppGroupCodec.encodeAll(current)
        }
    }

    suspend fun deleteGroup(groupId: String) {
        context.dataStore.edit { prefs ->
            val current = readGroups(prefs).filterNot { it.id == groupId }
            prefs[Keys.APP_GROUPS] = AppGroupCodec.encodeAll(current)
        }
    }

    suspend fun setBreathingCueEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BREATHING_CUE_ENABLED] = enabled
        }
    }

    suspend fun setBreathingPattern(pattern: BreathingPattern) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BREATHING_PATTERN] = pattern.id
        }
    }

    suspend fun setBreathingStyle(style: BreathingStyle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BREATHING_STYLE] = style.id
        }
    }

    private fun readGroups(prefs: Preferences): List<AppGroup> {
        val stored = prefs[Keys.APP_GROUPS]
        return if (stored != null) {
            AppGroupCodec.decodeAll(stored)
        } else {
            migrateLegacyToGroups(prefs)
        }
    }

    private fun migrateLegacyToGroups(prefs: Preferences): List<AppGroup> {
        val legacyPackages = prefs[Keys.LEGACY_GUARDED_PACKAGES] ?: emptySet()
        val legacyRules = prefs[Keys.LEGACY_ENABLED_FEATURE_RULES] ?: emptySet()
        if (legacyPackages.isEmpty() && legacyRules.isEmpty()) {
            return emptyList()
        }
        return listOf(
            AppGroup(
                id = "migrated-default-group",
                name = "My App Group",
                enabled = true,
                packages = legacyPackages,
                featureRuleIds = legacyRules,
                pauseDurationSeconds = prefs[Keys.LEGACY_PAUSE_DURATION_SECONDS]
                    ?: AppGroup.DEFAULT_PAUSE_SECONDS,
                customMessage = prefs[Keys.LEGACY_CUSTOM_MESSAGE]
                    ?: AppGroup.DEFAULT_MESSAGE,
                unlockGraceMinutes = prefs[Keys.LEGACY_UNLOCK_GRACE_MINUTES]
                    ?: AppGroup.DEFAULT_GRACE_MINUTES
            )
        )
    }
}
