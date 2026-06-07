package com.eye.protect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eye.protect.model.TimerMode
import com.eye.protect.service.TimerService
import com.eye.protect.ui.screen.LockOverlay
import com.eye.protect.ui.screen.MainScreen
import com.eye.protect.ui.theme.EyeProtectTheme
import com.eye.protect.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            EyeProtectTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                var showLockOverlay by remember { mutableStateOf(false) }

                // 监听 TimerService 工作结束回调 — 显示锁屏遮罩
                LaunchedEffect(Unit) {
                    TimerService.onWorkTimeUpCallback = { mode ->
                        showLockOverlay = true
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    MainScreen(
                        uiState = uiState,
                        onModeChange = { viewModel.setMode(it) },
                        onWorkDurationChange = { viewModel.setWorkDuration(it) },
                        onRestDurationChange = { viewModel.setRestDuration(it) },
                        onStartStop = {
                            if (uiState.isRunning) {
                                viewModel.stopTimer()
                            } else {
                                viewModel.startTimer()
                            }
                        }
                    )

                    // 锁屏遮罩覆盖层
                    if (showLockOverlay) {
                        LockOverlay(
                            countdownSeconds = uiState.countdownSeconds.toInt(),
                            restMinutes = if (uiState.mode == TimerMode.CYCLE) uiState.restDuration else 5,
                            onRestFinished = {
                                showLockOverlay = false
                                viewModel.recordLock()
                                // 循环模式自动开始下一轮
                                if (uiState.mode == TimerMode.CYCLE) {
                                    viewModel.restartTimer()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
