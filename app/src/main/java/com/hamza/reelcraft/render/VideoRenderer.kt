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
import com.hamza.reelcraft.model.SceneGenerator
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import kotlin.random.Random

class VideoRenderer(private val context: Context) {

    companion object {
        private const val TAG = "VideoRenderer"
        private const val WIDTH = 720
        private const val HEIGHT = 1280
        private const val FPS = 30
        private const val BIT_RATE = 3_000_000
    }

    private val cardPaint = Paint().apply { color = AndroidColor.parseColor("#33000000"); style = Paint.Style.FILL; isAntiAlias = true }
    private val borderPaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 3f; isAntiAlias = true }

    /**
     * Draw a context-aware animated background and Kinetic Typography animated text onto a 720x1280 Bitmap.
     */
    fun drawSlideBitmap(slide: SlideData, theme: Theme, slideIndex: Int, totalSlides: Int, animProgress: Float = 0.0f): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Ken Burns Slow Zoom (1.0 to 1.08)
        val currentScale = 1.0f + (0.08f * animProgress)
        canvas.save()

        // Hook slide (first slide) subtle shake/glitch effect in first 0.3s (animProgress < 0.1)
        if (slideIndex == 0 && animProgress < 0.1f) {
            val shakeX = (Random.nextFloat() - 0.5f) * 8f
            val shakeY = (Random.nextFloat() - 0.5f) * 8f
            canvas.translate(shakeX, shakeY)
        }

        canvas.scale(currentScale, currentScale, WIDTH / 2f, HEIGHT / 2f)

        // 1. Draw Context-Aware Background Scene
        drawBackgroundScene(canvas, slide.bgScene, animProgress, slide.seed)

        // Colors
        val textColor = try { AndroidColor.parseColor(theme.textColorHex) } catch (e: Exception) { AndroidColor.WHITE }
        val accentColor = try { AndroidColor.parseColor(slide.accentColor) } catch (e: Exception) { 
            try { AndroidColor.parseColor(theme.accentColorHex) } catch (ex: Exception) { AndroidColor.parseColor("#22D3EE") }
        }

        val marginX = 54
        val contentWidth = WIDTH - (2 * marginX)
        val topBound = HEIGHT * 0.15f // Safe zone: top 15% empty (Rule 6 / Step 3)
        val bottomBound = HEIGHT * 0.85f // Safe zone: bottom 15% empty

        // 3. Top Retention Progress Bar
        val progressPaint = Paint().apply {
            color = accentColor
            strokeWidth = 8f
            style = Paint.Style.FILL
            setShadowLayer(10f, 0f, 0f, accentColor)
        }
        val progressWidth = (WIDTH.toFloat() / totalSlides) * (slideIndex + animProgress)
        canvas.drawRoundRect(40f, 40f, progressWidth.coerceAtLeast(40f), 52f, 6f, 6f, progressPaint)

        // 4. Kinetic Typography Word-by-Word Animation (Step 3)
        val animAlpha = (animProgress * 3.0f).coerceIn(0.2f, 1.0f)
        drawKineticText(canvas, slide, contentWidth, marginX, topBound, bottomBound, textColor, accentColor, animProgress, animAlpha)

        // 5. Footer Watermark
        val footerPaint = Paint().apply {
            color = textColor
            alpha = (180 * animAlpha).toInt()
            textSize = 26f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            setShadowLayer(6f, 0f, 2f, AndroidColor.BLACK)
        }
        canvas.drawText("✨ ReelCraft • [${theme.name}]", WIDTH / 2f, bottomBound + 45f, footerPaint)

        canvas.restore()
        return bitmap
    }

    private fun drawBackgroundScene(canvas: Canvas, scene: String, progress: Float, seed: Long) {
        val spec = SceneGenerator.getSpec(scene, seed)
        SceneRenderer.render(canvas, spec, progress * 3.0f)
    }

    private fun drawKineticText(
        canvas: Canvas,
        slide: SlideData,
        contentWidth: Int,
        marginX: Int,
        topBound: Float,
        bottomBound: Float,
        textColor: Int,
        accentColor: Int,
        progress: Float,
        animAlpha: Float
    ) {
        // Dark overlay behind text for guaranteed contrast on any background
        val overlayPaint = Paint().apply {
            color = AndroidColor.parseColor("#44000000")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(marginX.toFloat() - 20f, topBound + 20f, (WIDTH - marginX + 20).toFloat(), bottomBound - 20f), 28f, 28f, overlayPaint)

        // Word-by-word kinetic typography timing calculation (Step 3)
        val headlineWords = slide.headline.split(" ")
        val visibleWordCount = ((progress * headlineWords.size * 1.5f).toInt()).coerceIn(1, headlineWords.size)
        val partialHeadline = headlineWords.take(visibleWordCount).joinToString(" ")

        // Headline Paint with soft glow/shadow
        val titlePaint = TextPaint().apply {
            color = textColor
            alpha = (255 * animAlpha).toInt()
            textSize = 54f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(16f, 0f, 4f, AndroidColor.BLACK)
        }

        val titleLayout = StaticLayout.Builder.obtain(partialHeadline, 0, partialHeadline.length, titlePaint, contentWidth - 40)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate((marginX + 20).toFloat(), topBound + 80f)
        titleLayout.draw(canvas)
        canvas.restore()

        // Body Text
        val bodyPaint = TextPaint().apply {
            color = textColor
            alpha = (255 * animAlpha).toInt()
            textSize = 38f
            isAntiAlias = true
            setShadowLayer(10f, 0f, 2f, AndroidColor.BLACK)
        }

        val bodyLayout = StaticLayout.Builder.obtain(slide.body, 0, slide.body.length, bodyPaint, contentWidth - 40)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate((marginX + 20).toFloat(), topBound + 350f)
        bodyLayout.draw(canvas)
        canvas.restore()
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

            var totalFrameTimeMs = 0L
            var frameCount = 0

            for ((index, slide) in slides.withIndex()) {
                for (i in 0 until totalFramesPerSlide) {
                    val startTime = System.currentTimeMillis()
                    val animProgress = i.toFloat() / totalFramesPerSlide.toFloat()
                    val slideBitmap = drawSlideBitmap(slide, theme, index, slides.size, animProgress)

                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        canvas.drawBitmap(slideBitmap, 0f, 0f, null)
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }
                    slideBitmap.recycle()

                    val frameTime = System.currentTimeMillis() - startTime
                    totalFrameTimeMs += frameTime
                    frameCount++

                    drainEncoder(false)
                }
            }

            val avgFrameTimeMs = if (frameCount > 0) totalFrameTimeMs.toFloat() / frameCount else 0f
            Log.i(TAG, "PERFORMANCE: Average frame render time = $avgFrameTimeMs ms per frame ($frameCount frames total)")

            drainEncoder(true)
            saveVideoToMediaStore(outputFile)

            return outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering kinetic typography video", e)
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
            put(MediaStore.Video.Media.DISPLAY_NAME, "ReelCraft_Kinetic_${System.currentTimeMillis()}.mp4")
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
