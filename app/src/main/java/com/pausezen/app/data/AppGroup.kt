package com.pausezen.app.data

import java.util.UUID

data class AppGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val enabled: Boolean = true,
    val packages: Set<String> = emptySet(),
    val featureRuleIds: Set<String> = emptySet(),
    val pauseDurationSeconds: Int = DEFAULT_PAUSE_SECONDS,
    val customMessage: String = DEFAULT_MESSAGE,
    val unlockGraceMinutes: Int = DEFAULT_GRACE_MINUTES,
    val dailyOpenLimit: Int = 0,          // 0 = unlimited
    val dailyTimeLimitMinutes: Int = 0    // 0 = unlimited
) {
    val unlockGraceMs: Long
        get() = unlockGraceMinutes * 60_000L

    val dailyTimeLimitMs: Long
        get() = dailyTimeLimitMinutes * 60_000L

    val totalTargetCount: Int
        get() = packages.size + featureRuleIds.size

    companion object {
        const val DEFAULT_PAUSE_SECONDS = 10
        const val DEFAULT_MESSAGE = "Is this important right now?"
        const val DEFAULT_GRACE_MINUTES = 5
    }
}

/**
 * Pure-Kotlin, zero-dependency serializer for AppGroup sets so it runs identically
 * on Android and in JVM unit tests. Backward-compatible with 9-field v1 records.
 */
object AppGroupCodec {
    private const val FIELD_SEP = "\u001F"
    private const val LIST_SEP = ","

    fun encodeAll(groups: List<AppGroup>): Set<String> {
        return groups.mapIndexed { index, group ->
            listOf(
                index.toString(),
                escape(group.id),
                escape(group.name),
                group.enabled.toString(),
                group.packages.sorted().joinToString(LIST_SEP) { escape(it) },
                group.featureRuleIds.sorted().joinToString(LIST_SEP) { escape(it) },
                group.pauseDurationSeconds.toString(),
                escape(group.customMessage),
                group.unlockGraceMinutes.toString(),
                group.dailyOpenLimit.toString(),
                group.dailyTimeLimitMinutes.toString()
            ).joinToString(FIELD_SEP)
        }.toSet()
    }

    fun decodeAll(encoded: Set<String>): List<AppGroup> {
        return encoded
            .mapNotNull { raw ->
                val parts = raw.split(FIELD_SEP)
                if (parts.size < 9) return@mapNotNull null
                val order = parts[0].toIntOrNull() ?: Int.MAX_VALUE
                val group = AppGroup(
                    id = unescape(parts[1]),
                    name = unescape(parts[2]),
                    enabled = parts[3].toBooleanStrictOrNull() ?: true,
                    packages = parts[4]
                        .split(LIST_SEP)
                        .filter { it.isNotBlank() }
                        .map { unescape(it) }
                        .toSet(),
                    featureRuleIds = parts[5]
                        .split(LIST_SEP)
                        .filter { it.isNotBlank() }
                        .map { unescape(it) }
                        .toSet(),
                    pauseDurationSeconds = parts[6].toIntOrNull()?.coerceIn(3, 60)
                        ?: AppGroup.DEFAULT_PAUSE_SECONDS,
                    customMessage = unescape(parts[7]).ifBlank { AppGroup.DEFAULT_MESSAGE },
                    unlockGraceMinutes = parts[8].toIntOrNull()?.coerceIn(1, 60)
                        ?: AppGroup.DEFAULT_GRACE_MINUTES,
                    dailyOpenLimit = parts.getOrNull(9)?.toIntOrNull()?.coerceIn(0, 500) ?: 0,
                    dailyTimeLimitMinutes = parts.getOrNull(10)?.toIntOrNull()?.coerceIn(0, 1440) ?: 0
                )
                order to group
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    private fun escape(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace(FIELD_SEP, "\\u001F")
            .replace(LIST_SEP, "\\c")
    }

    private fun unescape(value: String): String {
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            if (value[i] == '\\' && i + 1 < value.length) {
                when {
                    value.startsWith("\\u001F", i) -> {
                        sb.append(FIELD_SEP)
                        i += 6
                    }
                    value[i + 1] == 'c' -> {
                        sb.append(LIST_SEP)
                        i += 2
                    }
                    value[i + 1] == '\\' -> {
                        sb.append('\\')
                        i += 2
                    }
                    else -> {
                        sb.append(value[i])
                        i++
                    }
                }
            } else {
                sb.append(value[i])
                i++
            }
        }
        return sb.toString()
    }
}
