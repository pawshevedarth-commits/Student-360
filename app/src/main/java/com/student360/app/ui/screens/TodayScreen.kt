@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student360.app.data.local.entity.*
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.StudentCard
import com.student360.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Overview / Today Page (Academic Command Center)
 * Answers "What should I focus on today?" using shared real data:
 * - Time-aware Greeting
 * - Your Day Summary (Classes, Assignments, Goals)
 * - Attendance Overview Card with CTA linking to Attendance Management
 * - Smart Attendance Priority (Needs Attention)
 * - Recommended Today study session with instant timer launch
 * - Quick Actions (+ Assignment, + Task, + Study Session, + Goal)
 * - Today's Class Overview & Upcoming Timeline
 */
@Composable
fun TodayScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onStartStudySession: ((subjectId: Int, topic: String, durationMins: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()

    val todayLectures by viewModel.todayLectures.collectAsState()
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val targetPercentage by viewModel.targetPercentage.collectAsState()

    val allAssignments by repository.assignmentsFlow.collectAsState(initial = emptyList())
    val allGoals by repository.goalsFlow.collectAsState(initial = emptyList())
    val allExams by repository.examsFlow.collectAsState(initial = emptyList())

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddAssignDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    val formattedDate = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = targetPercentage.toInt()

    val overallStatusColor = when {
        overallPct >= targetPercentage -> colors.success
        overallPct >= (targetPercentage - 5.0) -> colors.warning
        else -> colors.danger
    }

    // Time-aware greeting
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --------------------------------------------------
            // 1. GREETING & COMMAND CENTER HEADER
            // --------------------------------------------------
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            letterSpacing = 1.2.sp
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
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 20.sp
                    )
                }
            }

            // --------------------------------------------------
            // 2. YOUR DAY METRICS (Classes, Assignments, Goals)
            // --------------------------------------------------
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "YOUR DAY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
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
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("${todayLectures.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary)
                                    Text("Classes", fontSize = 11.5.sp, color = colors.textSecondary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.elevatedCard,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("${pendingAssignments.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary)
                                    Text("Assignments", fontSize = 11.5.sp, color = colors.textSecondary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.elevatedCard,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("${activeGoals.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary)
                                    Text("Goals", fontSize = 11.5.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------
            // 3. ATTENDANCE OVERVIEW CARD (with CTA to Attendance Screen)
            // --------------------------------------------------
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ATTENDANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = overallStatusColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, overallStatusColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (overallPct >= targetPercentage) "Safe" else "Action Needed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = overallStatusColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Overall Attendance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", overallPct)}%",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Target",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "$targetPct%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }

                        // CTA to Dedicated Attendance Management Screen
                        OutlinedButton(
                            onClick = onNavigateToAttendance,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Manage Attendance ➔", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // --------------------------------------------------
            // 4. NEEDS ATTENTION (Smart Attendance Priority)
            // --------------------------------------------------
            if (criticalSubjectPair != null) {
                item {
                    val (critSub, critStats) = criticalSubjectPair
                    val critRec = viewModel.calculateRecommendation(
                        critStats.attended,
                        critStats.attended + critStats.missed,
                        critSub.targetPercentage
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.danger.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "NEEDS ATTENTION",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = colors.danger,
                                    letterSpacing = 0.8.sp
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = colors.danger
                                ) {
                                    Text(
                                        "HIGH PRIORITY",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
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

                                Button(
                                    onClick = onNavigateToAttendance,
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("View Attendance", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------
            // 5. RECOMMENDED TODAY (Study Recommendation)
            // --------------------------------------------------
            if (studyRecommendation != null) {
                item {
                    val (subId, subName, recDetail) = studyRecommendation
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.accent.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
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
                                    color = colors.accent,
                                    letterSpacing = 0.8.sp
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Start Session", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------
            // 6. QUICK ACTIONS (+ Assignment, + Task, + Study, + Goal)
            // --------------------------------------------------
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "QUICK ACTIONS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
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
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.accent)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Goal", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------
            // 7. COMING UP (Nearest Assignment or Exam)
            // --------------------------------------------------
            if (comingUpItem != null) {
                item {
                    val (comingTitle, comingBadge) = comingUpItem
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.elevatedCard,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
            }

            // --------------------------------------------------
            // 8. TODAY'S CLASSES OVERVIEW
            // --------------------------------------------------
            item {
                Text(
                    text = "TODAY'S CLASSES",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary,
                    letterSpacing = 1.sp
                )
            }

            if (todayLectures.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("No classes scheduled for today.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                            Text("Enjoy your study time or catch up on assignments.", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary.copy(alpha = 0.7f))
                        }
                    }
                }
            } else {
                items(todayLectures) { item ->
                    val subject = item.subject
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                                Text(subject.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                if (item.startTime.isNotBlank()) {
                                    Text("${item.startTime} – ${item.endTime} ${if (item.room.isNotBlank()) "· " + item.room else ""}", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                }
                            }
                            TextButton(onClick = onNavigateToAttendance) {
                                Text("Attendance ➔", fontSize = 12.sp, color = colors.accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
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

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, target: Double, dueDays: Int) -> Unit
) {
    val colors = LocalAppColors.current
    var title by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("10") }
    var dueDaysStr by remember { mutableStateOf("7") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                "Add New Goal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it },
                        label = { Text("Target Hours") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
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
                        value = dueDaysStr,
                        onValueChange = { dueDaysStr = it },
                        label = { Text("Days") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val target = targetStr.toDoubleOrNull() ?: 10.0
                        val days = dueDaysStr.toIntOrNull() ?: 7
                        onSave(title, target, days)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add Goal", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

