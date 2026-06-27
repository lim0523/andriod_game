package com.example.exam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.exam.databinding.ActivityGameBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

class GameActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGameBinding
    private lateinit var engine: GameEngine
    private lateinit var settings: GameSettings
    private var gameMode = GameMode.CLASSIC
    private val handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var elapsedTime = 0L
    private lateinit var gestureDetector: GestureDetector

    // 手势识别相关
    private var gestureHelper: GestureRecognizerHelper? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraStarted = false
    // 手势摇杆模式：当前方向（null = 停止）
    private var currentGestureDirection: Direction? = null
    // 手部位置平滑缓冲：取最近 5 帧平均，减少抖动导致的目标跳变
    private val handBuffer = mutableListOf<Pair<Float, Float>>()
    private val HAND_BUFFER_SIZE = 5

    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "需要摄像头权限才能使用手势操控", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = GameSettings(this)
        gameMode = GameMode.valueOf(intent.getStringExtra("mode") ?: "CLASSIC")
        engine = GameEngine(GameConfig(mode = gameMode, difficulty = settings.difficulty))
        binding.gameView.setup(engine, settings.snakeSkin, settings.foodSkin, settings.showGrid)
        setupControls()
        setupButtons()
        setupDpad()

        if (gameMode == GameMode.GESTURE) {
            binding.cameraContainer.visibility = View.VISIBLE
            binding.directionIndicator.visibility = View.VISIBLE
            setupGestureHelper()
            checkCameraPermission()
        }

        startCountdown()
    }

    // ============ 手势识别 ============

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupGestureHelper() {
        gestureHelper = GestureRecognizerHelper(this) { result ->
            runOnUiThread { handleGestureResult(result) }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, gestureHelper!!.analyzer) }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageAnalysis)
                cameraStarted = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "摄像头启动失败: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleGestureResult(result: GestureRecognizerHelper.Result) {
        // 更新骨架
        binding.cameraOverlay.updateLandmarks(result.landmarks)

        // 游戏未运行或蛇未初始化时，不处理手势（防止崩溃）
        if (!result.isTracking || engine.gameState != GameState.RUNNING || engine.snake.isEmpty()) {
            currentGestureDirection = null
            if (engine.snake.isEmpty()) binding.gameView.clearTarget()
            binding.tvGestureStatus.text = if (!result.isTracking) "等待手势..." else "准备中..."
            binding.tvGestureStatus.setTextColor(0xFF4A5568.toInt())
            resetArrows()
            return
        }

        // === 位置映射核心逻辑 ===
        // 平滑处理：取最近 5 帧的平均位置，减少手部抖动导致的目标跳变
        handBuffer.add(Pair(result.handX, result.handY))
        if (handBuffer.size > HAND_BUFFER_SIZE) handBuffer.removeAt(0)
        val smoothX = handBuffer.map { it.first }.average().toFloat()
        val smoothY = handBuffer.map { it.second }.average().toFloat()

        // 手在摄像头画面中的绝对位置 → 映射到棋盘坐标
        // 留 15% 边距让用户不需手伸到画面极边缘就能触达棋盘边缘
        val margin = 0.15f
        val mappedX = ((smoothX - margin) / (1f - 2f * margin)).coerceIn(0f, 1f)
        val mappedY = ((smoothY - margin) / (1f - 2f * margin)).coerceIn(0f, 1f)
        val targetCol = (mappedX * engine.gridSize).toInt().coerceIn(0, engine.gridSize - 1)
        val targetRow = (mappedY * engine.gridSize).toInt().coerceIn(0, engine.gridSize - 1)

        // 在棋盘上显示目标标记
        binding.gameView.setTarget(targetCol, targetRow)

        // 计算蛇头到目标的方向
        val head = engine.snake.last()
        val dx = targetCol - head.x
        val dy = targetRow - head.y

        // 到达目标 → 停止（精确到达，5帧平滑已防抖）
        val desiredDir: Direction? = when {
            dx == 0 && dy == 0 -> null  // 精确到达目标 → 停止
            abs(dx) >= abs(dy) -> if (dx > 0) Direction.RIGHT else Direction.LEFT
            else -> if (dy > 0) Direction.DOWN else Direction.UP
        }

        // 防止 180° 掉头：如果期望方向与当前相反，选垂直轴转弯
        currentGestureDirection = if (desiredDir != null && isOppositeDirection(desiredDir, engine.direction)) {
            when (engine.direction) {
                Direction.LEFT, Direction.RIGHT -> if (dy >= 0) Direction.DOWN else Direction.UP
                Direction.UP, Direction.DOWN -> if (dx >= 0) Direction.RIGHT else Direction.LEFT
            }
        } else {
            desiredDir
        }

        // 更新状态文字
        binding.tvGestureStatus.text = if (currentGestureDirection != null) {
            "追随 → ($targetCol,$targetRow)"
        } else {
            "已到达目标 ✓"
        }
        binding.tvGestureStatus.setTextColor(0xFF00FF88.toInt())

        if (currentGestureDirection != null) {
            highlightArrow(currentGestureDirection!!)
        } else {
            resetArrows()
        }
    }

    private fun isOppositeDirection(a: Direction, b: Direction): Boolean {
        return (a == Direction.UP && b == Direction.DOWN) ||
               (a == Direction.DOWN && b == Direction.UP) ||
               (a == Direction.LEFT && b == Direction.RIGHT) ||
               (a == Direction.RIGHT && b == Direction.LEFT)
    }

    private fun highlightArrow(direction: Direction) {
        resetArrows()
        val arrow = when (direction) {
            Direction.UP -> binding.arrowUp
            Direction.DOWN -> binding.arrowDown
            Direction.LEFT -> binding.arrowLeft
            Direction.RIGHT -> binding.arrowRight
        }
        arrow.setTextColor(0xFF00FF88.toInt())
        arrow.setShadowLayer(12f, 0f, 0f, 0xFF00FF88.toInt())
        handler.postDelayed({ resetArrows() }, 300)
    }

    private fun resetArrows() {
        val arrows = listOf(binding.arrowUp, binding.arrowDown, binding.arrowLeft, binding.arrowRight)
        for (a in arrows) {
            a.setTextColor(0x33FFFFFF.toInt())
            a.setShadowLayer(0f, 0f, 0f, 0)
        }
    }

    // ============ 游戏逻辑 ============

    private fun startCountdown() {
        engine.gameState = GameState.COUNTDOWN
        binding.tvCountdown.visibility = View.VISIBLE
        var count = 3
        binding.tvCountdown.text = count.toString()
        handler.postDelayed(object : Runnable {
            override fun run() {
                count--
                if (count > 0) { binding.tvCountdown.text = count.toString(); handler.postDelayed(this, 800) }
                else { binding.tvCountdown.visibility = View.GONE; startGame() }
            }
        }, 800)
    }

    private fun startGame() {
        engine.init()
        startTime = System.currentTimeMillis()
        if (engine.isGestureMode) gestureGameLoop() else gameLoop()
    }

    // 手势模式专用游戏循环：手在死区外 → 蛇持续移动，手回中心 → 蛇停止
    private val gestureGameRunnable = object : Runnable {
        override fun run() {
            if (engine.gameState == GameState.RUNNING && engine.snake.isNotEmpty()) {
                val dir = currentGestureDirection
                if (dir != null) {
                    engine.changeDirection(dir)
                    if (!engine.tick()) { onGameOver(); return }
                    binding.gameView.invalidate()
                    updateUI()
                }
                handler.postDelayed(this, 350)
            }
        }
    }
    private fun gestureGameLoop() { handler.postDelayed(gestureGameRunnable, 350) }

    private val gameRunnable = object : Runnable {
        override fun run() {
            if (engine.gameState == GameState.RUNNING) {
                if (!engine.tick()) { onGameOver(); return }
                binding.gameView.invalidate()
                updateUI()
                handler.postDelayed(this, engine.speed)
            }
        }
    }
    private fun gameLoop() { handler.postDelayed(gameRunnable, engine.speed) }

    private fun updateUI() {
        binding.tvScore.text = "得分: ${engine.score}"
        if (gameMode == GameMode.CHALLENGE) {
            val remaining = (60_000L - (System.currentTimeMillis() - startTime)).coerceAtLeast(0)
            engine.remainingTimeMs = remaining
            binding.tvTimer.text = String.format("%d.%d", remaining / 1000, (remaining % 1000) / 100)
            if (remaining <= 10_000L) binding.tvTimer.setTextColor(0xFFFF3366.toInt())
            if (remaining <= 0L) { engine.gameState = GameState.GAME_OVER; onGameOver() }
        } else {
            elapsedTime = System.currentTimeMillis() - startTime
            binding.tvTimer.text = String.format("%d:%02d", elapsedTime / 1000 / 60, elapsedTime / 1000 % 60)
        }
    }

    private fun setupControls() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x; val dy = e2.y - e1.y
                if (abs(dx) > abs(dy)) engine.changeDirection(if (dx > 0) Direction.RIGHT else Direction.LEFT)
                else engine.changeDirection(if (dy > 0) Direction.DOWN else Direction.UP)
                return true
            }
        })
        binding.gameView.setOnTouchListener { _, event ->
            if (settings.controlMode == "滑动" && !engine.isGestureMode) gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupDpad() {
        if (settings.controlMode == "虚拟方向键" && !engine.isGestureMode) {
            binding.dpadContainer.visibility = View.VISIBLE
        }
        binding.btnUp.setOnClickListener { engine.changeDirection(Direction.UP) }
        binding.btnDown.setOnClickListener { engine.changeDirection(Direction.DOWN) }
        binding.btnLeft.setOnClickListener { engine.changeDirection(Direction.LEFT) }
        binding.btnRight.setOnClickListener { engine.changeDirection(Direction.RIGHT) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (settings.controlMode == "滑动" && !engine.isGestureMode) gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun setupButtons() {
        binding.btnPause.setOnClickListener {
            if (engine.gameState == GameState.RUNNING) {
                engine.gameState = GameState.PAUSED
                binding.dpadContainer.visibility = View.GONE
                binding.pausePanel.visibility = View.VISIBLE
            }
        }
        binding.btnResume.setOnClickListener {
            binding.pausePanel.visibility = View.GONE
            engine.gameState = GameState.RUNNING
            if (settings.controlMode == "虚拟方向键" && !engine.isGestureMode) binding.dpadContainer.visibility = View.VISIBLE
            if (engine.isGestureMode) gestureGameLoop() else gameLoop()
        }
        binding.btnRestartPause.setOnClickListener { binding.pausePanel.visibility = View.GONE; restartGame() }
        binding.btnHomePause.setOnClickListener { finish() }
        binding.btnPlayAgain.setOnClickListener { binding.gameOverPanel.visibility = View.GONE; restartGame() }
        binding.btnHomeOver.setOnClickListener { finish() }
    }

    private fun restartGame() {
        handler.removeCallbacksAndMessages(null)
        handBuffer.clear()
        engine = GameEngine(GameConfig(mode = gameMode, difficulty = settings.difficulty))
        binding.gameView.setup(engine, settings.snakeSkin, settings.foodSkin, settings.showGrid)
        binding.gameView.clearTarget()
        currentGestureDirection = null
        binding.dpadContainer.visibility = if (settings.controlMode == "虚拟方向键" && !engine.isGestureMode) View.VISIBLE else View.GONE
        startCountdown()
    }

    private fun onGameOver() {
        elapsedTime = System.currentTimeMillis() - startTime
        binding.dpadContainer.visibility = View.GONE
        binding.gameView.clearTarget()
        currentGestureDirection = null
        handBuffer.clear()
        val score = engine.score; val len = engine.snake.size; val ate = engine.foodEaten; val sec = (elapsedTime / 1000).toInt()

        // 手势模式不记录排行榜，但仍更新成就
        settings.totalGames = settings.totalGames + 1
        Thread {
            if (gameMode != GameMode.GESTURE) {
                AppDatabase.getInstance(this).scoreDao().insert(ScoreRecord(score = score, mode = gameMode.name, snakeLength = len, foodEaten = ate, timeUsedSec = sec, date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())))
            }
            if (gameMode == GameMode.CLASSIC && score > settings.classicBest) settings.classicBest = score
            if (gameMode == GameMode.CHALLENGE && score > settings.challengeBest) settings.challengeBest = score
            AchievementManager.checkUnlocks(settings, score, len, gameMode)
            val skins = settings.unlockedSkins.toMutableSet()
            if (settings.classicBest >= 100) skins.add("NEON_BLUE")
            if (settings.challengeBest >= 200) skins.add("FLAME_RED")
            if (settings.totalGames >= 10) skins.add("RAINBOW")
            settings.unlockedSkins = skins
            val foods = settings.unlockedFoods.toMutableSet()
            if (settings.classicBest >= 500) foods.add("DIAMOND")
            settings.unlockedFoods = foods
            val boards = settings.unlockedBoards.toMutableSet()
            if (settings.totalGames >= 20) boards.add("STARRY")
            settings.unlockedBoards = boards
        }.start()

        runOnUiThread {
            binding.tvFinalScore.text = score.toString()
            binding.tvFinalLength.text = "${len}节"; binding.tvFinalFood.text = "${ate}个"
            binding.tvFinalTime.text = String.format("%d:%02d", sec / 60, sec % 60)
            val best = when (gameMode) {
                GameMode.CLASSIC -> settings.classicBest
                GameMode.CHALLENGE -> settings.challengeBest
                GameMode.GESTURE -> score
            }
            binding.tvNewRecord.visibility = if (score >= best && score > 0 && gameMode != GameMode.GESTURE) View.VISIBLE else View.GONE
            binding.gameOverPanel.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        if (engine.gameState == GameState.RUNNING) {
            engine.gameState = GameState.PAUSED
            binding.pausePanel.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        gestureHelper?.close()
        cameraExecutor.shutdown()
    }
}
