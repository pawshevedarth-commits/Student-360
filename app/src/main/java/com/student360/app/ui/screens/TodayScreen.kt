@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.student360.app.data.local.entity.*
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    onNavigateToSettings: () -> Unit = {},
    onStartStudySession: ((subjectId: Int, topic: String, durationMins: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()

    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayLectures by viewModel.todayLectures.collectAsState()
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val targetPercentage by viewModel.targetPercentage.collectAsState()

    val allAssignments by repository.assignmentsFlow.collectAsState(initial = emptyList())
    val allGoals by repository.goalsFlow.collectAsState(initial = emptyList())
    val allExams by repository.examsFlow.collectAsState(initial = emptyList())

    var showAddExtraDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddAssignDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(selectedDate) { dateFormatter.format(Date(selectedDate)) }

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = targetPercentage.toInt()

    // Command Center Computations (Real Data)
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "GOOD MORNING 👋"
            in 12..16 -> "GOOD AFTERNOON ☀️"
            in 17..21 -> "GOOD EVENING 🌙"
            else -> "GOOD NIGHT 🌌"
        }
    }

    val pendingAssignments = remember(allAssignments) {
        allAssignments.filter { it.status != AssignmentStatus.COMPLETED }
    }
    val activeGoals = remember(allGoals) {
        allGoals.filter { it.status == GoalStatus.ACTIVE }
    }

    // Critical subject below target (Smart Attendance Priority)
    val criticalSubjectPair = remember(subjectsWithStats) {
        subjectsWithStats.filter { (sub, stats) ->
            !sub.isArchived && stats.percentage < sub.targetPercentage
        }.minByOrNull { it.second.percentage }
    }

    // Study Recommendation (Priority Subject or Nearest Assignment)
    val studyRecommendation = remember(criticalSubjectPair, pendingAssignments, todayLectures) {
        if (criticalSubjectPair != null) {
            val (sub, _) = criticalSubjectPair
            Triple(sub.id, sub.name, "45 min study · Attendance Target ${sub.targetPercentage.toInt()}%")
        } else if (pendingAssignments.isNotEmpty()) {
            val nextAssign = pendingAssignments.minByOrNull { it.dueDate }!!
            val sub = subjectsWithStats.find { it.first.id == nextAssign.subjectId }?.first
            Triple(nextAssign.subjectId, sub?.name ?: nextAssign.name, "45 min study · ${nextAssign.name}")
        } else if (todayLectures.isNotEmpty()) {
            val firstSub = todayLectures.first().subject
            Triple(firstSub.id, firstSub.name, "45 min revision")
        } else {
            null
        }
    }

    // Coming Up (Nearest Assignment or Exam)
    val comingUpItem = remember(pendingAssignments, allExams, subjectsWithStats) {
        val now = System.currentTimeMillis()
        val nextAssign = pendingAssignments.minByOrNull { it.dueDate }
        val nextExam = allExams.filter { it.date >= now - 24 * 3600 * 1000L }.minByOrNull { it.date }

        if (nextAssign != null && (nextExam == null || nextAssign.dueDate <= nextExam.date)) {
            val days = ((nextAssign.dueDate - now) / (24 * 3600 * 1000L)).toInt()
            val dueStr = when {
                days < 0 -> "Overdue"
                days == 0 -> "Due today"
                days == 1 -> "Due tomorrow"
                else -> "Due in $days days"
            }
            val subName = subjectsWithStats.find { it.first.id == nextAssign.subjectId }?.first?.name ?: "Assignment"
            Pair("$subName: ${nextAssign.name}", dueStr)
        } else if (nextExam != null) {
            val days = ((nextExam.date - now) / (24 * 3600 * 1000L)).toInt()
            val dueStr = when {
                days == 0 -> "Today"
                days == 1 -> "Tomorrow"
                else -> "In $days days"
            }
            val subName = subjectsWithStats.find { it.first.id == nextExam.subjectId }?.first?.name ?: "Exam"
            Pair("$subName ${nextExam.examType.name}", dueStr)
        } else {
            null
        }
    }

    // Determine aggregate day status from marked records
    val dayStatusInfo = remember(todayLectures, colors) {
        val records = todayLectures.mapNotNull { it.attendanceRecord }
        if (records.isEmpty()) {
            Triple("Not marked", colors.textSecondary, DayAttendanceState.NOT_MARKED)
        } else {
            val attended = records.count { it.status == AttendanceStatus.PRESENT }
            val missed = records.count { it.status == AttendanceStatus.ABSENT }
            val off = records.count { it.status == AttendanceStatus.OFF }

            when {
                attended > 0 && missed == 0 && off == 0 -> Triple("Attended", colors.success, DayAttendanceState.ATTENDED)
                missed > 0 && attended == 0 && off == 0 -> Triple("Missed", colors.danger, DayAttendanceState.MISSED)
                off > 0 && attended == 0 && missed == 0 -> Triple("Off", colors.warning, DayAttendanceState.OFF)
                attended > 0 || missed > 0 -> Triple("Mixed", colors.accent, DayAttendanceState.MIXED)
                else -> Triple("Not marked", colors.textSecondary, DayAttendanceState.NOT_MARKED)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Header: Today | 57.41% | 75% | +
            item {
                StudentScreenHeader(
                    title = "Today",
                    overallPercentage = overallPct,
                    targetPercentage = targetPct,
                    onAddClick = { showAddExtraDialog = true }
                )
            }

            // 2. Date Title: Fri, 21 Aug 2026
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { viewModel.previousDay() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Day", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                        }
                        IconButton(
                            onClick = { viewModel.nextDay() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Day", tint = colors.textSecondary, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }

            // 3. TODAY COMMAND CENTER (Real Data)
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Greeting & Heading
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = greeting,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                letterSpacing = 1.1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.accent.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "Command Center",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // YOUR DAY Metrics
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "YOUR DAY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.elevatedCard,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("${todayLectures.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                                        Text("Classes", fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.elevatedCard,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("${pendingAssignments.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                                        Text("Assignments", fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.elevatedCard,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("${activeGoals.size}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = colors.textPrimary)
                                        Text("Goals", fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                }
                            }
                        }

                        // NEEDS ATTENTION (Smart Attendance Priority)
                        if (criticalSubjectPair != null) {
                            val (critSub, critStats) = criticalSubjectPair
                            val critRec = viewModel.calculateRecommendation(
                                critStats.attended,
                                critStats.attended + critStats.missed,
                                critSub.targetPercentage
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.danger.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "NEEDS ATTENTION",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = colors.danger
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.danger,
                                            modifier = Modifier.padding(0.dp)
                                        ) {
                                            Text(
                                                "HIGH PRIORITY",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        critSub.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        "${String.format(Locale.US, "%.1f", critStats.percentage)}% attendance · Target ${critSub.targetPercentage.toInt()}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.danger,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        critRec,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }

                        // RECOMMENDED TODAY
                        if (studyRecommendation != null) {
                            val (subId, subName, recDetail) = studyRecommendation
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.accent.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            "RECOMMENDED TODAY",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = colors.accent
                                        )
                                        Text(
                                            subName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            recDetail,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            onStartStudySession?.invoke(subId, "Today Study", 45)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Start Session", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }

                        // COMING UP
                        if (comingUpItem != null) {
                            val (comingTitle, comingBadge) = comingUpItem
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.elevatedCard,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                                        Text("COMING UP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = colors.textSecondary)
                                        Text(comingTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colors.accent.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f))
                                    ) {
                                        Text(comingBadge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                            }
                        }

                        // QUICK ACTIONS (Section 11)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "QUICK ACTIONS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAddAssignDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.accent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Assignment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { showAddTaskDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.accent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Task", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = {
                                        val firstSub = subjectsWithStats.firstOrNull()?.first
                                        onStartStudySession?.invoke(firstSub?.id ?: 0, "Study Session", 45)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.accent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Study Session", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { showAddGoalDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, colors.border),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.accent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Goal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // 4. Day Status Banner
            item {
                DayStatusBanner(
                    statusTitle = dayStatusInfo.first,
                    statusDotColor = dayStatusInfo.second,
                    currentState = dayStatusInfo.third,
                    selectedDateText = formattedDate,
                    onClearAll = {
                        viewModel.clearAllForDate(selectedDate)
                        Toast.makeText(context, "Attendance marks cleared", Toast.LENGTH_SHORT).show()
                    },
                    onMarkAllOff = {
                        viewModel.markAllForDate(selectedDate, AttendanceStatus.OFF)
                        Toast.makeText(context, "Marked day as Off", Toast.LENGTH_SHORT).show()
                    },
                    onMarkAllMissed = {
                        viewModel.markAllForDate(selectedDate, AttendanceStatus.ABSENT)
                        Toast.makeText(context, "Marked all as Missed", Toast.LENGTH_SHORT).show()
                    },
                    onMarkAllAttended = {
                        viewModel.markAllForDate(selectedDate, AttendanceStatus.PRESENT)
                        Toast.makeText(context, "Marked all as Attended", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 4. List of Subject Attendance Cards
            if (todayLectures.isEmpty() && subjectsWithStats.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No subjects found",
                        subtitle = "Add your academic courses in Subjects tab to track daily attendance.",
                        actionText = "+ Add Course",
                        onActionClick = { showAddExtraDialog = true }
                    )
                }
            } else if (todayLectures.isEmpty()) {
                item {
                    EmptyDayView(
                        title = "It's your day off!",
                        subtitle = "No lectures scheduled for $formattedDate.",
                        onAddClick = { showAddExtraDialog = true }
                    )
                }
            } else {
                items(todayLectures) { item ->
                    val subject = item.subject
                    val stats = item.stats
                    val currentStatus = item.attendanceRecord?.status

                    val totalConducted = stats.attended + stats.missed
                    val diff = stats.percentage - subject.targetPercentage
                    val statusColor = when {
                        diff >= 0 -> colors.success
                        diff >= -5.0 -> colors.warning
                        else -> colors.danger
                    }

                    val recommendation = viewModel.calculateRecommendation(
                        stats.attended,
                        totalConducted,
                        subject.targetPercentage
                    )

                    TodayAttendanceCard(
                        subjectName = subject.name,
                        percentage = stats.percentage,
                        target = subject.targetPercentage.toInt(),
                        recommendation = recommendation,
                        statusColor = statusColor,
                        startTime = item.startTime,
                        endTime = item.endTime,
                        room = item.room,
                        isExtra = item.isExtra,
                        attendedCount = stats.attended,
                        missedCount = stats.missed,
                        offCount = stats.off,
                        totalCount = totalConducted,
                        currentStatus = currentStatus,
                        onMarkStatus = { newStatus ->
                            viewModel.markLectureAttendance(
                                subjectId = subject.id,
                                date = selectedDate,
                                status = newStatus,
                                isExtra = item.isExtra,
                                timetableId = item.timetableEntry?.id
                            )
                        }
                    )
                }
            }
        }

        // Add Extra Lecture Dialog
        if (showAddExtraDialog) {
            AddExtraLectureDialog(
                subjects = subjectsWithStats.map { it.first },
                onDismiss = { showAddExtraDialog = false },
                onSave = { subjectId, start, end, room, faculty ->
                    viewModel.addExtraLecture(
                        subjectId = subjectId,
                        date = selectedDate,
                        startTime = start,
                        endTime = end,
                        room = room,
                        faculty = faculty
                    )
                    showAddExtraDialog = false
                    Toast.makeText(context, "Extra lecture added", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Quick Action: Add Assignment Dialog
        if (showAddAssignDialog) {
            val activeSubs = subjectsWithStats.map { it.first }.filter { !it.isArchived }
            if (activeSubs.isNotEmpty()) {
                AddAssignmentDialog(
                    subjects = activeSubs,
                    onDismiss = { showAddAssignDialog = false },
                    onSave = { name, subId, desc, dueDays, priority ->
                        coroutineScope.launch {
                            val dueDate = System.currentTimeMillis() + (dueDays * 24 * 3600 * 1000L)
                            repository.insertAssignment(
                                Assignment(
                                    subjectId = subId,
                                    name = name,
                                    description = desc,
                                    assignedDate = System.currentTimeMillis(),
                                    dueDate = dueDate,
                                    priority = priority,
                                    status = AssignmentStatus.NOT_STARTED
                                )
                            )
                            Toast.makeText(context, "Assignment created", Toast.LENGTH_SHORT).show()
                        }
                        showAddAssignDialog = false
                    }
                )
            } else {
                Toast.makeText(context, "Please add a subject first", Toast.LENGTH_SHORT).show()
                showAddAssignDialog = false
            }
        }

        // Quick Action: Add Task Dialog
        if (showAddTaskDialog) {
            val activeSubs = subjectsWithStats.map { it.first }.filter { !it.isArchived }
            AddTaskDialog(
                subjects = activeSubs,
                onDismiss = { showAddTaskDialog = false },
                onSave = { title, desc, category, subId, priority, duration ->
                    coroutineScope.launch {
                        repository.insertTask(
                            Task(
                                subjectId = subId,
                                title = title,
                                description = desc,
                                category = category,
                                priority = priority,
                                dueDate = System.currentTimeMillis() + 86400000L,
                                estimatedDuration = duration,
                                completed = false
                            )
                        )
                        Toast.makeText(context, "Task created", Toast.LENGTH_SHORT).show()
                    }
                    showAddTaskDialog = false
                }
            )
        }

        // Quick Action: Add Goal Dialog
        if (showAddGoalDialog) {
            AddGoalDialog(
                onDismiss = { showAddGoalDialog = false },
                onSave = { title, target, dueDays ->
                    coroutineScope.launch {
                        val deadline = System.currentTimeMillis() + (dueDays * 24 * 3600 * 1000L)
                        repository.insertGoal(
                            Goal(
                                title = title,
                                target = target,
                                currentProgress = 0.0,
                                deadline = deadline,
                                status = GoalStatus.ACTIVE
                            )
                        )
                        Toast.makeText(context, "Goal created", Toast.LENGTH_SHORT).show()
                    }
                    showAddGoalDialog = false
                }
            )
        }
    }
}

/**
 * Standardized Attendance Card matching Reference Screenshot
 */
@Composable
fun TodayAttendanceCard(
    subjectName: String,
    percentage: Double,
    target: Int,
    recommendation: String,
    statusColor: Color,
    startTime: String,
    endTime: String,
    room: String,
    isExtra: Boolean,
    attendedCount: Int,
    missedCount: Int,
    offCount: Int,
    totalCount: Int,
    currentStatus: AttendanceStatus?,
    onMarkStatus: (AttendanceStatus?) -> Unit
) {
    val colors = LocalAppColors.current

    StudentCard(
        backgroundColor = colors.card,
        borderColor = colors.border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Main Content Row: Left Fractional Badge + Subject Title & Recommendation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular / Fractional Badge (e.g. 41.67 / 75)
                AttendanceFractionBadge(
                    percentage = percentage,
                    target = target,
                    statusColor = statusColor
                )

                // Subject Name + Recommendation text
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (isExtra) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = colors.elevatedCard,
                                border = BorderStroke(1.dp, colors.border)
                            ) {
                                Text(
                                    text = "Extra",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = recommendation,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    if (startTime.isNotBlank()) {
                        Text(
                            text = "⏰ $startTime" + (if (endTime.isNotBlank()) " – $endTime" else "") + (if (room.isNotBlank()) " • $room" else ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bottom Right Status Controls (⊘, —, ✕, ✓)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAttendanceRoundButton(
                        symbol = "⊘",
                        isSelected = currentStatus == null,
                        activeColor = colors.textSecondary,
                        onClick = { onMarkStatus(null) }
                    )
                    QuickAttendanceRoundButton(
                        symbol = "—",
                        isSelected = currentStatus == AttendanceStatus.OFF,
                        activeColor = colors.warning,
                        onClick = { onMarkStatus(AttendanceStatus.OFF) }
                    )
                    QuickAttendanceRoundButton(
                        symbol = "✕",
                        isSelected = currentStatus == AttendanceStatus.ABSENT,
                        activeColor = colors.danger,
                        onClick = { onMarkStatus(AttendanceStatus.ABSENT) }
                    )
                    QuickAttendanceRoundButton(
                        symbol = "✓",
                        isSelected = currentStatus == AttendanceStatus.PRESENT,
                        activeColor = colors.success,
                        onClick = { onMarkStatus(AttendanceStatus.PRESENT) }
                    )
                }
            }
        }
    }
}

/**
 * Empty day view when no classes are scheduled.
 */
@Composable
fun EmptyDayView(
    title: String,
    subtitle: String,
    onAddClick: () -> Unit
) {
    val colors = LocalAppColors.current

    StudentCard(
        backgroundColor = colors.card,
        borderColor = colors.border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.elevatedCard),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = colors.accent, modifier = Modifier.size(24.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onAddClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text("+ Add Extra Lecture", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
        }
    }
}

/**
 * Add Extra Lecture Dialog
 */
@Composable
fun AddExtraLectureDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (Int, String, String, String, String) -> Unit
) {
    val colors = LocalAppColors.current

    if (subjects.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = colors.card,
            title = { Text("No Subjects", color = colors.textPrimary) },
            text = { Text("Please register a course in the Subjects tab first.", color = colors.textSecondary) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK", color = colors.accent) } }
        )
        return
    }

    var selectedSubjectId by remember { mutableStateOf(subjects.first().id) }
    var startTime by remember { mutableStateOf("10:00") }
    var endTime by remember { mutableStateOf("11:00") }
    var room by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val selectedSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                "Add Extra Lecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Subject Dropdown
                Box {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedSubject.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(colors.card)
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name, color = colors.textPrimary) },
                                onClick = {
                                    selectedSubjectId = sub.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Hall (optional)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty / Substitute (optional)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedSubjectId, startTime, endTime, room, faculty) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Lecture", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, Int) -> Unit
) {
    val colors = LocalAppColors.current
    var goalTitle by remember { mutableStateOf("") }
    var goalTarget by remember { mutableStateOf("10") }
    var goalDueDays by remember { mutableStateOf("14") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                "Add Academic Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = goalTitle,
                    onValueChange = { goalTitle = it },
                    label = { Text("Goal Title (e.g. Complete DBMS Unit 2)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goalTarget,
                    onValueChange = { goalTarget = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Target Value (e.g. 10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goalDueDays,
                    onValueChange = { goalDueDays = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Due in (Days from now)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = goalTarget.toDoubleOrNull() ?: 10.0
                    val days = goalDueDays.toIntOrNull() ?: 14
                    onSave(goalTitle, target, days)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp),
                enabled = goalTitle.isNotBlank()
            ) {
                Text("Save Goal", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
