package com.example.exam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exam.databinding.ActivityLeaderboardBinding
import com.google.android.material.tabs.TabLayout

class LeaderboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLeaderboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeaderboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener { finish() }

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("经典模式"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("挑战模式"))
        binding.rvLeaderboard.layoutManager = LinearLayoutManager(this)
        loadRecords("CLASSIC")

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                loadRecords(if (tab.position == 0) "CLASSIC" else "CHALLENGE")
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadRecords(mode: String) {
        Thread {
            val records = AppDatabase.getInstance(this).scoreDao().getTopByMode(mode)
            runOnUiThread {
                binding.rvLeaderboard.adapter = LeaderboardAdapter(records)
            }
        }.start()
    }

    private class LeaderboardAdapter(
        private val records: List<ScoreRecord>
    ) : RecyclerView.Adapter<LeaderboardAdapter.LeaderboardVH>() {

        class LeaderboardVH(view: View) : RecyclerView.ViewHolder(view) {
            val rank: TextView = view.findViewById(R.id.tv_rank)
            val score: TextView = view.findViewById(R.id.tv_record_score)
            val date: TextView = view.findViewById(R.id.tv_record_date)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderboardVH {
            return LeaderboardVH(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_leaderboard, parent, false))
        }

        override fun onBindViewHolder(holder: LeaderboardVH, position: Int) {
            val r = records[position]
            val medals = arrayOf("\uD83E\uDD47", "\uD83E\uDD48", "\uD83E\uDD49")
            holder.rank.text = if (position < 3) medals[position] else "${position + 1}"
            holder.score.text = "${r.score}分"
            holder.date.text = r.date
        }

        override fun getItemCount() = records.size
    }
}
