package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameScreenState
import com.example.game.GameViewModel
import com.example.ui.components.PauseDialog

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel
) {
    val profile = viewModel.playerProfile.collectAsState().value
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF070B12),
                        Color(0xFF0F172A),
                        Color(0xFF090D14)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Column: Branding, Title, Player Profile Card
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .padding(end = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Tactical Tag
                Surface(
                    color = Color(0x3300F5FF),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x8800F5FF))
                ) {
                    Text(
                        text = "NEXT-GEN 2026 HYPER-TACTICAL FPS",
                        color = Color(0xFF00F5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "STRIKE OPS 2026",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Incursión Táctica Hiper-Realista • Arsenal Cuántico & Balística 3D",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Player Career Card
                Surface(
                    color = Color(0xDD131A26),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C394F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF1E293B), CircleShape)
                                        .border(1.dp, Color(0xFFF59E0B), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MilitaryTech,
                                        contentDescription = "Rango",
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "OPERADOR DE ÉLITE",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Nivel de Acceso: Confidencial",
                                        color = Color(0xFF64748B),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Tactical Credits
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706))
                            ) {
                                Text(
                                    text = "$${profile?.credits ?: 1500} CR",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatColumn(label = "BAJAS TOTALES", value = "${profile?.totalKills ?: 0}")
                            StatColumn(label = "HEADSHOTS", value = "${profile?.totalHeadshots ?: 0}")
                            StatColumn(label = "MISIONES", value = "${profile?.missionsCompleted ?: 0}")
                        }
                    }
                }
            }

            // Right Column: Navigation Action Buttons
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Campaign / Missions
                Button(
                    onClick = { viewModel.navigateTo(GameScreenState.MISSION_SELECT) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("deploy_campaign_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DESPLEGAR CAMPAÑA",
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Survival Mode
                Button(
                    onClick = { viewModel.startMission(4) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("play_survival_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MODO SUPERVIVENCIA (OLEADAS)",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Armory / Upgrades
                Button(
                    onClick = { viewModel.navigateTo(GameScreenState.ARMORY) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2838)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF06B6D4)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_armory_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ARMERÍA Y MEJORAS",
                            color = Color(0xFF06B6D4),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Settings
                OutlinedButton(
                    onClick = { showSettings = true },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("settings_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AJUSTES TÁCTICOS",
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Settings Modal
        if (showSettings) {
            PauseDialog(
                viewModel = viewModel,
                onResume = { showSettings = false },
                onRestart = { showSettings = false },
                onQuit = { showSettings = false }
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
