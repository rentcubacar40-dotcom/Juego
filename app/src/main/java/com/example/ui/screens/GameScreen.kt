package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.engine.Raycaster3D
import com.example.game.GameScreenState
import com.example.game.GameViewModel
import com.example.ui.components.PauseDialog
import com.example.ui.components.TacticalHud

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val level = viewModel.currentLevel.collectAsState().value ?: return
    val currentWeapon = viewModel.currentWeapon.collectAsState().value
    val raycaster = remember { Raycaster3D() }
    var isPaused by remember { mutableStateOf(false) }

    BackHandler {
        isPaused = !isPaused
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Raycaster 3D Render Canvas (60 FPS Game Engine)
        Canvas(modifier = Modifier.fillMaxSize()) {
            raycaster.render(
                drawScope = this,
                map = level.map,
                mapWidth = level.mapWidth,
                mapHeight = level.mapHeight,
                playerX = viewModel.playerX,
                playerY = viewModel.playerY,
                playerAngle = viewModel.playerAngle,
                playerPitch = viewModel.playerPitch,
                crouchOffset = viewModel.crouchOffset,
                headBob = viewModel.headBob,
                nightVision = viewModel.nightVision,
                enemies = viewModel.activeEnemies,
                items = viewModel.activeItems,
                effects = viewModel.activeEffects,
                currentWeapon = currentWeapon,
                isAimingDownSights = viewModel.isAimingDownSights,
                adsProgress = viewModel.adsProgress,
                recoilOffset = viewModel.recoilOffset,
                isShootingFlash = viewModel.isShootingFlash,
                reloadProgress = viewModel.reloadProgress
            )
        }

        // 2. Tactical Military HUD Overlay
        TacticalHud(
            viewModel = viewModel,
            level = level,
            currentWeapon = currentWeapon,
            onPauseClick = { isPaused = true }
        )

        // 3. Pause Dialog if active
        if (isPaused) {
            PauseDialog(
                viewModel = viewModel,
                onResume = { isPaused = false },
                onRestart = {
                    isPaused = false
                    viewModel.startMission(level.id)
                },
                onQuit = {
                    isPaused = false
                    viewModel.navigateTo(GameScreenState.MAIN_MENU)
                }
            )
        }
    }
}
