package com.hamza.reelcraft.render

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
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

class VideoRenderer(private val context: Context) {

    companion object {
        private const val TAG = "VideoRenderer"
        private const val WIDTH = 720
        private const val HEIGHT = 1280
        private const val FPS = 30
        private const val BIT_RATE = 3_000_000 // 3 Mbps for crisp quality
    }

    /**
     * Draw a fancy, professional, modern template-based slide onto a 720x1280 Bitmap.
     */
    fun drawSlideBitmap(slide: SlideData, theme: Theme, slideIndex: Int, totalSlides: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Professional Gradient Background
        val baseColor = try { AndroidColor.parseColor(theme.backgroundColorHex) } catch (e: Exception) { AndroidColor.parseColor("#0F172A") }
        val darkBase = adjustColorBrightness(baseColor, 0.6f)
        val gradient = LinearGradient(
            0f, 0f, 0f, HEIGHT.toFloat(),
            intArrayOf(darkBase, baseColor, AndroidColor.BLACK),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        val bgPaint = Paint().apply { shader = gradient }
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // Colors
        val textColor = try { AndroidColor.parseColor(theme.textColorHex) } catch (e: Exception) { AndroidColor.WHITE }
        val accentColor = try { AndroidColor.parseColor(theme.accentColorHex) } catch (e: Exception) { AndroidColor.parseColor("#00FFCC") }

        val marginX = 54
        val contentWidth = WIDTH - (2 * marginX)
        val topBound = 180f

        // 2. Modern Top Retention Progress Bar & Hook Badge
        val progressPaint = Paint().apply {
            color = accentColor
            strokeWidth = 8f
            style = Paint.Style.FILL
            setShadowLayer(10f, 0f, 0f, accentColor)
        }
        val progressWidth = (WIDTH.toFloat() / totalSlides) * (slideIndex + 1)
        canvas.drawRoundRect(40f, 40f, progressWidth.coerceAtLeast(40f), 52f, 6f, 6f, progressPaint)

        // 3. Draw Template specific Fancy UI
        when (slide.templateType.lowercase()) {
            "riddle" -> drawFancyRiddleTemplate(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor)
            "quote" -> drawFancyQuoteTemplate(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor)
            "listicle" -> drawFancyListicleTemplate(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor, slideIndex + 1, totalSlides)
            else -> drawFancyStoryTemplate(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor)
        }

        // 4. Fancy Footer Branding
        val footerPaint = Paint().apply {
            color = textColor
            alpha = 180
            textSize = 26f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(6f, 0f, 2f, AndroidColor.BLACK)
        }
        canvas.drawText("✨ Made with ReelCraft", WIDTH / 2f, HEIGHT - 80f, footerPaint)

        return bitmap
    }

    private fun drawFancyStoryTemplate(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int) {
        // Category Badge
        drawBadge(canvas, "🔥 VIRAL STORY", marginX.toFloat(), topBound, accentColor)

        // Headline with drop shadow
        val titlePaint = TextPaint().apply {
            color = textColor
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

        // Translucent Glassmorphism Card for Body
        val cardTop = topBound + 380f
        val cardBottom = cardTop + 450f
        val cardPaint = Paint().apply {
            color = AndroidColor.parseColor("#22000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(marginX.toFloat() - 20f, cardTop, (WIDTH - marginX + 20).toFloat(), cardBottom)
        canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)

        // Card Border Glow
        val borderPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 3f
            alpha = 120
            isAntiAlias = true
        }
        canvas.drawRoundRect(cardRect, 24f, 24f, borderPaint)

        // Body Text inside card
        val bodyPaint = TextPaint().apply {
            color = textColor
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

    private fun drawFancyRiddleTemplate(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int) {
        drawBadge(canvas, "🧠 BRAIN TEASER / RIDDLE", marginX.toFloat(), topBound, accentColor)

        val titlePaint = TextPaint().apply {
            color = textColor
            textSize = 54f
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

        // Fancy Answer Box
        val boxTop = topBound + 420f
        val boxPaint = Paint().apply {
            color = AndroidColor.parseColor("#331E1E2E")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val boxRect = RectF(marginX.toFloat(), boxTop, (WIDTH - marginX).toFloat(), boxTop + 220f)
        canvas.drawRoundRect(boxRect, 28f, 28f, boxPaint)

        val boxBorder = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }
        canvas.drawRoundRect(boxRect, 28f, 28f, boxBorder)

        val bodyPaint = TextPaint().apply {
            color = textColor
            textSize = 40f
            isAntiAlias = true
            setShadowLayer(8f, 0f, 2f, AndroidColor.BLACK)
        }
        val bodyLayout = StaticLayout.Builder.obtain(slide.body, 0, slide.body.length, bodyPaint, contentWidth - 60)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat() + 30f, boxTop + 50f)
        bodyLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawFancyQuoteTemplate(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int) {
        drawBadge(canvas, "💡 INSPIRATION", marginX.toFloat(), topBound, accentColor)

        // Large Decorative Quote Marks
        val qPaint = Paint().apply {
            color = accentColor
            textSize = 120f
            alpha = 100
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("“", marginX.toFloat(), topBound + 220f, qPaint)

        val quotePaint = TextPaint().apply {
            color = textColor
            textSize = 56f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            setShadowLayer(12f, 0f, 4f, AndroidColor.BLACK)
        }
        val quoteLayout = StaticLayout.Builder.obtain(slide.headline, 0, slide.headline.length, quotePaint, contentWidth - 40)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate((marginX + 20).toFloat(), topBound + 220f)
        quoteLayout.draw(canvas)
        canvas.restore()

        // Author / Footer
        val authorPaint = Paint().apply {
            color = accentColor
            textSize = 38f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(8f, 0f, 2f, AndroidColor.BLACK)
        }
        canvas.drawText("— ${slide.body}", WIDTH / 2f, topBound + 650f, authorPaint)
    }

    private fun drawFancyListicleTemplate(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int, index: Int, total: Int) {
        drawBadge(canvas, "📌 POINT $index OF $total", marginX.toFloat(), topBound, accentColor)

        val titlePaint = TextPaint().apply {
            color = textColor
            textSize = 52f
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

        // Content Card
        val cardTop = topBound + 360f
        val cardPaint = Paint().apply {
            color = AndroidColor.parseColor("#25000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cardRect = RectF(marginX.toFloat(), cardTop, (WIDTH - marginX).toFloat(), cardTop + 420f)
        canvas.drawRoundRect(cardRect, 24f, 24f, cardPaint)

        val bodyPaint = TextPaint().apply {
            color = textColor
            textSize = 40f
            isAntiAlias = true
            setShadowLayer(8f, 0f, 2f, AndroidColor.BLACK)
        }
        val bodyLayout = StaticLayout.Builder.obtain(slide.body, 0, slide.body.length, bodyPaint, contentWidth - 40)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat() + 20f, cardTop + 50f)
        bodyLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawBadge(canvas: Canvas, text: String, left: Float, top: Float, accentColor: Int) {
        val badgePaint = Paint().apply {
            color = accentColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = AndroidColor.BLACK
            textSize = 28f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val textWidth = textPaint.measureText(text)
        val badgeRect = RectF(left, top, left + textWidth + 48f, top + 52f)
        canvas.drawRoundRect(badgeRect, 26f, 26f, badgePaint)

        canvas.drawText(text, left + (textWidth + 48f) / 2f, top + 36f, textPaint)
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
            var presentationTimeUs: Long = 0
            val frameIntervalUs = 1_000_000L / FPS
            val slideDurationMs = 3000L

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
                val slideBitmap = drawSlideBitmap(slide, theme, index, slides.size)
                val totalFrames = (slideDurationMs * FPS / 1000L).toInt()

                for (i in 0 until totalFrames) {
                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        canvas.drawBitmap(slideBitmap, 0f, 0f, null)
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    presentationTimeUs = i * frameIntervalUs + presentationTimeUs
                    drainEncoder(false)
                }
                slideBitmap.recycle()
            }

            drainEncoder(true)
            saveVideoToMediaStore(outputFile)

            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering fancy video", e)
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
            put(MediaStore.Video.Media.DISPLAY_NAME, "ReelCraft_Fancy_${System.currentTimeMillis()}.mp4")
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
