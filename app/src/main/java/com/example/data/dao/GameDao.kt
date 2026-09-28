package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.LevelProgress
import com.example.data.entity.PlayerProfile
import com.example.data.entity.WeaponEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
    fun getAllLevels(): Flow<List<LevelProgress>>

    @Query("SELECT * FROM level_progress WHERE levelId = :id")
    suspend fun getLevelById(id: Int): LevelProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLevels(levels: List<LevelProgress>)

    @Update
    suspend fun updateLevel(level: LevelProgress)

    @Query("SELECT * FROM weapon_loadout")
    fun getAllWeapons(): Flow<List<WeaponEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeapons(weapons: List<WeaponEntity>)

    @Update
    suspend fun updateWeapon(weapon: WeaponEntity)

    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun getPlayerProfile(): Flow<PlayerProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: PlayerProfile)

    @Update
    suspend fun updateProfile(profile: PlayerProfile)
}
