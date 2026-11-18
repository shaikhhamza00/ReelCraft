package com.hamza.reelcraft.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.Color as AndroidColor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.cos
import kotlin.math.sin

object ExplainerRenderer {
    private const val WIDTH = 720
    private const val HEIGHT = 1280

    private val bgPaint = Paint().apply { isAntiAlias = true }
    private val propPaint = Paint().apply { isAntiAlias = true }
    private val textPaint = TextPaint().apply { isAntiAlias = true }
    private val shadowPaint = Paint().apply { isAntiAlias = true }

    fun renderExplainerScene(
        canvas: Canvas,
        captionText: String,
        highlightWords: List<String>,
        propType: String,
        propParamValue: Float,
        progress: Float
    ) {
        // 1. Soft stylized background
        bgPaint.color = AndroidColor.parseColor("#1E1B4B")
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bgPaint)

        // 2. Render Animated Prop
        canvas.save()
        canvas.translate(WIDTH / 2f, HEIGHT * 0.4f)
        when (propType.uppercase()) {
            "CLOCK" -> drawClock(canvas, propParamValue)
            "CALENDAR" -> drawCalendar(canvas, propParamValue)
            "DIGITAL_PHONE" -> drawDigitalPhone(canvas, propParamValue)
            "MAP_SLICES" -> drawMapSlices(canvas, progress)
            "COUNTER_NUMBER" -> drawCounter(canvas, propParamValue)
            "BAR_COMPARISON" -> drawBarComparison(canvas, progress)
            else -> drawClock(canvas, propParamValue)
        }
        canvas.restore()

        // 3. Render Caption System
        drawCaptions(canvas, captionText, highlightWords, progress)
    }

    private fun drawClock(canvas: Canvas, progress: Float) {
        val radius = 180f
        propPaint.color = AndroidColor.parseColor("#334155")
        canvas.drawCircle(0f, 0f, radius + 15f, propPaint)
        propPaint.color = AndroidColor.parseColor("#F8FAFC")
        canvas.drawCircle(0f, 0f, radius, propPaint)

        propPaint.color = AndroidColor.parseColor("#64748B")
        propPaint.strokeWidth = 6f
        for (i in 0 until 12) {
            val angle = i * 30f * (Math.PI / 180f).toFloat()
            val startX = sin(angle) * (radius - 25f)
            val startY = -cos(angle) * (radius - 25f)
            val endX = sin(angle) * (radius - 10f)
            val endY = -cos(angle) * (radius - 10f)
            canvas.drawLine(startX, startY, endX, endY, propPaint)
        }

        val hourAngle = (progress * 360f) * (Math.PI / 180f).toFloat()
        val minuteAngle = (progress * 12f * 360f) * (Math.PI / 180f).toFloat()

        propPaint.color = AndroidColor.parseColor("#0F172A")
        propPaint.strokeWidth = 12f
        canvas.drawLine(0f, 0f, sin(hourAngle) * (radius * 0.5f), -cos(hourAngle) * (radius * 0.5f), propPaint)

        propPaint.color = AndroidColor.parseColor("#3B82F6")
        propPaint.strokeWidth = 8f
        canvas.drawLine(0f, 0f, sin(minuteAngle) * (radius * 0.75f), -cos(minuteAngle) * (radius * 0.75f), propPaint)

        propPaint.color = AndroidColor.parseColor("#EF4444")
        canvas.drawCircle(0f, 0f, 12f, propPaint)
    }

    private fun drawCalendar(canvas: Canvas, progress: Float) {
        val w = 320f
        val h = 380f
        val rect = RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)

        propPaint.color = AndroidColor.parseColor("#FFFFFF")
        canvas.drawRoundRect(rect, 24f, 24f, propPaint)

        propPaint.color = AndroidColor.parseColor("#EF4444")
        val headerRect = RectF(rect.left, rect.top, rect.right, rect.top + 90f)
        canvas.drawRoundRect(headerRect, 24f, 24f, propPaint)

        textPaint.color = AndroidColor.WHITE
        textPaint.textSize = 36f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("OCTOBER 2024", 0f, rect.top + 58f, textPaint)

        val activeDate = (progress * 30f).toInt().coerceIn(1, 31)
        val gridStartX = rect.left + 35f
        val gridStartY = rect.top + 130f

        for (row in 0 until 5) {
            for (col in 0 until 7) {
                val dateNum = row * 7 + col + 1
                if (dateNum > 31) break
                val cx = gridStartX + col * 38f + 15f
                val cy = gridStartY + row * 45f + 15f

                if (dateNum == activeDate) {
                    propPaint.color = AndroidColor.parseColor("#FFD21F")
                    canvas.drawCircle(cx, cy - 10f, 20f, propPaint)
                    textPaint.color = AndroidColor.parseColor("#0F172A")
                } else {
                    textPaint.color = AndroidColor.parseColor("#334155")
                }
                textPaint.textSize = 24f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(dateNum.toString(), cx, cy, textPaint)
            }
        }
    }

    private fun drawDigitalPhone(canvas: Canvas, progress: Float) {
        val w = 260f
        val h = 480f
        val rect = RectF(-w / 2f, -h / 2f, w / 2f, h / 2f)

        propPaint.color = AndroidColor.parseColor("#0F172A")
        canvas.drawRoundRect(rect, 36f, 36f, propPaint)

        val screenRect = RectF(rect.left + 12f, rect.top + 12f, rect.right - 12f, rect.bottom - 12f)
        propPaint.color = AndroidColor.parseColor("#38BDF8")
        canvas.drawRoundRect(screenRect, 28f, 28f, propPaint)

        textPaint.color = AndroidColor.WHITE
        textPaint.textSize = 48f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("12:45", 0f, screenRect.top + 120f, textPaint)

        textPaint.textSize = 22f
        textPaint.color = AndroidColor.parseColor("#E0F2FE")
        canvas.drawText("TimeZone Sync Active", 0f, screenRect.top + 170f, textPaint)
    }

    private fun drawMapSlices(canvas: Canvas, progress: Float) {
        val colors = listOf("#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6")
        val count = 5

        for (i in 0 until count) {
            val angle = i * (360f / count) * (Math.PI / 180f).toFloat()
            val offset = sin(progress * 6f + i) * 20f
            val cx = sin(angle) * (80f + offset)
            val cy = -cos(angle) * (80f + offset)

            propPaint.color = AndroidColor.parseColor(colors[i % colors.size])
            canvas.drawCircle(cx, cy, 55f, propPaint)

            textPaint.color = AndroidColor.WHITE
            textPaint.textSize = 32f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("#${i + 1}", cx, cy + 11f, textPaint)
        }
    }

    private fun drawCounter(canvas: Canvas, progress: Float) {
        val currentVal = (progress * 1984f).toInt()
        textPaint.color = AndroidColor.parseColor("#FFD21F")
        textPaint.textSize = 96f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(currentVal.toString(), 0f, 0f, textPaint)

        textPaint.textSize = 28f
        textPaint.color = AndroidColor.parseColor("#94A3B8")
        canvas.drawText("GLOBAL TIME ZONES", 0f, 60f, textPaint)
    }

    private fun drawBarComparison(canvas: Canvas, progress: Float) {
        val barWidth = 70f
        val maxH = 300f
        val h1 = maxH * (0.4f + progress * 0.3f)
        val h2 = maxH * (0.8f - progress * 0.2f)

        propPaint.color = AndroidColor.parseColor("#3B82F6")
        canvas.drawRect(-90f, maxH - h1, -90f + barWidth, maxH, propPaint)

        propPaint.color = AndroidColor.parseColor("#10B981")
        canvas.drawRect(20f, maxH - h2, 20f + barWidth, maxH, propPaint)

        textPaint.color = AndroidColor.WHITE
        textPaint.textSize = 24f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("GMT+0", -55f, maxH + 40f, textPaint)
        canvas.drawText("GMT+5", 55f, maxH + 40f, textPaint)
    }

    private fun drawCaptions(canvas: Canvas, captionText: String, highlightWords: List<String>, progress: Float) {
        val marginX = 54
        val contentWidth = WIDTH - (2 * marginX)
        val bottomY = HEIGHT - 220f

        shadowPaint.color = AndroidColor.parseColor("#DD0F172A")
        canvas.drawRoundRect(RectF(marginX.toFloat() - 20f, bottomY - 30f, (WIDTH - marginX + 20).toFloat(), bottomY + 140f), 32f, 32f, shadowPaint)

        val words = captionText.split(" ")
        val visibleCount = ((progress * words.size * 1.5f).toInt()).coerceIn(1, words.size)
        val visibleText = words.take(visibleCount).joinToString(" ")

        val captionPaint = TextPaint().apply {
            color = AndroidColor.WHITE
            textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            setShadowLayer(10f, 0f, 2f, AndroidColor.BLACK)
        }

        val layout = StaticLayout.Builder.obtain(visibleText, 0, visibleText.length, captionPaint, contentWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .build()

        canvas.save()
        canvas.translate(marginX.toFloat(), bottomY)
        layout.draw(canvas)
        canvas.restore()
    }
}
