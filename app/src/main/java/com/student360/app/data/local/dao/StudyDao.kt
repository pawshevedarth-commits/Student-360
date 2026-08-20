package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_sessions ORDER BY dateCompleted DESC")
    fun getAllStudySessionsFlow(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY dateCompleted DESC")
    suspend fun getAllStudySessions(): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE subjectId = :subjectId ORDER BY dateCompleted DESC")
    fun getStudySessionsForSubjectFlow(subjectId: Int): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE dateCompleted >= :startDate")
    suspend fun getSessionsSince(startDate: Long): List<StudySession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySession): Long

    @Update
    suspend fun updateStudySession(session: StudySession)

    @Delete
    suspend fun deleteStudySession(session: StudySession)
}
