package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TopicStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

@Entity(
    tableName = "exam_topics",
    foreignKeys = [
        ForeignKey(
            entity = Exam::class,
            parentColumns = ["id"],
            childColumns = ["examId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["examId"])]
)
data class ExamTopic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: Int,
    val topicName: String,
    val status: TopicStatus = TopicStatus.NOT_STARTED
)
