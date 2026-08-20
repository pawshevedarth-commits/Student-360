package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val faculty: String,
    val targetPercentage: Double = 75.0,
    val manualAttended: Int = 0, // Baseline override
    val manualConducted: Int = 0  // Baseline override
)
