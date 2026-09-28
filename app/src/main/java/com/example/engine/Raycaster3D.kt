package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.EffectType
import com.example.game.Enemy
import com.example.game.EnemyState
import com.example.game.EnemyType
import com.example.game.ItemType
import com.example.game.VisualEffect
import com.example.game.Weapon
import com.example.game.WeaponType
import com.example.game.WorldItem
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Raycaster3D {
    val fov = 1.15f // Field of view in radians (~66 degrees)
    val maxDepth = 18.0f // Maximum raycast rendering distance

    // Raycast hit information
    private class RayHit(
        var dist: Float = 0f,
        var wallType: Int = 0,
        var side: Int = 0, // 0 for vertical (X), 1 for horizontal (Y)
        var hitOffset: Float = 0f // 0..1 along the wall face
    )

    // Pre-allocated array of hits to avoid GC churn in game loop
    private val hits = Array(160) { RayHit() }
    private val zBuffer = FloatArray(160)

    fun render(
        drawScope: DrawScope,
        map: Array<IntArray>,
        mapWidth: Int,
        mapHeight: Int,
        playerX: Float,
        playerY: Float,
        playerAngle: Float,
        playerPitch: Float, // up/down tilt in pixels
        crouchOffset: Float, // height adjustment in pixels
        headBob: Float, // head bobbing offset
        nightVision: Boolean,
        enemies: List<Enemy>,
        items: List<WorldItem>,
        effects: List<VisualEffect>,
        currentWeapon: Weapon,
        isAimingDownSights: Boolean,
        adsProgress: Float, // 0..1
        recoilOffset: Offset,
        isShootingFlash: Boolean,
        reloadProgress: Float // 0..1 (0 when not reloading)
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 0f || height <= 0f) return

        val numRays = 140
        val columnWidth = width / numRays
        val halfHeight = height / 2f + playerPitch + crouchOffset + headBob

        // 1. Draw Floor & Ceiling
        renderSkyAndFloor(drawScope, width, height, halfHeight, nightVision)

        // 2. Cast Rays for Walls
        val halfFov = fov / 2f
        val wallProjScale = (width / 2f) / kotlin.math.tan(halfFov)

        for (i in 0 until numRays) {
            val rayAngle = playerAngle - halfFov + (i.toFloat() / numRays) * fov
            val hit = castRay(playerX, playerY, rayAngle, map, mapWidth, mapHeight)
            hits[i].dist = hit.dist
            hits[i].wallType = hit.wallType
            hits[i].side = hit.side
            hits[i].hitOffset = hit.hitOffset

            // Correct fish-eye distortion
            val correctedDist = max(0.15f, hit.dist * cos(rayAngle - playerAngle))
            zBuffer[i] = correctedDist

            // Calculate projected wall height
            val wallHeight = (wallProjScale / correctedDist)
            val wallTop = halfHeight - wallHeight / 2f
            val wallBottom = halfHeight + wallHeight / 2f

            // Wall color calculation with lighting and distance fog
            val (baseColor, accentColor) = getWallColors(hit.wallType, hit.side, nightVision)
            val fog = (1f - (correctedDist / maxDepth)).coerceIn(0.12f, 1f)

            // Modulate color by fog
            val r = ((baseColor.red * fog)).coerceIn(0f, 1f)
            val g = ((baseColor.green * fog)).coerceIn(0f, 1f)
            val b = ((baseColor.blue * fog)).coerceIn(0f, 1f)

            // Draw primary wall column
            drawScope.drawRect(
                color = Color(r, g, b, 1f),
                topLeft = Offset(i * columnWidth, wallTop.coerceAtLeast(0f)),
                size = Size(columnWidth + 1.2f, (wallBottom - wallTop).coerceAtMost(height))
            )

            // Render high-detail procedural wall features (stripes, panels, terminals)
            renderWallFeatures(
                drawScope = drawScope,
                wallType = hit.wallType,
                x = i * columnWidth,
                width = columnWidth + 1.2f,
                top = wallTop,
                height = wallHeight,
                hitOffset = hit.hitOffset,
                fog = fog,
                nightVision = nightVision
            )
        }

        // 3. Render 3D Sprites (Items, Barrels, Enemies, Effects)
        renderWorldSprites(
            drawScope = drawScope,
            screenWidth = width,
            screenHeight = height,
            halfHeight = halfHeight,
            wallProjScale = wallProjScale,
            numRays = numRays,
            columnWidth = columnWidth,
            playerX = playerX,
            playerY = playerY,
            playerAngle = playerAngle,
            nightVision = nightVision,
            enemies = enemies,
            items = items,
            effects = effects
        )

        // 4. Render Night Vision Scanlines & Grain if enabled
        if (nightVision) {
            renderNightVisionOverlay(drawScope, width, height)
        }

        // 5. Render First-Person Gun Model on screen
        renderFirstPersonWeapon(
            drawScope = drawScope,
            width = width,
            height = height,
            weapon = currentWeapon,
            adsProgress = adsProgress,
            recoilOffset = recoilOffset,
            isShootingFlash = isShootingFlash,
            reloadProgress = reloadProgress,
            nightVision = nightVision
        )
    }

    private fun castRay(
        px: Float,
        py: Float,
        angle: Float,
        map: Array<IntArray>,
        mapWidth: Int,
        mapHeight: Int
    ): RayHit {
        val rayDirX = cos(angle)
        val rayDirY = sin(angle)

        var mapX = floor(px).toInt()
        var mapY = floor(py).toInt()

        val deltaDistX = if (rayDirX == 0f) 1e30f else abs(1f / rayDirX)
        val deltaDistY = if (rayDirY == 0f) 1e30f else abs(1f / rayDirY)

        val stepX: Int
        var sideDistX: Float
        if (rayDirX < 0) {
            stepX = -1
            sideDistX = (px - mapX) * deltaDistX
        } else {
            stepX = 1
            sideDistX = (mapX + 1.0f - px) * deltaDistX
        }

        val stepY: Int
        var sideDistY: Float
        if (rayDirY < 0) {
            stepY = -1
            sideDistY = (py - mapY) * deltaDistY
        } else {
            stepY = 1
            sideDistY = (mapY + 1.0f - py) * deltaDistY
        }

        var hit = false
        var side = 0
        var wallType = 1
        var dist = 0f

        while (!hit && dist < maxDepth) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX
                mapX += stepX
                side = 0
            } else {
                sideDistY += deltaDistY
                mapY += stepY
                side = 1
            }

            if (mapX in 0 until mapWidth && mapY in 0 until mapHeight) {
                val tile = map[mapY][mapX]
                if (tile > 0) {
                    hit = true
                    wallType = tile
                }
            } else {
                hit = true
                wallType = 1
            }
        }

        val wallDist = if (side == 0) {
            (mapX - px + (1 - stepX) / 2f) / rayDirX
        } else {
            (mapY - py + (1 - stepY) / 2f) / rayDirY
        }

        var wallHitX = if (side == 0) py + wallDist * rayDirY else px + wallDist * rayDirX
        wallHitX -= floor(wallHitX)

        val result = RayHit()
        result.dist = max(0.1f, wallDist)
        result.wallType = wallType
        result.side = side
        result.hitOffset = wallHitX
        return result
    }

    private fun renderSkyAndFloor(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        horizonY: Float,
        nightVision: Boolean
    ) {
        val ceilingColor = if (nightVision) Color(0xFF031408) else Color(0xFF0D131D)
        val floorColor = if (nightVision) Color(0xFF06220E) else Color(0xFF141923)

        // Ceiling
        drawScope.drawRect(
            color = ceilingColor,
            topLeft = Offset(0f, 0f),
            size = Size(width, horizonY.coerceAtLeast(0f))
        )

        // Floor
        drawScope.drawRect(
            color = floorColor,
            topLeft = Offset(0f, horizonY.coerceAtLeast(0f)),
            size = Size(width, (height - horizonY).coerceAtLeast(0f))
        )

        // Tactical subtle floor perspective grid lines
        val gridColor = if (nightVision) Color(0x2200FF66) else Color(0x18F59E0B)
        for (i in 1..8) {
            val offset = (i * i * 3.5f)
            val lineY = horizonY + offset
            if (lineY in 0f..height) {
                drawScope.drawLine(
                    color = gridColor,
                    start = Offset(0f, lineY),
                    end = Offset(width, lineY),
                    strokeWidth = 1.0f
                )
            }
        }
    }

    private fun getWallColors(wallType: Int, side: Int, nightVision: Boolean): Pair<Color, Color> {
        val sideDim = if (side == 1) 0.72f else 1.0f

        if (nightVision) {
            val greenBase = Color(0xFF10B981)
            val dimmed = Color(
                greenBase.red * sideDim * 0.75f,
                greenBase.green * sideDim * 0.75f,
                greenBase.blue * sideDim * 0.75f,
                1f
            )
            return Pair(dimmed, Color(0xFF34D399))
        }

        return when (wallType) {
            1 -> { // Reinforced Concrete
                val base = Color(0xFF475569)
                Pair(
                    Color(base.red * sideDim, base.green * sideDim, base.blue * sideDim, 1f),
                    Color(0xFF64748B)
                )
            }
            2 -> { // High-Tech Server Rack
                val base = Color(0xFF1E293B)
                Pair(
                    Color(base.red * sideDim, base.green * sideDim, base.blue * sideDim, 1f),
                    Color(0xFF06B6D4)
                )
            }
            3 -> { // Military Storage Crate
                val base = Color(0xFF854D0E)
                Pair(
                    Color(base.red * sideDim, base.green * sideDim, base.blue * sideDim, 1f),
                    Color(0xFFCA8A04)
                )
            }
            4 -> { // Armored Blast Door with Hazard Stripes
                val base = Color(0xFF334155)
                Pair(
                    Color(base.red * sideDim, base.green * sideDim, base.blue * sideDim, 1f),
                    Color(0xFFF59E0B)
                )
            }
            5 -> { // Desert Sandstone
                val base = Color(0xFFB45309)
                Pair(
                    Color(base.red * sideDim, base.green * sideDim, base.blue * sideDim, 1f),
                    Color(0xFFD97706)
                )
            }
            else -> Pair(Color(0xFF334155), Color(0xFF94A3B8))
        }
    }

    private fun renderWallFeatures(
        drawScope: DrawScope,
        wallType: Int,
        x: Float,
        width: Float,
        top: Float,
        height: Float,
        hitOffset: Float,
        fog: Float,
        nightVision: Boolean
    ) {
        when (wallType) {
            1 -> { // Reinforced Military Concrete with panel seams & expansion joints
                // Vertical panel joint seam
                if (hitOffset in 0.48f..0.52f || hitOffset < 0.03f || hitOffset > 0.97f) {
                    drawScope.drawRect(
                        color = Color(0f, 0f, 0f, fog * 0.45f),
                        topLeft = Offset(x, top),
                        size = Size(width, height)
                    )
                }
                // Horizontal reinforcement groove & rebar line
                val seamY = top + height * 0.65f
                drawScope.drawRect(
                    color = Color(0f, 0f, 0f, fog * 0.4f),
                    topLeft = Offset(x, seamY),
                    size = Size(width, (height * 0.025f).coerceAtLeast(1.5f))
                )
            }
            2 -> { // High-Tech Server Rack with cooling vents & glowing status LEDs
                // Cooling ventilation horizontal grill slots
                val ventY = top + height * 0.15f
                val ventH = height * 0.25f
                if (((hitOffset * 25f).toInt() % 2 == 0) && hitOffset in 0.1f..0.9f) {
                    drawScope.drawRect(
                        color = Color(0f, 0f, 0f, fog * 0.5f),
                        topLeft = Offset(x, ventY),
                        size = Size(width, ventH)
                    )
                }
                // Server rack blinking LED slots
                if (hitOffset in 0.25f..0.75f) {
                    val ledY1 = top + height * 0.48f
                    val ledY2 = top + height * 0.62f
                    val ledColor = if (nightVision) Color(0xFF00FF66) else Color(0xFF00E5FF)
                    drawScope.drawRect(
                        color = Color(ledColor.red, ledColor.green, ledColor.blue, fog * 0.9f),
                        topLeft = Offset(x, ledY1),
                        size = Size(width, (height * 0.035f).coerceAtLeast(1.5f))
                    )
                    drawScope.drawRect(
                        color = Color(ledColor.red, ledColor.green, ledColor.blue, fog * 0.75f),
                        topLeft = Offset(x, ledY2),
                        size = Size(width, (height * 0.025f).coerceAtLeast(1.5f))
                    )
                }
            }
            3 -> { // Military Storage Crate with heavy metal corner braces & rivets
                if (hitOffset < 0.12f || hitOffset > 0.88f) {
                    val braceColor = if (nightVision) Color(0xFF0F3818) else Color(0xFF1E293B)
                    drawScope.drawRect(
                        color = Color(braceColor.red, braceColor.green, braceColor.blue, fog),
                        topLeft = Offset(x, top),
                        size = Size(width, height)
                    )
                    // Rivet accents
                    val rivetY1 = top + height * 0.2f
                    val rivetY2 = top + height * 0.8f
                    drawScope.drawRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(x, rivetY1),
                        size = Size(width, (height * 0.03f).coerceAtLeast(1.5f))
                    )
                    drawScope.drawRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(x, rivetY2),
                        size = Size(width, (height * 0.03f).coerceAtLeast(1.5f))
                    )
                }
            }
            4 -> { // Armored Blast Door with Hazard Stripes & Hydraulic Lock
                // Hydraulic lock core in center
                if (hitOffset in 0.44f..0.56f) {
                    val lockY = top + height * 0.4f
                    val lockH = height * 0.2f
                    drawScope.drawRect(
                        color = Color(0xFF0F172A),
                        topLeft = Offset(x, lockY),
                        size = Size(width, lockH)
                    )
                    // Pneumatic status indicator light (Cyan / Green)
                    val statusColor = if (nightVision) Color(0xFF00FF66) else Color(0xFF00E5FF)
                    drawScope.drawRect(
                        color = Color(statusColor.red, statusColor.green, statusColor.blue, fog),
                        topLeft = Offset(x, lockY + lockH * 0.35f),
                        size = Size(width, (lockH * 0.3f).coerceAtLeast(2f))
                    )
                }
                // Hazard caution diagonal stripes
                if (hitOffset in 0.15f..0.85f && !(hitOffset in 0.44f..0.56f)) {
                    val stripePattern = ((hitOffset * 10f).toInt() % 2 == 0)
                    if (stripePattern) {
                        val stripeColor = if (nightVision) Color(0xFF00FF66) else Color(0xFFF59E0B)
                        val stripeY = top + height * 0.72f
                        drawScope.drawRect(
                            color = Color(stripeColor.red, stripeColor.green, stripeColor.blue, fog * 0.95f),
                            topLeft = Offset(x, stripeY),
                            size = Size(width, (height * 0.12f).coerceAtLeast(2f))
                        )
                    }
                }
            }
            5 -> { // Desert Sandstone strata layers
                val strataY1 = top + height * 0.35f
                val strataY2 = top + height * 0.7f
                drawScope.drawRect(
                    color = Color(0f, 0f, 0f, fog * 0.3f),
                    topLeft = Offset(x, strataY1),
                    size = Size(width, (height * 0.03f).coerceAtLeast(1.5f))
                )
                drawScope.drawRect(
                    color = Color(0f, 0f, 0f, fog * 0.25f),
                    topLeft = Offset(x, strataY2),
                    size = Size(width, (height * 0.025f).coerceAtLeast(1.5f))
                )
            }
        }
    }

    private data class DrawableSprite(
        val distance: Float,
        val transformX: Float,
        val transformY: Float,
        val renderAction: (DrawScope, Float, Float, Float, Float) -> Unit
    )

    private fun renderWorldSprites(
        drawScope: DrawScope,
        screenWidth: Float,
        screenHeight: Float,
        halfHeight: Float,
        wallProjScale: Float,
        numRays: Int,
        columnWidth: Float,
        playerX: Float,
        playerY: Float,
        playerAngle: Float,
        nightVision: Boolean,
        enemies: List<Enemy>,
        items: List<WorldItem>,
        effects: List<VisualEffect>
    ) {
        val spriteList = mutableListOf<DrawableSprite>()

        // 1. Queue Items & Barrels
        for (item in items) {
            if (item.collected || (item.type == ItemType.EXPLOSIVE_BARREL && item.exploded)) continue
            val spriteX = item.x - playerX
            val spriteY = item.y - playerY

            // Transform sprite coordinate relative to player camera view
            val invDet = 1.0f / (cos(playerAngle + PI.toFloat() / 2f) * sin(playerAngle) - sin(playerAngle + PI.toFloat() / 2f) * cos(playerAngle))
            val transformX = invDet * (sin(playerAngle) * spriteX - cos(playerAngle) * spriteY)
            val transformY = invDet * (-sin(playerAngle + PI.toFloat() / 2f) * spriteX + cos(playerAngle + PI.toFloat() / 2f) * spriteY)

            if (transformY > 0.35f) {
                val dist = sqrt(spriteX * spriteX + spriteY * spriteY)
                spriteList.add(
                    DrawableSprite(dist, transformX, transformY) { scope, screenX, bottomY, sprWidth, sprHeight ->
                        drawItemSprite(scope, item, screenX, bottomY, sprWidth, sprHeight, nightVision)
                    }
                )
            }
        }

        // 2. Queue Enemies
        for (enemy in enemies) {
            val spriteX = enemy.x - playerX
            val spriteY = enemy.y - playerY

            val invDet = 1.0f / (cos(playerAngle + PI.toFloat() / 2f) * sin(playerAngle) - sin(playerAngle + PI.toFloat() / 2f) * cos(playerAngle))
            val transformX = invDet * (sin(playerAngle) * spriteX - cos(playerAngle) * spriteY)
            val transformY = invDet * (-sin(playerAngle + PI.toFloat() / 2f) * spriteX + cos(playerAngle + PI.toFloat() / 2f) * spriteY)

            if (transformY > 0.35f) {
                val dist = sqrt(spriteX * spriteX + spriteY * spriteY)
                spriteList.add(
                    DrawableSprite(dist, transformX, transformY) { scope, screenX, bottomY, sprWidth, sprHeight ->
                        drawEnemySprite(scope, enemy, screenX, bottomY, sprWidth, sprHeight, nightVision, dist)
                    }
                )
            }
        }

        // 3. Queue Visual Effects (Explosions, Blood Splatters, Sparks)
        for (effect in effects) {
            val spriteX = effect.x - playerX
            val spriteY = effect.y - playerY

            val invDet = 1.0f / (cos(playerAngle + PI.toFloat() / 2f) * sin(playerAngle) - sin(playerAngle + PI.toFloat() / 2f) * cos(playerAngle))
            val transformX = invDet * (sin(playerAngle) * spriteX - cos(playerAngle) * spriteY)
            val transformY = invDet * (-sin(playerAngle + PI.toFloat() / 2f) * spriteX + cos(playerAngle + PI.toFloat() / 2f) * spriteY)

            if (transformY > 0.25f) {
                val dist = sqrt(spriteX * spriteX + spriteY * spriteY)
                spriteList.add(
                    DrawableSprite(dist, transformX, transformY) { scope, screenX, bottomY, sprWidth, sprHeight ->
                        drawEffectSprite(scope, effect, screenX, bottomY, sprWidth, sprHeight)
                    }
                )
            }
        }

        // Sort sprites by distance: farthest first (Painter's Algorithm)
        spriteList.sortByDescending { it.distance }

        // Render each sprite if not occluded by walls
        for (sprite in spriteList) {
            val spriteScreenX = (screenWidth / 2f) * (1f + sprite.transformX / sprite.transformY)
            val spriteHeight = abs(wallProjScale / sprite.transformY)
            val spriteWidth = spriteHeight * 0.75f
            val spriteBottomY = halfHeight + spriteHeight / 2f

            // Calculate screen column index
            val colIndex = (spriteScreenX / columnWidth).toInt().coerceIn(0, numRays - 1)

            // Depth test: only draw if closer than the wall at that column
            if (sprite.transformY < zBuffer[colIndex] + 0.35f) {
                sprite.renderAction(drawScope, spriteScreenX, spriteBottomY, spriteWidth, spriteHeight)
            }
        }
    }

    private fun drawEnemySprite(
        scope: DrawScope,
        enemy: Enemy,
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        nightVision: Boolean,
        dist: Float
    ) {
        val topY = bottomY - height
        val isHurt = enemy.state == EnemyState.HURT
        val isDead = enemy.state == EnemyState.DEAD

        TacticalCharacterRenderer.drawRealisticEnemy(
            scope = scope,
            enemy = enemy,
            centerX = centerX,
            bottomY = bottomY,
            width = width,
            height = height,
            nightVision = nightVision,
            dist = dist
        )
        return
    }

    private fun drawItemSprite(
        scope: DrawScope,
        item: WorldItem,
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        nightVision: Boolean
    ) {
        when (item.type) {
            ItemType.EXPLOSIVE_BARREL -> {
                val barrelWidth = width * 0.55f
                val barrelHeight = height * 0.65f
                val barrelTop = bottomY - barrelHeight
                val barrelColor = if (nightVision) Color(0xFF1E3A8A) else Color(0xFFDC2626)

                // Main barrel body
                scope.drawRect(
                    color = barrelColor,
                    topLeft = Offset(centerX - barrelWidth / 2f, barrelTop),
                    size = Size(barrelWidth, barrelHeight)
                )
                // Metal bands
                scope.drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(centerX - barrelWidth / 2f, barrelTop + barrelHeight * 0.2f),
                    size = Size(barrelWidth, barrelHeight * 0.08f)
                )
                scope.drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(centerX - barrelWidth / 2f, barrelTop + barrelHeight * 0.7f),
                    size = Size(barrelWidth, barrelHeight * 0.08f)
                )
                // Hazard Flammable symbol
                scope.drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = barrelWidth * 0.22f,
                    center = Offset(centerX, barrelTop + barrelHeight * 0.45f)
                )
            }
            ItemType.MEDKIT -> {
                val medWidth = width * 0.5f
                val medHeight = height * 0.4f
                val medTop = bottomY - medHeight
                // White tactical medical box
                scope.drawRect(
                    color = Color(0xFFF8FAFC),
                    topLeft = Offset(centerX - medWidth / 2f, medTop),
                    size = Size(medWidth, medHeight)
                )
                // Red cross
                val crossSize = medWidth * 0.45f
                val crossThickness = crossSize * 0.35f
                val crossCenterX = centerX
                val crossCenterY = medTop + medHeight / 2f
                scope.drawRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(crossCenterX - crossSize / 2f, crossCenterY - crossThickness / 2f),
                    size = Size(crossSize, crossThickness)
                )
                scope.drawRect(
                    color = Color(0xFFEF4444),
                    topLeft = Offset(crossCenterX - crossThickness / 2f, crossCenterY - crossSize / 2f),
                    size = Size(crossThickness, crossSize)
                )
            }
            ItemType.AMMO_CRATE -> {
                val ammoWidth = width * 0.55f
                val ammoHeight = height * 0.38f
                val ammoTop = bottomY - ammoHeight
                // Olive drab military ammo box
                scope.drawRect(
                    color = Color(0xFF3F6212),
                    topLeft = Offset(centerX - ammoWidth / 2f, ammoTop),
                    size = Size(ammoWidth, ammoHeight)
                )
                // Brass ammunition rounds icon
                scope.drawRect(
                    color = Color(0xFFFBBF24),
                    topLeft = Offset(centerX - ammoWidth * 0.25f, ammoTop + ammoHeight * 0.3f),
                    size = Size(ammoWidth * 0.5f, ammoHeight * 0.35f)
                )
            }
            ItemType.ARMOR_VEST -> {
                val vestWidth = width * 0.5f
                val vestHeight = height * 0.45f
                val vestTop = bottomY - vestHeight
                // Tactical Kevlar body armor plate
                scope.drawRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(centerX - vestWidth / 2f, vestTop),
                    size = Size(vestWidth, vestHeight)
                )
                scope.drawRect(
                    color = Color(0xFF06B6D4),
                    topLeft = Offset(centerX - vestWidth * 0.3f, vestTop + vestHeight * 0.25f),
                    size = Size(vestWidth * 0.6f, vestHeight * 0.5f)
                )
            }
            ItemType.MISSION_INTEL -> {
                val laptopWidth = width * 0.55f
                val laptopHeight = height * 0.42f
                val laptopTop = bottomY - laptopHeight
                // Glowing military tactical laptop
                scope.drawRect(
                    color = Color(0xFF0F172A),
                    topLeft = Offset(centerX - laptopWidth / 2f, laptopTop),
                    size = Size(laptopWidth, laptopHeight)
                )
                // Glowing blue quantum screen
                scope.drawRect(
                    color = Color(0xFF00E5FF),
                    topLeft = Offset(centerX - laptopWidth * 0.4f, laptopTop + laptopHeight * 0.15f),
                    size = Size(laptopWidth * 0.8f, laptopHeight * 0.55f)
                )
            }
        }
    }

    private fun drawEffectSprite(
        scope: DrawScope,
        effect: VisualEffect,
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float
    ) {
        val progress = (1f - (effect.lifetime / effect.maxLifetime)).coerceIn(0f, 1f)
        val alpha = (effect.lifetime / effect.maxLifetime).coerceIn(0f, 1f)

        when (effect.type) {
            EffectType.EXPLOSION_BLAST -> {
                val radius = (width * 0.8f) * (0.4f + progress * 0.9f)
                // Outer fire ring
                scope.drawCircle(
                    color = Color(1f, 0.4f, 0f, alpha * 0.8f),
                    radius = radius,
                    center = Offset(centerX, bottomY - height * 0.5f)
                )
                // Inner white-hot core
                scope.drawCircle(
                    color = Color(1f, 0.95f, 0.7f, alpha),
                    radius = radius * 0.55f,
                    center = Offset(centerX, bottomY - height * 0.5f)
                )
            }
            EffectType.BLOOD_SPLATTER -> {
                val radius = (width * 0.3f) * (0.5f + progress * 0.5f)
                scope.drawCircle(
                    color = Color(0.8f, 0.05f, 0.05f, alpha * 0.9f),
                    radius = radius,
                    center = Offset(centerX, bottomY - height * 0.6f)
                )
            }
            EffectType.WALL_SPARK -> {
                val sparkColor = Color(1f, 0.85f, 0.2f, alpha)
                scope.drawCircle(
                    color = sparkColor,
                    radius = (width * 0.18f),
                    center = Offset(centerX, bottomY - height * 0.5f)
                )
            }
            EffectType.PLASMA_BOLT -> {
                val boltRadius = (width * 0.45f) * (0.8f + progress * 0.4f)
                // Outer cyan/neon magenta plasma glow
                scope.drawCircle(
                    color = Color(0.9f, 0.0f, 0.4f, alpha * 0.85f),
                    radius = boltRadius,
                    center = Offset(centerX, bottomY - height * 0.5f)
                )
                // Inner blinding electric core
                scope.drawCircle(
                    color = Color(0.2f, 1.0f, 1.0f, alpha),
                    radius = boltRadius * 0.5f,
                    center = Offset(centerX, bottomY - height * 0.5f)
                )
                scope.drawCircle(
                    color = Color.White,
                    radius = boltRadius * 0.25f,
                    center = Offset(centerX, bottomY - height * 0.5f)
                )
            }
            EffectType.ENERGY_SHIELD_BURST -> {
                val shieldRadius = (width * 0.65f) * (0.3f + progress * 0.8f)
                scope.drawCircle(
                    color = Color(0.0f, 0.9f, 1.0f, alpha * 0.7f),
                    radius = shieldRadius,
                    center = Offset(centerX, bottomY - height * 0.5f),
                    style = Stroke(width = 3.5f)
                )
                scope.drawCircle(
                    color = Color(0.0f, 0.6f, 1.0f, alpha * 0.3f),
                    radius = shieldRadius * 0.9f,
                    center = Offset(centerX, bottomY - height * 0.5f)
                )
            }
            EffectType.ION_TRAIL -> {
                val trailRadius = (width * 0.25f) * (1f - progress * 0.5f)
                scope.drawCircle(
                    color = Color(0.1f, 0.9f, 0.8f, alpha * 0.6f),
                    radius = trailRadius,
                    center = Offset(centerX, bottomY - height * 0.5f)
                )
            }
            else -> {}
        }
    }

    private fun renderNightVisionOverlay(drawScope: DrawScope, width: Float, height: Float) {
        // Green tactical phosphor tint
        drawScope.drawRect(
            color = Color(0x2800FF66),
            topLeft = Offset(0f, 0f),
            size = Size(width, height)
        )
        // Scanlines
        var y = 0f
        while (y < height) {
            drawScope.drawLine(
                color = Color(0x1F003311),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.5f
            )
            y += 5f
        }
        // Vignette corners
        drawScope.drawRect(
            color = Color(0x33001A09),
            topLeft = Offset(0f, 0f),
            size = Size(width, 24f)
        )
        drawScope.drawRect(
            color = Color(0x33001A09),
            topLeft = Offset(0f, height - 24f),
            size = Size(width, 24f)
        )
    }

    private fun renderFirstPersonWeapon(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        weapon: Weapon,
        adsProgress: Float,
        recoilOffset: Offset,
        isShootingFlash: Boolean,
        reloadProgress: Float,
        nightVision: Boolean
    ) {
        // If player is using AWP with full ADS scope, the screen transitions to the Sniper Scope Overlay!
        if (weapon.hasScope && adsProgress > 0.85f) {
            renderSniperScopeOverlay(drawScope, width, height)
            return
        }

        // Interpolate gun position: Hipfire (bottom-right) -> ADS (center alignment)
        val hipX = width * 0.66f
        val hipY = height * 0.72f
        val adsX = width * 0.50f
        val adsY = height * 0.70f

        val reloadDrop = if (reloadProgress > 0f) {
            sin(reloadProgress * PI.toFloat()) * (height * 0.28f)
        } else 0f

        val gunX = (hipX + (adsX - hipX) * adsProgress) + recoilOffset.x
        val gunY = (hipY + (adsY - hipY) * adsProgress) + recoilOffset.y + reloadDrop

        val gunScale = (height * 0.38f)

        // Draw tactical operator gloved hands holding the weapon
        drawTacticalHands(drawScope, gunX, gunY, gunScale, adsProgress, weapon.type)

        // Draw weapon model
        when (weapon.type) {
            WeaponType.ASSAULT_RIFLE -> drawM4a1Model(drawScope, gunX, gunY, gunScale, adsProgress, isShootingFlash, nightVision)
            WeaponType.PLASMA_RAILGUN -> drawRailgunModel(drawScope, gunX, gunY, gunScale, adsProgress, isShootingFlash, nightVision)
            WeaponType.SNIPER_RIFLE -> drawAwpModel(drawScope, gunX, gunY, gunScale, adsProgress, isShootingFlash, nightVision)
            WeaponType.SHOTGUN -> drawShotgunModel(drawScope, gunX, gunY, gunScale, adsProgress, isShootingFlash, nightVision)
            WeaponType.PISTOL -> drawPistolModel(drawScope, gunX, gunY, gunScale, adsProgress, isShootingFlash, nightVision)
            WeaponType.CYBER_BLADE -> drawCyberBladeModel(drawScope, gunX, gunY, gunScale, isShootingFlash)
        }

        // Draw muzzle flash lightburst
        if (isShootingFlash && weapon.type != WeaponType.CYBER_BLADE) {
            val muzzleX = gunX + (1f - adsProgress) * (gunScale * 0.2f)
            val muzzleY = gunY - gunScale * 0.55f

            drawScope.drawCircle(
                color = Color(0xFFFFCC00),
                radius = gunScale * 0.35f,
                center = Offset(muzzleX, muzzleY)
            )
            drawScope.drawCircle(
                color = Color(0xFFFFFFFF),
                radius = gunScale * 0.18f,
                center = Offset(muzzleX, muzzleY)
            )
        }
    }

    private fun drawTacticalHands(
        scope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        adsProgress: Float,
        type: WeaponType
    ) {
        if (type == WeaponType.CYBER_BLADE) return

        val sleeveColor = Color(0xFF1E242B)
        val gloveColor = Color(0xFF14181E)
        val knuckleColor = Color(0xFF2A313C)
        val watchBand = Color(0xFF0F172A)
        val watchBezel = Color(0xFF00E5FF)

        val handSize = scale * 0.35f

        // 1. Right Hand (Gripping the weapon handle from bottom right)
        val rHandX = x + (scale * 0.12f) * (1f - adsProgress)
        val rHandY = y - (scale * 0.05f)

        // Right forearm coming up from screen edge
        scope.drawRect(
            color = sleeveColor,
            topLeft = Offset(rHandX - handSize * 0.4f, rHandY + handSize * 0.4f),
            size = Size(handSize * 0.9f, handSize * 1.5f)
        )
        // Tactical smartwatch / compass on wrist
        scope.drawRect(
            color = watchBand,
            topLeft = Offset(rHandX - handSize * 0.35f, rHandY + handSize * 0.35f),
            size = Size(handSize * 0.8f, handSize * 0.22f)
        )
        scope.drawCircle(
            color = watchBezel,
            radius = handSize * 0.12f,
            center = Offset(rHandX, rHandY + handSize * 0.46f)
        )

        // Tactical Glove Hand Body
        scope.drawRoundRect(
            color = gloveColor,
            topLeft = Offset(rHandX - handSize * 0.35f, rHandY - handSize * 0.2f),
            size = Size(handSize * 0.75f, handSize * 0.65f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
        )
        // Carbon fiber knuckle plate
        scope.drawRoundRect(
            color = knuckleColor,
            topLeft = Offset(rHandX - handSize * 0.28f, rHandY - handSize * 0.12f),
            size = Size(handSize * 0.60f, handSize * 0.25f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        // Thumb wrapping weapon grip
        scope.drawRoundRect(
            color = gloveColor,
            topLeft = Offset(rHandX - handSize * 0.50f, rHandY - handSize * 0.15f),
            size = Size(handSize * 0.28f, handSize * 0.45f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )

        // 2. Left Hand (Support hand holding handguard / barrel from bottom left)
        if (adsProgress < 0.85f) {
            val lHandX = x - (scale * 0.22f)
            val lHandY = y - (scale * 0.40f)

            // Left forearm
            scope.drawRect(
                color = sleeveColor,
                topLeft = Offset(lHandX - handSize * 0.6f, lHandY + handSize * 0.5f),
                size = Size(handSize * 0.85f, handSize * 1.6f)
            )
            // Left tactical glove wrapping handguard
            scope.drawRoundRect(
                color = gloveColor,
                topLeft = Offset(lHandX - handSize * 0.3f, lHandY),
                size = Size(handSize * 0.65f, handSize * 0.6f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            scope.drawRoundRect(
                color = knuckleColor,
                topLeft = Offset(lHandX - handSize * 0.22f, lHandY + handSize * 0.08f),
                size = Size(handSize * 0.50f, handSize * 0.22f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )
        }
    }

    private fun drawM4a1Model(
        scope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        adsProgress: Float,
        isFlash: Boolean,
        nightVision: Boolean
    ) {
        val baseColor = if (nightVision) Color(0xFF0F3818) else Color(0xFF1E293B)
        val metalDark = Color(0xFF0F172A)
        val cyberCyan = Color(0xFF00F5FF)
        val holoColor = Color(0xFFFF3366)

        // Sleek 2026 ergonomic composite receiver & barrel
        val barrelWidth = scale * 0.26f
        val barrelHeight = scale * 1.0f
        scope.drawRect(
            color = baseColor,
            topLeft = Offset(x - barrelWidth / 2f, y - barrelHeight),
            size = Size(barrelWidth, barrelHeight)
        )
        // Angled tactical handguard with ventilated heat vents
        scope.drawRect(
            color = metalDark,
            topLeft = Offset(x - barrelWidth * 0.65f, y - barrelHeight * 0.85f),
            size = Size(barrelWidth * 1.3f, barrelHeight * 0.48f)
        )
        // Integrated smart laser guide line along receiver
        scope.drawRect(
            color = cyberCyan,
            topLeft = Offset(x - barrelWidth * 0.62f, y - barrelHeight * 0.80f),
            size = Size(3.5f, barrelHeight * 0.40f)
        )

        // OLED Digital Ammo Status display screen on weapon body
        val oledWidth = barrelWidth * 0.7f
        val oledHeight = barrelHeight * 0.12f
        val oledY = y - barrelHeight * 0.32f
        scope.drawRect(
            color = Color(0xFF001122),
            topLeft = Offset(x - oledWidth / 2f, oledY),
            size = Size(oledWidth, oledHeight)
        )
        scope.drawRect(
            color = cyberCyan,
            topLeft = Offset(x - oledWidth * 0.38f, oledY + oledHeight * 0.25f),
            size = Size(oledWidth * 0.76f, oledHeight * 0.5f)
        )

        // Floating Quantum Holographic Sight Frame
        val holoSize = scale * 0.24f
        val holoY = y - barrelHeight * 0.58f
        scope.drawRect(
            color = Color(0xFF030712),
            topLeft = Offset(x - holoSize / 2f, holoY),
            size = Size(holoSize, holoSize * 0.75f)
        )
        // Holographic glass projection tint
        scope.drawRect(
            color = Color(0x3300F5FF),
            topLeft = Offset(x - holoSize * 0.4f, holoY + holoSize * 0.1f),
            size = Size(holoSize * 0.8f, holoSize * 0.55f)
        )
        // High-precision illuminated reticle
        scope.drawCircle(
            color = holoColor,
            radius = 3.5f,
            center = Offset(x, holoY + holoSize * 0.38f)
        )
        scope.drawCircle(
            color = Color(0x66FF3366),
            radius = 11f,
            center = Offset(x, holoY + holoSize * 0.38f),
            style = Stroke(1.5f)
        )
    }

    private fun drawAwpModel(
        scope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        adsProgress: Float,
        isFlash: Boolean,
        nightVision: Boolean
    ) {
        val stockColor = Color(0xFF2E4053)
        val barrelColor = Color(0xFF0F172A)

        // Long heavy sniper barrel
        val barrelWidth = scale * 0.2f
        val barrelHeight = scale * 1.25f
        scope.drawRect(
            color = barrelColor,
            topLeft = Offset(x - barrelWidth / 2f, y - barrelHeight),
            size = Size(barrelWidth, barrelHeight)
        )
        // Massive optical scope cylinder
        val scopeWidth = scale * 0.35f
        val scopeHeight = scale * 0.55f
        scope.drawRect(
            color = Color(0xFF0B111A),
            topLeft = Offset(x - scopeWidth / 2f, y - barrelHeight * 0.7f),
            size = Size(scopeWidth, scopeHeight)
        )
        // Scope glass reflection
        scope.drawCircle(
            color = Color(0x8800E5FF),
            radius = scopeWidth * 0.38f,
            center = Offset(x, y - barrelHeight * 0.7f + scopeHeight * 0.5f)
        )
    }

    private fun drawShotgunModel(
        scope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        adsProgress: Float,
        isFlash: Boolean,
        nightVision: Boolean
    ) {
        val metalDark = Color(0xFF1E293B)
        val barrelWidth = scale * 0.38f
        val barrelHeight = scale * 0.85f

        // Twin shotgun barrel & pump magazine tube
        scope.drawRect(
            color = metalDark,
            topLeft = Offset(x - barrelWidth / 2f, y - barrelHeight),
            size = Size(barrelWidth, barrelHeight)
        )
        // Ribbed pump grip
        scope.drawRect(
            color = Color(0xFF475569),
            topLeft = Offset(x - barrelWidth * 0.6f, y - barrelHeight * 0.55f),
            size = Size(barrelWidth * 1.2f, barrelHeight * 0.3f)
        )
    }

    private fun drawPistolModel(
        scope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        adsProgress: Float,
        isFlash: Boolean,
        nightVision: Boolean
    ) {
        val slideColor = Color(0xFF334155)
        val slideWidth = scale * 0.22f
        val slideHeight = scale * 0.65f

        scope.drawRect(
            color = slideColor,
            topLeft = Offset(x - slideWidth / 2f, y - slideHeight),
            size = Size(slideWidth, slideHeight)
        )
        // Tritium front night sight
        scope.drawCircle(
            color = Color(0xFF00FF66),
            radius = 3.0f,
            center = Offset(x, y - slideHeight + 5f)
        )
    }

    private fun drawRailgunModel(
        scope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        adsProgress: Float,
        isFlash: Boolean,
        nightVision: Boolean
    ) {
        val chassisDark = Color(0xFF0F172A)
        val carbonFiber = Color(0xFF1E293B)
        val plasmaCyan = Color(0xFF00F5FF)
        val plasmaHotMagenta = Color(0xFFFF0055)

        val barrelWidth = scale * 0.26f
        val barrelHeight = scale * 1.35f

        // Heavy aerodynamic carbon-composite housing
        scope.drawRect(
            color = chassisDark,
            topLeft = Offset(x - barrelWidth / 2f, y - barrelHeight),
            size = Size(barrelWidth, barrelHeight)
        )

        // Twin electromagnetic acceleration parallel rails
        val railWidth = barrelWidth * 0.22f
        scope.drawRect(
            color = carbonFiber,
            topLeft = Offset(x - barrelWidth * 0.45f, y - barrelHeight * 0.95f),
            size = Size(railWidth, barrelHeight * 0.9f)
        )
        scope.drawRect(
            color = carbonFiber,
            topLeft = Offset(x + barrelWidth * 0.45f - railWidth, y - barrelHeight * 0.95f),
            size = Size(railWidth, barrelHeight * 0.9f)
        )

        // Glowing electromagnetic coils along the barrel
        for (i in 0..5) {
            val coilY = y - barrelHeight * (0.35f + i * 0.1f)
            val coilColor = if (isFlash) plasmaHotMagenta else plasmaCyan
            scope.drawRect(
                color = coilColor,
                topLeft = Offset(x - barrelWidth * 0.35f, coilY),
                size = Size(barrelWidth * 0.7f, 4.5f)
            )
        }

        // Central holographic charging core
        scope.drawCircle(
            color = if (isFlash) Color.White else plasmaCyan,
            radius = barrelWidth * 0.32f,
            center = Offset(x, y - barrelHeight * 0.65f)
        )

        // Quantum HUD sight projection
        val sightWidth = scale * 0.22f
        val sightY = y - barrelHeight * 0.45f
        scope.drawRect(
            color = Color(0x3300F5FF),
            topLeft = Offset(x - sightWidth / 2f, sightY),
            size = Size(sightWidth, sightWidth * 0.7f)
        )
        scope.drawCircle(
            color = plasmaCyan,
            radius = 3.5f,
            center = Offset(x, sightY + sightWidth * 0.35f)
        )
    }

    private fun drawCyberBladeModel(
        scope: DrawScope,
        x: Float,
        y: Float,
        scale: Float,
        isSlashing: Boolean
    ) {
        val slashAngle = if (isSlashing) -35f else 0f
        val bladeWidth = scale * 0.14f
        val bladeHeight = scale * 1.1f

        val hiltDark = Color(0xFF0B0F19)
        val bladeEdgeNeon = Color(0xFF00FFFF)
        val bladeCore = Color(0xFFE0F7FA)

        // Carbon-fiber angled hilt & finger-guard
        scope.drawRect(
            color = hiltDark,
            topLeft = Offset(x - bladeWidth * 0.8f, y - bladeHeight * 0.3f),
            size = Size(bladeWidth * 1.6f, bladeHeight * 0.3f)
        )

        // Futuristic mono-molecular blade spine
        scope.drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(x - bladeWidth / 2f, y - bladeHeight),
            size = Size(bladeWidth, bladeHeight * 0.72f)
        )

        // Hyper-frequency plasma edge with vibrant neon glow
        scope.drawRect(
            color = bladeEdgeNeon,
            topLeft = Offset(x - bladeWidth * 0.65f, y - bladeHeight),
            size = Size(bladeWidth * 0.3f, bladeHeight * 0.72f)
        )
        scope.drawRect(
            color = bladeCore,
            topLeft = Offset(x - bladeWidth * 0.5f, y - bladeHeight * 0.95f),
            size = Size(bladeWidth * 0.15f, bladeHeight * 0.65f)
        )

        // Cybernetic battery status LED on the hilt
        scope.drawCircle(
            color = Color(0xFF00FF66),
            radius = 3.0f,
            center = Offset(x, y - bladeHeight * 0.15f)
        )
    }

    private fun renderSniperScopeOverlay(drawScope: DrawScope, width: Float, height: Float) {
        val centerX = width / 2f
        val centerY = height / 2f
        val scopeRadius = min(width, height) * 0.44f

        // Black vignette masking outside the circular scope
        drawScope.drawRect(
            color = Color(0xEE05070A),
            topLeft = Offset(0f, 0f),
            size = Size(width, centerY - scopeRadius)
        )
        drawScope.drawRect(
            color = Color(0xEE05070A),
            topLeft = Offset(0f, centerY + scopeRadius),
            size = Size(width, height - (centerY + scopeRadius))
        )
        drawScope.drawRect(
            color = Color(0xEE05070A),
            topLeft = Offset(0f, centerY - scopeRadius),
            size = Size(centerX - scopeRadius, scopeRadius * 2f)
        )
        drawScope.drawRect(
            color = Color(0xEE05070A),
            topLeft = Offset(centerX + scopeRadius, centerY - scopeRadius),
            size = Size(width - (centerX + scopeRadius), scopeRadius * 2f)
        )

        // Outer scope ring
        drawScope.drawCircle(
            color = Color(0xFF0F172A),
            radius = scopeRadius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 16f)
        )

        // Precision Sniper Crosshairs
        val reticleColor = Color(0xFFFF3333)
        // Horizontal line
        drawScope.drawLine(
            color = reticleColor,
            start = Offset(centerX - scopeRadius, centerY),
            end = Offset(centerX + scopeRadius, centerY),
            strokeWidth = 2.0f
        )
        // Vertical line
        drawScope.drawLine(
            color = reticleColor,
            start = Offset(centerX, centerY - scopeRadius),
            end = Offset(centerX, centerY + scopeRadius),
            strokeWidth = 2.0f
        )

        // Mil-Dots / Range elevation ticks
        for (i in 1..4) {
            val tickDist = i * (scopeRadius * 0.18f)
            // Down ticks
            drawScope.drawLine(
                color = reticleColor,
                start = Offset(centerX - 8f, centerY + tickDist),
                end = Offset(centerX + 8f, centerY + tickDist),
                strokeWidth = 1.5f
            )
            // Horizontal ticks
            drawScope.drawLine(
                color = reticleColor,
                start = Offset(centerX + tickDist, centerY - 6f),
                end = Offset(centerX + tickDist, centerY + 6f),
                strokeWidth = 1.5f
            )
            drawScope.drawLine(
                color = reticleColor,
                start = Offset(centerX - tickDist, centerY - 6f),
                end = Offset(centerX - tickDist, centerY + 6f),
                strokeWidth = 1.5f
            )
        }

        // Center red illumination dot
        drawScope.drawCircle(
            color = Color(0xFFFF0000),
            radius = 3.5f,
            center = Offset(centerX, centerY)
        )
    }
}
