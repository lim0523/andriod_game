package com.example.exam

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CameraOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var landmarks: List<GestureRecognizerHelper.NormalizedLandmark> = emptyList()

    private val connections = listOf(
        // 拇指
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
        // 食指
        Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
        // 中指
        Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
        // 无名指
        Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
        // 小指
        Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),
        // 掌根
        Pair(5, 9), Pair(9, 13), Pair(13, 17)
    )

    private val fingerTips = setOf(4, 8, 12, 16, 20)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6600FF88")
        strokeWidth = 3f
        strokeCap = Paint.Cap.ROUND
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF00FF88")
        style = Paint.Style.FILL
    }

    private val tipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF00D4FF")
        style = Paint.Style.FILL
    }

    private val wristPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFFD700")
        style = Paint.Style.FILL
    }

    fun updateLandmarks(lm: List<GestureRecognizerHelper.NormalizedLandmark>) {
        landmarks = lm
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (landmarks.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        // 绘制连线
        for ((a, b) in connections) {
            if (a < landmarks.size && b < landmarks.size) {
                val pa = landmarks[a]
                val pb = landmarks[b]
                canvas.drawLine(pa.x * w, pa.y * h, pb.x * w, pb.y * h, linePaint)
            }
        }

        // 绘制关键点
        for ((i, lm) in landmarks.withIndex()) {
            val x = lm.x * w
            val y = lm.y * h
            val radius = when {
                i == 0 -> 7f  // 手腕
                i in fingerTips -> 5f  // 指尖
                else -> 3f  // 普通
            }
            val paint = when {
                i == 0 -> wristPaint
                i in fingerTips -> tipPaint
                else -> pointPaint
            }
            canvas.drawCircle(x, y, radius, paint)
        }
    }
}
