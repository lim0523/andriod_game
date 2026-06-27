package com.example.exam

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exam.databinding.ActivitySkinBinding

class SkinActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySkinBinding
    private lateinit var settings: GameSettings

    private lateinit var snakeAdapter: SkinAdapter<SnakeSkin>
    private lateinit var foodAdapter: SkinAdapter<FoodSkin>
    private lateinit var boardAdapter: SkinAdapter<BoardSkin>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySkinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = GameSettings(this)
        binding.btnBack.setOnClickListener { finish() }

        binding.rvSnakeSkins.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        snakeAdapter = SkinAdapter(
            SnakeSkin.entries.toList(),
            settings.unlockedSkins,
            settings.snakeSkin.name,
            { getUnlockCondition(it) }
        ) { skin ->
            settings.snakeSkin = skin
            snakeAdapter.updateSelected(skin.name)
        }
        binding.rvSnakeSkins.adapter = snakeAdapter

        binding.rvFoodSkins.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        foodAdapter = SkinAdapter(
            FoodSkin.entries.toList(),
            settings.unlockedFoods,
            settings.foodSkin.name,
            { getUnlockCondition(it) }
        ) { skin ->
            settings.foodSkin = skin
            foodAdapter.updateSelected(skin.name)
        }
        binding.rvFoodSkins.adapter = foodAdapter

        binding.rvBoardSkins.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        boardAdapter = SkinAdapter(
            BoardSkin.entries.toList(),
            settings.unlockedBoards,
            settings.boardSkin.name,
            { getUnlockCondition(it) }
        ) { skin ->
            settings.boardSkin = skin
            boardAdapter.updateSelected(skin.name)
        }
        binding.rvBoardSkins.adapter = boardAdapter
    }

    private fun getUnlockCondition(item: Any): String {
        return when (item) {
            is SnakeSkin -> when (item) {
                SnakeSkin.CLASSIC_GREEN -> "默认解锁"
                SnakeSkin.NEON_BLUE -> "经典模式单局100分解锁"
                SnakeSkin.FLAME_RED -> "挑战模式单局200分解锁"
                SnakeSkin.RAINBOW -> "累计游戏10局解锁"
            }
            is FoodSkin -> when (item) {
                FoodSkin.APPLE -> "默认解锁"
                FoodSkin.STAR -> "默认解锁"
                FoodSkin.DIAMOND -> "经典模式单局500分解锁"
            }
            is BoardSkin -> when (item) {
                BoardSkin.DARK_NIGHT -> "默认解锁"
                BoardSkin.CYBER_NEON -> "默认解锁"
                BoardSkin.STARRY -> "累计游戏20局解锁"
            }
            else -> "未知条件"
        }
    }

    class SkinAdapter<T>(
        private val items: List<T>,
        private val unlocked: Set<String>,
        private var selected: String,
        private val unlockCondition: (Any) -> String,
        private val onSelect: (T) -> Unit
    ) : RecyclerView.Adapter<SkinAdapter.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tv_skin_name)
            val status: TextView = view.findViewById(R.id.tv_skin_status)
            val preview: View = view.findViewById(R.id.color_preview)
        }

        fun updateSelected(newSelected: String) {
            selected = newSelected
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(LayoutInflater.from(parent.context).inflate(R.layout.item_skin, parent, false))
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val nm = when (item) {
                is SnakeSkin -> item.displayName
                is FoodSkin -> item.displayName
                is BoardSkin -> item.displayName
                else -> ""
            }
            val key = when (item) {
                is SnakeSkin -> item.name
                is FoodSkin -> item.name
                is BoardSkin -> item.name
                else -> ""
            }
            val color = when (item) {
                is SnakeSkin -> item.headColor
                is FoodSkin -> item.color
                else -> 0xFF1A1A2E.toInt()
            }
            holder.name.text = nm
            holder.preview.setBackgroundColor(color)
            val ok = key in unlocked
            val sel = key == selected
            holder.status.text = when {
                sel -> "\u2713"
                ok -> ""
                else -> "\uD83D\uDD12"
            }
            holder.status.setTextColor(if (sel) 0xFF00FF88.toInt() else 0xFFFFD700.toInt())

            holder.itemView.setOnClickListener {
                if (ok) {
                    onSelect(item)
                } else {
                    val condition = unlockCondition(item as Any)
                    AlertDialog.Builder(holder.itemView.context)
                        .setTitle("未解锁")
                        .setMessage(condition)
                        .setPositiveButton("知道了", null)
                        .show()
                }
            }
        }

        override fun getItemCount() = items.size
    }
}
