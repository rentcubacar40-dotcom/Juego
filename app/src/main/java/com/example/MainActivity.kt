package com.example

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.game.GameScreenState
import com.example.game.GameViewModel
import com.example.ui.screens.ArmoryScreen
import com.example.ui.screens.DebriefingScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MissionSelectScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemUi()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090D14)
                ) {
                    val currentScreen by viewModel.screenState.collectAsState()

                    when (currentScreen) {
                        GameScreenState.MAIN_MENU -> MainMenuScreen(viewModel)
                        GameScreenState.MISSION_SELECT -> MissionSelectScreen(viewModel)
                        GameScreenState.PLAYING,
                        GameScreenState.PAUSED -> GameScreen(viewModel)
                        GameScreenState.VICTORY_DEBRIEF -> DebriefingScreen(viewModel, isVictory = true)
                        GameScreenState.DEFEAT_DEBRIEF -> DebriefingScreen(viewModel, isVictory = false)
                        GameScreenState.ARMORY -> ArmoryScreen(viewModel)
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }
}
