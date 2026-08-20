package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "timetable_entries",
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
data class TimetableEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subjectId: Int,
    val dayOfWeek: Int, // 0 = Monday, 5 = Saturday
    val startTime: String, // "HH:MM" (24-hour format)
    val endTime: String,   // "HH:MM" (24-hour format)
    val room: String,
    val facultyOverride: String? = null
)
