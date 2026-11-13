package com.hamza.reelcraft.model

import android.graphics.Color
import kotlin.random.Random

enum class LayerType {
    GRADIENT_SKY,
    STARS,
    MOON_OR_SUN,
    CLOUDS,
    WAVES,
    MOUNTAINS_OR_DUNES,
    CITY_SILHOUETTE,
    TREES_FOREST,
    RAIN_OR_SNOW,
    EMBERS_OR_SPARKS,
    BOKEH,
    GRID_OR_LINES,
    LIGHT_RAYS,
    LIGHTNING,
    FOG,
    SHAPES,
    MOSQUE_SILHOUETTE,
    SPACE_NEBULA,
    AURORA,
    BLOBS_GRADIENT
}

data class LayerConfig(
    val type: LayerType,
    val parallaxSpeed: Float = 1.0f,
    val opacity: Float = 1.0f,
    val colors: List<Int> = emptyList(),
    val intensity: Float = 1.0f,
    val param1: Float = 0f,
    val param2: Float = 0f,
    val param3: Float = 0f
)

data class SceneSpec(
    val name: String,
    val palette: List<Int>,
    val intensity: Float = 1.0f,
    val layers: List<LayerConfig>
)

object SceneGenerator {
    val presetNames = listOf("NIGHT_SKY", "OCEAN", "FIRE_EMBERS", "CYBER_GRID", "AURORA_BOREALIS", "GOLDEN_SAND", "MYSTIC_PURPLE")

    val presets: Map<String, SceneSpec> = mapOf(
        "NIGHT_SKY" to SceneSpec(
            name = "NIGHT_SKY",
            palette = listOf(Color.parseColor("#090D16"), Color.parseColor("#1B2A4A"), Color.parseColor("#05070B")),
            intensity = 1.0f,
            layers = listOf(
                LayerConfig(LayerType.GRADIENT_SKY, parallaxSpeed = 0.1f, colors = listOf(Color.parseColor("#090D16"), Color.parseColor("#1B2A4A"), Color.parseColor("#05070B"))),
                LayerConfig(LayerType.STARS, parallaxSpeed = 0.3f, param1 = 30f),
                LayerConfig(LayerType.MOON_OR_SUN, parallaxSpeed = 0.5f, colors = listOf(Color.parseColor("#FFFBEB"), Color.parseColor("#FDE047")), param1 = 60f),
                LayerConfig(LayerType.MOUNTAINS_OR_DUNES, parallaxSpeed = 0.8f, colors = listOf(Color.parseColor("#0F172A"), Color.parseColor("#1E293B"))),
                LayerConfig(LayerType.BOKEH, parallaxSpeed = 1.2f, param1 = 15f)
            )
        ),
        "OCEAN" to SceneSpec(
            name = "OCEAN",
            palette = listOf(Color.parseColor("#0C4A6E"), Color.parseColor("#0369A1"), Color.parseColor("#082F49")),
            intensity = 1.0f,
            layers = listOf(
                LayerConfig(LayerType.GRADIENT_SKY, parallaxSpeed = 0.1f, colors = listOf(Color.parseColor("#0C4A6E"), Color.parseColor("#0369A1"), Color.parseColor("#082F49"))),
                LayerConfig(LayerType.LIGHT_RAYS, parallaxSpeed = 0.4f, param1 = 5f),
                LayerConfig(LayerType.WAVES, parallaxSpeed = 1.0f, colors = listOf(Color.parseColor("#0284C7"), Color.parseColor("#0369A1"), Color.parseColor("#0284C7"))),
                LayerConfig(LayerType.BOKEH, parallaxSpeed = 1.3f, param1 = 12f)
            )
        ),
        "FIRE_EMBERS" to SceneSpec(
            name = "FIRE_EMBERS",
            palette = listOf(Color.parseColor("#7C2D12"), Color.parseColor("#451A03"), Color.parseColor("#180A03")),
            intensity = 1.0f,
            layers = listOf(
                LayerConfig(LayerType.BLOBS_GRADIENT, parallaxSpeed = 0.2f, colors = listOf(Color.parseColor("#7C2D12"), Color.parseColor("#451A03"))),
                LayerConfig(LayerType.EMBERS_OR_SPARKS, parallaxSpeed = 1.2f, param1 = 25f),
                LayerConfig(LayerType.LIGHT_RAYS, parallaxSpeed = 0.6f, param1 = 3f)
            )
        ),
        "CYBER_GRID" to SceneSpec(
            name = "CYBER_GRID",
            palette = listOf(Color.parseColor("#030712"), Color.parseColor("#111827")),
            intensity = 1.0f,
            layers = listOf(
                LayerConfig(LayerType.GRADIENT_SKY, parallaxSpeed = 0.1f, colors = listOf(Color.parseColor("#030712"), Color.parseColor("#111827"))),
                LayerConfig(LayerType.GRID_OR_LINES, parallaxSpeed = 1.0f, colors = listOf(Color.parseColor("#22D3EE"))),
                LayerConfig(LayerType.SHAPES, parallaxSpeed = 0.8f, param1 = 10f)
            )
        ),
        "AURORA_BOREALIS" to SceneSpec(
            name = "AURORA_BOREALIS",
            palette = listOf(Color.parseColor("#022C22"), Color.parseColor("#064E3B"), Color.parseColor("#020617")),
            intensity = 1.0f,
            layers = listOf(
                LayerConfig(LayerType.GRADIENT_SKY, parallaxSpeed = 0.1f, colors = listOf(Color.parseColor("#022C22"), Color.parseColor("#020617"))),
                LayerConfig(LayerType.AURORA, parallaxSpeed = 0.5f, colors = listOf(Color.parseColor("#34D399"), Color.parseColor("#22D3EE"))),
                LayerConfig(LayerType.STARS, parallaxSpeed = 0.3f, param1 = 20f),
                LayerConfig(LayerType.MOUNTAINS_OR_DUNES, parallaxSpeed = 0.9f, colors = listOf(Color.parseColor("#064E3B")))
            )
        ),
        "GOLDEN_SAND" to SceneSpec(
            name = "GOLDEN_SAND",
            palette = listOf(Color.parseColor("#451A03"), Color.parseColor("#78350F")),
            intensity = 1.0f,
            layers = listOf(
                LayerConfig(LayerType.GRADIENT_SKY, parallaxSpeed = 0.1f, colors = listOf(Color.parseColor("#451A03"), Color.parseColor("#1C1917"))),
                LayerConfig(LayerType.MOUNTAINS_OR_DUNES, parallaxSpeed = 0.7f, colors = listOf(Color.parseColor("#78350F"), Color.parseColor("#92400E"))),
                LayerConfig(LayerType.EMBERS_OR_SPARKS, parallaxSpeed = 1.1f, colors = listOf(Color.parseColor("#FDE047")), param1 = 20f),
                LayerConfig(LayerType.LIGHT_RAYS, parallaxSpeed = 0.5f, param1 = 4f)
            )
        ),
        "MYSTIC_PURPLE" to SceneSpec(
            name = "MYSTIC_PURPLE",
            palette = listOf(Color.parseColor("#581C87"), Color.parseColor("#3B0764")),
            intensity = 1.0f,
            layers = listOf(
                LayerConfig(LayerType.SPACE_NEBULA, parallaxSpeed = 0.2f, colors = listOf(Color.parseColor("#581C87"), Color.parseColor("#3B0764"))),
                LayerConfig(LayerType.STARS, parallaxSpeed = 0.4f, param1 = 40f),
                LayerConfig(LayerType.BOKEH, parallaxSpeed = 1.2f, param1 = 20f),
                LayerConfig(LayerType.LIGHT_RAYS, parallaxSpeed = 0.6f, param1 = 6f)
            )
        )
    )

    fun getContextualSpec(contextText: String, niche: String, bgMood: String, seed: Long, slideIndex: Int, totalSlides: Int): SceneSpec {
        val query = "$contextText $niche $bgMood".lowercase()
        val baseSpec = when {
            query.contains("horror") || query.contains("dar") || query.contains("bhoot") ->
                SceneSpec("STORM_HORROR", listOf(Color.parseColor("#180A0A"), Color.parseColor("#450A0A")), 1.0f, listOf(
                    LayerConfig(LayerType.GRADIENT_SKY, 0.1f, colors = listOf(Color.parseColor("#180A0A"), Color.parseColor("#450A0A"))),
                    LayerConfig(LayerType.FOG, 0.4f),
                    LayerConfig(LayerType.CITY_SILHOUETTE, 0.7f),
                    LayerConfig(LayerType.RAIN_OR_SNOW, 1.2f, param1 = 30f)
                ))
            query.contains("motivation") || query.contains("kamyabi") || query.contains("success") ->
                SceneSpec("SUNRISE_MOTIVATION", listOf(Color.parseColor("#7C2D12"), Color.parseColor("#B45309")), 1.0f, listOf(
                    LayerConfig(LayerType.GRADIENT_SKY, 0.1f, colors = listOf(Color.parseColor("#451A03"), Color.parseColor("#B45309"))),
                    LayerConfig(LayerType.LIGHT_RAYS, 0.5f, param1 = 6f),
                    LayerConfig(LayerType.MOUNTAINS_OR_DUNES, 0.8f),
                    LayerConfig(LayerType.CLOUDS, 1.0f)
                ))
            query.contains("space") || query.contains("science") || query.contains("dunya") ->
                presets["MYSTIC_PURPLE"]!!
            query.contains("islamic") || query.contains("dua") || query.contains("hadees") || query.contains("allah") ->
                SceneSpec("MOSQUE_NIGHT", listOf(Color.parseColor("#0F172A"), Color.parseColor("#1E1B4B")), 1.0f, listOf(
                    LayerConfig(LayerType.GRADIENT_SKY, 0.1f, colors = listOf(Color.parseColor("#090D16"), Color.parseColor("#1E1B4B"))),
                    LayerConfig(LayerType.MOSQUE_SILHOUETTE, 0.6f),
                    LayerConfig(LayerType.STARS, 0.3f, param1 = 35f),
                    LayerConfig(LayerType.MOON_OR_SUN, 0.5f, colors = listOf(Color.parseColor("#FEF08A"), Color.parseColor("#FACC15")), param1 = 50f)
                ))
            query.contains("shayari") || query.contains("ishq") || query.contains("udasi") || query.contains("poetry") ->
                SceneSpec("SHAYARI_BOKEH", listOf(Color.parseColor("#3B0764"), Color.parseColor("#581C87")), 1.0f, listOf(
                    LayerConfig(LayerType.GRADIENT_SKY, 0.1f, colors = listOf(Color.parseColor("#3B0764"), Color.parseColor("#1E1B4B"))),
                    LayerConfig(LayerType.BOKEH, 1.2f, param1 = 25f),
                    LayerConfig(LayerType.RAIN_OR_SNOW, 1.0f, param1 = 15f)
                ))
            query.contains("riddle") || query.contains("quiz") || query.contains("puzzle") ->
                presets["CYBER_GRID"]!!
            query.contains("cricket") || query.contains("sports") || query.contains("game") ->
                SceneSpec("SPORTS_GRID", listOf(Color.parseColor("#064E3B"), Color.parseColor("#022C22")), 1.0f, listOf(
                    LayerConfig(LayerType.GRADIENT_SKY, 0.1f, colors = listOf(Color.parseColor("#064E3B"), Color.parseColor("#022C22"))),
                    LayerConfig(LayerType.BOKEH, 1.1f, param1 = 20f),
                    LayerConfig(LayerType.LIGHT_RAYS, 0.5f, param1 = 4f),
                    LayerConfig(LayerType.GRID_OR_LINES, 0.9f)
                ))
            query.contains("history") || query.contains("lahore") || query.contains("punjab") ->
                presets["GOLDEN_SAND"]!!
            query.contains("ocean") || query.contains("nature") || query.contains("pani") ->
                presets["OCEAN"]!!
            else ->
                presets[bgMood.uppercase()] ?: generateFromSeed(seed, bgMood)
        }

        // Climax boost on final slide
        val isClimax = slideIndex == totalSlides - 1
        val intensityMultiplier = if (isClimax) 1.5f else 1.0f
        val rng = Random(seed + slideIndex)

        val variedPalette = baseSpec.palette.mapIndexed { idx, c -> adjustColor(c, seed + slideIndex, idx) }
        val variedLayers = baseSpec.layers.map { layer ->
            layer.copy(
                parallaxSpeed = (layer.parallaxSpeed + (slideIndex * 0.05f)).coerceIn(0.1f, 2.2f),
                intensity = layer.intensity * intensityMultiplier,
                param1 = layer.param1 * intensityMultiplier
            )
        }

        return baseSpec.copy(
            palette = variedPalette,
            intensity = baseSpec.intensity * intensityMultiplier,
            layers = variedLayers
        )
    }

    private fun adjustColor(color: Int, seed: Long, index: Int): Int {
        val rng = Random(seed + index)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + rng.nextFloat() * 60f - 30f + 360f) % 360f
        hsv[1] = (hsv[1] * (0.85f + rng.nextFloat() * 0.3f)).coerceIn(0f, 1f)
        hsv[2] = (hsv[2] * (0.9f + rng.nextFloat() * 0.2f)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun generateFromSeed(seed: Long, seedStr: String): SceneSpec {
        val rng = Random(seed)
        val palettes = listOf(
            listOf(Color.parseColor("#0F172A"), Color.parseColor("#312E81"), Color.parseColor("#09090B")),
            listOf(Color.parseColor("#1E1B4B"), Color.parseColor("#831843"), Color.parseColor("#0F172A")),
            listOf(Color.parseColor("#064E3B"), Color.parseColor("#022C22"), Color.parseColor("#09090B")),
            listOf(Color.parseColor("#7C2D12"), Color.parseColor("#451A03"), Color.parseColor("#1C1917")),
            listOf(Color.parseColor("#3B0764"), Color.parseColor("#1E1B4B"), Color.parseColor("#09090B")),
            listOf(Color.parseColor("#0C4A6E"), Color.parseColor("#1E3A8A"), Color.parseColor("#030712"))
        )
        val basePalette = palettes[rng.nextInt(palettes.size)]
        val palette = basePalette.mapIndexed { idx, c -> adjustColor(c, seed, idx) }

        val numLayers = rng.nextInt(3, 7)
        val selectedLayers = mutableListOf<LayerConfig>()
        selectedLayers.add(LayerConfig(LayerType.GRADIENT_SKY, parallaxSpeed = 0.1f, colors = palette))

        val possibleLayers = listOf(
            LayerType.STARS, LayerType.MOON_OR_SUN, LayerType.CLOUDS,
            LayerType.WAVES, LayerType.MOUNTAINS_OR_DUNES, LayerType.CITY_SILHOUETTE,
            LayerType.TREES_FOREST, LayerType.RAIN_OR_SNOW, LayerType.EMBERS_OR_SPARKS,
            LayerType.BOKEH, LayerType.GRID_OR_LINES, LayerType.LIGHT_RAYS,
            LayerType.FOG, LayerType.SHAPES, LayerType.SPACE_NEBULA, LayerType.AURORA, LayerType.BLOBS_GRADIENT
        )

        for (i in 0 until numLayers) {
            val t = possibleLayers[rng.nextInt(possibleLayers.size)]
            if (selectedLayers.none { it.type == t }) {
                val speed = 0.3f + (rng.nextFloat() * 1.5f)
                selectedLayers.add(LayerConfig(t, parallaxSpeed = speed, colors = palette, param1 = rng.nextFloat() * 30f + 10f))
            }
        }

        return SceneSpec(
            name = seedStr.ifBlank { "GEN_$seed" },
            palette = palette,
            intensity = 1.0f,
            layers = selectedLayers
        )
    }

    fun pickUniquePreset(recentPresets: List<String>, seed: Long): String {
        val rng = Random(seed)
        val available = presetNames.filter { !recentPresets.contains(it) }
        if (available.isEmpty()) {
            return presetNames[rng.nextInt(presetNames.size)]
        }
        return available[rng.nextInt(available.size)]
    }
}
