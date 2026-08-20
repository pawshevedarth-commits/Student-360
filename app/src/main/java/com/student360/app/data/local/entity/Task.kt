package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TaskCategory {
    STUDY,
    ASSIGNMENT,
    COLLEGE,
    PERSONAL,
    CODING,
    OTHER
}

enum class TaskPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
}

@Entity(
    tableName = "tasks",
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
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int? = null, // Optional link to a subject
    val title: String,
    val description: String,
    val category: TaskCategory,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long,
    val estimatedDuration: Int = 30, // Minutes
    val completed: Boolean = false
)
