package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.StudentRepository
import com.student360.app.service.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _timetable = MutableStateFlow<List<TimetableEntry>>(emptyList())
    val timetable: StateFlow<List<TimetableEntry>> = _timetable.asStateFlow()

    init {
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest {
                _subjects.value = it
            }
        }
        viewModelScope.launch {
            repository.timetableFlow.collectLatest {
                _timetable.value = it
            }
        }
    }

    fun addTimetableEntry(
        subjectId: Int,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        facultyOverride: String?,
        notifyMinutesBefore: Int? // If null, do not notify
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

    fun deleteTimetableEntry(entry: TimetableEntry) {
        viewModelScope.launch {
            NotificationScheduler.cancelLectureAlarm(getApplication(), entry.id)
            repository.deleteTimetable(entry)
        }
    }
}
