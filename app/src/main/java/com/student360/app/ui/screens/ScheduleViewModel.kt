package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.StudentProfile
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.OverallStats
import com.student360.app.data.repository.StudentRepository
import com.student360.app.service.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _profile = MutableStateFlow<StudentProfile?>(null)
    val profile: StateFlow<StudentProfile?> = _profile.asStateFlow()

    private val _overallStats = MutableStateFlow<OverallStats?>(null)
    val overallStats: StateFlow<OverallStats?> = _overallStats.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _timetable = MutableStateFlow<List<TimetableEntry>>(emptyList())
    val timetable: StateFlow<List<TimetableEntry>> = _timetable.asStateFlow()

    init {
        viewModelScope.launch {
            repository.profileFlow.collectLatest {
                _profile.value = it
            }
        }
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest {
                _subjects.value = it
                calculateOverallStats()
            }
        }
        viewModelScope.launch {
            repository.allAttendanceFlow.collectLatest {
                calculateOverallStats()
            }
        }
        viewModelScope.launch {
            repository.timetableFlow.collectLatest {
                _timetable.value = it
            }
        }
    }

    private suspend fun calculateOverallStats() {
        val subjects = repository.getAllSubjects()
        val allAttendance = repository.getAllAttendance()

        var totalAttended = 0
        var totalConducted = 0

        subjects.forEach { subject ->
            val records = allAttendance.filter { it.subjectId == subject.id }
            val attended = records.count { it.status == com.student360.app.data.local.entity.AttendanceStatus.PRESENT } + subject.manualAttended
            val missed = records.count { it.status == com.student360.app.data.local.entity.AttendanceStatus.ABSENT } + (subject.manualConducted - subject.manualAttended).coerceAtLeast(0)
            totalAttended += attended
            totalConducted += (attended + missed)
        }

        val pct = if (totalConducted > 0) (totalAttended.toDouble() / totalConducted.toDouble()) * 100.0 else 100.0
        _overallStats.value = OverallStats(totalAttended, totalConducted, pct)
    }

    fun addTimetableEntry(
        subjectId: Int,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        facultyOverride: String?,
        notifyMinutesBefore: Int? = 10
    ) {
        viewModelScope.launch {
            val entry = TimetableEntry(
                subjectId = subjectId,
                dayOfWeek = dayOfWeek,
                startTime = startTime,
                endTime = endTime,
                room = room,
                facultyOverride = facultyOverride
            )
            repository.insertTimetable(entry)

            val updatedList = repository.getAllTimetable()
            val savedEntry = updatedList.find {
                it.subjectId == subjectId && it.dayOfWeek == dayOfWeek && it.startTime == startTime
            }
            if (savedEntry != null && notifyMinutesBefore != null) {
                val subject = repository.getSubjectById(subjectId)
                if (subject != null) {
                    NotificationScheduler.scheduleLectureAlarm(
                        getApplication(),
                        savedEntry,
                        subject.name,
                        notifyMinutesBefore
                    )
                }
            }
        }
    }

    fun addMultipleTimetableEntries(
        subjectIds: List<Int>,
        dayOfWeek: Int
    ) {
        if (subjectIds.isEmpty()) return
        viewModelScope.launch {
            val existing = repository.getTimetableForDay(dayOfWeek).sortedBy { it.startTime }
            val standardTimes = listOf(
                "09:00" to "10:00",
                "10:00" to "11:00",
                "11:00" to "12:00",
                "12:00" to "13:00",
                "14:00" to "15:00",
                "15:00" to "16:00",
                "16:00" to "17:00",
                "17:00" to "18:00"
            )

            var nextSlotIndex = existing.size
            for (subId in subjectIds) {
                val (startTime, endTime) = standardTimes.getOrElse(nextSlotIndex) {
                    val startHour = (9 + nextSlotIndex)
                    String.format(java.util.Locale.US, "%02d:00", startHour) to String.format(java.util.Locale.US, "%02d:00", startHour + 1)
                }
                val entry = TimetableEntry(
                    subjectId = subId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    room = "",
                    facultyOverride = null
                )
                repository.insertTimetable(entry)
                nextSlotIndex++
            }
        }
    }

    fun restoreTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            repository.insertTimetable(entry.copy(id = 0))
        }
    }

    fun updateTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            repository.updateTimetable(entry)
        }
    }

    fun updateEntrySubject(entry: TimetableEntry, newSubjectId: Int) {
        viewModelScope.launch {
            val updated = entry.copy(subjectId = newSubjectId)
            repository.updateTimetable(updated)
        }
    }

    fun deleteTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            NotificationScheduler.cancelLectureAlarm(getApplication(), entry.id)
            repository.deleteTimetable(entry)
        }
    }

    fun moveEntryUp(dayOfWeek: Int, index: Int) {
        if (index <= 0) return
        viewModelScope.launch {
            val dayEntries = repository.getTimetableForDay(dayOfWeek).sortedBy { it.startTime }.toMutableList()
            if (index < dayEntries.size) {
                val current = dayEntries[index]
                val prev = dayEntries[index - 1]
                val updatedCurrent = current.copy(startTime = prev.startTime, endTime = prev.endTime)
                val updatedPrev = prev.copy(startTime = current.startTime, endTime = current.endTime)
                repository.updateTimetable(updatedCurrent)
                repository.updateTimetable(updatedPrev)
            }
        }
    }

    fun moveEntryDown(dayOfWeek: Int, index: Int) {
        viewModelScope.launch {
            val dayEntries = repository.getTimetableForDay(dayOfWeek).sortedBy { it.startTime }.toMutableList()
            if (index in 0 until dayEntries.size - 1) {
                val current = dayEntries[index]
                val next = dayEntries[index + 1]
                val updatedCurrent = current.copy(startTime = next.startTime, endTime = next.endTime)
                val updatedNext = next.copy(startTime = current.startTime, endTime = current.endTime)
                repository.updateTimetable(updatedCurrent)
                repository.updateTimetable(updatedNext)
            }
        }
    }

    fun reorderDayEntries(dayOfWeek: Int, fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        viewModelScope.launch {
            val dayEntries = repository.getTimetableForDay(dayOfWeek).sortedBy { it.startTime }.toMutableList()
            if (fromIndex in dayEntries.indices && toIndex in dayEntries.indices) {
                // Preserve the day's existing time slot schedule
                val originalTimes = dayEntries.map { it.startTime to it.endTime }
                val item = dayEntries.removeAt(fromIndex)
                dayEntries.add(toIndex, item)

                dayEntries.forEachIndexed { i, entry ->
                    val (start, end) = originalTimes[i]
                    if (entry.startTime != start || entry.endTime != end) {
                        repository.updateTimetable(entry.copy(startTime = start, endTime = end))
                    }
                }
            }
        }
    }
}
