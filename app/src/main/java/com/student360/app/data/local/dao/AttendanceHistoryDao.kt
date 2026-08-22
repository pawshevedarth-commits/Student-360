package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.AttendanceHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceHistoryDao {
    @Query("SELECT * FROM attendance_history ORDER BY changeTimestamp DESC")
    fun getAllHistoryFlow(): Flow<List<AttendanceHistory>>

    @Query("SELECT * FROM attendance_history WHERE subjectId = :subjectId ORDER BY changeTimestamp DESC")
    fun getHistoryForSubjectFlow(subjectId: Int): Flow<List<AttendanceHistory>>

    @Query("SELECT * FROM attendance_history WHERE subjectId = :subjectId ORDER BY changeTimestamp DESC")
    suspend fun getHistoryForSubject(subjectId: Int): List<AttendanceHistory>

    @Query("SELECT * FROM attendance_history WHERE date = :date ORDER BY changeTimestamp DESC")
    suspend fun getHistoryForDate(date: Long): List<AttendanceHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: AttendanceHistory): Long

    @Delete
    suspend fun deleteHistory(history: AttendanceHistory)

    @Query("DELETE FROM attendance_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("SELECT * FROM attendance_history WHERE subjectId = :subjectId AND date = :date ORDER BY changeTimestamp DESC LIMIT 1")
    suspend fun getLatestHistoryForRecord(subjectId: Int, date: Long): AttendanceHistory?
}
