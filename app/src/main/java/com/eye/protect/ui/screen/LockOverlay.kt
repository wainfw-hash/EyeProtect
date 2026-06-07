package com.eye.protect.ui.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eye.protect.ui.theme.Green500
import com.eye.protect.ui.theme.OverlayBackground
import com.eye.protect.ui.theme.White
import kotlinx.coroutines.delay

@Composable
fun LockOverlay(
    countdownSeconds: Int = 5,
    onCountdownFinished: () -> Unit
) {
    var currentSecond by remember { mutableIntStateOf(countdownSeconds) }

    LaunchedEffect(Unit) {
        for (i in countdownSeconds downTo 1) {
            delay(1000L)
            currentSecond = i - 1
        }
        onCountdownFinished()
    }

    val progress by animateFloatAsState(
        targetValue = if (currentSecond > 0)
            (countdownSeconds - currentSecond).toFloat() / countdownSeconds
        else
            1f,
        animationSpec = tween(durationMillis = 500)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OverlayBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "👁️",
                fontSize = 64.sp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = "该休息啦！",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // 圆形进度条
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // 背景圆环
                    drawArc(
                        color = Color.White.copy(alpha = 0.3f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 进度圆环（从满到空）
                    drawArc(
                        color = Green500,
                        startAngle = -90f,
                        sweepAngle = 360f * (1f - progress),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "${currentSecond}",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "${currentSecond} 秒后自动锁屏",
                fontSize = 16.sp,
                color = White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}
