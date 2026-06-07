package com.eye.protect

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eye.protect.model.TimerMode
import com.eye.protect.receiver.DeviceAdminReceiver
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

                // 每次从设置页返回时，刷新设备管理员状态
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.checkDeviceAdmin()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

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
                        },
                        onEnableDeviceAdmin = { openDeviceAdminSettings(this@MainActivity) }
                    )

                    // 锁屏遮罩覆盖层
                    if (showLockOverlay) {
                        LockOverlay(
                            countdownSeconds = uiState.countdownSeconds.toInt(),
                            onCountdownFinished = {
                                showLockOverlay = false
                                viewModel.onTimerFinished()
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

    private fun openDeviceAdminSettings(context: Context) {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    DeviceAdminReceiver.getComponentName(context)
                )
                putExtra(
                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    context.getString(R.string.device_admin_explanation)
                )
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果系统不支持直接跳转，回退到安全设置页面
            try {
                val fallback = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                context.startActivity(fallback)
            } catch (_: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "请手动前往：设置 → 安全 → 设备管理 → 开启护眼锁屏",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
