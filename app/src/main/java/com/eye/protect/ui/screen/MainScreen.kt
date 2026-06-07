package com.eye.protect.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eye.protect.model.MainUiState
import com.eye.protect.model.TimerMode
import com.eye.protect.ui.theme.LightGreenBg
import com.eye.protect.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: MainUiState,
    onModeChange: (TimerMode) -> Unit,
    onWorkDurationChange: (Int) -> Unit,
    onRestDurationChange: (Int) -> Unit,
    onStartStop: () -> Unit,
    onEnableDeviceAdmin: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("护眼锁屏")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 设备管理员权限提示
            if (!uiState.isDeviceAdminActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "需要开启设备管理权限才能锁屏",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onEnableDeviceAdmin) {
                            Text("去设置")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // 今日统计卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = LightGreenBg
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = "${uiState.totalLocksToday}",
                        label = "今日锁屏"
                    )
                    StatItem(
                        value = "${uiState.totalWorkMinutesToday}",
                        label = "累计工作(分钟)"
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 设置区卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("模式", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimerMode.entries.forEach { mode ->
                            FilterChip(
                                selected = uiState.mode == mode,
                                onClick = { onModeChange(mode) },
                                label = { Text(mode.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 工作时长
                    DurationSelector(
                        label = "工作时长",
                        value = uiState.workDuration,
                        onValueChange = onWorkDurationChange
                    )

                    // 循环模式显示休息时长
                    if (uiState.mode == TimerMode.CYCLE) {
                        Spacer(Modifier.height(12.dp))
                        DurationSelector(
                            label = "休息时长",
                            value = uiState.restDuration,
                            onValueChange = onRestDurationChange
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // 剩余时间显示
            if (uiState.isRunning) {
                Text(
                    text = formatTime(uiState.remainingSeconds),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
            }

            // 开始/停止按钮
            Button(
                onClick = onStartStop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRunning)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ),
                enabled = uiState.isDeviceAdminActive
            ) {
                Icon(
                    imageVector = if (uiState.isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isRunning) "停止" else "开始",
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = if (uiState.isRunning) "点击停止" else "点击开始",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DurationSelector(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { onValueChange(value - Constants.DURATION_STEP) }) {
            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            text = "$value ${Constants.MINUTES}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center
        )
        IconButton(onClick = { onValueChange(value + Constants.DURATION_STEP) }) {
            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatTime(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}
