package com.student360.app.service

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.student360.app.data.local.Student360Database
import com.student360.app.data.local.entity.*
import com.student360.app.data.repository.StudentRepository
import java.io.InputStream
import java.io.OutputStream

object BackupRestoreManager {

    data class BackupData(
        val version: Int = 1,
        val profile: StudentProfile?,
        val subjects: List<Subject>,
        val attendance: List<AttendanceRecord>,
        val timetable: List<TimetableEntry>,
        val collegeDays: List<CollegeDay>,
        val exams: List<Exam>,
        val examTopics: List<ExamTopic>,
        val tasks: List<Task>,
        val assignments: List<Assignment>,
        val studySessions: List<StudySession>,
        val goals: List<Goal>,
        val alerts: List<Alert>
    )

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportBackup(repository: StudentRepository, outputStream: OutputStream): Boolean {
        return try {
            val backup = BackupData(
                version = 1,
                profile = repository.getProfile(),
                subjects = repository.getAllSubjects(),
                attendance = repository.getAllAttendance(),
                timetable = repository.getAllTimetable(),
                collegeDays = repository.getAllCollegeDays(),
                exams = repository.getAllExams(),
                examTopics = getExamTopicsHelper(repository),
                tasks = repository.getAllTasks(),
                assignments = repository.getAllAssignments(),
                studySessions = repository.getAllStudySessions(),
                goals = repository.getAllGoals(),
                alerts = repository.getAllAlerts()
            )
            val jsonString = gson.toJson(backup)
            outputStream.use { out ->
                out.write(jsonString.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importBackup(
        context: Context,
        repository: StudentRepository,
        inputStream: InputStream
    ): Boolean {
        return try {
            val jsonString = inputStream.use { input ->
                input.bufferedReader().use { it.readText() }
            }
            val backup = gson.fromJson(jsonString, BackupData::class.java)
            
            if (backup == null || backup.version != 1) {
                return false
            }

            val db = Student360Database.getDatabase(context)
            
            // Clear all data first
            db.clearAllTables()

            // Repopulate database
            backup.profile?.let { repository.saveProfile(it) }
            backup.subjects.forEach { repository.insertSubject(it) }
            backup.attendance.forEach { repository.insertAttendance(it) }
            backup.timetable.forEach { repository.insertTimetable(it) }
            backup.collegeDays.forEach { repository.insertCollegeDay(it) }
            backup.exams.forEach { repository.insertExam(it) }
            backup.examTopics.forEach { repository.insertExamTopic(it) }
            backup.tasks.forEach { repository.insertTask(it) }
            backup.assignments.forEach { repository.insertAssignment(it) }
            backup.studySessions.forEach { repository.insertStudySession(it) }
            backup.goals.forEach { repository.insertGoal(it) }
            backup.alerts.forEach { repository.insertAlert(it) }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun getExamTopicsHelper(repository: StudentRepository): List<ExamTopic> {
        val list = mutableListOf<ExamTopic>()
        val exams = repository.getAllExams()
        exams.forEach { exam ->
            list.addAll(repository.getTopicsForExam(exam.id))
        }
        return list
    }
}
