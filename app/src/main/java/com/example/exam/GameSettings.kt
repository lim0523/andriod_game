package com.example.exam

import android.content.Context
import android.content.SharedPreferences

class GameSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("snake_settings", Context.MODE_PRIVATE)

    var controlMode: String
        get() = prefs.getString("control_mode", "滑动") ?: "滑动"
        set(v) = prefs.edit().putString("control_mode", v).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(v) = prefs.edit().putBoolean("sound", v).apply()

    var musicEnabled: Boolean
        get() = prefs.getBoolean("music", true)
        set(v) = prefs.edit().putBoolean("music", v).apply()

    var difficulty: Difficulty
        get() = Difficulty.valueOf(prefs.getString("difficulty", "NORMAL") ?: "NORMAL")
        set(v) = prefs.edit().putString("difficulty", v.name).apply()

    var showGrid: Boolean
        get() = prefs.getBoolean("grid", true)
        set(v) = prefs.edit().putBoolean("grid", v).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(v) = prefs.edit().putBoolean("vibration", v).apply()

    var snakeSkin: SnakeSkin
        get() = SnakeSkin.valueOf(prefs.getString("snake_skin", "CLASSIC_GREEN") ?: "CLASSIC_GREEN")
        set(v) = prefs.edit().putString("snake_skin", v.name).apply()

    var foodSkin: FoodSkin
        get() = FoodSkin.valueOf(prefs.getString("food_skin", "APPLE") ?: "APPLE")
        set(v) = prefs.edit().putString("food_skin", v.name).apply()

    var boardSkin: BoardSkin
        get() = BoardSkin.valueOf(prefs.getString("board_skin", "DARK_NIGHT") ?: "DARK_NIGHT")
        set(v) = prefs.edit().putString("board_skin", v.name).apply()

    var totalGames: Int
        get() = prefs.getInt("total_games", 0)
        set(v) = prefs.edit().putInt("total_games", v).apply()

    var classicBest: Int
        get() = prefs.getInt("classic_best", 0)
        set(v) = prefs.edit().putInt("classic_best", v).apply()

    var challengeBest: Int
        get() = prefs.getInt("challenge_best", 0)
        set(v) = prefs.edit().putInt("challenge_best", v).apply()

    var unlockedSkins: Set<String>
        get() = (prefs.getStringSet("unlocked_skins", setOf("CLASSIC_GREEN", "GEM_PURPLE"))
            ?: setOf("CLASSIC_GREEN", "GEM_PURPLE")) + "GEM_PURPLE"
        set(v) = prefs.edit().putStringSet("unlocked_skins", v).apply()

    var unlockedFoods: Set<String>
        get() = prefs.getStringSet("unlocked_foods", setOf("APPLE", "STAR")) ?: setOf("APPLE", "STAR")
        set(v) = prefs.edit().putStringSet("unlocked_foods", v).apply()

    var unlockedBoards: Set<String>
        get() = prefs.getStringSet("unlocked_boards", setOf("DARK_NIGHT", "CYBER_NEON")) ?: setOf("DARK_NIGHT", "CYBER_NEON")
        set(v) = prefs.edit().putStringSet("unlocked_boards", v).apply()

    var achievements: Set<String>
        get() = prefs.getStringSet("achievements", emptySet()) ?: emptySet()
        set(v) = prefs.edit().putStringSet("achievements", v).apply()
}
