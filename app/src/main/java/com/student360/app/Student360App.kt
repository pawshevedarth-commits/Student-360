package com.student360.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class Student360App : Application() {

    companion object {
        const val CHANNEL_LECTURES = "channel_lectures"
        const val CHANNEL_UNMARKED = "channel_unmarked"
        const val CHANNEL_STUDY = "channel_study"
        const val CHANNEL_GENERAL = "channel_general"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channels = listOf(
                NotificationChannel(
                    CHANNEL_LECTURES,
                    "Lecture Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications scheduled before classes start."
                },
                NotificationChannel(
                    CHANNEL_UNMARKED,
                    "Attendance Logs Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Alerts reminding you to log your attendance after class ends."
                },
                NotificationChannel(
                    CHANNEL_STUDY,
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Session timers and streak reminder alerts."
                },
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "General & Achievements",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Updates on goals, exam reminders, and achievements."
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
