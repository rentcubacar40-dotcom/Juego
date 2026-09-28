package com.example.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.Enemy
import com.example.game.EnemyState
import com.example.game.EnemyType
import kotlin.math.sin

/**
 * High-Fidelity Tactical Character Renderer for Strike Ops 3D.
 * Renders realistic military operators with authentic uniforms, plate carriers,
 * ballistic helmets, combat boots, tactical gear, and weapons.
 */
object TacticalCharacterRenderer {

    fun drawRealisticEnemy(
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

        // 1. Defeated / Dead state (fallen soldier on ground with gear)
        if (isDead) {
            drawFallenSoldier(scope, centerX, bottomY, width, height, nightVision)
            return
        }

        // Realistic Distance Fog / Shadowing
        val fogFactor = (1f - (dist / 16f)).coerceIn(0.2f, 1f)
        val flashRed = isHurt

        // Breathing / Combat stance idle micro-sway
        val timeSec = System.currentTimeMillis() / 1000f
        val swayY = sin(timeSec * 3f + enemy.id) * (height * 0.015f)

        when (enemy.type) {
            EnemyType.COMMANDER -> drawHeavyJuggernaut(
                scope, enemy, centerX, bottomY + swayY, width, height, nightVision, fogFactor, flashRed
            )
            EnemyType.SNIPER -> drawGhostSniper(
                scope, enemy, centerX, bottomY + swayY, width, height, nightVision, fogFactor, flashRed
            )
            EnemyType.RECON -> drawReconScout(
                scope, enemy, centerX, bottomY + swayY, width, height, nightVision, fogFactor, flashRed
            )
            else -> drawSpecOpsOperator(
                scope, enemy, centerX, bottomY + swayY, width, height, nightVision, fogFactor, flashRed
            )
        }

        // Tactical Health & Threat Indicator (Only if damaged or close)
        if (enemy.health < enemy.type.maxHp && dist < 14f) {
            drawTacticalHealthBar(scope, enemy, centerX, topY - height * 0.05f, width)
        }
    }

    /**
     * SPECOPS OPERATOR (Tier 1 Black-Ops Operator)
     * Crye G3 combat uniform, FAST Ballistic Helmet, GPNVG-18 Night Vision,
     * JPC Plate Carrier with Molle and mag pouches, assault carbine.
     */
    private fun drawSpecOpsOperator(
        scope: DrawScope,
        enemy: Enemy,
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        nightVision: Boolean,
        fog: Float,
        flashRed: Boolean
    ) {
        // Base Palette
        val suitBase = if (flashRed) Color(0xFFDC2626) else if (nightVision) Color(0xFF0F3A1E) else Color(0xFF1E242B)
        val armorColor = if (nightVision) Color(0xFF1D5A32) else Color(0xFF13171C)
        val pouchColor = if (nightVision) Color(0xFF287A44) else Color(0xFF2B333D)
        val skinColor = if (nightVision) Color(0xFF34D399) else Color(0xFF948170)

        // 1. Shadow beneath operator
        scope.drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x99000000), Color.Transparent),
                center = Offset(centerX, bottomY),
                radius = width * 0.6f
            ),
            topLeft = Offset(centerX - width * 0.55f, bottomY - height * 0.04f),
            size = Size(width * 1.1f, height * 0.08f)
        )

        // 2. Combat Boots & Trousers
        val legW = width * 0.18f
        val legH = height * 0.38f
        val legY = bottomY - legH

        // Left Leg & Boot
        drawCombatLeg(scope, centerX - width * 0.22f, legY, legW, legH, suitBase, armorColor, nightVision)
        // Right Leg & Boot
        drawCombatLeg(scope, centerX + width * 0.04f, legY, legW, legH, suitBase, armorColor, nightVision)

        // 3. Torso & Tactical Plate Carrier
        val torsoW = width * 0.48f
        val torsoH = height * 0.34f
        val torsoY = legY - torsoH * 0.92f

        // Combat shirt torso base
        scope.drawRect(
            color = suitBase,
            topLeft = Offset(centerX - torsoW / 2f, torsoY),
            size = Size(torsoW, torsoH)
        )

        // Plate Carrier Vest (Ceramic plate shape)
        val vestW = torsoW * 0.88f
        val vestH = torsoH * 0.82f
        val vestY = torsoY + torsoH * 0.08f
        scope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(armorColor, Color(armorColor.red * 0.8f, armorColor.green * 0.8f, armorColor.blue * 0.8f))
            ),
            topLeft = Offset(centerX - vestW / 2f, vestY),
            size = Size(vestW, vestH)
        )

        // Molle Webbing & Triple Mag Pouches
        val magY = vestY + vestH * 0.45f
        val magW = vestW * 0.26f
        val magH = vestH * 0.38f
        for (i in -1..1) {
            val magX = centerX + i * (magW * 1.12f) - magW / 2f
            scope.drawRect(
                color = pouchColor,
                topLeft = Offset(magX, magY),
                size = Size(magW, magH)
            )
            // Brass bullet tips peeking out of magazine
            val bulletColor = if (nightVision) Color(0xFF00FF66) else Color(0xFFD97706)
            scope.drawRect(
                color = bulletColor,
                topLeft = Offset(magX + magW * 0.2f, magY - magH * 0.1f),
                size = Size(magW * 0.6f, magH * 0.12f)
            )
        }

        // Tactical Patch on Chest (Insignia)
        scope.drawRect(
            color = if (nightVision) Color(0xFF00E5FF) else Color(0xFFF59E0B),
            topLeft = Offset(centerX - vestW * 0.25f, vestY + vestH * 0.15f),
            size = Size(vestW * 0.5f, vestH * 0.12f)
        )

        // 4. Arms & Hands holding weapon
        val armW = width * 0.14f
        val armH = height * 0.28f
        // Left arm (supporting foregrip)
        scope.drawRect(
            color = suitBase,
            topLeft = Offset(centerX - torsoW * 0.62f, torsoY + height * 0.05f),
            size = Size(armW, armH)
        )
        // Right arm (trigger grip)
        scope.drawRect(
            color = suitBase,
            topLeft = Offset(centerX + torsoW * 0.48f - armW, torsoY + height * 0.05f),
            size = Size(armW, armH)
        )

        // 5. Head, Helmet, Balaclava & NVG
        val headRadius = width * 0.17f
        val headCenterY = torsoY - headRadius * 0.85f

        // Neck
        scope.drawRect(
            color = suitBase,
            topLeft = Offset(centerX - headRadius * 0.45f, headCenterY),
            size = Size(headRadius * 0.9f, headRadius * 0.9f)
        )

        // Balaclava Face Mask
        scope.drawCircle(
            color = Color(0xFF14181F),
            radius = headRadius * 0.85f,
            center = Offset(centerX, headCenterY)
        )

        // Eye slit & Tactical Eyes
        scope.drawRect(
            color = skinColor,
            topLeft = Offset(centerX - headRadius * 0.5f, headCenterY - headRadius * 0.18f),
            size = Size(headRadius * 1.0f, headRadius * 0.35f)
        )
        // Eyes
        scope.drawCircle(
            color = Color(0xFF1E293B),
            radius = headRadius * 0.1f,
            center = Offset(centerX - headRadius * 0.22f, headCenterY)
        )
        scope.drawCircle(
            color = Color(0xFF1E293B),
            radius = headRadius * 0.1f,
            center = Offset(centerX + headRadius * 0.22f, headCenterY)
        )

        // FAST Ballistic Helmet
        val helmetColor = if (nightVision) Color(0xFF1B4E2B) else Color(0xFF2A313C)
        scope.drawArc(
            color = helmetColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(centerX - headRadius * 1.02f, headCenterY - headRadius * 1.15f),
            size = Size(headRadius * 2.04f, headRadius * 1.6f)
        )

        // GPNVG-18 Panoramic Quad-Lens Night Vision Goggles
        val nvgY = headCenterY - headRadius * 0.4f
        val nvgLensColor = if (enemy.state == EnemyState.COMBAT) Color(0xFFFF2222) else Color(0xFF00FFCC)
        for (idx in -2..1) {
            val lensX = centerX + idx * (headRadius * 0.32f) + headRadius * 0.16f
            scope.drawCircle(
                color = Color(0xFF0A0D12),
                radius = headRadius * 0.16f,
                center = Offset(lensX, nvgY)
            )
            scope.drawCircle(
                color = nvgLensColor,
                radius = headRadius * 0.10f,
                center = Offset(lensX, nvgY)
            )
        }

        // Comms Headset Earcup & Boom Mic
        scope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(centerX - headRadius * 1.05f, headCenterY - headRadius * 0.2f),
            size = Size(headRadius * 0.22f, headRadius * 0.45f)
        )
        scope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(centerX + headRadius * 0.85f, headCenterY - headRadius * 0.2f),
            size = Size(headRadius * 0.22f, headRadius * 0.45f)
        )

        // 6. Tactical Assault Carbine (aimed at player)
        val rifleW = width * 0.65f
        val rifleH = height * 0.11f
        val rifleY = torsoY + torsoH * 0.38f
        val rifleX = centerX - rifleW * 0.35f

        // Gun Receiver & Barrel
        scope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(rifleX, rifleY),
            size = Size(rifleW, rifleH)
        )
        // Holo sight on top of rifle rail
        scope.drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(rifleX + rifleW * 0.25f, rifleY - rifleH * 0.6f),
            size = Size(rifleW * 0.22f, rifleH * 0.65f)
        )
        scope.drawCircle(
            color = if (enemy.state == EnemyState.COMBAT) Color(0xFFFF3333) else Color(0xFF00E5FF),
            radius = rifleH * 0.22f,
            center = Offset(rifleX + rifleW * 0.36f, rifleY - rifleH * 0.28f)
        )

        // Curved STANAG Magazine
        scope.drawRect(
            color = Color(0xFF262D38),
            topLeft = Offset(rifleX + rifleW * 0.32f, rifleY + rifleH),
            size = Size(rifleW * 0.14f, rifleH * 1.2f)
        )

        // Muzzle Flash when shooting
        if (enemy.isAimingAtPlayer && enemy.state == EnemyState.COMBAT && System.currentTimeMillis() - enemy.lastShotTime < 95L) {
            val muzzleX = rifleX + rifleW
            val muzzleY = rifleY + rifleH * 0.4f
            drawRealisticMuzzleFlash(scope, muzzleX, muzzleY, width * 0.4f)
        }
    }

    /**
     * HEAVY JUGGERNAUT / WARLORD
     * Massive titanium blast plates, EOD blast collar, reflective gold visor,
     * heavy drum shotgun.
     */
    private fun drawHeavyJuggernaut(
        scope: DrawScope,
        enemy: Enemy,
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        nightVision: Boolean,
        fog: Float,
        flashRed: Boolean
    ) {
        val armorDark = if (flashRed) Color(0xFFDC2626) else if (nightVision) Color(0xFF0A2B15) else Color(0xFF111827)
        val plateColor = if (nightVision) Color(0xFF1C6335) else Color(0xFF374151)
        val goldVisor = if (nightVision) Color(0xFF00FF99) else Color(0xFFF59E0B)

        // Heavy massive legs
        val legW = width * 0.24f
        val legH = height * 0.36f
        val legY = bottomY - legH
        drawCombatLeg(scope, centerX - width * 0.28f, legY, legW, legH, armorDark, plateColor, nightVision)
        drawCombatLeg(scope, centerX + width * 0.04f, legY, legW, legH, armorDark, plateColor, nightVision)

        // Heavy Torso Blast Armor
        val torsoW = width * 0.65f
        val torsoH = height * 0.40f
        val torsoY = legY - torsoH * 0.95f

        // Heavy chest plate
        scope.drawRect(
            brush = Brush.verticalGradient(listOf(plateColor, armorDark)),
            topLeft = Offset(centerX - torsoW / 2f, torsoY),
            size = Size(torsoW, torsoH)
        )

        // Hazard warning stripes across blast chest
        val stripeY = torsoY + torsoH * 0.3f
        val stripeH = torsoH * 0.2f
        for (s in 0..5) {
            val sColor = if (s % 2 == 0) (if (nightVision) Color(0xFF00FF66) else Color(0xFFF59E0B)) else Color(0xFF111827)
            scope.drawRect(
                color = sColor,
                topLeft = Offset(centerX - torsoW * 0.4f + s * (torsoW * 0.13f), stripeY),
                size = Size(torsoW * 0.13f, stripeH)
            )
        }

        // Heavy Pauldrons (Shoulder blast guards)
        scope.drawRect(
            color = plateColor,
            topLeft = Offset(centerX - torsoW * 0.65f, torsoY - height * 0.03f),
            size = Size(width * 0.22f, height * 0.18f)
        )
        scope.drawRect(
            color = plateColor,
            topLeft = Offset(centerX + torsoW * 0.43f, torsoY - height * 0.03f),
            size = Size(width * 0.22f, height * 0.18f)
        )

        // Heavy Blast Helmet with Visor
        val headRadius = width * 0.20f
        val headY = torsoY - headRadius * 0.9f
        scope.drawCircle(
            color = armorDark,
            radius = headRadius,
            center = Offset(centerX, headY)
        )
        // Reflective Gold/Amber Visor Slit
        scope.drawRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF78350F), goldVisor, Color(0xFF78350F))),
            topLeft = Offset(centerX - headRadius * 0.7f, headY - headRadius * 0.2f),
            size = Size(headRadius * 1.4f, headRadius * 0.4f)
        )

        // Heavy Drum Shotgun
        val gunW = width * 0.7f
        val gunH = height * 0.14f
        val gunY = torsoY + torsoH * 0.45f
        scope.drawRect(
            color = Color(0xFF0A0F1D),
            topLeft = Offset(centerX - gunW * 0.3f, gunY),
            size = Size(gunW, gunH)
        )
        // Drum Magazine
        scope.drawCircle(
            color = Color(0xFF1E293B),
            radius = gunH * 0.8f,
            center = Offset(centerX + gunW * 0.1f, gunY + gunH)
        )

        if (enemy.isAimingAtPlayer && enemy.state == EnemyState.COMBAT && System.currentTimeMillis() - enemy.lastShotTime < 95L) {
            drawRealisticMuzzleFlash(scope, centerX + gunW * 0.7f, gunY + gunH * 0.4f, width * 0.5f)
        }
    }

    /**
     * GHOST SNIPER
     * 3D Ghillie cowl, hooded camouflage burlap strips, long-barrel sniper rifle,
     * glowing scope glint or aim laser.
     */
    private fun drawGhostSniper(
        scope: DrawScope,
        enemy: Enemy,
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        nightVision: Boolean,
        fog: Float,
        flashRed: Boolean
    ) {
        val camoBase = if (flashRed) Color(0xFFDC2626) else if (nightVision) Color(0xFF0D3319) else Color(0xFF2E3832)
        val ghillieAccent = if (nightVision) Color(0xFF1B5A2D) else Color(0xFF475545)

        // Ghillie fringed body silhouette
        val bodyW = width * 0.5f
        val bodyH = height * 0.75f
        val bodyY = bottomY - bodyH

        scope.drawRect(
            color = camoBase,
            topLeft = Offset(centerX - bodyW / 2f, bodyY),
            size = Size(bodyW, bodyH)
        )

        // Burlap foliage texture strips
        for (i in 0..12) {
            val stripX = centerX - bodyW * 0.45f + (i * 17f) % bodyW
            val stripY = bodyY + (i * 29f) % bodyH
            val stripW = width * 0.12f
            val stripH = height * 0.08f
            scope.drawRect(
                color = if (i % 2 == 0) ghillieAccent else Color(0xFF1C221D),
                topLeft = Offset(stripX, stripY),
                size = Size(stripW, stripH)
            )
        }

        // Hooded Sniper Cowl
        val headRadius = width * 0.18f
        val headY = bodyY + headRadius * 0.3f
        scope.drawCircle(
            color = camoBase,
            radius = headRadius * 1.15f,
            center = Offset(centerX, headY)
        )
        // Shrouded Dark Face
        scope.drawCircle(
            color = Color(0xFF0F1412),
            radius = headRadius * 0.7f,
            center = Offset(centerX, headY)
        )

        // Long Anti-Material Sniper Rifle
        val rifleW = width * 0.85f
        val rifleH = height * 0.09f
        val rifleY = bodyY + bodyH * 0.45f
        val rifleX = centerX - rifleW * 0.25f

        scope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(rifleX, rifleY),
            size = Size(rifleW, rifleH)
        )
        // High-Magnification Scope with glint
        val scopeW = rifleW * 0.32f
        val scopeH = rifleH * 0.9f
        scope.drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(rifleX + rifleW * 0.15f, rifleY - scopeH),
            size = Size(scopeW, scopeH)
        )
        // Optical Scope Lens Reflection
        val glintColor = if (nightVision) Color(0xFF00FFCC) else Color(0xFF06B6D4)
        scope.drawCircle(
            color = glintColor,
            radius = scopeH * 0.45f,
            center = Offset(rifleX + rifleW * 0.15f, rifleY - scopeH * 0.5f)
        )

        // Sniper Aim Warning Beam
        if (enemy.isAimingAtPlayer && enemy.aimLaserCharge > 0.15f) {
            val laserAlpha = enemy.aimLaserCharge.coerceIn(0.2f, 1.0f)
            scope.drawLine(
                color = Color(1f, 0f, 0f, laserAlpha),
                start = Offset(rifleX + rifleW, rifleY + rifleH / 2f),
                end = Offset(centerX + (if (centerX < 500f) 700f else -700f), bottomY + 300f),
                strokeWidth = 3.0f
            )
        }

        if (enemy.isAimingAtPlayer && enemy.state == EnemyState.COMBAT && System.currentTimeMillis() - enemy.lastShotTime < 95L) {
            drawRealisticMuzzleFlash(scope, rifleX + rifleW, rifleY + rifleH * 0.4f, width * 0.55f)
        }
    }

    /**
     * RECON SCOUT
     * Lightweight desert/urban camo operator, tactical cap, headset, fast SMG.
     */
    private fun drawReconScout(
        scope: DrawScope,
        enemy: Enemy,
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        nightVision: Boolean,
        fog: Float,
        flashRed: Boolean
    ) {
        val camoBase = if (flashRed) Color(0xFFDC2626) else if (nightVision) Color(0xFF173F23) else Color(0xFF4A443A)
        val vestColor = if (nightVision) Color(0xFF265D37) else Color(0xFF2B2620)

        // Agile legs
        val legW = width * 0.16f
        val legH = height * 0.38f
        val legY = bottomY - legH
        drawCombatLeg(scope, centerX - width * 0.20f, legY, legW, legH, camoBase, vestColor, nightVision)
        drawCombatLeg(scope, centerX + width * 0.04f, legY, legW, legH, camoBase, vestColor, nightVision)

        // Lightweight Chest Rig
        val torsoW = width * 0.44f
        val torsoH = height * 0.32f
        val torsoY = legY - torsoH * 0.9f
        scope.drawRect(
            color = camoBase,
            topLeft = Offset(centerX - torsoW / 2f, torsoY),
            size = Size(torsoW, torsoH)
        )
        scope.drawRect(
            color = vestColor,
            topLeft = Offset(centerX - torsoW * 0.4f, torsoY + torsoH * 0.2f),
            size = Size(torsoW * 0.8f, torsoH * 0.65f)
        )

        // Head with Tactical Cap & Ballistic Sunglasses
        val headRadius = width * 0.16f
        val headY = torsoY - headRadius * 0.8f
        // Skin face
        scope.drawCircle(
            color = if (nightVision) Color(0xFF34D399) else Color(0xFFBD9E87),
            radius = headRadius * 0.8f,
            center = Offset(centerX, headY)
        )
        // Tactical Cap Visor
        scope.drawRect(
            color = vestColor,
            topLeft = Offset(centerX - headRadius * 0.9f, headY - headRadius * 0.8f),
            size = Size(headRadius * 1.8f, headRadius * 0.6f)
        )
        // Ballistic Sunglasses (Oakley M-Frames)
        scope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(centerX - headRadius * 0.5f, headY - headRadius * 0.15f),
            size = Size(headRadius * 1.0f, headRadius * 0.35f)
        )

        // Compact SMG
        val smgW = width * 0.55f
        val smgH = height * 0.10f
        val smgY = torsoY + torsoH * 0.4f
        scope.drawRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(centerX - smgW * 0.3f, smgY),
            size = Size(smgW, smgH)
        )

        if (enemy.isAimingAtPlayer && enemy.state == EnemyState.COMBAT && System.currentTimeMillis() - enemy.lastShotTime < 95L) {
            drawRealisticMuzzleFlash(scope, centerX + smgW * 0.7f, smgY + smgH * 0.4f, width * 0.35f)
        }
    }

    /**
     * Helper to render realistic combat trousers with knee pads and combat boots.
     */
    private fun drawCombatLeg(
        scope: DrawScope,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        pantColor: Color,
        padColor: Color,
        nightVision: Boolean
    ) {
        // Trousers
        scope.drawRect(
            color = pantColor,
            topLeft = Offset(x, y),
            size = Size(w, h * 0.72f)
        )
        // Reinforced Tactical Knee Pad with polymer cap
        val padY = y + h * 0.28f
        val padH = h * 0.26f
        scope.drawRoundRect(
            color = padColor,
            topLeft = Offset(x - w * 0.08f, padY),
            size = Size(w * 1.16f, padH),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
        )

        // Heavy Tactical Combat Boot with tread sole
        val bootY = y + h * 0.72f
        val bootH = h * 0.28f
        val bootColor = if (nightVision) Color(0xFF05170C) else Color(0xFF0F172A)
        scope.drawRect(
            color = bootColor,
            topLeft = Offset(x - w * 0.05f, bootY),
            size = Size(w * 1.1f, bootH)
        )
        // Boot tread sole
        scope.drawRect(
            color = Color.Black,
            topLeft = Offset(x - w * 0.1f, bootY + bootH * 0.75f),
            size = Size(w * 1.2f, bootH * 0.25f)
        )
    }

    /**
     * Renders a fallen soldier with realistic dropped gear and pool of blood.
     */
    private fun drawFallenSoldier(
        scope: DrawScope,
        centerX: Float,
        bottomY: Float,
        width: Float,
        height: Float,
        nightVision: Boolean
    ) {
        val collapsedH = height * 0.26f
        val bodyColor = if (nightVision) Color(0xFF0A2212) else Color(0xFF1E293B)
        val bloodColor = if (nightVision) Color(0x6600FF66) else Color(0x996B0707)

        // Expanding blood pool
        scope.drawOval(
            color = bloodColor,
            topLeft = Offset(centerX - width * 0.65f, bottomY - collapsedH * 0.55f),
            size = Size(width * 1.3f, collapsedH * 0.85f)
        )

        // Collapsed torso and tactical vest
        scope.drawOval(
            color = bodyColor,
            topLeft = Offset(centerX - width * 0.5f, bottomY - collapsedH),
            size = Size(width * 1.0f, collapsedH)
        )

        // Dropped tactical helmet
        val helmetColor = if (nightVision) Color(0xFF123D20) else Color(0xFF334155)
        scope.drawCircle(
            color = helmetColor,
            radius = width * 0.16f,
            center = Offset(centerX - width * 0.35f, bottomY - collapsedH * 0.4f)
        )

        // Dropped weapon lying on floor
        scope.drawRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(centerX + width * 0.1f, bottomY - collapsedH * 0.35f),
            size = Size(width * 0.45f, collapsedH * 0.25f)
        )
    }

    /**
     * Radiant 4-pointed realistic muzzle flash with incandescent white core and sparks.
     */
    private fun drawRealisticMuzzleFlash(
        scope: DrawScope,
        flashX: Float,
        flashY: Float,
        radius: Float
    ) {
        // Corona
        scope.drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFF59E0B), Color(0x00EF4444)),
                center = Offset(flashX, flashY),
                radius = radius
            ),
            radius = radius,
            center = Offset(flashX, flashY)
        )

        // 4-pointed radiant star spikes
        val spikeLen = radius * 1.3f
        val spikeW = radius * 0.15f
        scope.drawRect(
            color = Color(0xFFFFA500),
            topLeft = Offset(flashX - spikeLen, flashY - spikeW / 2f),
            size = Size(spikeLen * 2f, spikeW)
        )
        scope.drawRect(
            color = Color(0xFFFFA500),
            topLeft = Offset(flashX - spikeW / 2f, flashY - spikeLen),
            size = Size(spikeW, spikeLen * 2f)
        )

        // Ultra-hot incandescent white core
        scope.drawCircle(
            color = Color.White,
            radius = radius * 0.32f,
            center = Offset(flashX, flashY)
        )
    }

    /**
     * Tactical Health bar & threat level above enemy's head.
     */
    private fun drawTacticalHealthBar(
        scope: DrawScope,
        enemy: Enemy,
        centerX: Float,
        barY: Float,
        width: Float
    ) {
        val barW = width * 0.85f
        val barH = 5.5f
        val hpPct = (enemy.health / enemy.type.maxHp).coerceIn(0f, 1f)

        // Dark background with border
        scope.drawRect(
            color = Color(0xCC050A10),
            topLeft = Offset(centerX - barW / 2f, barY),
            size = Size(barW, barH)
        )
        scope.drawRect(
            color = Color(0xFF334155),
            topLeft = Offset(centerX - barW / 2f, barY),
            size = Size(barW, barH),
            style = Stroke(width = 1f)
        )

        val hpColor = when {
            hpPct > 0.5f -> Color(0xFF10B981) // Green
            hpPct > 0.25f -> Color(0xFFF59E0B) // Amber
            else -> Color(0xFFEF4444) // Red
        }

        scope.drawRect(
            color = hpColor,
            topLeft = Offset(centerX - barW / 2f, barY),
            size = Size(barW * hpPct, barH)
        )
    }
}
