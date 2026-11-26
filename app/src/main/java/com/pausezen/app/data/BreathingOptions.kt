package com.pausezen.app.data

enum class BreathingPattern(
    val id: String,
    val displayName: String,
    val cadenceLabel: String,
    val inhaleMs: Int,
    val holdAfterInhaleMs: Int,
    val exhaleMs: Int,
    val holdAfterExhaleMs: Int
) {
    CALM(
        id = "calm",
        displayName = "Calm",
        cadenceLabel = "3.5s in · 3.5s out",
        inhaleMs = 3_500,
        holdAfterInhaleMs = 600,
        exhaleMs = 3_500,
        holdAfterExhaleMs = 300
    ),
    BOX(
        id = "box",
        displayName = "Box Breathing",
        cadenceLabel = "4s · 4s · 4s · 4s",
        inhaleMs = 4_000,
        holdAfterInhaleMs = 4_000,
        exhaleMs = 4_000,
        holdAfterExhaleMs = 4_000
    ),
    RELAX_478(
        id = "relax_478",
        displayName = "4-7-8 Relax",
        cadenceLabel = "4s in · 7s hold · 8s out",
        inhaleMs = 4_000,
        holdAfterInhaleMs = 7_000,
        exhaleMs = 8_000,
        holdAfterExhaleMs = 400
    ),
    RESONANT_55(
        id = "resonant_55",
        displayName = "Resonant",
        cadenceLabel = "5s in · 5s out",
        inhaleMs = 5_000,
        holdAfterInhaleMs = 0,
        exhaleMs = 5_000,
        holdAfterExhaleMs = 0
    );

    companion object {
        fun fromId(id: String?): BreathingPattern {
            return entries.firstOrNull { it.id == id } ?: CALM
        }
    }
}

enum class BreathingStyle(
    val id: String,
    val displayName: String,
    val subtitle: String
) {
    BLOOM(
        id = "bloom",
        displayName = "Bloom",
        subtitle = "Layered expanding orb"
    ),
    LOTUS(
        id = "lotus",
        displayName = "Lotus",
        subtitle = "6 rotating petals"
    ),
    HORIZON(
        id = "horizon",
        displayName = "Horizon",
        subtitle = "Rising & ebbing tide"
    );

    companion object {
        fun fromId(id: String?): BreathingStyle {
            return entries.firstOrNull { it.id == id } ?: BLOOM
        }
    }
}
