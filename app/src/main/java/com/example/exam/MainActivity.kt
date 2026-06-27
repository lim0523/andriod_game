package com.example.exam

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.exam.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: GameSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = GameSettings(this)

        binding.btnStart.setOnClickListener { showModeSelection() }
        binding.btnSkin.setOnClickListener { startActivity(Intent(this, SkinActivity::class.java)) }
        binding.btnLeaderboard.setOnClickListener { startActivity(Intent(this, LeaderboardActivity::class.java)) }
        binding.btnAchievements.setOnClickListener { startActivity(Intent(this, AchievementActivity::class.java)) }
        binding.btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        val best = maxOf(settings.classicBest, settings.challengeBest)
        binding.tvBestScore.text = "最高分: $best"
    }

    private fun showModeSelection() {
        val items = arrayOf("经典模式 (无尽)", "挑战模式 (限时60秒)", "手势休闲 (手势识别)")
        android.app.AlertDialog.Builder(this)
            .setTitle("选择游戏模式")
            .setItems(items) { _, which ->
                val mode = when (which) {
                    0 -> GameMode.CLASSIC
                    1 -> GameMode.CHALLENGE
                    2 -> GameMode.GESTURE
                    else -> GameMode.CLASSIC
                }
                startActivity(Intent(this, GameActivity::class.java).apply {
                    putExtra("mode", mode.name)
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
