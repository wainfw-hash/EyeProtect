package com.eye.protect

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.eye.protect.util.Constants

class EyeProtectApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            Constants.CHANNEL_ID,
            getString(R.string.timer_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.timer_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
