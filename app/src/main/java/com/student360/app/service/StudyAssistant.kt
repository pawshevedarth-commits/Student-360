package com.student360.app.service

import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.Exam
import com.student360.app.data.local.entity.ExamTopic
import com.student360.app.data.repository.SubjectStats
import java.text.SimpleDateFormat
import java.util.*

object StudyAssistant {

    data class RecommendationCandidate(
        val subject: Subject,
        val stats: SubjectStats,
        val nextExam: Exam?,
        val topics: List<ExamTopic>,
        val priorityScore: Double,
        val reason: String
    )

    fun calculatePriorityScore(
        subject: Subject,
        stats: SubjectStats,
        nextExam: Exam?,
        topics: List<ExamTopic>
    ): RecommendationCandidate {
        val weightExam = 50.0
        val weightPrep = 30.0
        val extraWeight = 20.0

        var urgencyScore = 0.0
        var reasonExam = ""
        if (nextExam != null) {
            val daysToExam = ExamEngine.getDaysRemaining(nextExam.date).coerceAtLeast(1)
            urgencyScore = (1.0 / daysToExam.toDouble()) * weightExam
            reasonExam = "Exam in $daysToExam days. "
        }

        val prepPercent = ExamEngine.calculatePrepPercentage(topics)
        val weaknessScore = (1.0 - (prepPercent / 100.0)) * weightPrep
        val reasonPrep = if (topics.isNotEmpty()) "Prep is ${prepPercent.toInt()}% complete. " else ""

        val isBelowTarget = stats.percentage < subject.targetPercentage
        val attendancePenalty = if (isBelowTarget) extraWeight else 0.0
        val reasonAttendance = if (isBelowTarget) "Attendance (${String.format("%.1f", stats.percentage)}%) is below target (${subject.targetPercentage.toInt()}%). " else ""

        val totalScore = urgencyScore + weaknessScore + attendancePenalty
        
        val reason = (reasonExam + reasonPrep + reasonAttendance).trim().ifBlank { "Regular revision scheduled." }

        return RecommendationCandidate(
            subject = subject,
            stats = stats,
            nextExam = nextExam,
            topics = topics,
            priorityScore = totalScore,
            reason = reason
        )
    }

    data class ScheduleBlock(
        val startTime: String,
        val endTime: String,
        val label: String,
        val isBreak: Boolean = false
    )

    fun planMyDay(
        candidates: List<RecommendationCandidate>,
        availableHours: Double
    ): List<ScheduleBlock> {
        val totalMinutes = (availableHours * 60).toInt()
        val blocks = mutableListOf<ScheduleBlock>()
        
        val sortedCandidates = candidates.sortedByDescending { it.priorityScore }
        if (sortedCandidates.isEmpty() || totalMinutes <= 0) return emptyList()

        var currentMinutes = 0
        var candidateIndex = 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        while (currentMinutes < totalMinutes) {
            val remainingMins = totalMinutes - currentMinutes
            val blockLength = if (remainingMins >= 45) 45 else remainingMins
            
            val startStr = timeFormat.format(calendar.time)
            calendar.add(Calendar.MINUTE, blockLength)
            val endStr = timeFormat.format(calendar.time)

            val candidate = sortedCandidates[candidateIndex % sortedCandidates.size]
            blocks.add(
                ScheduleBlock(
                    startTime = startStr,
                    endTime = endStr,
                    label = "${candidate.subject.name} - Study Session (${candidate.subject.code})"
                )
            )

            currentMinutes += blockLength
            candidateIndex++

            if (currentMinutes < totalMinutes) {
                val breakStart = timeFormat.format(calendar.time)
                val breakLength = if (totalMinutes - currentMinutes >= 15) 15 else (totalMinutes - currentMinutes)
                calendar.add(Calendar.MINUTE, breakLength)
                val breakEnd = timeFormat.format(calendar.time)

                blocks.add(
                    ScheduleBlock(
                        startTime = breakStart,
                        endTime = breakEnd,
                        label = "Break",
                        isBreak = true
                    )
                )
                currentMinutes += breakLength
            }
        }

        return blocks
    }
}
