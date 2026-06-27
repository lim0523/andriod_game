package com.example.exam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exam.databinding.ActivityAchievementsBinding

class AchievementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAchievementsBinding
    private lateinit var settings: GameSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = GameSettings(this)
        binding.btnBack.setOnClickListener { finish() }

        binding.rvAchievements.layoutManager = LinearLayoutManager(this)
        binding.rvAchievements.adapter = AchievementAdapter(
            AchievementManager.ALL, settings.achievements, settings
        )
    }

    override fun onResume() {
        super.onResume()
        binding.rvAchievements.adapter = AchievementAdapter(
            AchievementManager.ALL, settings.achievements, settings
        )
    }

    class AchievementAdapter(
        private val achievements: List<Achievement>,
        private val unlocked: Set<String>,
        private val settings: GameSettings
    ) : RecyclerView.Adapter<AchievementAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_achievement_name)
            val status: TextView = view.findViewById(R.id.tv_achievement_status)
            val desc: TextView = view.findViewById(R.id.tv_achievement_desc)
            val progress: ProgressBar = view.findViewById(R.id.progress_achievement)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_achievement, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val a = achievements[position]
            val isUnlocked = a.id in unlocked
            holder.name.text = a.name
            holder.desc.text = a.desc
            if (isUnlocked) {
                holder.status.text = "已解锁"
                holder.status.setTextColor(0xFF00FF88.toInt())
                holder.name.setTextColor(0xFF00FF88.toInt())
                holder.progress.progress = 100
            } else {
                holder.status.text = "未解锁"
                holder.status.setTextColor(0xFF4A5568.toInt())
                holder.name.setTextColor(0xFFFFFFFF.toInt())
                holder.progress.progress = AchievementManager.getProgress(a, settings)
            }
        }

        override fun getItemCount() = achievements.size
    }
}
