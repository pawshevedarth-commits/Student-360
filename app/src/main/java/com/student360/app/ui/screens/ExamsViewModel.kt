package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.Exam
import com.student360.app.data.local.entity.ExamTopic
import com.student360.app.data.local.entity.ExamType
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TopicStatus
import com.student360.app.data.repository.StudentRepository
import com.student360.app.service.ExamEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExamsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _examsWithPrep = MutableStateFlow<List<ExamWithPrep>>(emptyList())
    val examsWithPrep: StateFlow<List<ExamWithPrep>> = _examsWithPrep.asStateFlow()

    init {
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest {
                _subjects.value = it
                loadExamsData()
            }
        }
        viewModelScope.launch {
            repository.examsFlow.collectLatest {
                loadExamsData()
            }
        }
    }

    fun loadExamsData() {
        viewModelScope.launch {
            val exams = repository.getAllExams()
            val list = exams.map { exam ->
                val topics = repository.getTopicsForExam(exam.id)
                val prepPercent = ExamEngine.calculatePrepPercentage(topics)
                val daysLeft = ExamEngine.getDaysRemaining(exam.date)
                ExamWithPrep(exam, topics, prepPercent, daysLeft)
            }
            _examsWithPrep.value = list
        }
    }

    fun addExam(
        subjectId: Int,
        examType: ExamType,
        date: Long,
        time: String,
        venue: String,
        maxMarks: Int,
        targetMarks: Int,
        initialTopics: List<String>
    ) {
        viewModelScope.launch {
            val exam = Exam(
                subjectId = subjectId,
                examType = examType,
                date = date,
                time = time,
                venue = venue,
                maxMarks = maxMarks,
                targetMarks = targetMarks
            )
            val examId = repository.insertExam(exam).toInt()
            
            initialTopics.forEach { topic ->
                if (topic.isNotBlank()) {
                    repository.insertExamTopic(ExamTopic(examId = examId, topicName = topic))
                }
            }
            loadExamsData()
        }
    }

    fun addTopicToExam(examId: Int, topicName: String) {
        viewModelScope.launch {
            repository.insertExamTopic(ExamTopic(examId = examId, topicName = topicName))
            loadExamsData()
        }
    }

    fun updateTopicStatus(topic: ExamTopic, newStatus: TopicStatus) {
        viewModelScope.launch {
            repository.updateExamTopic(topic.copy(status = newStatus))
            loadExamsData()
        }
    }

    fun deleteExam(exam: Exam) {
        viewModelScope.launch {
            repository.deleteExam(exam)
            loadExamsData()
        }
    }
}

data class ExamWithPrep(
    val exam: Exam,
    val topics: List<ExamTopic>,
    val prepPercentage: Double,
    val daysRemaining: Int
)
