package com.example.exam

data class Achievement(
    val id: String,
    val name: String,
    val desc: String,
    val condition: (GameSettings, Int, Int) -> Boolean
)

object AchievementManager {
    val ALL = listOf(
        Achievement("first_game", "初出茅庐", "完成第1局游戏", { s, _, _ -> s.totalGames >= 1 }),
        Achievement("score_100", "百分玩家", "经典模式单局100+", { s, score, _ -> s.classicBest >= 100 }),
        Achievement("challenge_200", "速度之王", "挑战模式单局200+", { s, _, _ -> s.challengeBest >= 200 }),
        Achievement("length_30", "贪吃达人", "单局蛇身30节", { _, _, len -> len >= 30 }),
        Achievement("games_10", "坚持不懈", "累计游戏10局", { s, _, _ -> s.totalGames >= 10 }),
        Achievement("score_500", "传奇猎手", "经典模式单局500+", { s, _, _ -> s.classicBest >= 500 })
    )

    fun checkUnlocks(settings: GameSettings, score: Int, snakeLength: Int, mode: GameMode): List<Achievement> {
        val newUnlocks = mutableListOf<Achievement>()
        val current = settings.achievements.toMutableSet()
        for (a in ALL) {
            if (a.id !in current && a.condition(settings, score, snakeLength)) {
                current.add(a.id)
                newUnlocks.add(a)
            }
        }
        if (newUnlocks.isNotEmpty()) settings.achievements = current
        return newUnlocks
    }

    fun getProgress(achievement: Achievement, settings: GameSettings): Int {
        return when (achievement.id) {
            "first_game" -> minOf(settings.totalGames, 1) * 100
            "score_100" -> minOf(settings.classicBest, 100)
            "challenge_200" -> minOf(settings.challengeBest, 200)
            "length_30" -> 0 // 动态，无法精确计算
            "games_10" -> minOf(settings.totalGames * 10, 100)
            "score_500" -> minOf(settings.classicBest / 5, 100)
            else -> 0
        }
    }
}
