package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AssignmentPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

enum class AssignmentStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

@Entity(
    tableName = "assignments",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Assignment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val name: String,
    val description: String,
    val assignedDate: Long,
    val dueDate: Long,
    val priority: AssignmentPriority = AssignmentPriority.MEDIUM,
    val status: AssignmentStatus = AssignmentStatus.NOT_STARTED
)
