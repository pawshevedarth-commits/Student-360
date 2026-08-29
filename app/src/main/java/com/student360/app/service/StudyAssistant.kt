package com.student360.app.service

import com.student360.app.data.local.entity.Assignment
import com.student360.app.data.local.entity.Goal
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
        val reason: String,
        val pendingAssignment: Assignment? = null
    )

    fun calculatePriorityScore(
        subject: Subject,
        stats: SubjectStats,
        nextExam: Exam?,
        topics: List<ExamTopic>,
        pendingAssignments: List<Assignment> = emptyList(),
        activeGoals: List<Goal> = emptyList()
    ): RecommendationCandidate {
        val weightExam = 50.0
        val weightPrep = 30.0
        val extraWeight = 25.0
        val assignmentWeight = 35.0

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
        val reasonAttendance = if (isBelowTarget) "Attendance (${String.format(Locale.US, "%.1f", stats.percentage)}%) is below target (${subject.targetPercentage.toInt()}%). " else ""

        // Check for pending assignments due within 4 days
        val subjectAssignments = pendingAssignments.filter { it.subjectId == subject.id }
        val nearestAssignment = subjectAssignments.minByOrNull { it.dueDate }
        var assignmentScore = 0.0
        var reasonAssignment = ""
        if (nearestAssignment != null) {
            val now = System.currentTimeMillis()
            val daysUntilDue = ((nearestAssignment.dueDate - now) / (24 * 3600 * 1000L)).coerceAtLeast(0)
            if (daysUntilDue <= 4) {
                assignmentScore = assignmentWeight / (daysUntilDue + 1).toDouble()
                reasonAssignment = "${nearestAssignment.name} due in $daysUntilDue days. "
            }
        }
        val hasLinkedGoal = activeGoals.any { it.title.contains(subject.name, ignoreCase = true) }
        val goalScore = if (hasLinkedGoal) 15.0 else 0.0
        val reasonGoal = if (hasLinkedGoal) "Active study goal linked. " else ""

        val totalScore = urgencyScore + weaknessScore + attendancePenalty + assignmentScore + goalScore
        
        val reason = (reasonAttendance + reasonAssignment + reasonGoal + reasonExam + reasonPrep).trim().ifBlank { "Regular revision scheduled." }

        return RecommendationCandidate(
            subject = subject,
            stats = stats,
            nextExam = nextExam,
            topics = topics,
            priorityScore = totalScore,
            reason = reason,
            pendingAssignment = nearestAssignment
        )
    }

    data class ScheduleBlock(
        val id: String = UUID.randomUUID().toString(),
        val startTime: String,
        val endTime: String,
        val label: String,
        val subject: Subject? = null,
        val topic: String = "Revision",
        val durationMins: Int = 45,
        val isBreak: Boolean = false,
        val isCompleted: Boolean = false
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
            val topicName = if (candidate.pendingAssignment != null) {
                "Assignment: ${candidate.pendingAssignment.name}"
            } else if (candidate.topics.isNotEmpty()) {
                candidate.topics.first().topicName
            } else {
                "Revision"
            }

            blocks.add(
                ScheduleBlock(
                    startTime = startStr,
                    endTime = endStr,
                    label = "${candidate.subject.name} - $topicName",
                    subject = candidate.subject,
                    topic = topicName,
                    durationMins = blockLength,
                    isBreak = false,
                    isCompleted = false
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
                        durationMins = breakLength,
                        isBreak = true
                    )
                )
                currentMinutes += breakLength
            }
        }

        return blocks
    }
}
