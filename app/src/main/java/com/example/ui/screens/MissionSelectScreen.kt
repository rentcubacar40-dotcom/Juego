package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.game.LevelData
import com.example.game.LevelMission

@Composable
fun MissionSelectScreen(viewModel: GameViewModel) {
    val progressList = viewModel.levels.collectAsState().value
    var selectedLevelId by remember { mutableIntStateOf(1) }

    val mission1 = remember { LevelData.getLevel(1) }
    val mission2 = remember { LevelData.getLevel(2) }
    val mission3 = remember { LevelData.getLevel(3) }
    val mission4 = remember { LevelData.getLevel(4) }
    val allMissions = listOf(mission1, mission2, mission3, mission4)

    val selectedMission = allMissions.find { it.id == selectedLevelId } ?: mission1
    val selectedProgress = progressList.find { it.levelId == selectedLevelId }
    val isUnlocked = selectedProgress?.unlocked ?: (selectedLevelId == 1)

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(GameScreenState.MAIN_MENU) },
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color(0xFF131A26), CircleShape)
                        .border(1.dp, Color(0xFF2C394F), CircleShape)
                        .testTag("back_to_menu_button")
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
                        text = "DOSSIER DE OPERACIONES TÁCTICAS",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Selecciona una zona de inserción para desplegar las fuerzas especiales",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Two-Pane Layout: Left List of Missions, Right Mission Detail Dossier
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Mission Cards List
                LazyColumn(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(allMissions) { mission ->
                        val prog = progressList.find { it.levelId == mission.id }
                        val unlocked = prog?.unlocked ?: (mission.id == 1)
                        val isSelected = mission.id == selectedLevelId

                        MissionCardItem(
                            mission = mission,
                            unlocked = unlocked,
                            stars = prog?.stars ?: 0,
                            highScore = prog?.highScore ?: 0,
                            isSelected = isSelected,
                            onClick = { selectedLevelId = mission.id }
                        )
                    }
                }

                // Right Column: Mission Briefing & Launch
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C394F)),
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            // Top Tag & Codename
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color(selectedMission.themeColor).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(selectedMission.themeColor))
                                ) {
                                    Text(
                                        text = "OPERACIÓN ${selectedMission.id} • ${selectedMission.codename}",
                                        color = Color(selectedMission.themeColor),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (selectedMission.isNightOps) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = Color(0xFF00FF66),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "VISOR NVG REQUERIDO", color = Color(0xFF00FF66), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = selectedMission.title,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Ubicación: ${selectedMission.location}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Briefing text
                            Surface(
                                color = Color(0xFF131A26),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedMission.briefing,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Objectives
                            Text(
                                text = "OBJETIVOS DE LA MISIÓN:",
                                color = Color(0xFFF59E0B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Principal: ${selectedMission.primaryObjective}",
                                color = Color.White,
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "• Secundario: ${selectedMission.secondaryObjective}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        // Bottom Actions: Reward & Deploy Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "RECOMPENSA:", color = Color(0xFF64748B), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "+$${selectedMission.creditReward} CR",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Button(
                                onClick = {
                                    if (isUnlocked) {
                                        viewModel.startMission(selectedMission.id)
                                    }
                                },
                                enabled = isUnlocked,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF59E0B),
                                    disabledContainerColor = Color(0xFF1E2838)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(46.dp)
                                    .testTag("start_selected_mission_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isUnlocked) Icons.Default.PlayArrow else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (isUnlocked) Color(0xFF0F172A) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isUnlocked) "INICIAR INSERCIÓN" else "BLOQUEADO",
                                        color = if (isUnlocked) Color(0xFF0F172A) else Color(0xFF64748B),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionCardItem(
    mission: LevelMission,
    unlocked: Boolean,
    stars: Int,
    highScore: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> Color(0xFFF59E0B)
        unlocked -> Color(0xFF2C394F)
        else -> Color(0xFF1E2838)
    }

    Surface(
        color = if (isSelected) Color(0xFF1A2333) else Color(0xFF131A26),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked) { onClick() }
            .testTag("mission_card_${mission.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MISIÓN 0${mission.id}",
                        color = if (unlocked) Color(mission.themeColor) else Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = mission.codename,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = mission.title,
                    color = if (unlocked) Color.White else Color(0xFF64748B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (unlocked && highScore > 0) {
                    Text(
                        text = "Récord: $highScore pts",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (!unlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                // Star ratings (up to 3 stars)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= stars) Color(0xFFFBBF24) else Color(0xFF334155),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
