package com.eye.protect.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eye.protect.data.repository.StatRepository
import com.eye.protect.data.settings.SettingsDataStore
import com.eye.protect.model.MainUiState
import com.eye.protect.model.TimerMode
import com.eye.protect.receiver.DeviceAdminReceiver
import com.eye.protect.service.TimerService
import com.eye.protect.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val statRepository = StatRepository(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        observeStats()
        checkDeviceAdmin()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.mode.collect { mode ->
                _uiState.update { it.copy(mode = mode) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.workDuration.collect { duration ->
                _uiState.update {
                    it.copy(
                        workDuration = duration,
                        remainingSeconds = duration * 60
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsDataStore.restDuration.collect { duration ->
                _uiState.update { it.copy(restDuration = duration) }
            }
        }
        viewModelScope.launch {
            settingsDataStore.countdownSeconds.collect { seconds ->
                _uiState.update { it.copy(countdownSeconds = seconds.toLong()) }
            }
        }
    }

    private fun observeStats() {
        viewModelScope.launch {
            statRepository.getLockCountToday().collect { count ->
                _uiState.update { it.copy(totalLocksToday = count) }
            }
        }
        viewModelScope.launch {
            statRepository.getTotalWorkMinutesToday().collect { minutes ->
                _uiState.update { it.copy(totalWorkMinutesToday = minutes) }
            }
        }
    }

    fun checkDeviceAdmin() {
        val isActive = DeviceAdminReceiver.isActive(getApplication())
        _uiState.update { it.copy(isDeviceAdminActive = isActive) }
    }

    fun setMode(mode: TimerMode) {
        viewModelScope.launch {
            settingsDataStore.setMode(mode)
        }
    }

    fun setWorkDuration(minutes: Int) {
        val clamped = minutes.coerceIn(Constants.MIN_DURATION, Constants.MAX_DURATION)
        viewModelScope.launch {
            settingsDataStore.setWorkDuration(clamped)
        }
    }

    fun setRestDuration(minutes: Int) {
        val clamped = minutes.coerceIn(Constants.MIN_DURATION, Constants.MAX_DURATION)
        viewModelScope.launch {
            settingsDataStore.setRestDuration(clamped)
        }
    }

    fun startTimer(): Boolean {
        val state = _uiState.value
        if (!state.isDeviceAdminActive) {
            _uiState.update { it.copy(isDeviceAdminActive = false) } // force refresh
            checkDeviceAdmin()
            return false
        }

        _uiState.update { it.copy(isRunning = true) }

        TimerService.onTickCallback = { seconds ->
            _uiState.update { it.copy(remainingSeconds = seconds.toInt()) }
        }
        TimerService.onWorkTimeUpCallback = { mode ->
            _uiState.update { it.copy(isRunning = false) }
            viewModelScope.launch {
                statRepository.recordLock(
                    workDuration = state.workDuration,
                    mode = state.mode.name
                )
            }
        }

        TimerService.start(
            getApplication(),
            state.mode,
            state.workDuration,
            state.restDuration,
            state.countdownSeconds
        )
        return true
    }

    fun stopTimer() {
        TimerService.stop(getApplication())
        _uiState.update {
            it.copy(
                isRunning = false,
                remainingSeconds = it.workDuration * 60
            )
        }
    }

    fun onTimerFinished() {
        DeviceAdminReceiver.lockNow(getApplication())
    }

    fun restartTimer() {
        startTimer()
    }
}
