package com.example.game

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffect
import com.example.audio.SoundManager
import com.example.data.GameDatabase
import com.example.data.GameRepository
import com.example.data.entity.LevelProgress
import com.example.data.entity.PlayerProfile
import com.example.data.entity.WeaponEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import java.util.Random

enum class GameScreenState {
    MAIN_MENU,
    MISSION_SELECT,
    PLAYING,
    PAUSED,
    VICTORY_DEBRIEF,
    DEFEAT_DEBRIEF,
    ARMORY
}

enum class FireMode {
    FULL_AUTO,
    SEMI_AUTO
}

data class HitMarkerState(
    val visible: Boolean = false,
    val isHeadshot: Boolean = false,
    val timestamp: Long = 0L
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    val soundManager = SoundManager(application)
    private val repository: GameRepository
    private val random = Random()

    init {
        val db = GameDatabase.getDatabase(application)
        repository = GameRepository(db.gameDao())
    }

    val levels: StateFlow<List<LevelProgress>> = repository.levels.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val weapons: StateFlow<List<WeaponEntity>> = repository.weapons.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playerProfile: StateFlow<PlayerProfile?> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Current Navigation Screen
    private val _screenState = MutableStateFlow(GameScreenState.MAIN_MENU)
    val screenState: StateFlow<GameScreenState> = _screenState.asStateFlow()

    // Active Mission
    private val _currentLevel = MutableStateFlow<LevelMission?>(null)
    val currentLevel: StateFlow<LevelMission?> = _currentLevel.asStateFlow()

    // Player state
    var playerX = 1.5f
    var playerY = 1.5f
    var playerAngle = 0f
    var playerPitch = 0f
    var playerHealth = 100f
    val maxPlayerHealth = 100f
    var playerArmor = 50f
    val maxPlayerArmor = 100f
    var stamina = 100f

    var isCrouching = false
    var isSprinting = false
    var isAimingDownSights = false
    var adsProgress = 0f
    var nightVision = false
    var headBob = 0f
    var headBobTimer = 0f
    var crouchOffset = 0f

    // Weapon & Gunplay state
    private val baseArsenal = Weapon.createArsenal()
    val availableWeapons = mutableMapOf<String, Weapon>()
    private val _currentWeapon = MutableStateFlow(baseArsenal["m4a1"]!!)
    val currentWeapon: StateFlow<Weapon> = _currentWeapon.asStateFlow()

    var isFiring = false
    var fireMode = FireMode.FULL_AUTO
    var recoilOffset = Offset.Zero
    var isShootingFlash = false
    var isReloading = false
    var reloadProgress = 0f
    private var lastShotTime = 0L
    private var reloadStartTime = 0L

    // Hit reaction & Combat HUD
    private val _hitMarker = MutableStateFlow(HitMarkerState())
    val hitMarker: StateFlow<HitMarkerState> = _hitMarker.asStateFlow()

    var damageVignetteAlpha = 0f
    var damageIndicatorAngle: Float? = null

    // Game world entities
    val activeEnemies = mutableListOf<Enemy>()
    val activeItems = mutableListOf<WorldItem>()
    val activeEffects = mutableListOf<VisualEffect>()

    // Mission Stats
    var missionKills = 0
    var missionHeadshots = 0
    var shotsFired = 0
    var shotsHit = 0
    var missionTimeSeconds = 0
    var intelCollected = false
    var survivalWave = 1

    // Controls input
    private var moveStickX = 0f
    private var moveStickY = 0f
    var lookSensitivity = 1.0f

    private var gameLoopJob: Job? = null
    private var lastHeartbeatTime = 0L

    fun navigateTo(screen: GameScreenState) {
        _screenState.value = screen
        if (screen != GameScreenState.PLAYING) {
            isFiring = false
        }
    }

    fun startMission(levelId: Int) {
        val level = LevelData.getLevel(levelId)
        _currentLevel.value = level

        // Setup weapon based on player's equipped weapon
        initArsenal()
        val equippedId = playerProfile.value?.equippedWeaponId ?: "m4a1"
        _currentWeapon.value = availableWeapons[equippedId] ?: availableWeapons["m4a1"]!!

        // Reset player stats
        playerX = level.playerStartX
        playerY = level.playerStartY
        playerAngle = level.playerStartAngle
        playerPitch = 0f
        playerHealth = 100f
        playerArmor = 50f
        stamina = 100f
        isCrouching = false
        isSprinting = false
        isAimingDownSights = false
        adsProgress = 0f
        nightVision = level.isNightOps
        recoilOffset = Offset.Zero
        damageVignetteAlpha = 0f
        isReloading = false

        missionKills = 0
        missionHeadshots = 0
        shotsFired = 0
        shotsHit = 0
        missionTimeSeconds = 0
        intelCollected = false
        survivalWave = 1

        // Deep copy enemies & items for the run
        activeEnemies.clear()
        for (e in level.enemies) {
            activeEnemies.add(e.copy(patrolWaypoints = e.patrolWaypoints.toList()))
        }
        activeItems.clear()
        for (it in level.items) {
            activeItems.add(it.copy())
        }
        activeEffects.clear()

        _screenState.value = GameScreenState.PLAYING
        startGameLoop()
    }

    private fun initArsenal() {
        availableWeapons.clear()
        val profileWeapons = weapons.value
        for ((id, baseWeapon) in baseArsenal) {
            val weaponEntity = profileWeapons.find { it.weaponId == id }
            val dmgMult = 1.0f + (weaponEntity?.damageLevel?.minus(1) ?: 0) * 0.15f
            val capMult = 1.0f + (weaponEntity?.capacityLevel?.minus(1) ?: 0) * 0.20f
            val stabMult = 1.0f + (weaponEntity?.stabilityLevel?.minus(1) ?: 0) * 0.20f
            availableWeapons[id] = baseWeapon.copyWeapon(dmgMult, capMult, stabMult)
        }
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            var secondAccumulator = 0f

            while (isActive) {
                val now = System.currentTimeMillis()
                val dt = ((now - lastTime) / 1000f).coerceIn(0.005f, 0.05f)
                lastTime = now

                if (_screenState.value == GameScreenState.PLAYING) {
                    secondAccumulator += dt
                    if (secondAccumulator >= 1.0f) {
                        missionTimeSeconds++
                        secondAccumulator -= 1.0f
                    }
                    updateGameTick(dt, now)
                }

                delay(16L) // Target ~60 FPS
            }
        }
    }

    private fun updateGameTick(dt: Float, now: Long) {
        val level = _currentLevel.value ?: return

        // 1. Handle player movement
        updatePlayerMovement(dt, level.map, level.mapWidth, level.mapHeight)

        // 2. ADS smooth interpolation
        val targetAds = if (isAimingDownSights) 1.0f else 0.0f
        adsProgress += (targetAds - adsProgress) * (dt * 12f)

        // 3. Crouch height interpolation
        val targetCrouch = if (isCrouching) 55f else 0f
        crouchOffset += (targetCrouch - crouchOffset) * (dt * 10f)

        // 4. Recoil spring recovery
        recoilOffset = Offset(
            recoilOffset.x * (1f - dt * 14f),
            recoilOffset.y * (1f - dt * 14f)
        )

        // 5. Automatic weapon fire handling
        if (isFiring && (fireMode == FireMode.FULL_AUTO || currentWeapon.value.type == WeaponType.ASSAULT_RIFLE)) {
            tryFireWeapon(now)
        }

        // 6. Reload handling
        if (isReloading) {
            val elapsed = now - reloadStartTime
            val duration = currentWeapon.value.reloadTimeMs
            reloadProgress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            if (elapsed >= duration) {
                completeReload()
            }
        }

        // 7. Update Enemy AI
        updateEnemies(dt, now, level.map, level.mapWidth, level.mapHeight)

        // 8. Visual effects decay
        val itEffect = activeEffects.iterator()
        while (itEffect.hasNext()) {
            val effect = itEffect.next()
            effect.lifetime -= dt
            if (effect.lifetime <= 0f) {
                itEffect.remove()
            }
        }

        // 9. Damage vignette decay & Low HP Heartbeat
        damageVignetteAlpha = (damageVignetteAlpha - dt * 1.5f).coerceAtLeast(0f)
        if (playerHealth in 1f..30f && now - lastHeartbeatTime > 800L) {
            lastHeartbeatTime = now
            soundManager.play(SoundEffect.HEARTBEAT)
            soundManager.vibrateHurt()
            damageVignetteAlpha = 0.45f
        }

        // 10. Check item collection (walking near items)
        checkItemPickups()

        // 11. Check Endless Survival wave progression
        if (level.isEndless && activeEnemies.all { it.isDead }) {
            spawnSurvivalWave()
        }

        // 12. Check Victory / Defeat conditions
        checkGameEndConditions(level)
    }

    private fun updatePlayerMovement(dt: Float, map: Array<IntArray>, mapWidth: Int, mapHeight: Int) {
        val isMoving = abs(moveStickX) > 0.1f || abs(moveStickY) > 0.1f
        var speed = if (isCrouching) 1.8f else 3.2f

        if (isSprinting && isMoving && stamina > 5f && !isCrouching) {
            speed *= 1.6f
            stamina = (stamina - dt * 25f).coerceAtLeast(0f)
        } else {
            stamina = (stamina + dt * 18f).coerceAtMost(100f)
        }

        if (isMoving) {
            // Head bobbing calculation
            headBobTimer += dt * (if (isSprinting) 16f else 10f)
            headBob = sin(headBobTimer) * (if (isSprinting) 7f else 4f)

            // Strafe and forward vectors
            val forwardX = cos(playerAngle)
            val forwardY = sin(playerAngle)
            val rightX = -sin(playerAngle)
            val rightY = cos(playerAngle)

            val dirX = forwardX * moveStickY + rightX * moveStickX
            val dirY = forwardY * moveStickY + rightY * moveStickX
            val length = sqrt(dirX * dirX + dirY * dirY)

            if (length > 0.01f) {
                val normX = dirX / length
                val normY = dirY / length

                val nextX = playerX + normX * speed * dt
                val nextY = playerY + normY * speed * dt

                // Collision with wall tiles (margin of 0.22)
                val margin = 0.25f
                if (!isWallTile(nextX + margin * normX, playerY, map, mapWidth, mapHeight) &&
                    !isWallTile(nextX - margin * normX, playerY, map, mapWidth, mapHeight)) {
                    playerX = nextX
                }
                if (!isWallTile(playerX, nextY + margin * normY, map, mapWidth, mapHeight) &&
                    !isWallTile(playerX, nextY - margin * normY, map, mapWidth, mapHeight)) {
                    playerY = nextY
                }
            }
        } else {
            headBob *= 0.85f
        }
    }

    private fun isWallTile(x: Float, y: Float, map: Array<IntArray>, width: Int, height: Int): Boolean {
        val mx = x.toInt()
        val my = y.toInt()
        if (mx !in 0 until width || my !in 0 until height) return true
        return map[my][mx] > 0
    }

    private fun updateEnemies(dt: Float, now: Long, map: Array<IntArray>, mapWidth: Int, mapHeight: Int) {
        for (enemy in activeEnemies) {
            if (enemy.isDead) continue

            // Hurt recovery
            if (enemy.hurtTimer > 0f) {
                enemy.hurtTimer -= dt
                if (enemy.hurtTimer <= 0f) {
                    enemy.state = EnemyState.COMBAT
                }
            }

            val dx = playerX - enemy.x
            val dy = playerY - enemy.y
            val distToPlayer = sqrt(dx * dx + dy * dy)
            val angleToPlayer = atan2(dy, dx)
            enemy.angle = angleToPlayer

            // Check Line of Sight through walls
            val hasLos = hasLineOfSight(enemy.x, enemy.y, playerX, playerY, map, mapWidth, mapHeight)

            if (hasLos && distToPlayer <= enemy.type.attackRange) {
                if (enemy.state != EnemyState.COMBAT) {
                    enemy.state = EnemyState.COMBAT
                    soundManager.play(SoundEffect.ENEMY_ALERT)
                }
                enemy.isAimingAtPlayer = true

                // Sniper Laser sight charging
                if (enemy.type == EnemyType.SNIPER) {
                    enemy.aimLaserCharge = (enemy.aimLaserCharge + dt * 1.5f).coerceAtMost(1.0f)
                }

                // Shoot at player
                if (now - enemy.lastShotTime > enemy.type.fireIntervalMs) {
                    if (enemy.type != EnemyType.SNIPER || enemy.aimLaserCharge >= 0.95f) {
                        enemyShootPlayer(enemy, distToPlayer, now)
                        enemy.lastShotTime = now
                        enemy.aimLaserCharge = 0f
                    }
                }
            } else {
                enemy.isAimingAtPlayer = false
                enemy.aimLaserCharge = 0f
                // Patrol movement if not in active combat
                if (enemy.patrolWaypoints.isNotEmpty()) {
                    val targetWp = enemy.patrolWaypoints[enemy.currentWaypointIndex]
                    val wdx = targetWp.first - enemy.x
                    val wdy = targetWp.second - enemy.y
                    val wdist = sqrt(wdx * wdx + wdy * wdy)
                    if (wdist < 0.25f) {
                        enemy.currentWaypointIndex = (enemy.currentWaypointIndex + 1) % enemy.patrolWaypoints.size
                    } else {
                        val moveSpeed = enemy.type.moveSpeed * 0.6f * dt
                        enemy.x += (wdx / wdist) * moveSpeed
                        enemy.y += (wdy / wdist) * moveSpeed
                    }
                }
            }
        }
    }

    private fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float, map: Array<IntArray>, width: Int, height: Int): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val steps = maxOf(abs(dx), abs(dy)) * 3f
        if (steps <= 0f) return true
        val stepX = dx / steps
        val stepY = dy / steps

        var curX = x1
        var curY = y1
        for (i in 0 until steps.toInt()) {
            curX += stepX
            curY += stepY
            val mx = curX.toInt()
            val my = curY.toInt()
            if (mx in 0 until width && my in 0 until height) {
                if (map[my][mx] > 0) return false
            }
        }
        return true
    }

    private fun enemyShootPlayer(enemy: Enemy, dist: Float, now: Long) {
        // Accuracy calculation based on distance and crouching
        val baseAccuracy = if (isCrouching) 0.55f else 0.85f
        val hitChance = (baseAccuracy - dist * 0.03f).coerceIn(0.25f, 0.9f)

        if (random.nextFloat() < hitChance) {
            // Apply damage to armor first, then health
            var damage = enemy.type.damage
            if (playerArmor > 0f) {
                val absorbed = damage * 0.65f
                playerArmor = (playerArmor - absorbed).coerceAtLeast(0f)
                damage -= absorbed
            }
            playerHealth = (playerHealth - damage).coerceAtLeast(0f)

            // Audio & Haptic feedback
            soundManager.play(SoundEffect.PLAYER_HURT)
            soundManager.vibrateHurt()
            damageVignetteAlpha = 0.7f

            // Directional damage indicator
            val angleDiff = enemy.angle - playerAngle
            damageIndicatorAngle = angleDiff
        }
    }

    fun onFirePress() {
        isFiring = true
        tryFireWeapon(System.currentTimeMillis())
    }

    fun onFireRelease() {
        isFiring = false
    }

    private fun tryFireWeapon(now: Long) {
        val weapon = currentWeapon.value
        if (isReloading || now - lastShotTime < weapon.fireRateMs) return

        if (weapon.currentAmmo <= 0) {
            soundManager.play(SoundEffect.DRY_FIRE)
            lastShotTime = now
            return
        }

        // Deduct ammo & trigger shot sound & recoil
        weapon.currentAmmo--
        lastShotTime = now
        shotsFired++

        soundManager.play(weapon.fireSound)
        soundManager.vibrateRecoil()

        // Recoil kickback
        val kick = weapon.recoilKick * (if (isCrouching) 0.65f else 1.0f) * (if (isAimingDownSights) 0.55f else 1.0f)
        recoilOffset = Offset((random.nextFloat() - 0.5f) * kick * 2f, -kick * 3.5f)
        isShootingFlash = true

        viewModelScope.launch {
            delay(50L)
            isShootingFlash = false
        }

        // Perform Hitscan Raycast
        performPlayerHitscan(weapon)

        // Alert nearby enemies to gunfire noise
        alertNearbyEnemies()
    }

    private fun performPlayerHitscan(weapon: Weapon) {
        val level = _currentLevel.value ?: return
        val spread = weapon.baseSpread * (if (isAimingDownSights) 0.25f else 1.0f) * (if (isCrouching) 0.5f else 1.0f)
        val shotAngle = playerAngle + (random.nextFloat() - 0.5f) * spread

        var closestHitDist = 20.0f
        var hitEnemy: Enemy? = null
        var hitHeadshot = false
        var hitItem: WorldItem? = null

        // 1. Raycast wall limit
        val cosA = cos(shotAngle)
        val sinA = sin(shotAngle)

        // 2. Check enemy hits
        for (enemy in activeEnemies) {
            if (enemy.isDead) continue
            val ex = enemy.x - playerX
            val ey = enemy.y - playerY
            val enemyDist = sqrt(ex * ex + ey * ey)
            if (enemyDist > closestHitDist) continue

            val enemyAngle = atan2(ey, ex)
            var angleDiff = abs(shotAngle - enemyAngle)
            while (angleDiff > PI) angleDiff = (abs(angleDiff - 2 * PI)).toFloat()

            val angularRadius = 0.35f / enemyDist
            if (angleDiff < angularRadius) {
                // Check if wall is blocking between player and enemy
                if (hasLineOfSight(playerX, playerY, enemy.x, enemy.y, level.map, level.mapWidth, level.mapHeight)) {
                    closestHitDist = enemyDist
                    hitEnemy = enemy
                    // If aim pitch is high or tight center, headshot!
                    hitHeadshot = (angleDiff < angularRadius * 0.45f) || (weapon.hasScope && isAimingDownSights)
                }
            }
        }

        // 3. Check explosive barrels and props
        for (item in activeItems) {
            if (item.type != ItemType.EXPLOSIVE_BARREL || item.exploded) continue
            val ix = item.x - playerX
            val iy = item.y - playerY
            val itemDist = sqrt(ix * ix + iy * iy)
            if (itemDist > closestHitDist) continue

            val itemAngle = atan2(iy, ix)
            var angleDiff = abs(shotAngle - itemAngle)
            while (angleDiff > PI) angleDiff = (abs(angleDiff - 2 * PI)).toFloat()

            if (angleDiff < 0.35f / itemDist) {
                if (hasLineOfSight(playerX, playerY, item.x, item.y, level.map, level.mapWidth, level.mapHeight)) {
                    closestHitDist = itemDist
                    hitItem = item
                    hitEnemy = null
                }
            }
        }

        // Spawn projectile visual effects
        if (weapon.type == WeaponType.PLASMA_RAILGUN) {
            val boltX = playerX + cosA * (closestHitDist * 0.5f)
            val boltY = playerY + sinA * (closestHitDist * 0.5f)
            activeEffects.add(
                VisualEffect(
                    id = System.nanoTime(),
                    x = boltX,
                    y = boltY,
                    z = 0f,
                    type = EffectType.PLASMA_BOLT,
                    lifetime = 0.28f,
                    maxLifetime = 0.28f,
                    size = 1.6f
                )
            )
        }

        // Apply hit outcome
        if (hitEnemy != null) {
            shotsHit++
            val fatal = hitEnemy.takeDamage(weapon.baseDamage, hitHeadshot)
            if (hitHeadshot) {
                soundManager.play(SoundEffect.HIT_HEADSHOT)
                _hitMarker.value = HitMarkerState(visible = true, isHeadshot = true, timestamp = System.currentTimeMillis())
                if (fatal) {
                    missionKills++
                    missionHeadshots++
                }
            } else {
                soundManager.play(SoundEffect.HIT_BODY)
                _hitMarker.value = HitMarkerState(visible = true, isHeadshot = false, timestamp = System.currentTimeMillis())
                if (fatal) {
                    missionKills++
                }
            }

            // Spawn blood effect or cyber shield burst
            activeEffects.add(
                VisualEffect(
                    id = System.nanoTime(),
                    x = hitEnemy.x,
                    y = hitEnemy.y,
                    z = 0f,
                    type = if (weapon.type == WeaponType.PLASMA_RAILGUN) EffectType.ENERGY_SHIELD_BURST else EffectType.BLOOD_SPLATTER,
                    lifetime = 0.35f,
                    maxLifetime = 0.35f
                )
            )

            // Auto-hide hitmarker after 180ms
            viewModelScope.launch {
                delay(180L)
                _hitMarker.value = HitMarkerState(visible = false)
            }
        } else if (hitItem != null) {
            shotsHit++
            explodeBarrel(hitItem, level)
        } else {
            // Spawn wall sparks at shot impact point
            val hitX = playerX + cosA * 4.0f
            val hitY = playerY + sinA * 4.0f
            activeEffects.add(
                VisualEffect(
                    id = System.nanoTime(),
                    x = hitX,
                    y = hitY,
                    z = 0f,
                    type = if (weapon.type == WeaponType.PLASMA_RAILGUN) EffectType.ENERGY_SHIELD_BURST else EffectType.WALL_SPARK,
                    lifetime = 0.25f,
                    maxLifetime = 0.25f
                )
            )
        }
    }

    private fun explodeBarrel(barrel: WorldItem, level: LevelMission) {
        barrel.exploded = true
        soundManager.play(SoundEffect.EXPLOSION)
        soundManager.vibrateExplosion()

        // Spawn explosion effect
        activeEffects.add(
            VisualEffect(
                id = System.nanoTime(),
                x = barrel.x,
                y = barrel.y,
                z = 0f,
                type = EffectType.EXPLOSION_BLAST,
                lifetime = 0.65f,
                maxLifetime = 0.65f,
                size = 2.5f
            )
        )

        // Damage all enemies in blast radius (3.2 units)
        for (enemy in activeEnemies) {
            if (enemy.isDead) continue
            val dist = sqrt((enemy.x - barrel.x) * (enemy.x - barrel.x) + (enemy.y - barrel.y) * (enemy.y - barrel.y))
            if (dist < 3.2f) {
                val blastDamage = 140f * (1f - dist / 3.2f)
                val fatal = enemy.takeDamage(blastDamage, false)
                if (fatal) missionKills++
            }
        }

        // Damage player if caught in blast radius
        val pDist = sqrt((playerX - barrel.x) * (playerX - barrel.x) + (playerY - barrel.y) * (playerY - barrel.y))
        if (pDist < 3.0f) {
            val pDamage = 60f * (1f - pDist / 3.0f)
            playerHealth = (playerHealth - pDamage).coerceAtLeast(0f)
            damageVignetteAlpha = 0.9f
            soundManager.play(SoundEffect.PLAYER_HURT)
        }
    }

    private fun alertNearbyEnemies() {
        for (enemy in activeEnemies) {
            if (enemy.isDead) continue
            val dist = sqrt((enemy.x - playerX) * (enemy.x - playerX) + (enemy.y - playerY) * (enemy.y - playerY))
            if (dist < 7.5f) {
                enemy.state = EnemyState.COMBAT
            }
        }
    }

    fun onReloadPress() {
        val weapon = currentWeapon.value
        if (isReloading || weapon.currentAmmo == weapon.maxMagazine || weapon.reserveAmmo <= 0) return
        isReloading = true
        reloadStartTime = System.currentTimeMillis()
        soundManager.play(SoundEffect.RELOAD_START)
    }

    private fun completeReload() {
        isReloading = false
        reloadProgress = 0f
        val weapon = currentWeapon.value
        val needed = weapon.maxMagazine - weapon.currentAmmo
        val toAdd = minOf(needed, weapon.reserveAmmo)
        weapon.currentAmmo += toAdd
        weapon.reserveAmmo -= toAdd
        soundManager.play(SoundEffect.RELOAD_FINISH)
    }

    fun toggleAds() {
        isAimingDownSights = !isAimingDownSights
    }

    fun toggleCrouch() {
        isCrouching = !isCrouching
    }

    fun toggleSprint() {
        isSprinting = !isSprinting
    }

    fun toggleNightVision() {
        nightVision = !nightVision
        soundManager.play(SoundEffect.PICKUP)
    }

    fun toggleFireMode() {
        fireMode = if (fireMode == FireMode.FULL_AUTO) FireMode.SEMI_AUTO else FireMode.FULL_AUTO
        soundManager.play(SoundEffect.PICKUP)
    }

    fun switchWeapon(weaponId: String) {
        val weapon = availableWeapons[weaponId] ?: return
        _currentWeapon.value = weapon
        isReloading = false
        reloadProgress = 0f
        soundManager.play(SoundEffect.RELOAD_START)
    }

    fun onMoveInput(x: Float, y: Float) {
        moveStickX = x.coerceIn(-1f, 1f)
        moveStickY = y.coerceIn(-1f, 1f)
    }

    fun onLookSwipe(deltaX: Float, deltaY: Float) {
        val sens = 0.004f * lookSensitivity
        playerAngle += deltaX * sens
        while (playerAngle > PI * 2) playerAngle -= (PI * 2).toFloat()
        while (playerAngle < 0) playerAngle += (PI * 2).toFloat()

        playerPitch = (playerPitch - deltaY * (0.6f * lookSensitivity)).coerceIn(-140f, 140f)
    }

    private fun checkItemPickups() {
        for (item in activeItems) {
            if (item.collected || (item.type == ItemType.EXPLOSIVE_BARREL && item.exploded)) continue
            val dist = sqrt((playerX - item.x) * (playerX - item.x) + (playerY - item.y) * (playerY - item.y))
            if (dist < 0.85f) {
                when (item.type) {
                    ItemType.MEDKIT -> {
                        if (playerHealth < maxPlayerHealth) {
                            playerHealth = (playerHealth + 45f).coerceAtMost(maxPlayerHealth)
                            item.collected = true
                            soundManager.play(SoundEffect.PICKUP)
                        }
                    }
                    ItemType.ARMOR_VEST -> {
                        if (playerArmor < maxPlayerArmor) {
                            playerArmor = maxPlayerArmor
                            item.collected = true
                            soundManager.play(SoundEffect.PICKUP)
                        }
                    }
                    ItemType.AMMO_CRATE -> {
                        val weapon = currentWeapon.value
                        weapon.reserveAmmo += weapon.maxMagazine * 3
                        item.collected = true
                        soundManager.play(SoundEffect.PICKUP)
                    }
                    ItemType.MISSION_INTEL -> {
                        intelCollected = true
                        item.collected = true
                        soundManager.play(SoundEffect.OBJECTIVE_COMPLETE)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun spawnSurvivalWave() {
        survivalWave++
        soundManager.play(SoundEffect.OBJECTIVE_COMPLETE)

        // Spawn next wave of hostiles around the arena corners
        val spawnCoords = listOf(
            2.5f to 2.5f,
            13.5f to 2.5f,
            2.5f to 13.5f,
            13.5f to 13.5f
        )
        val enemyCount = 3 + survivalWave
        for (i in 0 until enemyCount) {
            val coord = spawnCoords[i % spawnCoords.size]
            val type = when {
                survivalWave >= 4 && i == 0 -> EnemyType.COMMANDER
                survivalWave >= 2 && i % 2 == 1 -> EnemyType.SNIPER
                i % 2 == 0 -> EnemyType.MERCENARY
                else -> EnemyType.RECON
            }
            activeEnemies.add(
                Enemy(
                    id = System.currentTimeMillis().toInt() + i,
                    type = type,
                    x = coord.first + (random.nextFloat() - 0.5f),
                    y = coord.second + (random.nextFloat() - 0.5f),
                    angle = 0f
                )
            )
        }
    }

    private fun checkGameEndConditions(level: LevelMission) {
        // Defeat: Player health 0
        if (playerHealth <= 0f) {
            _screenState.value = GameScreenState.DEFEAT_DEBRIEF
            soundManager.play(SoundEffect.PLAYER_HURT)
            return
        }

        // Victory condition:
        // In Campaign: All enemies dead and intel collected (if required)
        if (!level.isEndless) {
            val allEnemiesDefeated = activeEnemies.all { it.isDead }
            val intelConditionMet = !level.items.any { it.type == ItemType.MISSION_INTEL } || intelCollected

            if (allEnemiesDefeated && intelConditionMet) {
                // Mission Accomplished!
                _screenState.value = GameScreenState.VICTORY_DEBRIEF
                soundManager.play(SoundEffect.OBJECTIVE_COMPLETE)

                // Calculate stars & score
                var stars = 1
                if (missionHeadshots >= 3 || missionKills >= level.targetKills) stars++
                if (missionTimeSeconds < 120 && playerHealth > 40f) stars++

                val accuracyBonus = if (shotsFired > 0) ((shotsHit.toFloat() / shotsFired) * 1000).toInt() else 0
                val totalScore = (missionKills * 350) + (missionHeadshots * 500) + (maxOf(0, 180 - missionTimeSeconds) * 20) + accuracyBonus

                viewModelScope.launch {
                    repository.saveMissionResult(
                        levelId = level.id,
                        score = totalScore,
                        timeSeconds = missionTimeSeconds,
                        kills = missionKills,
                        headshots = missionHeadshots,
                        starsEarned = stars,
                        creditsReward = level.creditReward
                    )
                }
            }
        }
    }

    // Armory Store interactions
    fun buyWeapon(weaponId: String, cost: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.unlockWeapon(weaponId, cost)
            if (success) {
                initArsenal()
                soundManager.play(SoundEffect.PICKUP)
            }
            onResult(success)
        }
    }

    fun upgradeWeaponStat(weaponId: String, upgradeType: String, cost: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.upgradeWeapon(weaponId, upgradeType, cost)
            if (success) {
                initArsenal()
                soundManager.play(SoundEffect.PICKUP)
            }
            onResult(success)
        }
    }

    fun equipWeapon(weaponId: String) {
        viewModelScope.launch {
            repository.equipWeapon(weaponId)
            initArsenal()
            _currentWeapon.value = availableWeapons[weaponId] ?: _currentWeapon.value
            soundManager.play(SoundEffect.RELOAD_FINISH)
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}
