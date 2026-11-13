package com.hamza.reelcraft.render

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import com.hamza.reelcraft.model.SlideData
import com.hamza.reelcraft.model.Theme
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin

class VideoRenderer(private val context: Context) {

    companion object {
        private const val TAG = "VideoRenderer"
        private const val WIDTH = 720
        private const val HEIGHT = 1280
        private const val FPS = 30
        private const val BIT_RATE = 3_000_000
    }

    // Reusable Paint & Path objects for high performance (Rule 3)
    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val starPaint = Paint().apply { color = AndroidColor.WHITE; isAntiAlias = true }
    private val wavePath = Path()
    private val wavePaint = Paint().apply { isAntiAlias = true }
    private val sparkPaint = Paint().apply { isAntiAlias = true }
    private val vignettePaint = Paint().apply { isAntiAlias = true }
    private val cardPaint = Paint().apply { color = AndroidColor.parseColor("#25000000"); style = Paint.Style.FILL; isAntiAlias = true }
    private val borderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }

    /**
     * Draw a context-aware animated background and fancy slide onto a 720x1280 Bitmap.
     */
    fun drawSlideBitmap(slide: SlideData, theme: Theme, slideIndex: Int, totalSlides: Int, animProgress: Float = 0.0f): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Ken Burns Slow Zoom (1.0 to 1.08)
        val currentScale = 1.0f + (0.08f * animProgress)
        canvas.save()
        canvas.scale(currentScale, currentScale, WIDTH / 2f, HEIGHT / 2f)

        // 1. Draw Context-Aware Background Scene (`NIGHT_SKY`, `OCEAN`, `FIRE_EMBERS`, or fallback)
        drawBackgroundScene(canvas, slide.bgScene, animProgress, theme)

        // 2. Draw Vignette & Subtle Film Grain overlay
        drawVignetteAndGrain(canvas)

        // Colors
        val textColor = try { AndroidColor.parseColor(theme.textColorHex) } catch (e: Exception) { AndroidColor.WHITE }
        val accentColor = try { AndroidColor.parseColor(slide.accentColor) } catch (e: Exception) { 
            try { AndroidColor.parseColor(theme.accentColorHex) } catch (ex: Exception) { AndroidColor.parseColor("#22D3EE") }
        }

        val marginX = 54
        val contentWidth = WIDTH - (2 * marginX)
        val topBound = 180f

        // 3. Top Retention Progress Bar
        val progressPaint = Paint().apply {
            color = accentColor
            strokeWidth = 8f
            style = Paint.Style.FILL
            setShadowLayer(10f, 0f, 0f, accentColor)
        }
        val progressWidth = (WIDTH.toFloat() / totalSlides) * (slideIndex + animProgress)
        canvas.drawRoundRect(40f, 40f, progressWidth.coerceAtLeast(40f), 52f, 6f, 6f, progressPaint)

        // 4. Draw Template Content with Animation Alpha
        val animAlpha = (animProgress * 3.0f).coerceIn(0.2f, 1.0f)
        val badgeTitle = getBadgeForScene(slide.bgScene, slide.templateType)

        when (slide.templateType.lowercase()) {
            "riddle" -> drawTemplateContent(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor, animAlpha, badgeTitle)
            "quote" -> drawTemplateContent(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor, animAlpha, badgeTitle)
            "listicle" -> drawTemplateContent(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor, animAlpha, badgeTitle)
            else -> drawTemplateContent(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor, animAlpha, badgeTitle)
        }

        // 5. Footer Watermark
        val footerPaint = Paint().apply {
            color = textColor
            alpha = (180 * animAlpha).toInt()
            textSize = 26f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(6f, 0f, 2f, AndroidColor.BLACK)
        }
        canvas.drawText("✨ ReelCraft • [${theme.name}]", WIDTH / 2f, HEIGHT - 80f, footerPaint)

        canvas.restore()
        return bitmap
    }

    private fun drawBackgroundScene(canvas: Canvas, scene: String, progress: Float, theme: Theme) {
        when (scene.uppercase()) {
            "NIGHT_SKY" -> drawNightSky(canvas, progress)
            "OCEAN" -> drawOceanWaves(canvas, progress)
            "FIRE_EMBERS" -> drawFireEmbers(canvas, progress)
            else -> {
                // Fallback gradient based on theme
                val baseColor = try { AndroidColor.parseColor(theme.backgroundColorHex) } catch (e: Exception) { AndroidColor.parseColor("#0B0B12") }
                val grad = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), intArrayOf(baseColor, AndroidColor.BLACK), floatArrayOf(0f, 1f), Shader.TileMode.CLAMP)
                bgPaint.shader = grad
                canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)
            }
        }
    }

    private fun drawNightSky(canvas: Canvas, progress: Float) {
        // Sky gradient
        val grad = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), intArrayOf(AndroidColor.parseColor("#090D16"), AndroidColor.parseColor("#1B2A4A"), AndroidColor.parseColor("#05070B")), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP)
        bgPaint.shader = grad
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // Moon
        val moonPaint = Paint().apply { color = AndroidColor.parseColor("#FFFBEB"); isAntiAlias = true; setShadowLayer(30f, 0f, 0f, AndroidColor.parseColor("#FDE047")) }
        canvas.drawCircle(WIDTH - 150f, 220f, 60f, moonPaint)

        // Twinkling Stars (reusing starPaint)
        val starCoords = listOf(100f to 150f, 250f to 300f, 400f to 120f, 550f to 280f, 150f to 500f, 600f to 600f, 300f to 750f, 650f to 200f)
        for ((idx, coord) in starCoords.withIndex()) {
            val twinkle = (sin((progress * 10f) + idx) * 2f + 4f).toFloat()
            starPaint.alpha = ((sin((progress * 8f) + idx) * 100 + 155).toInt()).coerceIn(50, 255)
            canvas.drawCircle(coord.first, coord.second, twinkle, starPaint)
        }
    }

    private fun drawOceanWaves(canvas: Canvas, progress: Float) {
        // Sky gradient
        val grad = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), intArrayOf(AndroidColor.parseColor("#0C4A6E"), AndroidColor.parseColor("#0369A1"), AndroidColor.parseColor("#082F49")), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        bgPaint.shader = grad
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // Layered Waves
        val shift = sin(progress * 6.0f) * 30f
        wavePath.reset()
        wavePath.moveTo(0f, HEIGHT * 0.7f)
        wavePath.cubicTo(WIDTH * 0.3f + shift, HEIGHT * 0.65f, WIDTH * 0.7f - shift, HEIGHT * 0.75f, WIDTH.toFloat(), HEIGHT * 0.7f)
        wavePath.lineTo(WIDTH.toFloat(), HEIGHT.toFloat())
        wavePath.lineTo(0f, HEIGHT.toFloat())
        wavePath.close()
        wavePaint.color = AndroidColor.parseColor("#0284C7")
        canvas.drawPath(wavePath, wavePaint)

        wavePath.reset()
        wavePath.moveTo(0f, HEIGHT * 0.8f)
        wavePath.cubicTo(WIDTH * 0.4f - shift, HEIGHT * 0.85f, WIDTH * 0.6f + shift, HEIGHT * 0.75f, WIDTH.toFloat(), HEIGHT * 0.82f)
        wavePath.lineTo(WIDTH.toFloat(), HEIGHT.toFloat())
        wavePath.lineTo(0f, HEIGHT.toFloat())
        wavePath.close()
        wavePaint.color = AndroidColor.parseColor("#0369A1")
        canvas.drawPath(wavePath, wavePaint)
    }

    private fun drawFireEmbers(canvas: Canvas, progress: Float) {
        // Warm dark radial gradient
        val radShader = RadialGradient(WIDTH / 2f, HEIGHT.toFloat(), WIDTH.toFloat() * 0.8f, intArrayOf(AndroidColor.parseColor("#7C2D12"), AndroidColor.parseColor("#451A03"), AndroidColor.parseColor("#180A03")), floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
        bgPaint.shader = radShader
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // Rising Sparks
        val sparkCoords = listOf(120f to 0.8f, 240f to 0.5f, 360f to 0.9f, 480f to 0.3f, 600f to 0.7f, 200f to 0.2f, 500f to 0.6f)
        for ((idx, coord) in sparkCoords.withIndex()) {
            val yOffset = ((progress + coord.second) % 1.0f) * HEIGHT
            val currentY = HEIGHT - yOffset
            val currentX = coord.first + (sin(yOffset * 0.02f + idx) * 20f)
            sparkPaint.color = if (idx % 2 == 0) AndroidColor.parseColor("#FDE047") else AndroidColor.parseColor("#FB923C")
            sparkPaint.alpha = ((1.0f - (yOffset / HEIGHT)) * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(currentX, currentY, 6f, sparkPaint)
        }
    }

    private fun drawVignetteAndGrain(canvas: Canvas) {
        val vignetteShader = RadialGradient(WIDTH / 2f, HEIGHT / 2f, WIDTH.toFloat() * 0.8f, intArrayOf(AndroidColor.TRANSPARENT, AndroidColor.parseColor("#99000000")), floatArrayOf(0.6f, 1.0f), Shader.TileMode.CLAMP)
        vignettePaint.shader = vignetteShader
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), vignettePaint)
    }

    private fun getBadgeForScene(scene: String, templateType: String): String {
        return when {
            scene.uppercase() == "NIGHT_SKY" -> "🌌 NIGHT SKY STORY"
            scene.uppercase() == "OCEAN" -> "🌊 OCEAN TIDE"
            scene.uppercase() == "FIRE_EMBERS" -> "🔥 FIRE & EMBERS"
            else -> when (templateType.lowercase()) {
                "riddle" -> "🧠 BRAIN TEASER"
                "quote" -> "💡 INSPIRATION"
                "listicle" -> "📌 KEY POINTS"
                else -> "🔥 VIRAL STORY"
            }
        }
    }

    private fun drawTemplateContent(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int, animAlpha: Float, badge: String) {
        // Badge
        val badgePaint = Paint().apply {
            color = accentColor
            alpha = (255 * animAlpha).toInt()
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = AndroidColor.BLACK
            alpha = (255 * animAlpha).toInt()
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textWidth = textPaint.measureText(badge)
        val badgeRect = RectF(marginX.toFloat(), topBound, marginX + textWidth + 48f, topBound + 52f)
        canvas.drawRoundRect(badgeRect, 26f, 26f, badgePaint)
        canvas.drawText(badge, marginX + (textWidth + 48f) / 2f, topBound + 36f, textPaint)

        // Headline
        val titlePaint = TextPaint().apply {
            color = textColor
            alpha = (255 * animAlpha).toInt()
            textSize = 58f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(12f, 0f, 4f, AndroidColor.BLACK)
        }
        val titleLayout = StaticLayout.Builder.obtain(slide.headline, 0, slide.headline.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat(), topBound + 120f)
        titleLayout.draw(canvas)
        canvas.restore()

        // Card & Body
        val cardTop = topBound + 380f
        val cardBottom = cardTop + 450f
        canvas.drawRoundRect(RectF(marginX.toFloat() - 20f, cardTop, (WIDTH - marginX + 20).toFloat(), cardBottom), 24f, 24f, cardPaint)

        borderPaint.color = accentColor
        borderPaint.alpha = (150 * animAlpha).toInt()
        canvas.drawRoundRect(RectF(marginX.toFloat() - 20f, cardTop, (WIDTH - marginX + 20).toFloat(), cardBottom), 24f, 24f, borderPaint)

        val bodyPaint = TextPaint().apply {
            color = textColor
            alpha = (255 * animAlpha).toInt()
            textSize = 42f
            isAntiAlias = true
            setShadowLayer(8f, 0f, 2f, AndroidColor.BLACK)
        }
        val bodyLayout = StaticLayout.Builder.obtain(slide.body, 0, slide.body.length, bodyPaint, contentWidth - 40)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat() + 20f, cardTop + 60f)
        bodyLayout.draw(canvas)
        canvas.restore()
    }

    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val a = AndroidColor.alpha(color)
        val r = (AndroidColor.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (AndroidColor.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (AndroidColor.blue(color) * factor).toInt().coerceIn(0, 255)
        return AndroidColor.argb(a, r, g, b)
    }

    fun renderProjectToMp4(slides: List<SlideData>, theme: Theme, outputFileName: String = "reel_${System.currentTimeMillis()}.mp4"): File? {
        if (slides.isEmpty()) return null

        val outputFile = File(context.cacheDir, outputFileName)
        if (outputFile.exists()) outputFile.delete()

        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, WIDTH, HEIGHT).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
            setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: android.view.Surface? = null

        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = codec.createInputSurface()
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var trackIndex = -1
            var muxerStarted = false

            val bufferInfo = MediaCodec.BufferInfo()
            val slideDurationMs = 3000L
            val totalFramesPerSlide = (slideDurationMs * FPS / 1000L).toInt()

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    try {
                        codec?.signalEndOfInputStream()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error signaling end of input stream", e)
                    }
                }

                while (true) {
                    val outputBufferId = codec?.dequeueOutputBuffer(bufferInfo, 10000) ?: -1
                    if (outputBufferId == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) break
                    } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) {
                            throw RuntimeException("Format changed twice")
                        }
                        val newFormat = codec?.outputFormat
                        trackIndex = muxer?.addTrack(newFormat!!) ?: -1
                        muxer?.start()
                        muxerStarted = true
                    } else if (outputBufferId < 0) {
                        // ignore
                    } else {
                        val encodedData = codec?.getOutputBuffer(outputBufferId)
                            ?: throw RuntimeException("encoderOutputBuffer $outputBufferId was null")

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            bufferInfo.size = 0
                        }

                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                throw RuntimeException("muxer hasn't started")
                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer?.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }

                        codec?.releaseOutputBuffer(outputBufferId, false)

                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            for ((index, slide) in slides.withIndex()) {
                for (i in 0 until totalFramesPerSlide) {
                    val animProgress = i.toFloat() / totalFramesPerSlide.toFloat()
                    val slideBitmap = drawSlideBitmap(slide, theme, index, slides.size, animProgress)

                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        canvas.drawBitmap(slideBitmap, 0f, 0f, null)
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    slideBitmap.recycle()

                    drainEncoder(false)
                }
            }

            drainEncoder(true)
            saveVideoToMediaStore(outputFile)

            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering cinematic scene video", e)
            return null
        } finally {
            try {
                inputSurface?.release()
                codec?.stop()
                codec?.release()
                muxer?.stop()
                muxer?.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up codec/muxer", e)
            }
        }
    }

    private fun saveVideoToMediaStore(videoFile: File): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "ReelCraft_Cinematic_${System.currentTimeMillis()}.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/ReelCraft")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                videoFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video to MediaStore", e)
            return false
        }
    }
}
