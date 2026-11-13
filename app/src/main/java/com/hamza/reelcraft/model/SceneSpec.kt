package com.hamza.reelcraft.model

import android.graphics.Color
import kotlin.math.abs
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

    fun getSpec(name: String, seed: Long): SceneSpec {
        val upper = name.uppercase().trim()
        val baseSpec = presets[upper] ?: generateFromSeed(seed, upper)

        // Step 2: Seed-based palette variation (Hue +- 30 deg, saturation, brightness)
        val rng = Random(seed)
        val adjustedPalette = baseSpec.palette.mapIndexed { idx, color ->
            adjustColor(color, seed, idx)
        }

        // Step 2: Seed-based layer variations (speeds, particle density, wave amplitudes)
        val adjustedLayers = baseSpec.layers.map { layer ->
            layer.copy(
                parallaxSpeed = (layer.parallaxSpeed * (0.8f + rng.nextFloat() * 0.4f)).coerceIn(0.1f, 2.0f),
                param1 = layer.param1 * (0.7f + rng.nextFloat() * 0.6f),
                colors = layer.colors.ifEmpty { adjustedPalette }
            )
        }

        return baseSpec.copy(
            palette = adjustedPalette,
            layers = adjustedLayers
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
            listOf(Color.parseColor("#0C4A6E"), Color.parseColor("#1E3A8A"), Color.parseColor("#030712")),
            listOf(Color.parseColor("#2E1065"), Color.parseColor("#701A75"), Color.parseColor("#09090B")),
            listOf(Color.parseColor("#14532D"), Color.parseColor("#065F46"), Color.parseColor("#020617"))
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
