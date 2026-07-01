package com.example.exam

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SnakeGameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var engine: GameEngine? = null
    private var skin: SnakeSkin = SnakeSkin.CLASSIC_GREEN
    private var foodSkin: FoodSkin = FoodSkin.APPLE
    private var showGrid = true
    // 手势模式：手部映射的目标位置（null = 不显示）
    private var targetCol: Int = -1
    private var targetRow: Int = -1
    private var hasTarget: Boolean = false
    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD700.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val targetGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFD700.toInt()
        style = Paint.Style.FILL
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF080812.toInt() }
    private val boardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val boardVignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF16352F.toInt(); strokeWidth = 1.25f }
    private val majorGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1F5A4C.toInt(); strokeWidth = 2f }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00FF88.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f }
    private val borderGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00FF88.toInt(); style = Paint.Style.STROKE; strokeWidth = 18f }
    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00D4FF.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.5f }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val segmentGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val segmentStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val segmentHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val eyePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF07120D.toInt(); style = Paint.Style.FILL }
    private val foodPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foodStemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF7BFF6A.toInt(); style = Paint.Style.FILL }
    private val foodHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF9AB2.toInt(); style = Paint.Style.FILL }
    private val foodGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val obstaclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF4A5568.toInt() }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun setup(e: GameEngine, s: SnakeSkin, f: FoodSkin, grid: Boolean) {
        engine = e; skin = s; foodSkin = f; showGrid = grid
    }

    fun setTarget(col: Int, row: Int) {
        targetCol = col; targetRow = row; hasTarget = true; invalidate()
    }

    fun clearTarget() {
        hasTarget = false; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val eng = engine ?: return
        // 棋盘尺寸：取宽高较小者，保证正方形
        val size = minOf(width, height).toFloat()
        val ox = (width - size) / 2f
        // 棋盘居中显示（顶部信息栏和底部方向键都是悬浮叠加，不占布局空间）
        val oy = (height - size) / 2f
        val cs = size / eng.gridSize

        // 外部背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        boardBgPaint.shader = LinearGradient(
            ox, oy, ox + size, oy + size,
            0xFF071018.toInt(),
            0xFF10081E.toInt(),
            Shader.TileMode.CLAMP
        )
        boardVignettePaint.shader = RadialGradient(
            ox + size * 0.5f,
            oy + size * 0.45f,
            size * 0.72f,
            intArrayOf(0x2217FFE2, 0x00000000, 0xAA03030A.toInt()),
            floatArrayOf(0f, 0.58f, 1f),
            Shader.TileMode.CLAMP
        )

        // 棋盘背景（暗色渐变 + 边缘压暗，与霓虹元素拉开层级）
        canvas.drawRect(ox, oy, ox + size, oy + size, boardBgPaint)
        canvas.drawRect(ox, oy, ox + size, oy + size, boardVignettePaint)
        // 棋盘外发光（宽柔光层）
        borderGlowPaint.alpha = 32
        canvas.drawRect(ox - 4f, oy - 4f, ox + size + 4f, oy + size + 4f, borderGlowPaint)
        // 棋盘边框（霓虹绿粗线）
        borderPaint.alpha = 220
        canvas.drawRect(ox, oy, ox + size, oy + size, borderPaint)
        // 棋盘内边线（细亮线，增强边界感）
        innerShadowPaint.alpha = 100
        canvas.drawRect(ox + 3f, oy + 3f, ox + size - 3f, oy + size - 3f, innerShadowPaint)

        if (showGrid) {
            gridPaint.alpha = 120
            majorGridPaint.alpha = 95
            for (i in 0..eng.gridSize) {
                val paint = if (i % 5 == 0) majorGridPaint else gridPaint
                canvas.drawLine(ox + i * cs, oy, ox + i * cs, oy + size, paint)
                canvas.drawLine(ox, oy + i * cs, ox + size, oy + i * cs, paint)
            }
        }

        for (obs in eng.getObstacles()) {
            canvas.drawRect(cellRect(obs, cs, ox, oy), obstaclePaint)
        }

        drawFood(canvas, eng.food, cs, ox, oy)
        if (eng.bonusFood != null) drawFood(canvas, eng.bonusFood!!, cs, ox, oy)

        // 绘制手势目标位置标记（金色光圈）
        if (hasTarget && targetCol >= 0 && targetRow >= 0) {
            val tcx = ox + (targetCol + 0.5f) * cs
            val tcy = oy + (targetRow + 0.5f) * cs
            targetGlowPaint.alpha = 40
            canvas.drawCircle(tcx, tcy, cs * 0.6f, targetGlowPaint)
            targetPaint.alpha = 200
            canvas.drawCircle(tcx, tcy, cs * 0.38f, targetPaint)
            // 十字准星
            targetPaint.alpha = 150
            canvas.drawLine(tcx - cs * 0.5f, tcy, tcx - cs * 0.25f, tcy, targetPaint)
            canvas.drawLine(tcx + cs * 0.25f, tcy, tcx + cs * 0.5f, tcy, targetPaint)
            canvas.drawLine(tcx, tcy - cs * 0.5f, tcx, tcy - cs * 0.25f, targetPaint)
            canvas.drawLine(tcx, tcy + cs * 0.25f, tcx, tcy + cs * 0.5f, targetPaint)
        }

        val snake = eng.snake
        for ((i, p) in snake.withIndex()) {
            val rect = cellRect(p, cs, ox, oy)
            val ratio = if (snake.size > 1) i.toFloat() / (snake.size - 1) else 0f
            if (i == snake.lastIndex) {
                drawSnakeSegment(canvas, rect, cs, skin.headColor, true)
            } else {
                val bodyColor = if (skin == SnakeSkin.RAINBOW) {
                    Color.HSVToColor(floatArrayOf(((ratio * 360 + System.currentTimeMillis() * 0.1) % 360).toFloat(), 1f, 1f))
                } else lerpColor(skin.bodyStartColor, skin.bodyEndColor, ratio)
                drawSnakeSegment(canvas, rect, cs, bodyColor, false)
            }
        }
    }

    private fun drawSnakeSegment(canvas: Canvas, rect: RectF, cs: Float, color: Int, isHead: Boolean) {
        val gap = cs * if (isHead) 0.08f else 0.12f
        val segment = RectF(rect.left + gap, rect.top + gap, rect.right - gap, rect.bottom - gap)
        val radius = cs * 0.16f

        segmentGlowPaint.color = withAlpha(color, if (isHead) 110 else 80)
        val glow = RectF(segment.left - cs * 0.16f, segment.top - cs * 0.16f, segment.right + cs * 0.16f, segment.bottom + cs * 0.16f)
        canvas.drawRoundRect(glow, cs * 0.22f, cs * 0.22f, segmentGlowPaint)

        bodyPaint.color = color
        canvas.drawRoundRect(segment, radius, radius, bodyPaint)

        segmentStrokePaint.color = withAlpha(0xFFFFFFFF.toInt(), if (isHead) 130 else 80)
        segmentStrokePaint.strokeWidth = maxOf(1f, cs * 0.06f)
        canvas.drawRoundRect(segment, radius, radius, segmentStrokePaint)

        segmentHighlightPaint.color = withAlpha(0xFFFFFFFF.toInt(), if (isHead) 95 else 55)
        val highlight = RectF(
            segment.left + cs * 0.14f,
            segment.top + cs * 0.12f,
            segment.right - cs * 0.18f,
            segment.top + cs * 0.24f
        )
        canvas.drawRoundRect(highlight, cs * 0.06f, cs * 0.06f, segmentHighlightPaint)

        if (isHead) drawSnakeEyes(canvas, segment, cs)
    }

    private fun drawSnakeEyes(canvas: Canvas, segment: RectF, cs: Float) {
        val eyeR = cs * 0.055f
        val y = segment.top + segment.height() * 0.36f
        canvas.drawCircle(segment.left + segment.width() * 0.34f, y, eyeR, eyePaint)
        canvas.drawCircle(segment.left + segment.width() * 0.66f, y, eyeR, eyePaint)
    }

    private fun drawFood(canvas: Canvas, pos: Point, cs: Float, ox: Float, oy: Float) {
        val rect = cellRect(pos, cs, ox, oy)
        val cx = rect.centerX(); val cy = rect.centerY(); val r = cs * 0.4f
        foodPaint.color = foodSkin.color
        when (foodSkin) {
            FoodSkin.APPLE -> {
                drawPixelApple(canvas, cx, cy, cs)
            }
            FoodSkin.STAR -> {
                val path = Path()
                for (i in 0 until 10) {
                    val angle = Math.toRadians((i * 36.0) - 90)
                    val radius = if (i % 2 == 0) r else r * 0.4f
                    val x = cx + (radius * Math.cos(angle)).toFloat()
                    val y = cy + (radius * Math.sin(angle)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close(); canvas.drawPath(path, foodPaint)
                glowPaint.color = foodSkin.color and 0x33FFFFFF.toInt()
                canvas.drawCircle(cx, cy, r * 1.4f, glowPaint)
            }
            FoodSkin.DIAMOND -> {
                canvas.save(); canvas.rotate(45f, cx, cy)
                canvas.drawRect(cx - r * 0.7f, cy - r * 0.7f, cx + r * 0.7f, cy + r * 0.7f, foodPaint)
                canvas.restore()
                glowPaint.color = foodSkin.color and 0x33FFFFFF.toInt()
                canvas.drawCircle(cx, cy, r * 1.3f, glowPaint)
            }
        }
    }

    private fun drawPixelApple(canvas: Canvas, cx: Float, cy: Float, cs: Float) {
        val unit = cs * 0.11f
        foodGlowPaint.color = withAlpha(foodSkin.color, 95)
        canvas.drawCircle(cx, cy + unit * 0.35f, cs * 0.62f, foodGlowPaint)

        foodPaint.color = foodSkin.color
        val apple = Path().apply {
            moveTo(cx - unit * 2f, cy - unit * 1.1f)
            lineTo(cx - unit * 0.75f, cy - unit * 2.2f)
            lineTo(cx + unit * 0.75f, cy - unit * 2.2f)
            lineTo(cx + unit * 2f, cy - unit * 1.1f)
            lineTo(cx + unit * 2.25f, cy + unit * 1.2f)
            lineTo(cx + unit * 1.25f, cy + unit * 2.45f)
            lineTo(cx - unit * 1.25f, cy + unit * 2.45f)
            lineTo(cx - unit * 2.25f, cy + unit * 1.2f)
            close()
        }
        canvas.drawPath(apple, foodPaint)

        foodHighlightPaint.alpha = 210
        canvas.drawRect(cx - unit * 1.15f, cy - unit * 0.85f, cx - unit * 0.25f, cy + unit * 0.05f, foodHighlightPaint)

        foodStemPaint.color = 0xFF8B5A2B.toInt()
        canvas.drawRect(cx - unit * 0.2f, cy - unit * 2.95f, cx + unit * 0.25f, cy - unit * 1.95f, foodStemPaint)
        foodStemPaint.color = 0xFF7BFF6A.toInt()
        val leaf = Path().apply {
            moveTo(cx + unit * 0.2f, cy - unit * 2.55f)
            lineTo(cx + unit * 1.45f, cy - unit * 2.8f)
            lineTo(cx + unit * 0.85f, cy - unit * 1.85f)
            close()
        }
        canvas.drawPath(leaf, foodStemPaint)
    }

    private fun cellRect(pos: Point, cs: Float, ox: Float, oy: Float): RectF {
        val p = cs * 0.01f
        return RectF(ox + pos.x * cs + p, oy + pos.y * cs + p, ox + (pos.x + 1) * cs - p, oy + (pos.y + 1) * cs - p)
    }

    private fun lerpColor(start: Int, end: Int, ratio: Float): Int {
        return Color.rgb(
            (Color.red(start) + (Color.red(end) - Color.red(start)) * ratio).toInt(),
            (Color.green(start) + (Color.green(end) - Color.green(start)) * ratio).toInt(),
            (Color.blue(start) + (Color.blue(end) - Color.blue(start)) * ratio).toInt()
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
