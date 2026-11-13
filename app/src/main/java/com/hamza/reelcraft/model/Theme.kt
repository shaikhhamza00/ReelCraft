package com.hamza.reelcraft.model

data class Theme(
    val name: String,
    val backgroundColorHex: String,
    val textColorHex: String,
    val accentColorHex: String,
    val fontName: String = "Default"
) {
    companion object {
        val Kinetic = Theme("KINETIC", "#0B0B12", "#FFFFFF", "#7C4DFF")
        val Neon = Theme("NEON", "#0D0221", "#FFFF00", "#FF007F")
        val DarkCinematic = Theme("DARK_CINEMATIC", "#08080C", "#F2F2F7", "#38BDF8")
        val Minimal = Theme("MINIMAL", "#F8F9FA", "#212529", "#007BFF")
        val GradientMesh = Theme("GRADIENT_MESH", "#111827", "#FFFFFF", "#22D3EE")
        val VintagePaper = Theme("VINTAGE_PAPER", "#FDF6E3", "#263238", "#D35400")

        val allThemes = listOf(Kinetic, Neon, DarkCinematic, Minimal, GradientMesh, VintagePaper)

        fun fromName(name: String): Theme {
            return allThemes.find { it.name.equals(name, ignoreCase = true) } ?: Kinetic
        }
    }
}
