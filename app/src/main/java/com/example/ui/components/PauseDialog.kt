package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.game.GameViewModel

@Composable
fun PauseDialog(
    viewModel: GameViewModel,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onQuit: () -> Unit
) {
    var sensitivity by remember { mutableFloatStateOf(viewModel.lookSensitivity) }
    var soundEnabled by remember { mutableStateOf(viewModel.soundManager.soundEnabled) }
    var hapticsEnabled by remember { mutableStateOf(viewModel.soundManager.hapticsEnabled) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xBB050810)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xFF131A26),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.5f.dp, Color(0xFF2C394F)),
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MISIÓN EN PAUSA",
                    color = Color(0xFFF59E0B),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Ajustes de Combate Táctico",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Sensitivity Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sensibilidad de Mira", color = Color.White, fontSize = 12.sp)
                        Text(
                            text = String.format("%.1fx", sensitivity),
                            color = Color(0xFFF59E0B),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = sensitivity,
                        onValueChange = {
                            sensitivity = it
                            viewModel.lookSensitivity = it
                        },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF59E0B),
                            activeTrackColor = Color(0xFFF59E0B)
                        ),
                        modifier = Modifier.testTag("sensitivity_slider")
                    )
                }

                // Audio & Haptics Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Efectos de Sonido", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            viewModel.soundManager.soundEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF59E0B),
                            checkedTrackColor = Color(0xFFB45309)
                        ),
                        modifier = Modifier.testTag("sound_toggle")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Retroalimentación Háptica", color = Color.White, fontSize = 13.sp)
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            viewModel.soundManager.hapticsEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFF59E0B),
                            checkedTrackColor = Color(0xFFB45309)
                        ),
                        modifier = Modifier.testTag("haptics_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("resume_mission_button")
                ) {
                    Text(text = "REANUDAR COMBATE", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRestart,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("restart_mission_button")
                    ) {
                        Text(text = "REINICIAR", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onQuit,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("abort_mission_button")
                    ) {
                        Text(text = "ABORTAR", color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
