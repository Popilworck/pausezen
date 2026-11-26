package com.pausezen.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dailyStatsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pausezen_daily_stats")

data class DayUsageSnapshot(
    val date: String,
    val opensByTarget: Map<String, Int> = emptyMap(),
    val attemptsByTarget: Map<String, Int> = emptyMap(),
    val screenTimeMsByTarget: Map<String, Long> = emptyMap()
)

class DailyStatsRepository(
    private val context: Context,
    private val todayProvider: () -> LocalDate = { LocalDate.now() }
) {
    private object LegacyKeys {
        val STATS_DATE = stringPreferencesKey("stats_date")
        val OPEN_ENTRIES = stringSetPreferencesKey("open_entries")
    }

    val historyFlow: Flow<Map<String, DayUsageSnapshot>> = context.dailyStatsDataStore.data.map { prefs ->
        val opensByDate = mutableMapOf<String, Map<String, Int>>()
        val attemptsByDate = mutableMapOf<String, Map<String, Int>>()
        val timeByDate = mutableMapOf<String, Map<String, Long>>()

        // Read legacy single-day keys if present
        val legacyDate = prefs[LegacyKeys.STATS_DATE]
        val legacyEntries = prefs[LegacyKeys.OPEN_ENTRIES]
        if (!legacyDate.isNullOrBlank() && legacyEntries != null) {
            opensByDate[legacyDate] = parseEntries(legacyEntries)
        }

        // Read multi-day keys
        prefs.asMap().forEach { (key, value) ->
            val name = key.name
            @Suppress("UNCHECKED_CAST")
            val setVal = value as? Set<String> ?: return@forEach
            when {
                name.startsWith(OPENS_PREFIX) -> {
                    val date = name.removePrefix(OPENS_PREFIX)
                    opensByDate[date] = parseEntries(setVal)
                }
                name.startsWith(ATTEMPTS_PREFIX) -> {
                    val date = name.removePrefix(ATTEMPTS_PREFIX)
                    attemptsByDate[date] = parseEntries(setVal)
                }
                name.startsWith(TIME_PREFIX) -> {
                    val date = name.removePrefix(TIME_PREFIX)
                    timeByDate[date] = parseLongEntries(setVal)
                }
            }
        }

        val allDates = (opensByDate.keys + attemptsByDate.keys + timeByDate.keys).sortedDescending()
        allDates.associateWith { date ->
            DayUsageSnapshot(
                date = date,
                opensByTarget = opensByDate[date] ?: emptyMap(),
                attemptsByTarget = attemptsByDate[date] ?: emptyMap(),
                screenTimeMsByTarget = timeByDate[date] ?: emptyMap()
            )
        }
    }

    val todayOpensFlow: Flow<Map<String, Int>> = historyFlow.map { history ->
        history[todayProvider().toString()]?.opensByTarget ?: emptyMap()
    }

    val todayScreenTimeFlow: Flow<Map<String, Long>> = historyFlow.map { history ->
        history[todayProvider().toString()]?.screenTimeMsByTarget ?: emptyMap()
    }

    suspend fun incrementOpen(targetKey: String) {
        if (targetKey.isBlank()) return
        val today = todayProvider()
        val todayStr = today.toString()
        val key = stringSetPreferencesKey("$OPENS_PREFIX$todayStr")

        context.dailyStatsDataStore.edit { prefs ->
            val currentMap = readOpensForDate(prefs, todayStr).toMutableMap()
            currentMap[targetKey] = (currentMap[targetKey] ?: 0) + 1
            prefs[key] = serializeEntries(currentMap)
            pruneOldKeys(prefs, today)
        }
    }

    suspend fun incrementAttempt(targetKey: String) {
        if (targetKey.isBlank()) return
        val today = todayProvider()
        val todayStr = today.toString()
        val key = stringSetPreferencesKey("$ATTEMPTS_PREFIX$todayStr")

        context.dailyStatsDataStore.edit { prefs ->
            val currentMap = parseEntries(prefs[key] ?: emptySet()).toMutableMap()
            currentMap[targetKey] = (currentMap[targetKey] ?: 0) + 1
            prefs[key] = serializeEntries(currentMap)
            pruneOldKeys(prefs, today)
        }
    }

    suspend fun addScreenTimeMs(targetKey: String, deltaMs: Long) {
        if (targetKey.isBlank() || deltaMs <= 0L) return
        val today = todayProvider()
        val todayStr = today.toString()
        val key = stringSetPreferencesKey("$TIME_PREFIX$todayStr")

        context.dailyStatsDataStore.edit { prefs ->
            val currentMap = parseLongEntries(prefs[key] ?: emptySet()).toMutableMap()
            currentMap[targetKey] = (currentMap[targetKey] ?: 0L) + deltaMs
            prefs[key] = serializeLongEntries(currentMap)
            pruneOldKeys(prefs, today)
        }
    }

    private fun readOpensForDate(prefs: Preferences, dateStr: String): Map<String, Int> {
        val multiDay = prefs[stringSetPreferencesKey("$OPENS_PREFIX$dateStr")]
        if (multiDay != null) return parseEntries(multiDay)
        if (prefs[LegacyKeys.STATS_DATE] == dateStr) {
            return parseEntries(prefs[LegacyKeys.OPEN_ENTRIES] ?: emptySet())
        }
        return emptyMap()
    }

    private fun pruneOldKeys(prefs: androidx.datastore.preferences.core.MutablePreferences, today: LocalDate) {
        val cutoff = today.minusDays(RETENTION_DAYS)
        val keysToRemove = prefs.asMap().keys.filter { prefKey ->
            val dateStr = when {
                prefKey.name.startsWith(OPENS_PREFIX) -> prefKey.name.removePrefix(OPENS_PREFIX)
                prefKey.name.startsWith(ATTEMPTS_PREFIX) -> prefKey.name.removePrefix(ATTEMPTS_PREFIX)
                prefKey.name.startsWith(TIME_PREFIX) -> prefKey.name.removePrefix(TIME_PREFIX)
                else -> null
            } ?: return@filter false
            val parsed = runCatching { LocalDate.parse(dateStr) }.getOrNull() ?: return@filter false
            parsed.isBefore(cutoff)
        }
        keysToRemove.forEach { prefs.remove(it) }
    }

    companion object {
        private const val OPENS_PREFIX = "opens_"
        private const val ATTEMPTS_PREFIX = "attempts_"
        private const val TIME_PREFIX = "time_"
        private const val RETENTION_DAYS = 30L

        internal fun parseEntries(entries: Set<String>): Map<String, Int> {
            val result = mutableMapOf<String, Int>()
            for (entry in entries) {
                val sep = entry.lastIndexOf('|')
                if (sep <= 0 || sep >= entry.length - 1) continue
                val key = entry.substring(0, sep)
                val count = entry.substring(sep + 1).toIntOrNull() ?: continue
                if (count > 0) result[key] = count
            }
            return result
        }

        internal fun serializeEntries(map: Map<String, Int>): Set<String> {
            return map.entries
                .filter { it.key.isNotBlank() && it.value > 0 }
                .map { "${it.key}|${it.value}" }
                .toSet()
        }

        internal fun parseLongEntries(entries: Set<String>): Map<String, Long> {
            val result = mutableMapOf<String, Long>()
            for (entry in entries) {
                val sep = entry.lastIndexOf('|')
                if (sep <= 0 || sep >= entry.length - 1) continue
                val key = entry.substring(0, sep)
                val value = entry.substring(sep + 1).toLongOrNull() ?: continue
                if (value > 0L) result[key] = value
            }
            return result
        }

        internal fun serializeLongEntries(map: Map<String, Long>): Set<String> {
            return map.entries
                .filter { it.key.isNotBlank() && it.value > 0L }
                .map { "${it.key}|${it.value}" }
                .toSet()
        }
    }
}
