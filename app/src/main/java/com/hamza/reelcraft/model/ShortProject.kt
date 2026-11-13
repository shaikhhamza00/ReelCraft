package com.hamza.reelcraft.model

data class ShortProject(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val niche: String,
    val language: String,
    val themeName: String,
    val slides: List<SlideData>
)

data class SlideData(
    val templateType: String = "story", // story, riddle, quote, listicle
    val headline: String,
    val body: String,
    val footer: String = "",
    val emphasisWords: List<String> = emptyList(),
    val bgScene: String = "NIGHT_SKY", // NIGHT_SKY, OCEAN, FIRE_EMBERS, SPACE, RAIN_CITY, etc.
    val accentColor: String = "#22D3EE",
    val transition: String = "FADE", // FADE, SLIDE_UP, ZOOM_PUNCH, GLITCH
    val durationMs: Long = 3000L
)
