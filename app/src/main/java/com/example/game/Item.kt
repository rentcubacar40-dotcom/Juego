package com.example.game

enum class ItemType {
    EXPLOSIVE_BARREL,
    MEDKIT,
    AMMO_CRATE,
    MISSION_INTEL,
    ARMOR_VEST
}

data class WorldItem(
    val id: Int,
    val type: ItemType,
    var x: Float,
    var y: Float,
    var health: Float = if (type == ItemType.EXPLOSIVE_BARREL) 25f else 1f,
    var collected: Boolean = false,
    var exploded: Boolean = false
)

data class VisualEffect(
    val id: Long,
    val x: Float,
    val y: Float,
    val z: Float,
    val type: EffectType,
    var lifetime: Float,
    val maxLifetime: Float,
    val size: Float = 1.0f
)

enum class EffectType {
    MUZZLE_FLASH,
    BLOOD_SPLATTER,
    WALL_SPARK,
    EXPLOSION_BLAST,
    SMOKE_PUFF,
    PLASMA_BOLT,
    ION_TRAIL,
    ENERGY_SHIELD_BURST
}
