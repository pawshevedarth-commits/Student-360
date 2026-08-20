package com.student360.app.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.student360.app.data.local.entity.TimetableEntry
import java.util.*

object NotificationScheduler {

    fun scheduleLectureAlarm(context: Context, entry: TimetableEntry, subjectName: String, minutesBefore: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.student360.app.ACTION_LECTURE_ALARM"
            putExtra("EXTRA_SUBJECT_NAME", subjectName)
            putExtra("EXTRA_ROOM", entry.room)
            putExtra("EXTRA_TIME", entry.startTime)
            putExtra("EXTRA_ENTRY_ID", entry.id)
            putExtra("EXTRA_MINUTES_BEFORE", minutesBefore)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entry.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = getNextLectureTimeMillis(entry.dayOfWeek, entry.startTime, minutesBefore)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelLectureAlarm(context: Context, entryId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = "com.student360.app.ACTION_LECTURE_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            entryId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun getNextLectureTimeMillis(dayOfWeek: Int, startTime: String, minutesBefore: Int): Long {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        // Mapping: Entry dayOfWeek is 0 (Mon) to 5 (Sat)
        // Calendar days are Sunday = 1, Monday = 2, ..., Saturday = 7
        val targetCalendarDay = when (dayOfWeek) {
            0 -> Calendar.MONDAY
            1 -> Calendar.TUESDAY
            2 -> Calendar.WEDNESDAY
            3 -> Calendar.THURSDAY
            4 -> Calendar.FRIDAY
            5 -> Calendar.SATURDAY
            else -> Calendar.MONDAY
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, -minutesBefore)
        }

        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        var daysDiff = targetCalendarDay - currentDay
        
        if (daysDiff < 0 || (daysDiff == 0 && calendar.timeInMillis <= System.currentTimeMillis())) {
            daysDiff += 7
        }
        
        calendar.add(Calendar.DAY_OF_YEAR, daysDiff)
        return calendar.timeInMillis
    }
}
