package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    OFF
}

@Entity(
    tableName = "attendance_records",
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
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val date: Long, // Epoch timestamp (ms) for the date of the record
    val status: AttendanceStatus,
    val isExtra: Boolean = false,
    val timetableId: Int? = null // Link to scheduled timetable entry if applicable
)
