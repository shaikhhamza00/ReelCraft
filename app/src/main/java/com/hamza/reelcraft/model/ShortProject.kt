package com.hamza.reelcraft.model

data class ShortProject(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val niche: String, // Riddle, Facts, Motivation, Story, Quotes, Quiz, Shayari, Horror
    val language: String, // Urdu, Roman Urdu, Hindi, English
    val themeName: String,
    val slides: List<SlideData>
)

data class SlideData(
    val templateType: String, // "story", "riddle", "quote", "listicle"
    val headline: String,
    val body: String,
    val footer: String = ""
)
