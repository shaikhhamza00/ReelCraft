package com.hamza.reelcraft.render

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
        private const val BIT_RATE = 2_000_000 // 2 Mbps
    }

    /**
     * Stage 2: Draw a template-based slide onto a 720x1280 Bitmap with Theme and Safe Zones
     */
    fun drawSlideBitmap(slide: SlideData, theme: Theme, slideIndex: Int, totalSlides: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background color from theme
        val bgColor = try {
            AndroidColor.parseColor(theme.backgroundColorHex)
        } catch (e: Exception) {
            AndroidColor.parseColor("#1A1A2E")
        }
        canvas.drawColor(bgColor)

        val textColor = try {
            AndroidColor.parseColor(theme.textColorHex)
        } catch (e: Exception) {
            AndroidColor.WHITE
        }

        val accentColor = try {
            AndroidColor.parseColor(theme.accentColorHex)
        } catch (e: Exception) {
            AndroidColor.YELLOW
        }

        // Safe zone margins (15% top/bottom, 10% left/right)
        val marginX = (WIDTH * 0.1f).toInt()
        val contentWidth = WIDTH - (2 * marginX)
        val topBound = HEIGHT * 0.15f
        val bottomBound = HEIGHT * 0.85f

        // Draw top progress bar for video retention
        val progressPaint = Paint().apply {
            color = accentColor
            strokeWidth = 6f
            style = Paint.Style.FILL
        }
        val progressWidth = (WIDTH.toFloat() / totalSlides) * (slideIndex + 1)
        canvas.drawRect(0f, 0f, progressWidth, 12f, progressPaint)

        // Draw Template specific UI
        when (slide.templateType.lowercase()) {
            "riddle" -> drawRiddleTemplate(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor)
            "quote" -> drawQuoteTemplate(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor)
            "listicle" -> drawListicleTemplate(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor, slideIndex + 1, totalSlides)
            else -> drawStoryTemplate(canvas, slide, contentWidth, marginX, topBound, textColor, accentColor)
        }

        // Draw Footer Watermark / Branding
        val footerPaint = Paint().apply {
            color = textColor
            alpha = 150
            textSize = 24f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Made with ReelCraft", WIDTH / 2f, bottomBound + 40f, footerPaint)

        return bitmap
    }

    private fun drawStoryTemplate(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int) {
        // Accent badge
        val badgePaint = Paint().apply {
            color = accentColor
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("FEATURED STORY", WIDTH / 2f, topBound + 40f, badgePaint)

        // Headline
        val titlePaint = TextPaint().apply {
            color = textColor
            textSize = 52f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val titleLayout = StaticLayout.Builder.obtain(slide.headline, 0, slide.headline.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat(), topBound + 120f)
        titleLayout.draw(canvas)
        canvas.restore()

        // Body
        val bodyPaint = TextPaint().apply {
            color = textColor
            textSize = 38f
            isAntiAlias = true
        }
        val bodyLayout = StaticLayout.Builder.obtain(slide.body, 0, slide.body.length, bodyPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat(), topBound + 350f)
        bodyLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawRiddleTemplate(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int) {
        val badgePaint = Paint().apply {
            color = accentColor
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("🧠 RIDDLE CHALLENGE", WIDTH / 2f, topBound + 40f, badgePaint)

        val titlePaint = TextPaint().apply {
            color = textColor
            textSize = 48f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val titleLayout = StaticLayout.Builder.obtain(slide.headline, 0, slide.headline.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat(), topBound + 150f)
        titleLayout.draw(canvas)
        canvas.restore()

        // Countdown timer box
        val boxPaint = Paint().apply {
            color = accentColor
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val rect = RectF(marginX.toFloat(), topBound + 480f, (WIDTH - marginX).toFloat(), topBound + 600f)
        canvas.drawRoundRect(rect, 16f, 16f, boxPaint)

        val bodyPaint = Paint().apply {
            color = textColor
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(slide.body, WIDTH / 2f, topBound + 550f, bodyPaint)
    }

    private fun drawQuoteTemplate(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int) {
        val quotePaint = TextPaint().apply {
            color = textColor
            textSize = 56f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }
        val quoteLayout = StaticLayout.Builder.obtain("“${slide.headline}”", 0, slide.headline.length + 2, quotePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat(), topBound + 200f)
        quoteLayout.draw(canvas)
        canvas.restore()

        val authorPaint = Paint().apply {
            color = accentColor
            textSize = 36f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("- ${slide.body}", WIDTH / 2f, topBound + 600f, authorPaint)
    }

    private fun drawListicleTemplate(canvas: Canvas, slide: SlideData, contentWidth: Int, marginX: Int, topBound: Float, textColor: Int, accentColor: Int, index: Int, total: Int) {
        val badgePaint = Paint().apply {
            color = accentColor
            textSize = 36f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText("POINT $index OF $total", WIDTH / 2f, topBound + 40f, badgePaint)

        val titlePaint = TextPaint().apply {
            color = textColor
            textSize = 52f
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
        val titleLayout = StaticLayout.Builder.obtain(slide.headline, 0, slide.headline.length, titlePaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat(), topBound + 150f)
        titleLayout.draw(canvas)
        canvas.restore()

        val bodyPaint = TextPaint().apply {
            color = textColor
            textSize = 38f
            isAntiAlias = true
        }
        val bodyLayout = StaticLayout.Builder.obtain(slide.body, 0, slide.body.length, bodyPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat(), topBound + 350f)
        bodyLayout.draw(canvas)
        canvas.restore()
    }

    /**
     * Encode list of SlideData with Theme into an MP4 file using MediaCodec + Surface + MediaMuxer
     */
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
            val slideDurationMs = 3000L // 3 seconds per slide

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

            // Draw and feed frames for each slide
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

            // Save to MediaStore (Movies)
            saveVideoToMediaStore(outputFile)

            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering video with templates", e)
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
            put(MediaStore.Video.Media.DISPLAY_NAME, "ReelCraft_${System.currentTimeMillis()}.mp4")
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
