package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DayStatus {
    COLLEGE_DAY,
    HOLIDAY,
    EXAM,
    STUDY_LEAVE,
    NO_CLASSES,
    PARTIAL_DAY
}

@Entity(tableName = "college_days")
data class CollegeDay(
    @PrimaryKey val date: Long, // Midnight epoch timestamp in milliseconds
    val status: DayStatus,
    val notes: String? = null
)
