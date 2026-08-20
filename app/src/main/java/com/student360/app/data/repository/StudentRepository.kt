package com.student360.app.data.repository

import android.content.Context
import com.student360.app.data.local.Student360Database
import com.student360.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class StudentRepository(context: Context) {

    private val db = Student360Database.getDatabase(context)
    private val profileDao = db.profileDao()
    private val subjectDao = db.subjectDao()
    private val timetableDao = db.timetableDao()
    private val examDao = db.examDao()
    private val taskDao = db.taskDao()
    private val studyDao = db.studyDao()
    private val goalDao = db.goalDao()
    private val alertDao = db.alertDao()

    // Profile Methods
    val profileFlow: Flow<StudentProfile?> = profileDao.getProfileFlow()
    suspend fun getProfile(): StudentProfile? = profileDao.getProfile()
    suspend fun saveProfile(profile: StudentProfile) = profileDao.insertOrUpdateProfile(profile)

    // Subjects Methods
    val subjectsFlow: Flow<List<Subject>> = subjectDao.getAllSubjectsFlow()
    suspend fun getAllSubjects(): List<Subject> = subjectDao.getAllSubjects()
    suspend fun insertSubject(subject: Subject): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)
    suspend fun deleteSubject(subject: Subject) = subjectDao.deleteSubject(subject)
    suspend fun getSubjectById(id: Int): Subject? = subjectDao.getSubjectById(id)

    // Attendance Methods
    val allAttendanceFlow: Flow<List<AttendanceRecord>> = subjectDao.getAllAttendanceFlow()
    suspend fun getAllAttendance(): List<AttendanceRecord> = subjectDao.getAllAttendance()
    suspend fun getAttendanceForSubject(subjectId: Int): List<AttendanceRecord> = subjectDao.getAttendanceForSubject(subjectId)
    suspend fun insertAttendance(record: AttendanceRecord) = subjectDao.insertAttendance(record)
    suspend fun deleteAttendanceById(id: Int) = subjectDao.deleteAttendanceById(id)
    suspend fun getAttendanceForDate(date: Long): List<AttendanceRecord> = subjectDao.getAttendanceForDate(date)

    // Timetable Methods
    val timetableFlow: Flow<List<TimetableEntry>> = timetableDao.getAllTimetableFlow()
    suspend fun getAllTimetable(): List<TimetableEntry> = timetableDao.getAllTimetable()
    fun getTimetableForDayFlow(dayOfWeek: Int): Flow<List<TimetableEntry>> = timetableDao.getTimetableForDayFlow(dayOfWeek)
    suspend fun getTimetableForDay(dayOfWeek: Int): List<TimetableEntry> = timetableDao.getTimetableForDay(dayOfWeek)
    suspend fun insertTimetable(entry: TimetableEntry) = timetableDao.insertTimetable(entry)
    suspend fun deleteTimetable(entry: TimetableEntry) = timetableDao.deleteTimetable(entry)

    // CollegeDay Methods
    val collegeDaysFlow: Flow<List<CollegeDay>> = timetableDao.getAllCollegeDaysFlow()
    suspend fun getAllCollegeDays(): List<CollegeDay> = timetableDao.getAllCollegeDays()
    suspend fun getCollegeDay(date: Long): CollegeDay? = timetableDao.getCollegeDay(date)
    suspend fun insertCollegeDay(day: CollegeDay) = timetableDao.insertCollegeDay(day)
    suspend fun deleteCollegeDay(day: CollegeDay) = timetableDao.deleteCollegeDay(day)

    // Exam Methods
    val examsFlow: Flow<List<Exam>> = examDao.getAllExamsFlow()
    suspend fun getAllExams(): List<Exam> = examDao.getAllExams()
    suspend fun getExamById(id: Int): Exam? = examDao.getExamById(id)
    suspend fun insertExam(exam: Exam): Long = examDao.insertExam(exam)
    suspend fun updateExam(exam: Exam) = examDao.updateExam(exam)
    suspend fun deleteExam(exam: Exam) = examDao.deleteExam(exam)

    fun getTopicsForExamFlow(examId: Int): Flow<List<ExamTopic>> = examDao.getTopicsForExamFlow(examId)
    suspend fun getTopicsForExam(examId: Int): List<ExamTopic> = examDao.getTopicsForExam(examId)
    suspend fun insertExamTopic(topic: ExamTopic) = examDao.insertTopic(topic)
    suspend fun updateExamTopic(topic: ExamTopic) = examDao.updateTopic(topic)
    suspend fun deleteExamTopic(topic: ExamTopic) = examDao.deleteTopic(topic)

    // Task & Assignment Methods
    val tasksFlow: Flow<List<Task>> = taskDao.getAllTasksFlow()
    suspend fun getAllTasks(): List<Task> = taskDao.getAllTasks()
    suspend fun insertTask(task: Task) = taskDao.insertTask(task)
    suspend fun updateTask(task: Task) = taskDao.updateTask(task)
    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    val assignmentsFlow: Flow<List<Assignment>> = taskDao.getAllAssignmentsFlow()
    suspend fun getAllAssignments(): List<Assignment> = taskDao.getAllAssignments()
    suspend fun getAssignmentById(id: Int): Assignment? = taskDao.getAssignmentById(id)
    suspend fun insertAssignment(assignment: Assignment) = taskDao.insertAssignment(assignment)
    suspend fun updateAssignment(assignment: Assignment) = taskDao.updateAssignment(assignment)
    suspend fun deleteAssignment(assignment: Assignment) = taskDao.deleteAssignment(assignment)

    // Study Methods
    val studySessionsFlow: Flow<List<StudySession>> = studyDao.getAllStudySessionsFlow()
    suspend fun getAllStudySessions(): List<StudySession> = studyDao.getAllStudySessions()
    suspend fun insertStudySession(session: StudySession) = studyDao.insertStudySession(session)
    suspend fun deleteStudySession(session: StudySession) = studyDao.deleteStudySession(session)

    // Goals Methods
    val goalsFlow: Flow<List<Goal>> = goalDao.getAllGoalsFlow()
    suspend fun getAllGoals(): List<Goal> = goalDao.getAllGoals()
    suspend fun insertGoal(goal: Goal) = goalDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal)

    // Alerts Methods
    val alertsFlow: Flow<List<Alert>> = alertDao.getAllAlertsFlow()
    val unreadAlertsFlow: Flow<List<Alert>> = alertDao.getUnreadAlertsFlow()
    suspend fun getAllAlerts(): List<Alert> = alertDao.getAllAlerts()
    suspend fun insertAlert(alert: Alert) = alertDao.insertAlert(alert)
    suspend fun markAlertAsRead(id: Int) = alertDao.markAsRead(id)
    suspend fun markAllAlertsAsRead() = alertDao.markAllAsRead()
    suspend fun deleteAlert(alert: Alert) = alertDao.deleteAlert(alert)
    suspend fun deleteAllAlerts() = alertDao.deleteAllAlerts()

    // Calculated Statistics Logic
    suspend fun getSubjectStats(subjectId: Int): SubjectStats {
        val subject = getSubjectById(subjectId) ?: return SubjectStats(0, 0, 0, 0.0)
        val records = getAttendanceForSubject(subjectId)

        val attendedRecords = records.count { it.status == AttendanceStatus.PRESENT }
        val missedRecords = records.count { it.status == AttendanceStatus.ABSENT }
        val offRecords = records.count { it.status == AttendanceStatus.OFF }

        val totalAttended = subject.manualAttended + attendedRecords
        val totalConducted = subject.manualConducted + attendedRecords + missedRecords

        val percentage = if (totalConducted > 0) {
            (totalAttended.toDouble() / totalConducted.toDouble()) * 100.0
        } else {
            100.0 // Default to 100% if no classes have been conducted
        }

        return SubjectStats(
            attended = totalAttended,
            missed = missedRecords,
            off = offRecords,
            percentage = percentage
        )
    }

    suspend fun getOverallAttendanceStats(): OverallStats {
        val subjects = getAllSubjects()
        var totalAttended = 0
        var totalConducted = 0

        subjects.forEach { subject ->
            val records = getAttendanceForSubject(subject.id)
            val attended = records.count { it.status == AttendanceStatus.PRESENT }
            val missed = records.count { it.status == AttendanceStatus.ABSENT }

            totalAttended += subject.manualAttended + attended
            totalConducted += subject.manualConducted + attended + missed
        }

        val percentage = if (totalConducted > 0) {
            (totalAttended.toDouble() / totalConducted.toDouble()) * 100.0
        } else {
            100.0
        }

        return OverallStats(totalAttended, totalConducted, percentage)
    }
}

data class SubjectStats(
    val attended: Int,
    val missed: Int,
    val off: Int,
    val percentage: Double
)

data class OverallStats(
    val totalAttended: Int,
    val totalConducted: Int,
    val percentage: Double
)
