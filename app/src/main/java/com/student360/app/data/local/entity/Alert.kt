package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertType {
    ATTENDANCE,
    LECTURE,
    EXAM,
    ASSIGNMENT,
    STUDY,
    ACHIEVEMENT
}

@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: AlertType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
