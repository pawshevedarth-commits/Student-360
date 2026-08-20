package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    FAILED
}

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val target: Double,
    val currentProgress: Double,
    val deadline: Long,
    val status: GoalStatus = GoalStatus.ACTIVE
)
