package com.hamza.reelcraft.model

import androidx.compose.ui.graphics.Color

data class Slide(
    val title: String,
    val subtitle: String,
    val backgroundColorHex: String = "#1A1A2E",
    val textColorHex: String = "#FFFFFF",
    val durationMs: Long = 3000L
)
