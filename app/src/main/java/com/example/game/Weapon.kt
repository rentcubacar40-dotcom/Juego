package com.example.game

import com.example.audio.SoundEffect

enum class WeaponType {
    ASSAULT_RIFLE,
    SNIPER_RIFLE,
    SHOTGUN,
    PISTOL,
    PLASMA_RAILGUN,
    CYBER_BLADE
}

data class Weapon(
    val id: String,
    val name: String,
    val type: WeaponType,
    val baseDamage: Float,
    val fireRateMs: Long,
    val maxMagazine: Int,
    var currentAmmo: Int,
    var reserveAmmo: Int,
    val reloadTimeMs: Long,
    val recoilKick: Float,
    val baseSpread: Float,
    val hasScope: Boolean = false,
    val scopeZoom: Float = 1.0f,
    val fireSound: SoundEffect,
    val description: String,
    val price: Int = 0,
    val sciFiTier: String = "Gen-6 Hyper-Tactical (2026)",
    val laserColor: Long = 0xFF00FFCC
) {
    fun copyWeapon(damageMultiplier: Float = 1.0f, capacityMultiplier: Float = 1.0f, stabilityMultiplier: Float = 1.0f): Weapon {
        val boostedMag = (maxMagazine * capacityMultiplier).toInt()
        return copy(
            baseDamage = baseDamage * damageMultiplier,
            maxMagazine = boostedMag,
            currentAmmo = boostedMag,
            reserveAmmo = reserveAmmo * 2,
            recoilKick = recoilKick / stabilityMultiplier,
            baseSpread = baseSpread / stabilityMultiplier
        )
    }

    companion object {
        fun createArsenal(): Map<String, Weapon> = mapOf(
            "m4a1" to Weapon(
                id = "m4a1",
                name = "Apex XM-26 Smart Carbine",
                type = WeaponType.ASSAULT_RIFLE,
                baseDamage = 34f,
                fireRateMs = 100L,
                maxMagazine = 35,
                currentAmmo = 35,
                reserveAmmo = 140,
                reloadTimeMs = 1600L,
                recoilKick = 3.6f,
                baseSpread = 0.025f,
                hasScope = false,
                scopeZoom = 1.4f,
                fireSound = SoundEffect.M4A1_FIRE,
                description = "Fusil de asalto hiper-táctico 2026 con mira holográfica HUD cuántica, rieles angulados y compensador de gas.",
                price = 0,
                sciFiTier = "Tactical Tier 1",
                laserColor = 0xFF00F5FF
            ),
            "railgun" to Weapon(
                id = "railgun",
                name = "Titan VX-9 Plasma Railgun",
                type = WeaponType.PLASMA_RAILGUN,
                baseDamage = 180f,
                fireRateMs = 1200L,
                maxMagazine = 4,
                currentAmmo = 4,
                reserveAmmo = 16,
                reloadTimeMs = 2800L,
                recoilKick = 14.0f,
                baseSpread = 0.001f,
                hasScope = true,
                scopeZoom = 3.4f,
                fireSound = SoundEffect.PLASMA_FIRE,
                description = "Cañón acelerador electromagnético futurista. Dispara dardos de plasma a Mach 7 con retícula balística predictiva.",
                price = 3200,
                sciFiTier = "Hyper-Spec Prototype",
                laserColor = 0xFFFF0055
            ),
            "awp" to Weapon(
                id = "awp",
                name = "Spectre 50-CAL Ghost Sniper",
                type = WeaponType.SNIPER_RIFLE,
                baseDamage = 145f,
                fireRateMs = 1100L,
                maxMagazine = 6,
                currentAmmo = 6,
                reserveAmmo = 24,
                reloadTimeMs = 2400L,
                recoilKick = 10.5f,
                baseSpread = 0.003f,
                hasScope = true,
                scopeZoom = 3.0f,
                fireSound = SoundEffect.AWP_FIRE,
                description = "Rifle anti-material de fibra de carbono y tungsteno con visor térmico inteligente con zoom dinámico.",
                price = 2800,
                sciFiTier = "Long-Range Elite",
                laserColor = 0xFF00FF66
            ),
            "shotgun" to Weapon(
                id = "shotgun",
                name = "Vulkan SG-12 Ion Breacher",
                type = WeaponType.SHOTGUN,
                baseDamage = 115f,
                fireRateMs = 650L,
                maxMagazine = 10,
                currentAmmo = 10,
                reserveAmmo = 40,
                reloadTimeMs = 2200L,
                recoilKick = 8.0f,
                baseSpread = 0.09f,
                hasScope = false,
                scopeZoom = 1.2f,
                fireSound = SoundEffect.SHOTGUN_FIRE,
                description = "Escopeta de combate ionizada con doble cañón superpuesto y retroceso hidráulico asistido por exoesqueleto.",
                price = 2100,
                sciFiTier = "CQB Heavy Enforcer",
                laserColor = 0xFFFF9900
            ),
            "deagle" to Weapon(
                id = "deagle",
                name = "Krypton .50 Heavy Mag",
                type = WeaponType.PISTOL,
                baseDamage = 62f,
                fireRateMs = 260L,
                maxMagazine = 8,
                currentAmmo = 8,
                reserveAmmo = 48,
                reloadTimeMs = 1400L,
                recoilKick = 5.8f,
                baseSpread = 0.02f,
                hasScope = false,
                scopeZoom = 1.3f,
                fireSound = SoundEffect.PISTOL_FIRE,
                description = "Pistola pesada de grado militar aeroespacial con puntero láser integrado y munición perforante.",
                price = 0,
                sciFiTier = "Sidearm Specialist",
                laserColor = 0xFF00E5FF
            ),
            "knife" to Weapon(
                id = "knife",
                name = "Cyber-Katana Nano-Edge",
                type = WeaponType.CYBER_BLADE,
                baseDamage = 125f,
                fireRateMs = 380L,
                maxMagazine = 1,
                currentAmmo = 1,
                reserveAmmo = 1,
                reloadTimeMs = 0L,
                recoilKick = 0f,
                baseSpread = 0f,
                hasScope = false,
                scopeZoom = 1.0f,
                fireSound = SoundEffect.CYBER_SLASH,
                description = "Hoja táctica de nanocarbono con filo térmico azul cyan vibrante. Bajas sigilosas letales de un solo tajo.",
                price = 1500,
                sciFiTier = "Stealth SpecOps",
                laserColor = 0xFF00FFFF
            )
        )
    }
}
