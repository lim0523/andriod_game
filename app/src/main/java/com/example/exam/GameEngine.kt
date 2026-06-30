package com.example.exam

import android.graphics.Point
import kotlin.random.Random

data class GameConfig(
    val mode: GameMode = GameMode.CLASSIC,
    val difficulty: Difficulty = Difficulty.NORMAL,
    val gridSize: Int = 20
)

class GameEngine(private val config: GameConfig) {

    val gridSize = config.gridSize
    val snake = mutableListOf<Point>()
    var food = Point(0, 0)
    var bonusFood: Point? = null
    var direction = Direction.RIGHT
    var nextDirection = Direction.RIGHT
    var score = 0
    var foodEaten = 0
    var gameState = GameState.IDLE
    var remainingTimeMs = 60_000L
    var speedBoostEndTime = 0L
    private var obstacles = mutableListOf<Point>()

    val speed: Long
        get() {
            val base = when (config.difficulty) {
                Difficulty.EASY -> 200L
                Difficulty.NORMAL -> 150L
                Difficulty.HARD -> 100L
            }
            val speedUp = if (config.mode == GameMode.CLASSIC) (score / 50) * 15L else 0L
            val boost = if (System.currentTimeMillis() < speedBoostEndTime) -(base * 0.5).toLong() else 0L
            return (base - speedUp + boost).coerceAtLeast(60L)
        }

    val canWrap: Boolean get() = config.difficulty == Difficulty.EASY

    val isGestureMode: Boolean get() = config.mode == GameMode.GESTURE

    fun init() {
        snake.clear()
        obstacles.clear()
        val mid = gridSize / 2
        for (i in 3 downTo 0) snake.add(Point(mid - i, mid))
        direction = Direction.RIGHT
        nextDirection = Direction.RIGHT
        score = 0
        foodEaten = 0
        remainingTimeMs = 60_000L
        speedBoostEndTime = 0L
        spawnFood()
        if (config.difficulty == Difficulty.HARD) spawnObstacles()
        gameState = GameState.RUNNING
    }

    fun changeDirection(newDir: Direction) {
        if (!newDir.isOpposite(direction)) nextDirection = newDir
    }

    fun tick(): Boolean {
        if (gameState != GameState.RUNNING) return false
        direction = nextDirection
        val head = snake.last()
        val newHead = Point(head.x + direction.dx, head.y + direction.dy)
        if (canWrap) {
            if (newHead.x < 0) newHead.x = gridSize - 1
            if (newHead.x >= gridSize) newHead.x = 0
            if (newHead.y < 0) newHead.y = gridSize - 1
            if (newHead.y >= gridSize) newHead.y = 0
        } else {
            if (newHead.x < 0 || newHead.x >= gridSize || newHead.y < 0 || newHead.y >= gridSize) {
                gameState = GameState.GAME_OVER
                return false
            }
        }
        if (snake.contains(newHead) || obstacles.contains(newHead)) {
            gameState = GameState.GAME_OVER
            return false
        }
        snake.add(newHead)
        if (newHead == food) {
            score += 10
            foodEaten++
            spawnFood()
            if (config.mode == GameMode.CHALLENGE && bonusFood == null && Random.nextFloat() < 0.3f) spawnBonusFood()
        } else if (newHead == bonusFood) {
            if (Random.nextFloat() < 0.5f) score += 20 else speedBoostEndTime = System.currentTimeMillis() + 5000L
            bonusFood = null
            foodEaten++
        } else {
            snake.removeAt(0)
        }
        return true
    }

    private fun spawnFood() {
        val occupied = snake.toSet() + obstacles.toSet()
        val empty = (0 until gridSize).flatMap { x -> (0 until gridSize).map { y -> Point(x, y) } }.filter { it !in occupied }
        if (empty.isEmpty()) { gameState = GameState.GAME_OVER; return }
        food = empty[Random.nextInt(empty.size)]
    }

    private fun spawnBonusFood() {
        val occupied = snake.toSet() + obstacles.toSet() + setOf(food)
        val empty = (0 until gridSize).flatMap { x -> (0 until gridSize).map { y -> Point(x, y) } }.filter { it !in occupied }
        if (empty.isNotEmpty()) bonusFood = empty[Random.nextInt(empty.size)]
    }

    private fun spawnObstacles() {
        val occupied = snake.toSet()
        repeat(5) {
            val p = Point(Random.nextInt(1, gridSize - 1), Random.nextInt(1, gridSize - 1))
            if (p !in occupied) obstacles.add(p)
        }
    }

    fun getObstacles(): List<Point> = obstacles
}
