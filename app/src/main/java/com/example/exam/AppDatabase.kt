package com.example.exam

import android.content.Context
import androidx.room.*

@Entity(tableName = "scores")
data class ScoreRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val mode: String,
    val snakeLength: Int,
    val foodEaten: Int,
    val timeUsedSec: Int,
    val date: String
)

@Dao
interface ScoreDao {
    @Query("SELECT * FROM scores WHERE mode = :mode ORDER BY score DESC LIMIT 10")
    fun getTopByMode(mode: String): List<ScoreRecord>

    @Insert
    fun insert(record: ScoreRecord)

    @Query("SELECT MAX(score) FROM scores WHERE mode = :mode")
    fun getBestScore(mode: String): Int?
}

@Database(entities = [ScoreRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scoreDao(): ScoreDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "snake_db").build().also { INSTANCE = it }
            }
        }
    }
}
