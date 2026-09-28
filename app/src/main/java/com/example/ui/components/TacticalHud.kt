package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.Enemy
import com.example.game.FireMode
import com.example.game.GameViewModel
import com.example.game.ItemType
import com.example.game.LevelMission
import com.example.game.Weapon
import com.example.game.WorldItem
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun TacticalHud(
    viewModel: GameViewModel,
    level: LevelMission,
    currentWeapon: Weapon,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {

        // 1. Right Look/Aim Touch Swipe Area (covers right half of screen)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 220.dp) // Leave left area for joystick
                .testTag("look_swipe_surface")
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        viewModel.onLookSwipe(dragAmount.x, dragAmount.y)
                    }
                }
        )

        // 2. Damage Vignette Flash when taking hits
        if (viewModel.damageVignetteAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xDCB91C1C).copy(alpha = viewModel.damageVignetteAlpha)
                            )
                        )
                    )
            )
        }

        // 3. Central Hit Marker & Reticle
        CentralReticleAndHitmarker(
            isAds = viewModel.isAimingDownSights,
            hasScope = currentWeapon.hasScope,
            hitVisible = viewModel.hitMarker.value.visible,
            isHeadshot = viewModel.hitMarker.value.isHeadshot,
            modifier = Modifier.align(Alignment.Center)
        )

        // 4. Top Bar: Mini Radar, Mission Objective Chip, Compass, Pause Button
        TopTacticalBar(
            viewModel = viewModel,
            level = level,
            onPauseClick = onPauseClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )

        // 5. Left Side Controls: Virtual Movement Joystick & Tactical Stance Toggles
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Tactical Stance Row (Sprint, Crouch, NVG)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Sprint button
                TacticalSmallToggle(
                    icon = Icons.Default.DirectionsRun,
                    active = viewModel.isSprinting,
                    label = "SPRINT",
                    testTag = "sprint_button",
                    onClick = { viewModel.toggleSprint() }
                )
                // Crouch button
                TacticalSmallToggle(
                    icon = Icons.Default.Adjust,
                    active = viewModel.isCrouching,
                    label = "AGACH",
                    testTag = "crouch_button",
                    onClick = { viewModel.toggleCrouch() }
                )
                // Night Vision Goggles
                TacticalSmallToggle(
                    icon = Icons.Default.Visibility,
                    active = viewModel.nightVision,
                    label = "NVG",
                    testTag = "nvg_button",
                    onClick = { viewModel.toggleNightVision() }
                )
            }

            // Virtual Movement Joystick
            VirtualJoystick(
                onMove = { x, y -> viewModel.onMoveInput(x, y) }
            )
        }

        // 6. Center Bottom: Health & Armor Vital Status Bars
        VitalStatusWidget(
            health = viewModel.playerHealth,
            maxHealth = viewModel.maxPlayerHealth,
            armor = viewModel.playerArmor,
            maxArmor = viewModel.maxPlayerArmor,
            stamina = viewModel.stamina,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        )

        // 7. Right Side Controls: Weapon Carousel, Fire Button, ADS, Reload
        RightCombatControls(
            viewModel = viewModel,
            currentWeapon = currentWeapon,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
private fun TopTacticalBar(
    viewModel: GameViewModel,
    level: LevelMission,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Mini Radar Widget
        MiniRadarWidget(
            playerX = viewModel.playerX,
            playerY = viewModel.playerY,
            playerAngle = viewModel.playerAngle,
            enemies = viewModel.activeEnemies,
            items = viewModel.activeItems
        )

        // Mission Objective Card
        Surface(
            color = Color(0xCC0F172A),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C394F)),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (level.isEndless) "OLEADA ${viewModel.survivalWave}" else level.codename,
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${viewModel.missionTimeSeconds / 60}:${(viewModel.missionTimeSeconds % 60).toString().padStart(2, '0')}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                val targetText = if (level.isEndless) {
                    "Bajas: ${viewModel.missionKills} (Récord)"
                } else {
                    "Hostiles: ${viewModel.missionKills}/${level.targetKills} • ${if (viewModel.intelCollected) "INTEL ASEGURADO" else "Intel Pendiente"}"
                }
                Text(
                    text = targetText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Pause Button
        IconButton(
            onClick = onPauseClick,
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xCC0F172A), CircleShape)
                .border(1.dp, Color(0xFF334155), CircleShape)
                .testTag("pause_button")
        ) {
            Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pausa",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun MiniRadarWidget(
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    enemies: List<Enemy>,
    items: List<WorldItem>,
    size: Float = 75f
) {
    Canvas(
        modifier = Modifier
            .size(75.dp)
            .clip(CircleShape)
            .background(Color(0xD9050B14))
            .border(1.5f.dp, Color(0xFF06B6D4), CircleShape)
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.width / 2f * 0.9f
        val radarScale = radius / 9.0f // 9 map units view distance

        // Radar grid circles
        drawCircle(color = Color(0x2200E5FF), radius = radius * 0.5f, center = center, style = Stroke(1f))
        drawCircle(color = Color(0x3300E5FF), radius = radius, center = center, style = Stroke(1f))
        drawLine(color = Color(0x2200E5FF), start = Offset(center.x - radius, center.y), end = Offset(center.x + radius, center.y), strokeWidth = 1f)
        drawLine(color = Color(0x2200E5FF), start = Offset(center.x, center.y - radius), end = Offset(center.x, center.y + radius), strokeWidth = 1f)

        // Draw Player blip in center (amber triangle pointing forward)
        val pPath = Path().apply {
            moveTo(center.x, center.y - 6f)
            lineTo(center.x - 4f, center.y + 4f)
            lineTo(center.x + 4f, center.y + 4f)
            close()
        }
        drawPath(pPath, color = Color(0xFFF59E0B))

        // Draw Enemies (red blips) rotated relative to player heading
        for (enemy in enemies) {
            if (enemy.isDead) continue
            val dx = enemy.x - playerX
            val dy = enemy.y - playerY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > 9.0f) continue

            // Rotate relative to player angle
            val enemyAngle = atan2(dy, dx)
            val relAngle = enemyAngle - playerAngle + (PI / 2f).toFloat()
            val blipX = center.x + cos(relAngle) * (dist * radarScale)
            val blipY = center.y + sin(relAngle) * (dist * radarScale)

            drawCircle(
                color = Color(0xFFEF4444),
                radius = 3f,
                center = Offset(blipX, blipY)
            )
        }

        // Draw Objectives / Intel (cyan diamond)
        for (item in items) {
            if (item.collected || item.type != ItemType.MISSION_INTEL) continue
            val dx = item.x - playerX
            val dy = item.y - playerY
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > 14.0f) continue

            val itemAngle = atan2(dy, dx)
            val relAngle = itemAngle - playerAngle + (PI / 2f).toFloat()
            val blipX = center.x + cos(relAngle) * (dist.coerceAtMost(9.0f) * radarScale)
            val blipY = center.y + sin(relAngle) * (dist.coerceAtMost(9.0f) * radarScale)

            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 4f,
                center = Offset(blipX, blipY)
            )
        }
    }
}

@Composable
private fun CentralReticleAndHitmarker(
    isAds: Boolean,
    hasScope: Boolean,
    hitVisible: Boolean,
    isHeadshot: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            // Hipfire Crosshairs (2026 Smart Adaptive Reticle)
            if (!isAds) {
                val crossColor = Color(0xCC00F5FF)
                val gap = 14f
                val length = 12f
                val stroke = 2.0f

                // Corner target brackets
                // Top-Left bracket
                drawLine(crossColor, Offset(center.x - gap - length, center.y - gap), Offset(center.x - gap, center.y - gap), stroke)
                drawLine(crossColor, Offset(center.x - gap, center.y - gap - length), Offset(center.x - gap, center.y - gap), stroke)
                // Top-Right bracket
                drawLine(crossColor, Offset(center.x + gap, center.y - gap), Offset(center.x + gap + length, center.y - gap), stroke)
                drawLine(crossColor, Offset(center.x + gap, center.y - gap - length), Offset(center.x + gap, center.y - gap), stroke)
                // Bottom-Left bracket
                drawLine(crossColor, Offset(center.x - gap - length, center.y + gap), Offset(center.x - gap, center.y + gap), stroke)
                drawLine(crossColor, Offset(center.x - gap, center.y + gap + length), Offset(center.x - gap, center.y + gap), stroke)
                // Bottom-Right bracket
                drawLine(crossColor, Offset(center.x + gap, center.y + gap), Offset(center.x + gap + length, center.y + gap), stroke)
                drawLine(crossColor, Offset(center.x + gap, center.y + gap + length), Offset(center.x + gap, center.y + gap), stroke)

                // Micro center point
                drawCircle(color = Color(0xFF00F5FF), radius = 2.0f, center = center)
            } else if (!hasScope) {
                // Quantum Holographic Sight Display (ADS)
                val reticleNeon = Color(0xFFFF0055)
                val outerRing = Color(0x6600F5FF)

                drawCircle(color = reticleNeon, radius = 3.5f, center = center)
                drawCircle(color = outerRing, radius = 18f, center = center, style = Stroke(1.5f))
                drawCircle(color = Color(0x33FF0055), radius = 32f, center = center, style = Stroke(1.0f))

                // Elevation marks
                drawLine(reticleNeon, Offset(center.x - 6f, center.y + 12f), Offset(center.x + 6f, center.y + 12f), 1.5f)
                drawLine(reticleNeon, Offset(center.x - 4f, center.y + 20f), Offset(center.x + 4f, center.y + 20f), 1.5f)
            }

            // Hit Marker (Diagonal ticks 'X' with critical pulse)
            if (hitVisible) {
                val hitColor = if (isHeadshot) Color(0xFFFF0033) else Color(0xFF00FFCC)
                val hitSize = if (isHeadshot) 18f else 13f
                val stroke = if (isHeadshot) 3.5f else 2.2f

                drawLine(hitColor, Offset(center.x - hitSize, center.y - hitSize), Offset(center.x - 5f, center.y - 5f), stroke)
                drawLine(hitColor, Offset(center.x + 5f, center.y + 5f), Offset(center.x + hitSize, center.y + hitSize), stroke)
                drawLine(hitColor, Offset(center.x + hitSize, center.y - hitSize), Offset(center.x + 5f, center.y - 5f), stroke)
                drawLine(hitColor, Offset(center.x - 5f, center.y + 5f), Offset(center.x - hitSize, center.y + hitSize), stroke)
            }
        }

        if (hitVisible && isHeadshot) {
            Text(
                text = "HEADSHOT!",
                color = Color(0xFFEF4444),
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 40.dp)
            )
        }
    }
}

@Composable
private fun VitalStatusWidget(
    health: Float,
    maxHealth: Float,
    armor: Float,
    maxArmor: Float,
    stamina: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xD9090D14),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C394F)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Health Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "Salud",
                    tint = if (health > 30f) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Segmented bar
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E2838))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(health / maxHealth)
                            .height(8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = if (health > 30f)
                                        listOf(Color(0xFF059669), Color(0xFF10B981))
                                    else
                                        listOf(Color(0xFF991B1B), Color(0xFFEF4444))
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${health.toInt()} HP",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Armor Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Blindaje",
                    tint = Color(0xFF06B6D4),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF1E2838))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((armor / maxArmor).coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(Color(0xFF06B6D4))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${armor.toInt()} AP",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun RightCombatControls(
    viewModel: GameViewModel,
    currentWeapon: Weapon,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Weapon Switch Quick Bar
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            viewModel.availableWeapons.keys.forEach { weaponId ->
                val weapon = viewModel.availableWeapons[weaponId] ?: return@forEach
                val isSelected = currentWeapon.id == weaponId
                Surface(
                    color = if (isSelected) Color(0xFFF59E0B) else Color(0xAA0F172A),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFFFBBF24) else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .clickable { viewModel.switchWeapon(weaponId) }
                        .testTag("weapon_slot_$weaponId")
                ) {
                    Text(
                        text = weapon.name.substringBefore(" ").uppercase(),
                        color = if (isSelected) Color(0xFF0B0F19) else Color(0xFFCBD5E1),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Ammunition Counter Box
        Surface(
            color = Color(0xD90F172A),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C394F))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentWeapon.name.uppercase(),
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${currentWeapon.currentAmmo}",
                            color = if (currentWeapon.currentAmmo > 5) Color(0xFFF59E0B) else Color(0xFFEF4444),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = " / ${currentWeapon.reserveAmmo}",
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                // Fire Mode Toggle
                Surface(
                    color = Color(0xFF1E2838),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.clickable { viewModel.toggleFireMode() }
                ) {
                    Text(
                        text = if (viewModel.fireMode == FireMode.FULL_AUTO) "AUTO" else "SEMI",
                        color = Color(0xFF06B6D4),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Main Actions Cluster (Reload, ADS, Fire Button)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reload Button
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color(0xD91E2838), CircleShape)
                    .border(1.5f.dp, Color(0xFF475569), CircleShape)
                    .clickable { viewModel.onReloadPress() }
                    .testTag("reload_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recargar",
                    tint = if (viewModel.isReloading) Color(0xFFF59E0B) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // ADS / Scope Aim Button
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(
                        if (viewModel.isAimingDownSights) Color(0xFFF59E0B) else Color(0xD91E2838),
                        CircleShape
                    )
                    .border(2.dp, Color(0xFFF59E0B), CircleShape)
                    .clickable { viewModel.toggleAds() }
                    .testTag("ads_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveRedEye,
                    contentDescription = "Apuntar",
                    tint = if (viewModel.isAimingDownSights) Color(0xFF0F172A) else Color(0xFFF59E0B),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Primary FIRE Button (Large 80dp circular button with touch press/release)
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFDC2626), Color(0xFF991B1B))
                        ),
                        CircleShape
                    )
                    .border(3.dp, Color(0xFFFCA5A5), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.onFirePress()
                                tryAwaitRelease()
                                viewModel.onFireRelease()
                            }
                        )
                    }
                    .testTag("fire_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Adjust,
                    contentDescription = "Disparar",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }
    }
}

@Composable
private fun TacticalSmallToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (active) Color(0xFFF59E0B) else Color(0xCC0F172A),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) Color(0xFFFBBF24) else Color(0xFF334155)
        ),
        modifier = Modifier
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = if (active) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
