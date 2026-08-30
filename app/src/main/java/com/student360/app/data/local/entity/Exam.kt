package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExamType(val displayName: String) {
    MIDTERM("Midterm"),
    INTERNAL("Internal"),
    UNIT_TEST("Unit Test"),
    PRACTICAL("Practical"),
    ASSIGNMENT_TEST("Assignment / Test"),
    FINAL("Final"),
    END_SEM("End Sem"),
    VIVA("Viva"),
    SUPPLEMENTARY("Supplementary"),
    OTHER("Other")
}

@Entity(
    tableName = "exams",
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
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val examType: ExamType,
    val date: Long, // Midnight epoch timestamp in milliseconds
    val time: String, // "HH:MM"
    val venue: String,
    val maxMarks: Int,
    val targetMarks: Int
)
