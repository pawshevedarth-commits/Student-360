package com.student360.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_profile")
data class StudentProfile(
    @PrimaryKey val id: Int = 1, // Only one student profile exists locally
    val name: String,
    val rollNumber: String,
    val branch: String,
    val semester: Int,
    val division: String,
    val collegeName: String,
    val profilePicUri: String? = null,
    val onboarded: Boolean = false
)
