package com.eye.protect.model

import com.eye.protect.util.Constants

data class MainUiState(
    val mode: TimerMode = TimerMode.SINGLE,
    val workDuration: Int = Constants.DEFAULT_WORK_DURATION,
    val restDuration: Int = Constants.DEFAULT_REST_DURATION,
    val isRunning: Boolean = false,
    val remainingSeconds: Int = Constants.DEFAULT_WORK_DURATION * 60,
    val totalLocksToday: Int = 0,
    val totalWorkMinutesToday: Int = 0,
    val isDeviceAdminActive: Boolean = false,
    val countdownSeconds: Long = Constants.DEFAULT_COUNTDOWN_SECONDS
)
