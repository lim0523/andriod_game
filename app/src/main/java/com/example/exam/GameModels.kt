package com.example.exam

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun isOpposite(other: Direction): Boolean {
        return dx + other.dx == 0 && dy + other.dy == 0
    }
}

enum class GameMode { CLASSIC, CHALLENGE, GESTURE }

enum class Difficulty { EASY, NORMAL, HARD }

enum class GameState { IDLE, COUNTDOWN, RUNNING, PAUSED, GAME_OVER }

enum class SnakeSkin(val displayName: String, val headColor: Int, val bodyStartColor: Int, val bodyEndColor: Int) {
    CLASSIC_GREEN("经典绿", 0xFF00FF88.toInt(), 0xFF00FF88.toInt(), 0xFF00CC66.toInt()),
    NEON_BLUE("霓虹蓝", 0xFF00D4FF.toInt(), 0xFF00D4FF.toInt(), 0xFF0066FF.toInt()),
    GEM_PURPLE("霓虹宝石", 0xFFFF4DFF.toInt(), 0xFFB65CFF.toInt(), 0xFFFF2E88.toInt()),
    FLAME_RED("火焰红", 0xFFFF3366.toInt(), 0xFFFF6600.toInt(), 0xFFFF3366.toInt()),
    RAINBOW("彩虹蛇", 0xFFFF0000.toInt(), 0xFFFF0000.toInt(), 0xFF0000FF.toInt())
}

enum class FoodSkin(val displayName: String, val color: Int) {
    APPLE("苹果", 0xFFFF3366.toInt()),
    STAR("星星", 0xFFFFD700.toInt()),
    DIAMOND("钻石", 0xFF00D4FF.toInt())
}

enum class BoardSkin(val displayName: String) {
    DARK_NIGHT("经典暗夜"),
    CYBER_NEON("赛博霓虹"),
    STARRY("星空")
}
