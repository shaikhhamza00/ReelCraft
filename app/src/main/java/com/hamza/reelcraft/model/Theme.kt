package com.hamza.reelcraft.model

data class Theme(
    val name: String,
    val backgroundColorHex: String,
    val textColorHex: String,
    val accentColorHex: String,
    val fontName: String = "Default"
) {
    companion object {
        val Dark = Theme("Dark", "#1A1A2E", "#FFFFFF", "#00FFCC")
        val Neon = Theme("Neon", "#0D0221", "#FFFF00", "#FF007F")
        val Minimal = Theme("Minimal", "#F8F9FA", "#212529", "#007BFF")
        val Book = Theme("Book", "#FDF6E3", "#657B83", "#B58900")

        fun fromName(name: String): Theme {
            return when (name.lowercase()) {
                "neon" -> Neon
                "minimal" -> Minimal
                "book" -> Book
                else -> Dark
            }
        }
    }
}
