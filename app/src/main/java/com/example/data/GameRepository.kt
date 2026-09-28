package com.example.data

import com.example.data.dao.GameDao
import com.example.data.entity.LevelProgress
import com.example.data.entity.PlayerProfile
import com.example.data.entity.WeaponEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(private val dao: GameDao) {
    val levels: Flow<List<LevelProgress>> = dao.getAllLevels()
    val weapons: Flow<List<WeaponEntity>> = dao.getAllWeapons()
    val profile: Flow<PlayerProfile?> = dao.getPlayerProfile()

    suspend fun saveMissionResult(
        levelId: Int,
        score: Int,
        timeSeconds: Int,
        kills: Int,
        headshots: Int,
        starsEarned: Int,
        creditsReward: Int
    ) {
        val currentLevel = dao.getLevelById(levelId)
        val bestScore = maxOf(currentLevel?.highScore ?: 0, score)
        val bestStars = maxOf(currentLevel?.stars ?: 0, starsEarned)
        val bestTime = if ((currentLevel?.bestTimeSeconds ?: 0) > 0) {
            minOf(currentLevel?.bestTimeSeconds ?: timeSeconds, timeSeconds)
        } else timeSeconds

        dao.updateLevel(
            LevelProgress(
                levelId = levelId,
                unlocked = true,
                completed = true,
                stars = bestStars,
                highScore = bestScore,
                bestTimeSeconds = bestTime,
                kills = (currentLevel?.kills ?: 0) + kills,
                headshots = (currentLevel?.headshots ?: 0) + headshots
            )
        )

        // Unlock next level if available
        if (levelId < 4) {
            val nextLevel = dao.getLevelById(levelId + 1)
            if (nextLevel != null && !nextLevel.unlocked) {
                dao.updateLevel(nextLevel.copy(unlocked = true))
            }
        }

        // Update profile credits and stats
        val currentProfile = dao.getPlayerProfile().firstOrNull() ?: PlayerProfile()
        dao.updateProfile(
            currentProfile.copy(
                credits = currentProfile.credits + creditsReward,
                totalKills = currentProfile.totalKills + kills,
                totalHeadshots = currentProfile.totalHeadshots + headshots,
                missionsCompleted = currentProfile.missionsCompleted + 1
            )
        )
    }

    suspend fun unlockWeapon(weaponId: String, cost: Int): Boolean {
        val currentProfile = dao.getPlayerProfile().firstOrNull() ?: return false
        if (currentProfile.credits < cost) return false

        dao.updateProfile(currentProfile.copy(credits = currentProfile.credits - cost))
        val currentWeapons = dao.getAllWeapons().firstOrNull() ?: emptyList()
        val weapon = currentWeapons.find { it.weaponId == weaponId } ?: WeaponEntity(weaponId = weaponId, unlocked = true)
        dao.updateWeapon(weapon.copy(unlocked = true))
        return true
    }

    suspend fun upgradeWeapon(weaponId: String, upgradeType: String, cost: Int): Boolean {
        val currentProfile = dao.getPlayerProfile().firstOrNull() ?: return false
        if (currentProfile.credits < cost) return false

        val currentWeapons = dao.getAllWeapons().firstOrNull() ?: emptyList()
        val weapon = currentWeapons.find { it.weaponId == weaponId } ?: return false

        val updated = when (upgradeType) {
            "damage" -> if (weapon.damageLevel < 5) weapon.copy(damageLevel = weapon.damageLevel + 1) else return false
            "capacity" -> if (weapon.capacityLevel < 5) weapon.copy(capacityLevel = weapon.capacityLevel + 1) else return false
            "stability" -> if (weapon.stabilityLevel < 5) weapon.copy(stabilityLevel = weapon.stabilityLevel + 1) else return false
            else -> return false
        }

        dao.updateProfile(currentProfile.copy(credits = currentProfile.credits - cost))
        dao.updateWeapon(updated)
        return true
    }

    suspend fun equipWeapon(weaponId: String) {
        val currentProfile = dao.getPlayerProfile().firstOrNull() ?: return
        dao.updateProfile(currentProfile.copy(equippedWeaponId = weaponId))
    }
}
