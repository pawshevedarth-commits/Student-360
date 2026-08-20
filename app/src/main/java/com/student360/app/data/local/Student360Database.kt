package com.student360.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.student360.app.data.local.dao.*
import com.student360.app.data.local.entity.*

@Database(
    entities = [
        StudentProfile::class,
        Subject::class,
        AttendanceRecord::class,
        TimetableEntry::class,
        CollegeDay::class,
        Exam::class,
        ExamTopic::class,
        Assignment::class,
        Task::class,
        StudySession::class,
        Goal::class,
        Alert::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class Student360Database : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun subjectDao(): SubjectDao
    abstract fun timetableDao(): TimetableDao
    abstract fun examDao(): ExamDao
    abstract fun taskDao(): TaskDao
    abstract fun studyDao(): StudyDao
    abstract fun goalDao(): GoalDao
    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var INSTANCE: Student360Database? = null

        fun getDatabase(context: Context): Student360Database {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    Student360Database::class.java,
                    "student360_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
