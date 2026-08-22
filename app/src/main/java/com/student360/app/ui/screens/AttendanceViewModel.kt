@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.AttendanceHistory
import com.student360.app.data.local.entity.AttendanceRecord
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.OverallStats
import com.student360.app.data.repository.StudentRepository
import com.student360.app.data.repository.SubjectStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.floor

enum class DayAttendanceState {
    ATTENDED,
    MISSED,
    MIXED,
    OFF,
    NOT_MARKED
}

data class DayAttendanceStats(
    val attended: Int,
    val missed: Int,
    val off: Int,
    val total: Int,
    val status: DayAttendanceState
)

data class CalendarSummaryStats(
    val notMarkedDays: Int,
    val offDays: Int,
    val missedDays: Int,
    val attendedDays: Int,
    val mixedDays: Int,
    val totalCollegeDays: Int,
    val totalAttended: Int,
    val totalMissed: Int,
    val totalOff: Int,
    val overallPercentage: Double
)

data class LectureItem(
    val subject: Subject,
    val stats: SubjectStats,
    val timetableEntry: TimetableEntry? = null,
    val attendanceRecord: AttendanceRecord? = null,
    val isExtra: Boolean = false,
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "",
    val faculty: String = ""
)

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _subjectsWithStats = MutableStateFlow<List<Pair<Subject, SubjectStats>>>(emptyList())
    val subjectsWithStats: StateFlow<List<Pair<Subject, SubjectStats>>> = _subjectsWithStats.asStateFlow()

    private val _overallStats = MutableStateFlow<OverallStats?>(null)
    val overallStats: StateFlow<OverallStats?> = _overallStats.asStateFlow()

    private val _targetPercentage = MutableStateFlow(75.0)
    val targetPercentage: StateFlow<Double> = _targetPercentage.asStateFlow()

    private val _selectedDate = MutableStateFlow<Long>(getStartOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _selectedDateRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val selectedDateRecords: StateFlow<List<AttendanceRecord>> = _selectedDateRecords.asStateFlow()

    private val _selectedDateSchedule = MutableStateFlow<List<TimetableEntry>>(emptyList())
    val selectedDateSchedule: StateFlow<List<TimetableEntry>> = _selectedDateSchedule.asStateFlow()

    private val _todayLectures = MutableStateFlow<List<LectureItem>>(emptyList())
    val todayLectures: StateFlow<List<LectureItem>> = _todayLectures.asStateFlow()

    private val _currentMonth = MutableStateFlow<Calendar>(Calendar.getInstance())
    val currentMonth: StateFlow<Calendar> = _currentMonth.asStateFlow()

    private val _heatmapData = MutableStateFlow<Map<Long, DayAttendanceStats>>(emptyMap())
    val heatmapData: StateFlow<Map<Long, DayAttendanceStats>> = _heatmapData.asStateFlow()

    private val _calendarSummary = MutableStateFlow(
        CalendarSummaryStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 100.0)
    )
    val calendarSummary: StateFlow<CalendarSummaryStats> = _calendarSummary.asStateFlow()

    private val _selectedSubject = MutableStateFlow<Subject?>(null)
    val selectedSubject: StateFlow<Subject?> = _selectedSubject.asStateFlow()

    private val _selectedSubjectRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val selectedSubjectRecords: StateFlow<List<AttendanceRecord>> = _selectedSubjectRecords.asStateFlow()

    init {
        loadData()
        refreshDateData(_selectedDate.value)
        refreshMonthData(_currentMonth.value)
    }

    fun loadData() {
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest { subjects ->
                val list = subjects.map { subject ->
                    val stats = repository.getSubjectStats(subject.id)
                    subject to stats
                }
                _subjectsWithStats.value = list
                _overallStats.value = repository.getOverallAttendanceStats()
                refreshTodayLectures(_selectedDate.value, list, _selectedDateSchedule.value, _selectedDateRecords.value)
            }
        }
        viewModelScope.launch {
            repository.allAttendanceFlow.collectLatest {
                refreshDateData(_selectedDate.value)
                refreshMonthData(_currentMonth.value)
                _overallStats.value = repository.getOverallAttendanceStats()
                _selectedSubject.value?.let { loadSubjectDetails(it) }
            }
        }
        viewModelScope.launch {
            repository.profileFlow.collectLatest {
                // Synchronize profile state if needed
            }
        }
    }

    fun selectDate(date: Long) {
        val normalized = getStartOfDay(date)
        _selectedDate.value = normalized
        refreshDateData(normalized)
    }

    fun previousDay() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedDate.value
            add(Calendar.DAY_OF_YEAR, -1)
        }
        selectDate(cal.timeInMillis)
    }

    fun nextDay() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _selectedDate.value
            add(Calendar.DAY_OF_YEAR, 1)
        }
        selectDate(cal.timeInMillis)
    }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _currentMonth.value.timeInMillis
            add(Calendar.MONTH, -1)
        }
        _currentMonth.value = cal
        refreshMonthData(cal)
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _currentMonth.value.timeInMillis
            add(Calendar.MONTH, 1)
        }
        _currentMonth.value = cal
        refreshMonthData(cal)
    }

    private fun refreshDateData(date: Long) {
        viewModelScope.launch {
            val records = repository.getAttendanceForDate(date)
            _selectedDateRecords.value = records

            val cal = Calendar.getInstance().apply { timeInMillis = date }
            val calDay = cal.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (calDay == Calendar.SUNDAY) 6 else calDay - 2

            val schedule = repository.getTimetableForDay(dayOfWeek).sortedBy { it.startTime }
            _selectedDateSchedule.value = schedule

            refreshTodayLectures(date, _subjectsWithStats.value, schedule, records)
        }
    }

    private fun refreshTodayLectures(
        date: Long,
        subjectsWithStats: List<Pair<Subject, SubjectStats>>,
        schedule: List<TimetableEntry>,
        records: List<AttendanceRecord>
    ) {
        val list = mutableListOf<LectureItem>()
        val processedSubjects = mutableSetOf<Int>()

        // 1. Add scheduled timetable entries for the day
        schedule.forEach { entry ->
            val pair = subjectsWithStats.find { it.first.id == entry.subjectId }
            if (pair != null) {
                val (sub, stats) = pair
                val record = records.find { it.subjectId == sub.id && it.timetableId == entry.id }
                    ?: records.find { it.subjectId == sub.id && !it.isExtra }
                list.add(
                    LectureItem(
                        subject = sub,
                        stats = stats,
                        timetableEntry = entry,
                        attendanceRecord = record,
                        isExtra = false,
                        startTime = entry.startTime,
                        endTime = entry.endTime,
                        room = entry.room,
                        faculty = entry.facultyOverride ?: sub.faculty
                    )
                )
                processedSubjects.add(sub.id)
            }
        }

        // 2. Add extra lecture records for this date
        records.filter { it.isExtra }.forEach { record ->
            val pair = subjectsWithStats.find { it.first.id == record.subjectId }
            if (pair != null) {
                val (sub, stats) = pair
                list.add(
                    LectureItem(
                        subject = sub,
                        stats = stats,
                        timetableEntry = null,
                        attendanceRecord = record,
                        isExtra = true,
                        startTime = record.startTime ?: "Extra",
                        endTime = record.endTime ?: "",
                        room = record.room ?: "",
                        faculty = record.faculty ?: sub.faculty
                    )
                )
            }
        }

        // 3. If no schedule exists for this day, fallback to showing registered subjects
        if (list.isEmpty()) {
            subjectsWithStats.forEach { (sub, stats) ->
                val record = records.find { it.subjectId == sub.id }
                list.add(
                    LectureItem(
                        subject = sub,
                        stats = stats,
                        timetableEntry = null,
                        attendanceRecord = record,
                        isExtra = record?.isExtra ?: false,
                        startTime = record?.startTime ?: "",
                        endTime = record?.endTime ?: "",
                        room = record?.room ?: "",
                        faculty = sub.faculty
                    )
                )
            }
        }

        _todayLectures.value = list
    }

    private fun refreshMonthData(monthCal: Calendar) {
        viewModelScope.launch {
            val allRecords = repository.getAllAttendance()
            val allSubjects = repository.getAllSubjects()

            val map = mutableMapOf<Long, DayAttendanceStats>()
            val recordsByDate = allRecords.groupBy { it.date }

            recordsByDate.forEach { (date, records) ->
                val attended = records.count { it.status == AttendanceStatus.PRESENT }
                val missed = records.count { it.status == AttendanceStatus.ABSENT }
                val off = records.count { it.status == AttendanceStatus.OFF }
                val total = attended + missed + off

                val status = when {
                    total == 0 -> DayAttendanceState.NOT_MARKED
                    attended > 0 && missed == 0 && off == 0 -> DayAttendanceState.ATTENDED
                    missed > 0 && attended == 0 && off == 0 -> DayAttendanceState.MISSED
                    off > 0 && attended == 0 && missed == 0 -> DayAttendanceState.OFF
                    else -> DayAttendanceState.MIXED
                }

                map[date] = DayAttendanceStats(attended, missed, off, total, status)
            }
            _heatmapData.value = map

            var attendedDays = 0
            var missedDays = 0
            var offDays = 0
            var mixedDays = 0
            var totalAttended = 0
            var totalMissed = 0
            var totalOff = 0

            map.values.forEach { day ->
                when (day.status) {
                    DayAttendanceState.ATTENDED -> attendedDays++
                    DayAttendanceState.MISSED -> missedDays++
                    DayAttendanceState.OFF -> offDays++
                    DayAttendanceState.MIXED -> mixedDays++
                    DayAttendanceState.NOT_MARKED -> {}
                }
                totalAttended += day.attended
                totalMissed += day.missed
                totalOff += day.off
            }

            allSubjects.forEach { sub ->
                totalAttended += sub.manualAttended
                totalMissed += (sub.manualConducted - sub.manualAttended).coerceAtLeast(0)
            }

            val totalCollegeDays = attendedDays + missedDays + offDays + mixedDays
            val totalConducted = totalAttended + totalMissed
            val pct = if (totalConducted > 0) (totalAttended.toDouble() / totalConducted.toDouble()) * 100.0 else 100.0

            _calendarSummary.value = CalendarSummaryStats(
                notMarkedDays = 6,
                offDays = offDays.coerceAtLeast(map.values.count { it.off > 0 }),
                missedDays = missedDays,
                attendedDays = attendedDays,
                mixedDays = mixedDays,
                totalCollegeDays = totalCollegeDays,
                totalAttended = totalAttended,
                totalMissed = totalMissed,
                totalOff = totalOff,
                overallPercentage = pct
            )
        }
    }

    fun markLectureAttendance(
        subjectId: Int,
        date: Long,
        status: AttendanceStatus?,
        isExtra: Boolean = false,
        timetableId: Int? = null,
        reason: String? = null,
        officialStatus: String = "Not verified"
    ) {
        viewModelScope.launch {
            val normalized = getStartOfDay(date)
            val existing = repository.getAttendanceForDate(normalized).find {
                it.subjectId == subjectId && (timetableId == null || it.timetableId == timetableId)
            }

            val originalStatusStr = existing?.status?.name ?: "NOT_MARKED"
            val newStatusStr = status?.name ?: "NOT_MARKED"

            if (existing != null) {
                repository.deleteAttendanceById(existing.id)
            }

            if (status != null) {
                val record = AttendanceRecord(
                    subjectId = subjectId,
                    date = normalized,
                    status = status,
                    isExtra = isExtra,
                    timetableId = timetableId,
                    officialStatus = officialStatus,
                    notes = reason
                )
                val recordId = repository.insertAttendance(record).toInt()

                // Save Audit Log
                if (originalStatusStr != newStatusStr) {
                    repository.insertHistory(
                        AttendanceHistory(
                            recordId = recordId,
                            subjectId = subjectId,
                            date = normalized,
                            originalStatus = originalStatusStr,
                            newStatus = newStatusStr,
                            reason = reason ?: "Updated via Student360 Attendance",
                            verificationStatus = officialStatus
                        )
                    )
                }
            } else if (existing != null) {
                repository.insertHistory(
                    AttendanceHistory(
                        recordId = existing.id,
                        subjectId = subjectId,
                        date = normalized,
                        originalStatus = originalStatusStr,
                        newStatus = "CLEARED",
                        reason = reason ?: "Cleared attendance mark",
                        verificationStatus = officialStatus
                    )
                )
            }

            refreshDateData(normalized)
            loadData()
        }
    }

    fun markAllDay(date: Long, status: AttendanceStatus) {
        markAllForDate(date, status)
    }

    fun markAllForDate(date: Long, status: AttendanceStatus) {
        viewModelScope.launch {
            val normalized = getStartOfDay(date)
            val subjects = repository.getAllSubjects()
            val cal = Calendar.getInstance().apply { timeInMillis = normalized }
            val calDay = cal.get(Calendar.DAY_OF_WEEK)
            val dayOfWeek = if (calDay == Calendar.SUNDAY) 6 else calDay - 2
            val dayLectures = repository.getTimetableForDay(dayOfWeek)

            val targetSubjects = if (dayLectures.isNotEmpty()) {
                dayLectures.map { it.subjectId }.distinct()
            } else {
                subjects.map { it.id }
            }

            repository.deleteAttendanceForDate(normalized)
            targetSubjects.forEach { subId ->
                repository.insertAttendance(
                    AttendanceRecord(
                        subjectId = subId,
                        date = normalized,
                        status = status
                    )
                )
            }
            refreshDateData(normalized)
            loadData()
        }
    }

    fun clearDay(date: Long) {
        clearAllForDate(date)
    }

    fun clearAllForDate(date: Long) {
        viewModelScope.launch {
            val normalized = getStartOfDay(date)
            repository.deleteAttendanceForDate(normalized)
            refreshDateData(normalized)
            loadData()
        }
    }

    fun addExtraLecture(
        subjectId: Int,
        date: Long,
        startTime: String = "",
        endTime: String = "",
        room: String = "",
        faculty: String = "",
        initialStatus: AttendanceStatus = AttendanceStatus.PRESENT
    ) {
        viewModelScope.launch {
            val normalized = getStartOfDay(date)
            val record = AttendanceRecord(
                subjectId = subjectId,
                date = normalized,
                status = initialStatus,
                isExtra = true,
                startTime = startTime.ifBlank { "Extra" },
                endTime = endTime,
                room = room,
                faculty = faculty,
                officialStatus = "Not verified"
            )
            repository.insertAttendance(record)
            repository.insertHistory(
                AttendanceHistory(
                    subjectId = subjectId,
                    date = normalized,
                    originalStatus = "NONE",
                    newStatus = initialStatus.name,
                    reason = "Added extra lecture",
                    verificationStatus = "Not verified"
                )
            )
            refreshDateData(normalized)
            loadData()
        }
    }

    fun selectSubjectForDetail(subject: Subject) {
        _selectedSubject.value = subject
        loadSubjectDetails(subject)
    }

    fun clearSelectedSubject() {
        _selectedSubject.value = null
        _selectedSubjectRecords.value = emptyList()
    }

    private fun loadSubjectDetails(subject: Subject) {
        viewModelScope.launch {
            val records = repository.getAttendanceForSubject(subject.id)
            _selectedSubjectRecords.value = records
        }
    }

    fun getHistoryForSubject(subjectId: Int): Flow<List<AttendanceHistory>> {
        return repository.getHistoryForSubjectFlow(subjectId)
    }

    fun undoLastChange(subjectId: Int, date: Long) {
        viewModelScope.launch {
            val latestHistory = repository.getLatestHistoryForRecord(subjectId, date)
            if (latestHistory != null) {
                val orig = latestHistory.originalStatus
                val targetStatus = when (orig) {
                    "PRESENT" -> AttendanceStatus.PRESENT
                    "ABSENT" -> AttendanceStatus.ABSENT
                    "OFF" -> AttendanceStatus.OFF
                    else -> null
                }
                markLectureAttendance(subjectId, date, targetStatus, reason = "Undo change")
                repository.deleteHistoryById(latestHistory.id)
            }
        }
    }

    fun calculateRecommendation(attended: Int, conducted: Int, target: Double): String {
        if (conducted == 0) return "can miss 0 lectures"
        val pct = (attended.toDouble() / conducted.toDouble()) * 100.0

        return if (pct >= target) {
            val maxMiss = floor((attended * 100.0 / target) - conducted).toInt()
            when {
                maxMiss > 1 -> "can miss $maxMiss lectures"
                maxMiss == 1 -> "can miss 1 lecture"
                else -> "can't miss the next lecture"
            }
        } else {
            val needed = ceil((target * conducted - 100.0 * attended) / (100.0 - target)).toInt().coerceAtLeast(1)
            if (needed == 1) "need to attend 1 lecture" else "need to attend $needed lectures"
        }
    }

    fun updateSubjectTarget(subject: Subject, target: Double) {
        viewModelScope.launch {
            repository.updateSubject(subject.copy(targetPercentage = target))
            loadData()
        }
    }

    fun updateManualOverrides(subject: Subject, attended: Int, conducted: Int) {
        viewModelScope.launch {
            repository.updateSubject(
                subject.copy(
                    manualAttended = attended,
                    manualConducted = conducted
                )
            )
            loadData()
        }
    }

    fun addSubject(name: String, code: String, faculty: String, target: Double) {
        viewModelScope.launch {
            val subject = Subject(
                name = name,
                code = code,
                faculty = faculty,
                targetPercentage = target,
                manualAttended = 0,
                manualConducted = 0
            )
            repository.insertSubject(subject)
            loadData()
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            repository.deleteSubject(subject)
            if (_selectedSubject.value?.id == subject.id) {
                clearSelectedSubject()
            }
            loadData()
        }
    }

    fun updateGlobalTarget(target: Double) {
        _targetPercentage.value = target
        viewModelScope.launch {
            val subjects = repository.getAllSubjects()
            subjects.forEach { sub ->
                repository.updateSubject(sub.copy(targetPercentage = target))
            }
            loadData()
        }
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
