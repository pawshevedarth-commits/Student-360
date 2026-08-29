package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.Assignment
import com.student360.app.data.local.entity.AssignmentStatus
import com.student360.app.data.local.entity.Goal
import com.student360.app.data.local.entity.GoalStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.service.StudyAssistant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _candidates = MutableStateFlow<List<StudyAssistant.RecommendationCandidate>>(emptyList())
    val candidates: StateFlow<List<StudyAssistant.RecommendationCandidate>> = _candidates.asStateFlow()

    private val _plannedSchedule = MutableStateFlow<List<StudyAssistant.ScheduleBlock>>(emptyList())
    val plannedSchedule: StateFlow<List<StudyAssistant.ScheduleBlock>> = _plannedSchedule.asStateFlow()

    private val _assignments = MutableStateFlow<List<Assignment>>(emptyList())
    private val _goals = MutableStateFlow<List<Goal>>(emptyList())

    init {
        viewModelScope.launch {
            repository.assignmentsFlow.collectLatest {
                _assignments.value = it.filter { a -> a.status != AssignmentStatus.COMPLETED }
                loadData()
            }
        }
        viewModelScope.launch {
            repository.goalsFlow.collectLatest {
                _goals.value = it.filter { g -> g.status == GoalStatus.ACTIVE }
                loadData()
            }
        }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val subjects = repository.getAllSubjects()
            val list = mutableListOf<StudyAssistant.RecommendationCandidate>()
            subjects.forEach { subject ->
                val stats = repository.getSubjectStats(subject.id)
                val exams = repository.getAllExams().filter { it.subjectId == subject.id }
                val nextExam = exams.filter { it.date >= System.currentTimeMillis() }
                    .minByOrNull { it.date }
                val topics = nextExam?.let { repository.getTopicsForExam(it.id) } ?: emptyList()

                val candidate = StudyAssistant.calculatePriorityScore(
                    subject = subject,
                    stats = stats,
                    nextExam = nextExam,
                    topics = topics,
                    pendingAssignments = _assignments.value,
                    activeGoals = _goals.value
                )
                list.add(candidate)
            }
            _candidates.value = list.sortedByDescending { it.priorityScore }
        }
    }

    fun generateDaySchedule(availableHours: Double) {
        viewModelScope.launch {
            loadData()
            _plannedSchedule.value = StudyAssistant.planMyDay(_candidates.value, availableHours)
        }
    }

    fun toggleBlockCompleted(blockId: String) {
        _plannedSchedule.value = _plannedSchedule.value.map { block ->
            if (block.id == blockId) {
                block.copy(isCompleted = !block.isCompleted)
            } else {
                block
            }
        }
    }
}
