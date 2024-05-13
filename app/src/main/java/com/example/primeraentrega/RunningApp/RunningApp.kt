package com.example.primeraentrega.RunningApp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class RunningApp: Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "channel";
            val description = "channel description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("notificacion", name, importance)
            channel.setDescription(description)
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel (channel)
        }
    }
}