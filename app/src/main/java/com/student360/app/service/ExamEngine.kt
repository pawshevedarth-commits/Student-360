package com.student360.app.service

import com.student360.app.data.local.entity.Exam
import com.student360.app.data.local.entity.ExamTopic
import com.student360.app.data.local.entity.TopicStatus
import java.util.*

object ExamEngine {

    fun getDaysRemaining(examDateMillis: Long): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val examMidnight = Calendar.getInstance().apply {
            timeInMillis = examDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val diff = examMidnight - today
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }

    fun getUrgencyText(daysRemaining: Int): String {
        return when {
            daysRemaining < 0 -> "Past Exam"
            daysRemaining == 0 -> "Today"
            daysRemaining == 1 -> "Tomorrow"
            daysRemaining <= 3 -> "$daysRemaining days left (High Priority)"
            daysRemaining <= 7 -> "$daysRemaining days left (Approaching)"
            else -> "$daysRemaining days left"
        }
    }

    fun calculatePrepPercentage(topics: List<ExamTopic>): Double {
        if (topics.isEmpty()) return 0.0
        val completed = topics.count { it.status == TopicStatus.COMPLETED }
        val inProgress = topics.count { it.status == TopicStatus.IN_PROGRESS }
        
        // Count COMPLETED as 100% and IN_PROGRESS as 50%
        val score = completed.toDouble() + (inProgress.toDouble() * 0.5)
        return (score / topics.size.toDouble()) * 100.0
    }

    fun getWeakTopics(topics: List<ExamTopic>): List<ExamTopic> {
        return topics.filter { it.status != TopicStatus.COMPLETED }
    }
}
