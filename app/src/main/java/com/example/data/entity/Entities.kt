package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgress(
    @PrimaryKey val levelId: Int,
    val unlocked: Boolean,
    val completed: Boolean,
    val stars: Int,
    val highScore: Int,
    val bestTimeSeconds: Int,
    val kills: Int,
    val headshots: Int
)

@Entity(tableName = "weapon_loadout")
data class WeaponEntity(
    @PrimaryKey val weaponId: String,
    val unlocked: Boolean,
    val damageLevel: Int = 1,
    val capacityLevel: Int = 1,
    val stabilityLevel: Int = 1
)

@Entity(tableName = "player_profile")
data class PlayerProfile(
    @PrimaryKey val id: Int = 1,
    val credits: Int = 1500,
    val totalKills: Int = 0,
    val totalHeadshots: Int = 0,
    val missionsCompleted: Int = 0,
    val equippedWeaponId: String = "m4a1"
)
