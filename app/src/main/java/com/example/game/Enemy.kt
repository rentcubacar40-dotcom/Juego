package com.example.game

enum class EnemyType(
    val title: String,
    val maxHp: Float,
    val moveSpeed: Float,
    val damage: Float,
    val fireIntervalMs: Long,
    val attackRange: Float,
    val colorPrimary: Long,
    val colorAccent: Long
) {
    RECON("Recon Scout", 50f, 1.35f, 10f, 650L, 8.0f, 0xFF4B5563, 0xFFF59E0B),
    MERCENARY("SpecOps Merc", 80f, 1.0f, 16f, 500L, 10.0f, 0xFF1E293B, 0xFFEF4444),
    SNIPER("Ghost Sniper", 45f, 0.7f, 38f, 1600L, 14.0f, 0xFF334155, 0xFF06B6D4),
    COMMANDER("Warlord Boris", 220f, 0.85f, 22f, 400L, 9.0f, 0xFF111827, 0xFFDC2626)
}

enum class EnemyState {
    PATROL,
    ALERT,
    COMBAT,
    HURT,
    DEAD
}

data class Enemy(
    val id: Int,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var angle: Float = 0f,
    var health: Float = type.maxHp,
    var state: EnemyState = EnemyState.PATROL,
    var lastShotTime: Long = 0L,
    var alertTimer: Float = 0f,
    var hurtTimer: Float = 0f,
    var deathProgress: Float = 0f,
    var patrolWaypoints: List<Pair<Float, Float>> = emptyList(),
    var currentWaypointIndex: Int = 0,
    var isAimingAtPlayer: Boolean = false,
    var aimLaserCharge: Float = 0f
) {
    val isDead: Boolean get() = state == EnemyState.DEAD

    fun takeDamage(damage: Float, isHeadshot: Boolean): Boolean {
        if (isDead) return false
        val totalDamage = if (isHeadshot) damage * 2.2f else damage
        health -= totalDamage
        if (health <= 0f) {
            health = 0f
            state = EnemyState.DEAD
            return true // Fatal
        } else {
            state = EnemyState.HURT
            hurtTimer = 0.25f
        }
        return false
    }
}
