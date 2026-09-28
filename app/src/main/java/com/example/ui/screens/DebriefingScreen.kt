package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameScreenState
import com.example.game.GameViewModel

@Composable
fun DebriefingScreen(
    viewModel: GameViewModel,
    isVictory: Boolean
) {
    val level = viewModel.currentLevel.collectAsState().value

    BackHandler {
        viewModel.navigateTo(GameScreenState.MAIN_MENU)
    }

    val bannerColor = if (isVictory) Color(0xFF10B981) else Color(0xFFEF4444)
    val titleText = if (isVictory) "MISIÓN CUMPLIDA" else "BAJA EN ACCIÓN (K.I.A.)"
    val subtitleText = if (isVictory) {
        "Objetivos tácticos completados con éxito por la unidad de asalto"
    } else {
        "Operador neutralizado en combate. Extracción fallida."
    }

    val accuracyPercent = if (viewModel.shotsFired > 0) {
        ((viewModel.shotsHit.toFloat() / viewModel.shotsFired) * 100).toInt()
    } else 0

    val timeFormatted = "${viewModel.missionTimeSeconds / 60}:${(viewModel.missionTimeSeconds % 60).toString().padStart(2, '0')}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B12)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF0F172A),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.5f.dp, bannerColor),
            modifier = Modifier
                .width(460.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Status Header
                Text(
                    text = titleText,
                    color = bannerColor,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = subtitleText,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stars display (for victory)
                if (isVictory) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in 1..3) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Combat Stats Grid
                Surface(
                    color = Color(0xFF131A26),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatRow("TIEMPO EN OPERACIÓN", timeFormatted)
                        StatRow("HOSTILES ELIMINADOS", "${viewModel.missionKills} ${if (level != null) "/ " + level.targetKills else ""}")
                        StatRow("TIROS A LA CABEZA (CRÍTICOS)", "${viewModel.missionHeadshots}")
                        StatRow("PRECISIÓN DE DISPARO", "$accuracyPercent%")
                        if (isVictory && level != null) {
                            StatRow("CRÉDITOS OTORGADOS", "+$${level.creditReward} CR", Color(0xFFFBBF24))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (level != null) {
                                viewModel.startMission(level.id)
                            } else {
                                viewModel.navigateTo(GameScreenState.MAIN_MENU)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("retry_mission_button")
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "REINTENTAR", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.navigateTo(GameScreenState.MAIN_MENU) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("debrief_continue_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "VOLVER A LA BASE", color = Color(0xFF0F172A), fontWeight = FontWeight.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, valueColor: Color = Color.White) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
