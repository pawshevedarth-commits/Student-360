package com.student360.app.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.student360.app.MainActivity
import android.app.PendingIntent
import com.student360.app.Student360App
import com.student360.app.data.repository.StudentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val repository = StudentRepository(context.applicationContext)
            CoroutineScope(Dispatchers.IO).launch {
                val entries = repository.getAllTimetable()
                entries.forEach { entry ->
                    val subject = repository.getSubjectById(entry.subjectId)
                    if (subject != null) {
                        NotificationScheduler.scheduleLectureAlarm(
                            context.applicationContext,
                            entry,
                            subject.name,
                            10 // Reschedule using a default pre-alert window
                        )
                    }
                }
            }
            return
        }

        val subjectName = intent.getStringExtra("EXTRA_SUBJECT_NAME") ?: "Class"
        val room = intent.getStringExtra("EXTRA_ROOM") ?: "Room"
        val time = intent.getStringExtra("EXTRA_TIME") ?: "Time"
        val entryId = intent.getIntExtra("EXTRA_ENTRY_ID", 0)
        val minutesBefore = intent.getIntExtra("EXTRA_MINUTES_BEFORE", 10)

        showNotification(context, entryId, subjectName, room, time, minutesBefore)
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        subjectName: String,
        room: String,
        time: String,
        minutesBefore: Int
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = if (minutesBefore > 0) {
            "$subjectName starts in $minutesBefore minutes in $room at $time."
        } else {
            "$subjectName is starting now in $room."
        }

        val builder = NotificationCompat.Builder(context, Student360App.CHANNEL_LECTURES)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Upcoming Lecture Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
