package com.hamza.reelcraft.model

data class Theme(
    val name: String,
    val backgroundColorHex: String,
    val textColorHex: String,
    val accentColorHex: String,
    val fontName: String = "Default"
) {
    companion object {
        val Neon = Theme("Neon", "#0D0221", "#FFFF00", "#FF007F")
        val Dark = Theme("Dark", "#1A1A2E", "#FFFFFF", "#00FFCC")
        val Book = Theme("Book", "#FDF6E3", "#263238", "#D35400")
        val CinematicRoom = Theme("Cinematic Room", "#0A0F1D", "#FFFFFF", "#38BDF8")
        val Characters = Theme("Characters", "#180126", "#F3E8FF", "#EC4899")
        val Table = Theme("Table", "#0F172A", "#E2E8F0", "#10B981")
        val Minimal = Theme("Minimal", "#F8F9FA", "#212529", "#007BFF")

        val allThemes = listOf(Neon, Dark, Book, CinematicRoom, Characters, Table, Minimal)

        fun fromName(name: String): Theme {
            return allThemes.find { it.name.equals(name, ignoreCase = true) } ?: Neon
        }
    }
}
