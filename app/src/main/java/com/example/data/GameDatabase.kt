package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.GameDao
import com.example.data.entity.LevelProgress
import com.example.data.entity.PlayerProfile
import com.example.data.entity.WeaponEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [LevelProgress::class, WeaponEntity::class, PlayerProfile::class],
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "strike_ops_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default values on creation
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).gameDao()
                            seedInitialData(dao)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialData(dao: GameDao) {
            val defaultLevels = listOf(
                LevelProgress(levelId = 1, unlocked = true, completed = false, stars = 0, highScore = 0, bestTimeSeconds = 0, kills = 0, headshots = 0),
                LevelProgress(levelId = 2, unlocked = false, completed = false, stars = 0, highScore = 0, bestTimeSeconds = 0, kills = 0, headshots = 0),
                LevelProgress(levelId = 3, unlocked = false, completed = false, stars = 0, highScore = 0, bestTimeSeconds = 0, kills = 0, headshots = 0),
                LevelProgress(levelId = 4, unlocked = false, completed = false, stars = 0, highScore = 0, bestTimeSeconds = 0, kills = 0, headshots = 0)
            )
            dao.insertLevels(defaultLevels)

            val defaultWeapons = listOf(
                WeaponEntity(weaponId = "m4a1", unlocked = true, damageLevel = 1, capacityLevel = 1, stabilityLevel = 1),
                WeaponEntity(weaponId = "deagle", unlocked = true, damageLevel = 1, capacityLevel = 1, stabilityLevel = 1),
                WeaponEntity(weaponId = "shotgun", unlocked = false, damageLevel = 1, capacityLevel = 1, stabilityLevel = 1),
                WeaponEntity(weaponId = "awp", unlocked = false, damageLevel = 1, capacityLevel = 1, stabilityLevel = 1),
                WeaponEntity(weaponId = "railgun", unlocked = false, damageLevel = 1, capacityLevel = 1, stabilityLevel = 1),
                WeaponEntity(weaponId = "knife", unlocked = true, damageLevel = 1, capacityLevel = 1, stabilityLevel = 1)
            )
            dao.insertWeapons(defaultWeapons)

            dao.insertProfile(PlayerProfile())
        }
    }
}
