package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_history",
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
data class AttendanceHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recordId: Int? = null,
    val subjectId: Int,
    val date: Long,
    val originalStatus: String,
    val newStatus: String,
    val changeTimestamp: Long = System.currentTimeMillis(),
    val reason: String? = null,
    val verificationStatus: String = "Not verified"
)
