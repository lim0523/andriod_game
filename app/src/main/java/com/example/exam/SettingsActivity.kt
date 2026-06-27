package com.example.exam

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.exam.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: GameSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = GameSettings(this)
        binding.btnBack.setOnClickListener { finish() }

        binding.spinnerControl.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                settings.controlMode = resources.getStringArray(R.array.control_options)[position]
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        binding.switchSound.isChecked = settings.soundEnabled
        binding.switchSound.setOnCheckedChangeListener { _, isChecked -> settings.soundEnabled = isChecked }
        binding.switchMusic.isChecked = settings.musicEnabled
        binding.switchMusic.setOnCheckedChangeListener { _, isChecked -> settings.musicEnabled = isChecked }

        when (settings.difficulty) {
            Difficulty.EASY -> binding.rbEasy.isChecked = true
            Difficulty.NORMAL -> binding.rbNormal.isChecked = true
            Difficulty.HARD -> binding.rbHard.isChecked = true
        }
        binding.rgDifficulty.setOnCheckedChangeListener { _, checkedId ->
            settings.difficulty = when (checkedId) {
                R.id.rb_easy -> Difficulty.EASY
                R.id.rb_hard -> Difficulty.HARD
                else -> Difficulty.NORMAL
            }
        }

        binding.switchGrid.isChecked = settings.showGrid
        binding.switchGrid.setOnCheckedChangeListener { _, isChecked -> settings.showGrid = isChecked }
        binding.switchVibration.isChecked = settings.vibrationEnabled
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked -> settings.vibrationEnabled = isChecked }
    }
}