package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.*
import com.student360.app.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MyDayViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _myDayItems = MutableStateFlow<List<MyDayItem>>(emptyList())
    val myDayItems: StateFlow<List<MyDayItem>> = _myDayItems.asStateFlow()

    private val _completionPercentage = MutableStateFlow(0f)
    val completionPercentage: StateFlow<Float> = _completionPercentage.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest {
                _subjects.value = it
                refreshMyDay()
            }
        }
        viewModelScope.launch {
            repository.tasksFlow.collectLatest {
                refreshMyDay()
            }
        }
        viewModelScope.launch {
            repository.assignmentsFlow.collectLatest {
                refreshMyDay()
            }
        }
        viewModelScope.launch {
            repository.timetableFlow.collectLatest {
                refreshMyDay()
            }
        }
    }

    private suspend fun refreshMyDay() {
        val todayCalendar = Calendar.getInstance()
        val currentDayOfWeek = when (todayCalendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> -1
        }

        val items = mutableListOf<MyDayItem>()

        if (currentDayOfWeek != -1) {
            val todayLectures = repository.getTimetableForDay(currentDayOfWeek)
            todayLectures.forEach { entry ->
                val sub = repository.getSubjectById(entry.subjectId)
                val todayMidnight = getMidnightTime(System.currentTimeMillis())
                val todayAttendance = repository.getAttendanceForDate(todayMidnight)
                val loggedToday = todayAttendance.any { it.subjectId == entry.subjectId }

                items.add(
                    MyDayItem(
                        id = "lecture_${entry.id}",
                        title = "${sub?.name ?: "Lecture"}: ${entry.startTime} - ${entry.endTime}",
                        subtitle = "Room ${entry.room}",
                        category = "COLLEGE",
                        priority = TaskPriority.MEDIUM,
                        completed = loggedToday,
                        rawId = entry.id
                    )
                )
            }
        }

        val tasks = repository.getAllTasks()
        val todayTime = getMidnightTime(System.currentTimeMillis())
        tasks.forEach { task ->
            val taskMidnight = getMidnightTime(task.dueDate)
            if (taskMidnight == todayTime || (!task.completed && task.dueDate < System.currentTimeMillis())) {
                val catName = when (task.category) {
                    TaskCategory.COLLEGE -> "College"
                    TaskCategory.STUDY -> "Study"
                    TaskCategory.PERSONAL -> "Personal"
                    TaskCategory.ASSIGNMENT -> "Assignment"
                    TaskCategory.CODING -> "Coding"
                    TaskCategory.OTHER -> "Other"
                }
                items.add(
                    MyDayItem(
                        id = "task_${task.id}",
                        title = task.title,
                        subtitle = "$catName · ${task.estimatedDuration} min",
                        category = if (task.category == TaskCategory.STUDY) "STUDY" else "PERSONAL",
                        priority = task.priority,
                        completed = task.completed,
                        rawId = task.id
                    )
                )
            }
        }

        val assignments = repository.getAllAssignments()
        assignments.forEach { assign ->
            val assignMidnight = getMidnightTime(assign.dueDate)
            val isOverdue = !assign.status.name.equals("COMPLETED") && assign.dueDate < System.currentTimeMillis()
            if (assignMidnight == todayTime || assign.status != AssignmentStatus.COMPLETED) {
                val dueFormatted = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(assign.dueDate))
                items.add(
                    MyDayItem(
                        id = "assign_${assign.id}",
                        title = assign.name,
                        subtitle = "Due $dueFormatted" + if (isOverdue) " · OVERDUE" else "",
                        category = "ASSIGNMENTS",
                        priority = when (assign.priority) {
                            AssignmentPriority.LOW -> TaskPriority.LOW
                            AssignmentPriority.MEDIUM -> TaskPriority.MEDIUM
                            AssignmentPriority.HIGH -> TaskPriority.HIGH
                            AssignmentPriority.URGENT -> TaskPriority.URGENT
                        },
                        completed = assign.status == AssignmentStatus.COMPLETED,
                        rawId = assign.id
                    )
                )
            }
        }

        _myDayItems.value = items

        if (items.isNotEmpty()) {
            val completedCount = items.count { it.completed }
            _completionPercentage.value = (completedCount.toFloat() / items.size.toFloat())
        } else {
            _completionPercentage.value = 1f
        }
    }

    fun toggleItemCompleted(item: MyDayItem, completed: Boolean) {
        viewModelScope.launch {
            if (item.id.startsWith("task_")) {
                val task = repository.getAllTasks().find { it.id == item.rawId }
                if (task != null) {
                    repository.updateTask(task.copy(completed = completed))
                }
            } else if (item.id.startsWith("assign_")) {
                val assign = repository.getAssignmentById(item.rawId)
                if (assign != null) {
                    val newStatus = if (completed) AssignmentStatus.COMPLETED else AssignmentStatus.IN_PROGRESS
                    repository.updateAssignment(assign.copy(status = newStatus))
                }
            }
            refreshMyDay()
        }
    }

    fun addTask(title: String, description: String, category: TaskCategory, subjectId: Int?, priority: TaskPriority, dueDate: Long, duration: Int) {
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    subjectId = subjectId,
                    title = title,
                    description = description,
                    category = category,
                    priority = priority,
                    dueDate = dueDate,
                    estimatedDuration = duration,
                    completed = false
                )
            )
            refreshMyDay()
        }
    }

    fun addAssignment(name: String, subjectId: Int, description: String, dueDate: Long, priority: AssignmentPriority) {
        viewModelScope.launch {
            repository.insertAssignment(
                Assignment(
                    subjectId = subjectId,
                    name = name,
                    description = description,
                    assignedDate = System.currentTimeMillis(),
                    dueDate = dueDate,
                    priority = priority,
                    status = AssignmentStatus.NOT_STARTED
                )
            )
            refreshMyDay()
        }
    }

    fun addGoal(title: String, deadline: Long, target: Double) {
        viewModelScope.launch {
            val goal = com.student360.app.data.local.entity.Goal(
                title = title,
                target = target,
                currentProgress = 0.0,
                deadline = deadline
            )
            repository.insertGoal(goal)
            refreshMyDay()
        }
    }

    private fun getMidnightTime(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

data class MyDayItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String, // COLLEGE, STUDY, ASSIGNMENTS, PERSONAL
    val priority: TaskPriority,
    val completed: Boolean,
    val rawId: Int
)
