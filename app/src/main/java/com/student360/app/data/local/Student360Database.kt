package com.student360.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.student360.app.data.local.dao.*
import com.student360.app.data.local.entity.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create attendance_history table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `attendance_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `recordId` INTEGER,
                `subjectId` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `originalStatus` TEXT NOT NULL,
                `newStatus` TEXT NOT NULL,
                `changeTimestamp` INTEGER NOT NULL,
                `reason` TEXT,
                `verificationStatus` TEXT NOT NULL,
                FOREIGN KEY(`subjectId`) REFERENCES `subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_history_subjectId` ON `attendance_history` (`subjectId`)")

        // Add optional columns to attendance_records
        try {
            db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `officialStatus` TEXT NOT NULL DEFAULT 'Not verified'")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `notes` TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `startTime` TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `endTime` TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `room` TEXT DEFAULT NULL")
        } catch (_: Exception) {}
        try {
            db.execSQL("ALTER TABLE `attendance_records` ADD COLUMN `faculty` TEXT DEFAULT NULL")
        } catch (_: Exception) {}
    }
}

@Database(
    entities = [
        StudentProfile::class,
        Subject::class,
        AttendanceRecord::class,
        AttendanceHistory::class,
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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class Student360Database : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun subjectDao(): SubjectDao
    abstract fun attendanceHistoryDao(): AttendanceHistoryDao
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
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
