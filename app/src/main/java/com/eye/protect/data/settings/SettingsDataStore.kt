package com.eye.protect.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.eye.protect.model.TimerMode
import com.eye.protect.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "eye_protect_settings")

class SettingsDataStore(private val context: Context) {

    private object PreferencesKeys {
        val MODE = stringPreferencesKey(Constants.DS_MODE)
        val WORK_DURATION = intPreferencesKey(Constants.DS_WORK_DURATION)
        val REST_DURATION = intPreferencesKey(Constants.DS_REST_DURATION)
        val COUNTDOWN_SECONDS = intPreferencesKey(Constants.DS_COUNTDOWN_SECONDS)
    }

    val mode: Flow<TimerMode> = context.dataStore.data.map { prefs ->
        val name = prefs[PreferencesKeys.MODE] ?: TimerMode.SINGLE.name
        try { TimerMode.valueOf(name) } catch (_: Exception) { TimerMode.SINGLE }
    }

    val workDuration: Flow<Int> = context.dataStore.data.map {
        it[PreferencesKeys.WORK_DURATION] ?: Constants.DEFAULT_WORK_DURATION
    }

    val restDuration: Flow<Int> = context.dataStore.data.map {
        it[PreferencesKeys.REST_DURATION] ?: Constants.DEFAULT_REST_DURATION
    }

    val countdownSeconds: Flow<Int> = context.dataStore.data.map {
        it[PreferencesKeys.COUNTDOWN_SECONDS] ?: Constants.DEFAULT_COUNTDOWN_SECONDS.toInt()
    }

    suspend fun setMode(mode: TimerMode) {
        context.dataStore.edit {
            it[PreferencesKeys.MODE] = mode.name
        }
    }

    suspend fun setWorkDuration(minutes: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.WORK_DURATION] = minutes
        }
    }

    suspend fun setRestDuration(minutes: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.REST_DURATION] = minutes
        }
    }

    suspend fun setCountdownSeconds(seconds: Int) {
        context.dataStore.edit {
            it[PreferencesKeys.COUNTDOWN_SECONDS] = seconds
        }
    }
}
