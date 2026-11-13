package com.hamza.reelcraft.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Color as AndroidColor
import com.hamza.reelcraft.model.LayerConfig
import com.hamza.reelcraft.model.LayerType
import com.hamza.reelcraft.model.SceneSpec
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random

object SceneRenderer {
    private const val WIDTH = 720
    private const val HEIGHT = 1280

    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val starPaint = Paint().apply { color = AndroidColor.WHITE; isAntiAlias = true }
    private val wavePath = Path()
    private val wavePaint = Paint().apply { isAntiAlias = true }
    private val sparkPaint = Paint().apply { isAntiAlias = true }
    private val vignettePaint = Paint().apply { isAntiAlias = true }
    private val gridPaint = Paint().apply { strokeWidth = 2f; isAntiAlias = true }

    private val grainBitmap: Bitmap by lazy {
        Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply {
            val r = Random(42)
            for (x in 0 until 128) {
                for (y in 0 until 128) {
                    val n = r.nextInt(25)
                    setPixel(x, y, AndroidColor.argb(n, 255, 255, 255))
                }
            }
        }
    }
    private val grainPaint = Paint().apply { isAntiAlias = false }

    fun render(canvas: Canvas, spec: SceneSpec, timeSec: Float) {
        val sortedLayers = spec.layers.sortedBy { it.parallaxSpeed }

        for (layer in sortedLayers) {
            canvas.save()
            val shiftX = sin(timeSec * 0.5f) * layer.parallaxSpeed * 10f * spec.intensity
            val shiftY = cos(timeSec * 0.3f) * layer.parallaxSpeed * 5f * spec.intensity
            canvas.translate(shiftX, shiftY)

            drawLayer(canvas, layer, timeSec)
            canvas.restore()
        }

        drawVignetteAndGrain(canvas)
    }

    private fun drawLayer(canvas: Canvas, layer: LayerConfig, timeSec: Float) {
        val colors = if (layer.colors.isNotEmpty()) layer.colors else listOf(AndroidColor.parseColor("#090D16"), AndroidColor.parseColor("#1B2A4A"))

        when (layer.type) {
            LayerType.GRADIENT_SKY, LayerType.BLOBS_GRADIENT -> {
                val c1 = colors.getOrElse(0) { AndroidColor.parseColor("#090D16") }
                val c2 = colors.getOrElse(1) { AndroidColor.parseColor("#1B2A4A") }
                val c3 = colors.getOrElse(2) { c2 }
                val grad = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), intArrayOf(c1, c2, c3), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
                bgPaint.shader = grad
                canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)
            }
            LayerType.STARS -> {
                val count = layer.param1.toInt().coerceIn(10, 50)
                for (i in 0 until count) {
                    val x = (i * 73f) % WIDTH
                    val y = (i * 137f) % (HEIGHT * 0.7f)
                    starPaint.alpha = ((sin((timeSec * 4f) + i) * 100 + 155).toInt()).coerceIn(50, 255)
                    canvas.drawCircle(x, y, 3f, starPaint)
                }
            }
            LayerType.MOON_OR_SUN -> {
                val c1 = colors.getOrElse(0) { AndroidColor.parseColor("#FFFBEB") }
                val c2 = colors.getOrElse(1) { AndroidColor.parseColor("#FDE047") }
                val moonPaint = Paint().apply { color = c1; isAntiAlias = true; setShadowLayer(30f * layer.intensity, 0f, 0f, c2) }
                canvas.drawCircle(WIDTH - 180f, 240f, layer.param1.coerceAtLeast(40f), moonPaint)
            }
            LayerType.WAVES -> {
                val shift = sin(timeSec * 3.0f) * 30f * layer.intensity
                wavePath.reset()
                wavePath.moveTo(0f, HEIGHT * 0.7f)
                wavePath.cubicTo(WIDTH * 0.3f + shift, HEIGHT * 0.65f, WIDTH * 0.7f - shift, HEIGHT * 0.75f, WIDTH.toFloat(), HEIGHT * 0.7f)
                wavePath.lineTo(WIDTH.toFloat(), HEIGHT.toFloat())
                wavePath.lineTo(0f, HEIGHT.toFloat())
                wavePath.close()
                wavePaint.color = colors.getOrElse(0) { AndroidColor.parseColor("#0284C7") }
                canvas.drawPath(wavePath, wavePaint)

                wavePath.reset()
                wavePath.moveTo(0f, HEIGHT * 0.8f)
                wavePath.cubicTo(WIDTH * 0.4f - shift, HEIGHT * 0.85f, WIDTH * 0.6f + shift, HEIGHT * 0.75f, WIDTH.toFloat(), HEIGHT * 0.82f)
                wavePath.lineTo(WIDTH.toFloat(), HEIGHT.toFloat())
                wavePath.lineTo(0f, HEIGHT.toFloat())
                wavePath.close()
                wavePaint.color = colors.getOrElse(1) { AndroidColor.parseColor("#0369A1") }
                canvas.drawPath(wavePath, wavePaint)
            }
            LayerType.EMBERS_OR_SPARKS -> {
                val count = layer.param1.toInt().coerceIn(10, 40)
                for (i in 0 until count) {
                    val yOffset = ((timeSec + (i * 0.1f)) % 3.0f) / 3.0f * HEIGHT
                    val currentY = HEIGHT - yOffset
                    val currentX = (i * 110f) % WIDTH + (sin(yOffset * 0.02f + i) * 20f * layer.intensity)
                    sparkPaint.color = if (i % 2 == 0) AndroidColor.parseColor("#FDE047") else AndroidColor.parseColor("#FB923C")
                    sparkPaint.alpha = ((1.0f - (yOffset / HEIGHT)) * 255).toInt().coerceIn(0, 255)
                    canvas.drawCircle(currentX, currentY, 5f * layer.intensity, sparkPaint)
                }
            }
            LayerType.GRID_OR_LINES -> {
                gridPaint.color = colors.getOrElse(0) { AndroidColor.parseColor("#22D3EE") }
                gridPaint.alpha = (100 * layer.intensity).toInt().coerceIn(50, 255)
                val horizonY = HEIGHT * 0.5f
                val centerX = WIDTH / 2f
                for (i in -5..5) {
                    canvas.drawLine(centerX + (i * 120f), horizonY, centerX + (i * 300f), HEIGHT.toFloat(), gridPaint)
                }
                val lineOffset = (timeSec * 50f) % 50f
                var y = horizonY + lineOffset
                while (y < HEIGHT) {
                    canvas.drawLine(0f, y, WIDTH.toFloat(), y, gridPaint)
                    y += 50f
                }
            }
            LayerType.AURORA -> {
                val shift = sin(timeSec * 2f) * 40f
                wavePath.reset()
                wavePath.moveTo(0f, HEIGHT * 0.3f)
                wavePath.cubicTo(WIDTH * 0.25f + shift, HEIGHT * 0.2f, WIDTH * 0.75f - shift, HEIGHT * 0.4f, WIDTH.toFloat(), HEIGHT * 0.25f)
                wavePath.lineTo(WIDTH.toFloat(), HEIGHT * 0.6f)
                wavePath.cubicTo(WIDTH * 0.75f - shift, HEIGHT * 0.7f, WIDTH * 0.25f + shift, HEIGHT * 0.5f, 0f, HEIGHT * 0.65f)
                wavePath.close()

                val auroraGrad = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), intArrayOf(AndroidColor.parseColor("#34D399"), AndroidColor.parseColor("#22D3EE"), AndroidColor.TRANSPARENT), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                wavePaint.shader = auroraGrad
                wavePaint.alpha = 130
                canvas.drawPath(wavePath, wavePaint)
                wavePaint.shader = null
            }
            LayerType.SPACE_NEBULA -> {
                val radShader = RadialGradient(WIDTH / 2f, HEIGHT * 0.4f, WIDTH.toFloat() * 0.9f * layer.intensity, intArrayOf(AndroidColor.parseColor("#581C87"), AndroidColor.parseColor("#3B0764"), AndroidColor.parseColor("#09090B")), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                bgPaint.shader = radShader
                canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)
            }
            LayerType.MOUNTAINS_OR_DUNES -> {
                wavePath.reset()
                wavePath.moveTo(0f, HEIGHT * 0.75f)
                wavePath.cubicTo(WIDTH * 0.3f, HEIGHT * 0.65f, WIDTH * 0.7f, HEIGHT * 0.8f, WIDTH.toFloat(), HEIGHT * 0.7f)
                wavePath.lineTo(WIDTH.toFloat(), HEIGHT.toFloat())
                wavePath.lineTo(0f, HEIGHT.toFloat())
                wavePath.close()
                wavePaint.color = colors.getOrElse(0) { AndroidColor.parseColor("#1E293B") }
                canvas.drawPath(wavePath, wavePaint)
            }
            LayerType.CITY_SILHOUETTE -> {
                val buildingPaint = Paint().apply { color = colors.getOrElse(0) { AndroidColor.parseColor("#0F172A") }; isAntiAlias = true }
                val windowPaint = Paint().apply { color = AndroidColor.parseColor("#FDE047"); isAntiAlias = true }
                val buildingWidths = listOf(90f, 70f, 110f, 80f, 100f, 85f, 95f)
                val buildingHeights = listOf(350f, 450f, 300f, 500f, 400f, 480f, 380f)
                var currentX = 0f
                for (i in buildingWidths.indices) {
                    val w = buildingWidths[i]
                    val h = buildingHeights[i] * layer.intensity
                    val y = HEIGHT - h
                    canvas.drawRect(currentX, y, currentX + w, HEIGHT.toFloat(), buildingPaint)
                    if (i % 2 == 0) {
                        canvas.drawRect(currentX + 15f, y + 30f, currentX + 35f, y + 60f, windowPaint)
                        canvas.drawRect(currentX + 45f, y + 90f, currentX + 65f, y + 120f, windowPaint)
                    }
                    currentX += w
                }
            }
            LayerType.MOSQUE_SILHOUETTE -> {
                val mosquePaint = Paint().apply { color = colors.getOrElse(0) { AndroidColor.parseColor("#0B0F19") }; isAntiAlias = true }
                canvas.drawRect(100f, HEIGHT - (350f * layer.intensity), WIDTH - 100f, HEIGHT.toFloat(), mosquePaint)
                wavePath.reset()
                wavePath.addArc(WIDTH / 2f - 120f, HEIGHT - (520f * layer.intensity), WIDTH / 2f + 120f, HEIGHT - (330f * layer.intensity), 180f, 180f)
                canvas.drawPath(wavePath, mosquePaint)
                canvas.drawRect(140f, HEIGHT - (600f * layer.intensity), 190f, HEIGHT.toFloat(), mosquePaint)
                canvas.drawRect(WIDTH - 190f, HEIGHT - (600f * layer.intensity), WIDTH - 140f, HEIGHT.toFloat(), mosquePaint)
            }
            LayerType.RAIN_OR_SNOW -> {
                val rainPaint = Paint().apply { color = AndroidColor.parseColor("#93C5FD"); strokeWidth = 3f; isAntiAlias = true }
                val count = layer.param1.toInt().coerceIn(20, 60)
                for (i in 0 until count) {
                    val rx = (i * 47f) % WIDTH
                    val ry = ((timeSec * 300f + (i * 31f)) % HEIGHT)
                    canvas.drawLine(rx, ry, rx - 10f, ry + (40f * layer.intensity), rainPaint)
                }
            }
            LayerType.CLOUDS -> {
                val cloudPaint = Paint().apply { color = AndroidColor.parseColor("#334155"); alpha = (120 * layer.intensity).toInt().coerceIn(50, 255); isAntiAlias = true }
                val shift = sin(timeSec * 1.5f) * 50f
                canvas.drawCircle(150f + shift, 300f, 120f, cloudPaint)
                canvas.drawCircle(260f + shift, 270f, 150f, cloudPaint)
                canvas.drawCircle(500f - shift, 350f, 130f, cloudPaint)
            }
            LayerType.FOG -> {
                val fogPaint = Paint().apply { isAntiAlias = true }
                val fogGrad = LinearGradient(0f, HEIGHT * 0.5f, 0f, HEIGHT.toFloat(), intArrayOf(AndroidColor.TRANSPARENT, AndroidColor.parseColor("#66334155"), AndroidColor.TRANSPARENT), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
                fogPaint.shader = fogGrad
                canvas.drawRect(0f, HEIGHT * 0.5f, WIDTH.toFloat(), HEIGHT.toFloat(), fogPaint)
            }
            else -> {
                val c = colors.getOrElse(0) { AndroidColor.parseColor("#111827") }
                bgPaint.color = c
                canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)
            }
        }
    }

    private fun drawVignetteAndGrain(canvas: Canvas) {
        val vignetteShader = RadialGradient(WIDTH / 2f, HEIGHT / 2f, WIDTH.toFloat() * 0.8f, intArrayOf(AndroidColor.TRANSPARENT, AndroidColor.parseColor("#99000000")), floatArrayOf(0.6f, 1.0f), Shader.TileMode.CLAMP)
        vignettePaint.shader = vignetteShader
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), vignettePaint)

        canvas.drawBitmap(grainBitmap, 0f, 0f, grainPaint)
    }
}
