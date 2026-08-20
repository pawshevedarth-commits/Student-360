package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.StudentProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM student_profile WHERE id = 1")
    fun getProfileFlow(): Flow<StudentProfile?>

    @Query("SELECT * FROM student_profile WHERE id = 1")
    suspend fun getProfile(): StudentProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: StudentProfile)
}
