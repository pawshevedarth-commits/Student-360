package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.Goal
import com.student360.app.data.local.entity.GoalStatus
import com.student360.app.data.local.entity.StudySession
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.*

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _sessions = MutableStateFlow<List<StudySession>>(emptyList())
    val sessions: StateFlow<List<StudySession>> = _sessions.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()

    // Timer States
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    private val _timerSubjectId = MutableStateFlow<Int?>(null)
    val timerSubjectId: StateFlow<Int?> = _timerSubjectId.asStateFlow()

    private val _timerTopic = MutableStateFlow("")
    val timerTopic: StateFlow<String> = _timerTopic.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest {
                _subjects.value = it
            }
        }
        viewModelScope.launch {
            repository.studySessionsFlow.collectLatest {
                _sessions.value = it
            }
        }
        viewModelScope.launch {
            repository.goalsFlow.collectLatest {
                _goals.value = it
            }
        }
    }

    fun startTimer(subjectId: Int, topic: String) {
        _timerSubjectId.value = subjectId
        _timerTopic.value = topic
        _timerSeconds.value = 0
        _timerRunning.value = true
        runTimer()
    }

    fun pauseTimer() {
        _timerRunning.value = false
        timerJob?.cancel()
    }

    fun resumeTimer() {
        _timerRunning.value = true
        runTimer()
    }

    fun stopAndSaveTimer() {
        _timerRunning.value = false
        timerJob?.cancel()
        
        val durationMins = _timerSeconds.value / 60
        val subId = _timerSubjectId.value
        val topic = _timerTopic.value

        if (subId != null && durationMins > 0) {
            viewModelScope.launch {
                val session = StudySession(
                    subjectId = subId,
                    topic = topic.ifBlank { "Study Session" },
                    duration = durationMins,
                    dateCompleted = System.currentTimeMillis()
                )
                repository.insertStudySession(session)
                resetTimer()
            }
        } else {
            resetTimer()
        }
    }

    fun discardTimer() {
        _timerRunning.value = false
        timerJob?.cancel()
        resetTimer()
    }

    private fun resetTimer() {
        _timerSeconds.value = 0
        _timerSubjectId.value = null
        _timerTopic.value = ""
    }

    private fun runTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _timerRunning.value) {
                delay(1000)
                _timerSeconds.value += 1
            }
        }
    }

    fun getStudyStats(): StudyStats {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = cal.timeInMillis

        val list = _sessions.value
        val todayMins = list.filter { it.dateCompleted >= todayStart }.sumOf { it.duration }
        val weekMins = list.filter { it.dateCompleted >= weekStart }.sumOf { it.duration }
        val monthMins = list.filter { it.dateCompleted >= monthStart }.sumOf { it.duration }

        return StudyStats(
            todayHours = todayMins / 60.0 + (todayMins % 60) / 6000.0,
            weekHours = weekMins / 60.0,
            monthHours = monthMins / 60.0,
            todayMins = todayMins,
            weekMins = weekMins,
            monthMins = monthMins
        )
    }

    fun addGoal(title: String, target: Double, currentProgress: Double, dueDays: Int) {
        viewModelScope.launch {
            val deadline = System.currentTimeMillis() + (dueDays * 24 * 60 * 60 * 1000L)
            repository.insertGoal(
                Goal(
                    title = title,
                    target = target,
                    currentProgress = currentProgress,
                    deadline = deadline,
                    status = GoalStatus.ACTIVE
                )
            )
        }
    }

    fun updateGoalProgress(goal: Goal, progress: Double) {
        viewModelScope.launch {
            val newStatus = if (progress >= goal.target) GoalStatus.COMPLETED else goal.status
            repository.updateGoal(goal.copy(currentProgress = progress, status = newStatus))
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}

data class StudyStats(
    val todayHours: Double,
    val weekHours: Double,
    val monthHours: Double,
    val todayMins: Int,
    val weekMins: Int,
    val monthMins: Int
)
