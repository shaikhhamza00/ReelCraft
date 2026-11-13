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
    val bgScene: String = "NIGHT_SKY",
    val accentColor: String = "#22D3EE",
    val transition: String = "FADE",
    val durationMs: Long = 3000L,
    val voiceText: String = "", // Short, 8-15 words for speech
    val voiceStyle: String = "CALM_NARRATOR" // WHISPER_SLOW, ENERGETIC, CALM_NARRATOR, SUSPENSE, SOFT_EMOTIONAL, SERENE_MALE
) {
    companion object {
        fun getDefaultVoiceStyle(niche: String): String {
            return when (niche.lowercase()) {
                "horror" -> "WHISPER_SLOW"
                "motivation" -> "ENERGETIC"
                "story" -> "CALM_NARRATOR"
                "riddle", "quiz" -> "SUSPENSE"
                "shayari", "quotes" -> "SOFT_EMOTIONAL"
                "facts" -> "SERENE_MALE"
                else -> "CALM_NARRATOR"
            }
        }
    }
}
