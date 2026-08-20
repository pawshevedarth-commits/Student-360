package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.StudentProfile
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.OverallStats
import com.student360.app.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _profile = MutableStateFlow<StudentProfile?>(null)
    val profile: StateFlow<StudentProfile?> = _profile.asStateFlow()

    private val _overallStats = MutableStateFlow<OverallStats?>(null)
    val overallStats: StateFlow<OverallStats?> = _overallStats.asStateFlow()

    private val _nextLecture = MutableStateFlow<Pair<TimetableEntry, Subject>?>(null)
    val nextLecture: StateFlow<Pair<TimetableEntry, Subject>?> = _nextLecture.asStateFlow()

    private val _nextLectureCountdown = MutableStateFlow<String>("")
    val nextLectureCountdown: StateFlow<String> = _nextLectureCountdown.asStateFlow()

    private val _pendingTasksCount = MutableStateFlow(0)
    val pendingTasksCount: StateFlow<Int> = _pendingTasksCount.asStateFlow()

    private val _studyStreak = MutableStateFlow(0)
    val studyStreak: StateFlow<Int> = _studyStreak.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            repository.profileFlow.collectLatest {
                _profile.value = it
            }
        }
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest {
                calculateStatsAndNextLecture()
            }
        }
        viewModelScope.launch {
            repository.tasksFlow.collectLatest { tasks ->
                _pendingTasksCount.value = tasks.count { !it.completed }
            }
        }
        viewModelScope.launch {
            repository.studySessionsFlow.collectLatest { sessions ->
                _studyStreak.value = calculateStreak(sessions.map { it.dateCompleted })
            }
        }
    }

    private suspend fun calculateStatsAndNextLecture() {
        _overallStats.value = repository.getOverallAttendanceStats()
        
        val timetable = repository.getAllTimetable()
        val subjects = repository.getAllSubjects()
        if (timetable.isNotEmpty() && subjects.isNotEmpty()) {
            val nextPair = findNextLecture(timetable, subjects)
            _nextLecture.value = nextPair
            if (nextPair != null) {
                _nextLectureCountdown.value = computeCountdown(nextPair.first)
            } else {
                _nextLectureCountdown.value = ""
            }
        } else {
            _nextLecture.value = null
            _nextLectureCountdown.value = ""
        }
    }

    private fun findNextLecture(timetable: List<TimetableEntry>, subjects: List<Subject>): Pair<TimetableEntry, Subject>? {
        val calendar = Calendar.getInstance()
        val currentDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> -1
        }
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(Calendar.MINUTE)
        val currentTimeString = String.format("%02d:%02d", currentHour, currentMin)

        if (currentDay != -1) {
            val todayLectures = timetable.filter { it.dayOfWeek == currentDay && it.startTime > currentTimeString }
                .sortedBy { it.startTime }
            if (todayLectures.isNotEmpty()) {
                val nextEntry = todayLectures.first()
                val sub = subjects.find { it.id == nextEntry.subjectId }
                if (sub != null) return nextEntry to sub
            }
        }

        for (i in 1..7) {
            val checkDay = (currentDay + i) % 6
            val dayLectures = timetable.filter { it.dayOfWeek == checkDay }.sortedBy { it.startTime }
            if (dayLectures.isNotEmpty()) {
                val nextEntry = dayLectures.first()
                val sub = subjects.find { it.id == nextEntry.subjectId }
                if (sub != null) return nextEntry to sub
            }
        }

        return null
    }

    private fun computeCountdown(entry: TimetableEntry): String {
        val calendar = Calendar.getInstance()
        val currentDay = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> -1
        }

        val parts = entry.startTime.split(":")
        val startH = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val startM = parts.getOrNull(1)?.toIntOrNull() ?: 0

        if (currentDay == entry.dayOfWeek) {
            val curH = calendar.get(Calendar.HOUR_OF_DAY)
            val curM = calendar.get(Calendar.MINUTE)
            val diffMins = (startH * 60 + startM) - (curH * 60 + curM)
            if (diffMins > 0) {
                if (diffMins < 60) return "Starts in $diffMins minutes"
                val hours = diffMins / 60
                val mins = diffMins % 60
                return if (mins > 0) "Starts in $hours hrs $mins mins" else "Starts in $hours hrs"
            }
        }

        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return "Starts ${days[entry.dayOfWeek]} at ${entry.startTime}"
    }

    private fun calculateStreak(datesCompleted: List<Long>): Int {
        if (datesCompleted.isEmpty()) return 0
        
        val sortedDays = datesCompleted.map {
            val cal = Calendar.getInstance().apply {
                timeInMillis = it
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            cal.timeInMillis / (24 * 60 * 60 * 1000)
        }.distinct().sortedDescending()

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayDay = todayCal.timeInMillis / (24 * 60 * 60 * 1000)

        if (sortedDays.isEmpty() || (todayDay - sortedDays[0] > 1)) {
            return 0
        }

        var streak = 0
        var expectedDay = sortedDays[0]

        for (day in sortedDays) {
            if (expectedDay - day <= 1) {
                streak++
                expectedDay = day
            } else {
                break
            }
        }
        return streak
    }
}
