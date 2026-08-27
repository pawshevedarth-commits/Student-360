package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.local.entity.CollegeDay
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, startTime ASC")
    fun getAllTimetableFlow(): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries ORDER BY dayOfWeek ASC, startTime ASC")
    suspend fun getAllTimetable(): List<TimetableEntry>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    fun getTimetableForDayFlow(dayOfWeek: Int): Flow<List<TimetableEntry>>

    @Query("SELECT * FROM timetable_entries WHERE dayOfWeek = :dayOfWeek ORDER BY startTime ASC")
    suspend fun getTimetableForDay(dayOfWeek: Int): List<TimetableEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetable(entry: TimetableEntry): Long

    @Update
    suspend fun updateTimetable(entry: TimetableEntry)

    @Delete
    suspend fun deleteTimetable(entry: TimetableEntry)

    @Query("DELETE FROM timetable_entries WHERE subjectId = :subjectId")
    suspend fun deleteTimetableBySubjectId(subjectId: Int)

    // CollegeDay statuses
    @Query("SELECT * FROM college_days")
    fun getAllCollegeDaysFlow(): Flow<List<CollegeDay>>

    @Query("SELECT * FROM college_days")
    suspend fun getAllCollegeDays(): List<CollegeDay>

    @Query("SELECT * FROM college_days WHERE date = :date")
    suspend fun getCollegeDay(date: Long): CollegeDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollegeDay(day: CollegeDay)

    @Delete
    suspend fun deleteCollegeDay(day: CollegeDay)
}
