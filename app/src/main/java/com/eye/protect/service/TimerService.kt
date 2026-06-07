package com.eye.protect.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.eye.protect.MainActivity
import com.eye.protect.R
import com.eye.protect.model.TimerMode
import com.eye.protect.util.Constants

class TimerService : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var currentMode: TimerMode = TimerMode.SINGLE
    private var workDurationSeconds: Long = 0
    private var restDurationSeconds: Long = 0
    private var countdownBeforeLock: Long = Constants.DEFAULT_COUNTDOWN_SECONDS
    private var isRestPeriod: Boolean = false

    companion object {
        var onTickCallback: ((Long) -> Unit)? = null
        var onWorkTimeUpCallback: ((TimerMode) -> Unit)? = null

        fun start(
            context: Context,
            mode: TimerMode,
            workDurationMinutes: Int,
            restDurationMinutes: Int,
            countdownSeconds: Long = Constants.DEFAULT_COUNTDOWN_SECONDS
        ) {
            val intent = Intent(context, TimerService::class.java).apply {
                putExtra(Constants.EXTRA_MODE, mode.name)
                putExtra(Constants.EXTRA_WORK_DURATION, workDurationMinutes)
                putExtra(Constants.EXTRA_REST_DURATION, restDurationMinutes)
                putExtra(Constants.EXTRA_COUNTDOWN_SECONDS, countdownSeconds)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = Constants.COMMAND_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Constants.COMMAND_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        currentMode = try {
            TimerMode.valueOf(intent?.getStringExtra(Constants.EXTRA_MODE) ?: TimerMode.SINGLE.name)
        } catch (_: Exception) {
            TimerMode.SINGLE
        }

        val workMin = intent?.getIntExtra(Constants.EXTRA_WORK_DURATION, Constants.DEFAULT_WORK_DURATION)
            ?: Constants.DEFAULT_WORK_DURATION
        val restMin = intent?.getIntExtra(Constants.EXTRA_REST_DURATION, Constants.DEFAULT_REST_DURATION)
            ?: Constants.DEFAULT_REST_DURATION
        countdownBeforeLock = intent?.getLongExtra(Constants.EXTRA_COUNTDOWN_SECONDS, Constants.DEFAULT_COUNTDOWN_SECONDS)
            ?: Constants.DEFAULT_COUNTDOWN_SECONDS

        workDurationSeconds = workMin * 60L
        restDurationSeconds = restMin * 60L

        startForeground(Constants.NOTIFICATION_ID, buildNotification(workDurationSeconds, false))
        startWorkTimer()

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        countDownTimer = null
        onTickCallback = null
        onWorkTimeUpCallback = null
    }

    private fun startWorkTimer() {
        countDownTimer?.cancel()
        isRestPeriod = false
        countDownTimer = object : CountDownTimer(workDurationSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                updateNotification(seconds, false)
                onTickCallback?.invoke(seconds)
            }

            override fun onFinish() {
                onWorkTimeUpCallback?.invoke(currentMode)
            }
        }.start()
    }

    fun startRestTimer() {
        countDownTimer?.cancel()
        isRestPeriod = true
        countDownTimer = object : CountDownTimer(restDurationSeconds * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                updateNotification(seconds, true)
                onTickCallback?.invoke(seconds)
            }

            override fun onFinish() {
                startWorkTimer()
            }
        }.start()
    }

    private fun buildNotification(seconds: Long, isRest: Boolean): android.app.Notification {
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = Constants.COMMAND_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val openIntent = Intent(this, MainActivity::class.java)
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isRest) "☕ 休息中" else getString(R.string.notification_timer_title)

        return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(formatTime(seconds))
            .setSmallIcon(R.drawable.ic_eye)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(R.drawable.ic_eye, getString(R.string.notification_stop), stopPendingIntent)
            .build()
    }

    private fun updateNotification(remainingSeconds: Long, isRest: Boolean) {
        val notification = buildNotification(remainingSeconds, isRest)
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(Constants.NOTIFICATION_ID, notification)
    }

    private fun formatTime(seconds: Long): String {
        val min = seconds / 60
        val sec = seconds % 60
        return "%02d:%02d".format(min, sec)
    }
}
