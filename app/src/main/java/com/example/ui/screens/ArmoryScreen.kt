package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameScreenState
import com.example.game.GameViewModel
import com.example.game.Weapon

@Composable
fun ArmoryScreen(viewModel: GameViewModel) {
    val context = LocalContext.current
    val profile = viewModel.playerProfile.collectAsState().value
    val weaponEntities = viewModel.weapons.collectAsState().value
    val allWeapons = remember { Weapon.createArsenal().values.toList() }
    var selectedWeaponId by remember { mutableStateOf("m4a1") }

    val selectedWeapon = allWeapons.find { it.id == selectedWeaponId } ?: allWeapons.first()
    val selectedEntity = weaponEntities.find { it.weaponId == selectedWeaponId }
    val isUnlocked = selectedEntity?.unlocked ?: (selectedWeapon.price == 0)
    val isEquipped = profile?.equippedWeaponId == selectedWeaponId

    BackHandler {
        viewModel.navigateTo(GameScreenState.MAIN_MENU)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateTo(GameScreenState.MAIN_MENU) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF131A26), CircleShape)
                            .border(1.dp, Color(0xFF2C394F), CircleShape)
                            .testTag("armory_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "ARMERÍA Y MANTENIMIENTO TÁCTICO",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Mejora potencia de fuego, cargadores y estabilidad balística",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                // Balance Chip
                Surface(
                    color = Color(0xFF131A26),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "FONDOS: ", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "$${profile?.credits ?: 0} CR",
                            color = Color(0xFFFBBF24),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two Pane Layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Weapon Catalog
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allWeapons) { weapon ->
                        val ent = weaponEntities.find { it.weaponId == weapon.id }
                        val unlocked = ent?.unlocked ?: (weapon.price == 0)
                        val equipped = profile?.equippedWeaponId == weapon.id
                        val isSelected = weapon.id == selectedWeaponId

                        WeaponListItem(
                            weapon = weapon,
                            unlocked = unlocked,
                            equipped = equipped,
                            isSelected = isSelected,
                            onClick = { selectedWeaponId = weapon.id }
                        )
                    }
                }

                // Right Column: Weapon Inspector & Upgrades
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C394F)),
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Weapon Title & Type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedWeapon.name,
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = selectedWeapon.type.name.replace("_", " "),
                                            color = Color(0xFF06B6D4),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${selectedWeapon.sciFiTier}",
                                            color = Color(0xFFF59E0B),
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (isEquipped) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                                    ) {
                                        Text(
                                            text = "EQUIPADO",
                                            color = Color(0xFF10B981),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = selectedWeapon.description,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Stats & Upgrade Sliders
                            if (isUnlocked) {
                                UpgradeStatRow(
                                    label = "DAÑO BALÍSTICO",
                                    currentLevel = selectedEntity?.damageLevel ?: 1,
                                    cost = 600,
                                    onUpgrade = {
                                        viewModel.upgradeWeaponStat(selectedWeapon.id, "damage", 600) { success ->
                                            if (!success) Toast.makeText(context, "Créditos insuficientes", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                UpgradeStatRow(
                                    label = "CAPACIDAD CARGADOR",
                                    currentLevel = selectedEntity?.capacityLevel ?: 1,
                                    cost = 450,
                                    onUpgrade = {
                                        viewModel.upgradeWeaponStat(selectedWeapon.id, "capacity", 450) { success ->
                                            if (!success) Toast.makeText(context, "Créditos insuficientes", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                UpgradeStatRow(
                                    label = "ESTABILIDAD / CONTROL",
                                    currentLevel = selectedEntity?.stabilityLevel ?: 1,
                                    cost = 500,
                                    onUpgrade = {
                                        viewModel.upgradeWeaponStat(selectedWeapon.id, "stability", 500) { success ->
                                            if (!success) Toast.makeText(context, "Créditos insuficientes", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(Color(0xFF131A26), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Arma bloqueada por inteligencia militar",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Precio de adquisición: $${selectedWeapon.price} CR",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Actions: Buy or Equip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!isUnlocked) {
                                Button(
                                    onClick = {
                                        viewModel.buyWeapon(selectedWeapon.id, selectedWeapon.price) { success ->
                                            if (!success) Toast.makeText(context, "Créditos insuficientes", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("buy_weapon_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "DESBLOQUEAR ($${selectedWeapon.price} CR)", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else if (!isEquipped) {
                                Button(
                                    onClick = { viewModel.equipWeapon(selectedWeapon.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("equip_weapon_button")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "EQUIPAR ARMA", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Text(
                                    text = "Arma actualmente lista para despliegue",
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeaponListItem(
    weapon: Weapon,
    unlocked: Boolean,
    equipped: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) Color(0xFF1E2838) else Color(0xFF131A26),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color(0xFFF59E0B) else Color(0xFF2C394F)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("weapon_catalog_${weapon.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = weapon.name,
                    color = if (unlocked) Color.White else Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${weapon.maxMagazine} balas • ${weapon.type.name}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (!unlocked) {
                Text(
                    text = "$${weapon.price} CR",
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            } else if (equipped) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Equipada",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun UpgradeStatRow(
    label: String,
    currentLevel: Int,
    cost: Int,
    onUpgrade: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(text = "Nivel $currentLevel / 5", color = Color(0xFFF59E0B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { currentLevel / 5.0f },
                color = Color(0xFFF59E0B),
                trackColor = Color(0xFF1E2838),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        if (currentLevel < 5) {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(text = "+$cost CR", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        } else {
            Text(text = "MAX", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}
