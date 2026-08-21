package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.AttendanceRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjectsFlow(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjects(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Int): Subject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    // Attendance records queries
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getAttendanceForSubjectFlow(subjectId: Int): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    suspend fun getAttendanceForSubject(subjectId: Int): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    fun getAllAttendanceFlow(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC")
    suspend fun getAllAttendance(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun getAttendanceForDate(date: Long): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord): Long

    @Update
    suspend fun updateAttendance(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendance(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteAttendanceById(id: Int)

    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteAttendanceForDate(date: Long)

    @Query("DELETE FROM attendance_records WHERE date = :date AND subjectId = :subjectId")
    suspend fun deleteAttendanceForSubjectAndDate(subjectId: Int, date: Long)

    @Query("SELECT * FROM attendance_records WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getAttendanceBetweenDates(startDate: Long, endDate: Long): List<AttendanceRecord>
}
