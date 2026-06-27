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
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0D0D1A.toInt() }
    private val boardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF080814.toInt() }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A6A5C.toInt(); strokeWidth = 3f }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00FF88.toInt(); style = Paint.Style.STROKE; strokeWidth = 6f }
    private val borderGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00FF88.toInt(); style = Paint.Style.STROKE; strokeWidth = 14f }
    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF00FF88.toInt(); style = Paint.Style.STROKE; strokeWidth = 2f }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val foodPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
        // 棋盘背景（更暗，与外部形成层次）
        canvas.drawRect(ox, oy, ox + size, oy + size, boardBgPaint)
        // 棋盘外发光（宽柔光层）
        borderGlowPaint.alpha = 40
        canvas.drawRect(ox - 4f, oy - 4f, ox + size + 4f, oy + size + 4f, borderGlowPaint)
        // 棋盘边框（霓虹绿粗线）
        borderPaint.alpha = 220
        canvas.drawRect(ox, oy, ox + size, oy + size, borderPaint)
        // 棋盘内边线（细亮线，增强边界感）
        innerShadowPaint.alpha = 100
        canvas.drawRect(ox + 3f, oy + 3f, ox + size - 3f, oy + size - 3f, innerShadowPaint)

        if (showGrid) {
            gridPaint.alpha = 180
            for (i in 0..eng.gridSize) {
                canvas.drawLine(ox + i * cs, oy, ox + i * cs, oy + size, gridPaint)
                canvas.drawLine(ox, oy + i * cs, ox + size, oy + i * cs, gridPaint)
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
                headPaint.color = skin.headColor
                canvas.drawRoundRect(rect, cs * 0.3f, cs * 0.3f, headPaint)
                glowPaint.color = skin.headColor and 0x44FFFFFF.toInt()
                canvas.drawRoundRect(RectF(rect.left - cs * 0.18f, rect.top - cs * 0.18f, rect.right + cs * 0.18f, rect.bottom + cs * 0.18f), cs * 0.4f, cs * 0.4f, glowPaint)
            } else {
                bodyPaint.color = if (skin == SnakeSkin.RAINBOW) {
                    Color.HSVToColor(floatArrayOf(((ratio * 360 + System.currentTimeMillis() * 0.1) % 360).toFloat(), 1f, 1f))
                } else lerpColor(skin.bodyStartColor, skin.bodyEndColor, ratio)
                val s = cs * 0.01f
                canvas.drawRoundRect(RectF(rect.left + s, rect.top + s, rect.right - s, rect.bottom - s), cs * 0.25f, cs * 0.25f, bodyPaint)
            }
        }
    }

    private fun drawFood(canvas: Canvas, pos: Point, cs: Float, ox: Float, oy: Float) {
        val rect = cellRect(pos, cs, ox, oy)
        val cx = rect.centerX(); val cy = rect.centerY(); val r = cs * 0.4f
        foodPaint.color = foodSkin.color
        when (foodSkin) {
            FoodSkin.APPLE -> {
                canvas.drawCircle(cx, cy, r, foodPaint)
                glowPaint.color = foodSkin.color and 0x33FFFFFF.toInt()
                canvas.drawCircle(cx, cy, r * 1.6f, glowPaint)
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
}
