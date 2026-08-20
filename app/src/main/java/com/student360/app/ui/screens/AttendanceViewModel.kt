package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.AttendanceRecord
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.data.repository.SubjectStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)
    
    private val _subjectsWithStats = MutableStateFlow<List<Pair<Subject, SubjectStats>>>(emptyList())
    val subjectsWithStats: StateFlow<List<Pair<Subject, SubjectStats>>> = _subjectsWithStats.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest { subjects ->
                val list = subjects.map { subject ->
                    val stats = repository.getSubjectStats(subject.id)
                    subject to stats
                }
                _subjectsWithStats.value = list
            }
        }
    }

    fun markAttendance(subjectId: Int, status: AttendanceStatus) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                subjectId = subjectId,
                date = System.currentTimeMillis(),
                status = status
            )
            repository.insertAttendance(record)
            loadData()
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
}
