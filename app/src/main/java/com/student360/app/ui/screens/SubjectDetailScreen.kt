@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.student360.app.data.repository.SubjectStats
import com.student360.app.service.ExamEngine
import com.student360.app.ui.components.StudentCard
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SubjectDetailScreen(
    subject: Subject,
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    onBack: () -> Unit,
    onNavigateToExams: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val allSubjectsWithStats by viewModel.allSubjectsWithStats.collectAsState()
    val subjectRecords by viewModel.selectedSubjectRecords.collectAsState()

    val currentPair = allSubjectsWithStats.find { it.first.id == subject.id }
    val currentSubject = currentPair?.first ?: subject
    val stats = currentPair?.second

    val historyLogs by repository.getHistoryForSubjectFlow(subject.id).collectAsState(initial = emptyList())
    val allStudySessions by repository.studySessionsFlow.collectAsState(initial = emptyList())
    val studySessions = remember(allStudySessions, subject.id) {
        allStudySessions.filter { it.subjectId == subject.id }
    }
    val allAssignments by repository.assignmentsFlow.collectAsState(initial = emptyList())
    val allExams by repository.examsFlow.collectAsState(initial = emptyList())
    val allTimetable by repository.timetableFlow.collectAsState(initial = emptyList())

    val totalStudyMins = remember(studySessions) {
        studySessions.sumOf { it.duration }
    }
    val studyTimeStr = remember(totalStudyMins) {
        val h = totalStudyMins / 60
        val m = totalStudyMins % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    val subjectAssignments = remember(allAssignments, subject.id) {
        allAssignments.filter { it.subjectId == subject.id }
    }
    val completedAssignsCount = remember(subjectAssignments) {
        subjectAssignments.count { it.status == AssignmentStatus.COMPLETED }
    }

    val subjectExams = remember(allExams, subject.id) {
        allExams.filter { it.subjectId == subject.id && it.date >= System.currentTimeMillis() - 86400000L }
            .sortedBy { it.date }
    }
    val upcomingAssigns = remember(subjectAssignments) {
        subjectAssignments.filter { it.status != AssignmentStatus.COMPLETED && it.dueDate >= System.currentTimeMillis() - 86400000L }
            .sortedBy { it.dueDate }
    }
    val scheduledDaysStr = remember(allTimetable, subject.id) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        allTimetable.filter { it.subjectId == subject.id }.map { days[it.dayOfWeek.coerceIn(0, 6)] }.distinct().joinToString(", ")
    }

    var selectedStatusFilter by remember { mutableStateOf<AttendanceStatus?>(null) }
    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showBaselineDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSimulatorDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }

    val percentage = stats?.percentage ?: 100.0
    val targetPct = currentSubject.targetPercentage.toInt()

    val statusColor = when {
        percentage >= currentSubject.targetPercentage -> colors.success
        percentage >= currentSubject.targetPercentage - 5.0 -> colors.warning
        else -> colors.danger
    }

    val totalConducted = (stats?.attended ?: 0) + (stats?.missed ?: 0)
    val recommendation = viewModel.calculateRecommendation(
        stats?.attended ?: 0,
        totalConducted,
        currentSubject.targetPercentage
    )

    // Filter records based on selected chip
    val filteredRecords = remember(subjectRecords, selectedStatusFilter) {
        if (selectedStatusFilter == null) {
            subjectRecords.sortedByDescending { it.date }
        } else {
            subjectRecords.filter { it.status == selectedStatusFilter }.sortedByDescending { it.date }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Navigation & Title Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentSubject.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentSubject.faculty.isNotBlank()) {
                                Text(
                                    text = "Faculty: ${currentSubject.faculty}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Percentage Badge Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = "${String.format(Locale.US, "%.2f", percentage)} | $targetPct",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Subject",
                                tint = colors.danger,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (currentSubject.isArchived) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = colors.warning.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "This subject is archived",
                                    fontWeight = FontWeight.Bold,
                                    color = colors.warning,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "All historical attendance is preserved intact.",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.restoreSubject(currentSubject)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Restore", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Summary Stats Card
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = recommendation,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        recommendation.startsWith("can miss") -> colors.success
                                        recommendation.startsWith("need to attend") -> colors.danger
                                        else -> colors.accent
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Target: ${targetPct}% • Total Conducted: $totalConducted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            // Quick action buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { showSimulatorDialog = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.elevatedCard)
                                        .border(BorderStroke(1.dp, colors.border), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "What-If Simulator",
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showBaselineDialog = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.elevatedCard)
                                        .border(BorderStroke(1.dp, colors.border), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit baseline",
                                        tint = colors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { showHistoryDialog = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.elevatedCard)
                                        .border(BorderStroke(1.dp, colors.border), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Audit History",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Metric breakdown row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.elevatedCard,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetricColumn(title = "Attended", value = "${stats?.attended ?: 0}", color = colors.success)
                                MetricColumn(title = "Missed", value = "${stats?.missed ?: 0}", color = colors.danger)
                                MetricColumn(title = "Off", value = "${stats?.off ?: 0}", color = colors.warning)
                                MetricColumn(title = "Total", value = "$totalConducted", color = colors.textPrimary)
                            }
                        }
                    }
                }
            }

            // Academic Overview Card (Attendance, Study Time, Assignments)
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Academic Overview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
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
                                    Text("${String.format(Locale.US, "%.1f", percentage)}%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = statusColor)
                                    Text("Attendance", fontSize = 11.sp, color = colors.textSecondary)
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
                                    Text(studyTimeStr, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.accent)
                                    Text("Study Time", fontSize = 11.sp, color = colors.textSecondary)
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
                                    Text("$completedAssignsCount/${subjectAssignments.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textPrimary)
                                    Text("Assignments", fontSize = 11.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
            }

            // Upcoming for Subject (Classes, Assignments, Tests)
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Upcoming for ${currentSubject.name}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        if (scheduledDaysStr.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Weekly Classes", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                                Text(scheduledDaysStr, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = colors.accent)
                            }
                        }
                        if (upcomingAssigns.isNotEmpty()) {
                            upcomingAssigns.take(2).forEach { assign ->
                                val dateStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(assign.dueDate))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(assign.name, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = colors.danger, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (subjectExams.isNotEmpty()) {
                            subjectExams.take(3).forEach { ex ->
                                val dateStr = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ex.date))
                                val daysLeft = ExamEngine.getDaysRemaining(ex.date)
                                val countdownText = ExamEngine.getCountdownText(daysLeft)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            ex.examType.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            "$dateStr • $countdownText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (daysLeft in 0..3) colors.danger else colors.textSecondary
                                        )
                                    }
                                    TextButton(
                                        onClick = onNavigateToExams,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("View Exam", style = MaterialTheme.typography.labelSmall, color = colors.accent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        if (scheduledDaysStr.isBlank() && upcomingAssigns.isEmpty() && subjectExams.isEmpty()) {
                            Text("No upcoming classes, assignments, or exams recorded.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }

            // Filters Bar (e.g. "18 records", All, Present, Missed, Off)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Record count pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.elevatedCard,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "${subjectRecords.size} records",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }

                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.activePill,
                            selectedLabelColor = if (colors.isDark) Color.White else colors.accent,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
                    )

                    FilterChip(
                        selected = selectedStatusFilter == AttendanceStatus.PRESENT,
                        onClick = { selectedStatusFilter = AttendanceStatus.PRESENT },
                        label = { Text("Present") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.success.copy(alpha = 0.25f),
                            selectedLabelColor = colors.success,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
                    )

                    FilterChip(
                        selected = selectedStatusFilter == AttendanceStatus.ABSENT,
                        onClick = { selectedStatusFilter = AttendanceStatus.ABSENT },
                        label = { Text("Missed") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.danger.copy(alpha = 0.25f),
                            selectedLabelColor = colors.danger,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
                    )

                    FilterChip(
                        selected = selectedStatusFilter == AttendanceStatus.OFF,
                        onClick = { selectedStatusFilter = AttendanceStatus.OFF },
                        label = { Text("Off") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.warning.copy(alpha = 0.25f),
                            selectedLabelColor = colors.warning,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
                    )
                }
            }

            // Attendance Records List
            if (filteredRecords.isEmpty()) {
                item {
                    StudentCard(backgroundColor = colors.card, borderColor = colors.border) {
                        Text(
                            text = "No attendance records match the selected filter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(filteredRecords) { record ->
                    val statusText = when (record.status) {
                        AttendanceStatus.PRESENT -> "Present"
                        AttendanceStatus.ABSENT -> "Missed"
                        AttendanceStatus.OFF -> "Off"
                    }
                    val recordColor = when (record.status) {
                        AttendanceStatus.PRESENT -> colors.success
                        AttendanceStatus.ABSENT -> colors.danger
                        AttendanceStatus.OFF -> colors.warning
                    }

                    StudentCard(
                        backgroundColor = colors.card,
                        borderColor = colors.border,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingRecord = record }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = dateFormatter.format(Date(record.date)),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    if (record.isExtra) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = colors.accent.copy(alpha = 0.2f),
                                            border = BorderStroke(0.5.dp, colors.accent)
                                        ) {
                                            Text(
                                                text = "Extra",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.accent,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "Personal: $statusText • Official: ${record.officialStatus}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )

                                if (!record.notes.isNullOrBlank()) {
                                    Text(
                                        text = "Reason: ${record.notes}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.accent,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = recordColor.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, recordColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = recordColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit record",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edit Attendance Dialog (Sections 11 & 12)
        editingRecord?.let { record ->
            EditAttendanceRecordDialog(
                record = record,
                subject = currentSubject,
                onDismiss = { editingRecord = null },
                onSave = { newStatus, reason, officialStatus ->
                    if (newStatus != null) {
                        viewModel.markLectureAttendance(
                            subjectId = currentSubject.id,
                            date = record.date,
                            status = newStatus,
                            isExtra = record.isExtra,
                            timetableId = record.timetableId,
                            reason = reason,
                            officialStatus = officialStatus
                        )
                    } else {
                        viewModel.markLectureAttendance(
                            subjectId = currentSubject.id,
                            date = record.date,
                            status = null,
                            isExtra = record.isExtra,
                            timetableId = record.timetableId,
                            reason = reason,
                            officialStatus = officialStatus
                        )
                    }
                    editingRecord = null
                    Toast.makeText(context, "Attendance record updated", Toast.LENGTH_SHORT).show()
                },
                onUndo = {
                    viewModel.undoLastChange(currentSubject.id, record.date)
                    editingRecord = null
                    Toast.makeText(context, "Undid last change", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Baseline & Target Dialog
        if (showBaselineDialog) {
            ManualOverrideDialog(
                subject = currentSubject,
                currentAttended = stats?.attended ?: currentSubject.manualAttended,
                currentConducted = totalConducted,
                onDismiss = { showBaselineDialog = false },
                onSave = { attended, conducted ->
                    viewModel.updateManualOverrides(currentSubject, attended, conducted)
                    showBaselineDialog = false
                },
                onUpdateTarget = { target ->
                    viewModel.updateSubjectTarget(currentSubject, target)
                }
            )
        }

        // Audit History Log Dialog
        if (showHistoryDialog) {
            AttendanceHistoryLogDialog(
                subject = currentSubject,
                historyLogs = historyLogs,
                onDismiss = { showHistoryDialog = false }
            )
        }

        // Delete Subject Dialog (Safe Delete vs Permanent Delete)
        if (showDeleteDialog) {
            DeleteSubjectDialog(
                subject = currentSubject,
                stats = stats,
                onDismiss = { showDeleteDialog = false },
                onSafeDelete = {
                    viewModel.safeDeleteSubject(currentSubject)
                    showDeleteDialog = false
                    onBack()
                },
                onPermanentDelete = {
                    viewModel.permanentlyDeleteSubject(currentSubject)
                    showDeleteDialog = false
                    onBack()
                }
            )
        }

        // What-If Simulator Dialog for Current Subject
        if (showSimulatorDialog) {
            AttendanceSimulatorDialog(
                subjects = listOf(currentSubject),
                statsList = listOf(SubjectStats(stats?.attended ?: 0, stats?.missed ?: 0, totalConducted, percentage)),
                initialSelectedIndex = 0,
                onDismiss = { showSimulatorDialog = false }
            )
        }
    }
}

@Composable
fun MetricColumn(title: String, value: String, color: Color) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontSize = 10.sp)
    }
}

/**
 * Edit Attendance Dialog with Reason, Verification State, and Undo
 */
@Composable
fun EditAttendanceRecordDialog(
    record: AttendanceRecord,
    subject: Subject,
    onDismiss: () -> Unit,
    onSave: (AttendanceStatus?, String, String) -> Unit,
    onUndo: () -> Unit
) {
    val colors = LocalAppColors.current
    var selectedStatus by remember { mutableStateOf<AttendanceStatus?>(record.status) }
    var reason by remember { mutableStateOf(record.notes ?: "") }
    var officialStatus by remember { mutableStateOf(record.officialStatus) }

    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Edit Attendance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${subject.name} • ${dateFormatter.format(Date(record.date))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Select Personal Attendance State:", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedStatus == AttendanceStatus.PRESENT,
                        onClick = { selectedStatus = AttendanceStatus.PRESENT },
                        label = { Text("Present") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.success,
                            selectedLabelColor = Color.White,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
                    )
                    FilterChip(
                        selected = selectedStatus == AttendanceStatus.ABSENT,
                        onClick = { selectedStatus = AttendanceStatus.ABSENT },
                        label = { Text("Missed") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.danger,
                            selectedLabelColor = Color.White,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
                    )
                    FilterChip(
                        selected = selectedStatus == AttendanceStatus.OFF,
                        onClick = { selectedStatus = AttendanceStatus.OFF },
                        label = { Text("Off") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.warning,
                            selectedLabelColor = Color.White,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
                    )
                    FilterChip(
                        selected = selectedStatus == null,
                        onClick = { selectedStatus = null },
                        label = { Text("Clear") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.elevatedCard,
                            selectedLabelColor = Color.White,
                            containerColor = colors.card,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Correction Reason (optional)") },
                    placeholder = { Text("e.g. Accidentally marked absent") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Personal vs Official verification notice
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.elevatedCard,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ℹ️ Personal Attendance Tracker",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Text(
                            text = "Student360 tracks your personal log. Official college attendance remains: $officialStatus.",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedStatus, reason, officialStatus) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onUndo) {
                    Text("Undo Last", color = colors.warning)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        }
    )
}

/**
 * Audit History Log Modal showing attendance changes
 */
@Composable
fun AttendanceHistoryLogDialog(
    subject: Subject,
    historyLogs: List<AttendanceHistory>,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()) }
    val recordDateFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                text = "Attendance History Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (historyLogs.isEmpty()) {
                    Text(
                        text = "No recorded edits or changes for ${subject.name}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(historyLogs) { log ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.elevatedCard,
                                border = BorderStroke(1.dp, colors.border),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Class Date: ${recordDateFormatter.format(Date(log.date))}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "Changed: ${log.originalStatus} ➔ ${log.newStatus}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.accent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (!log.reason.isNullOrBlank()) {
                                        Text(
                                            text = "Reason: ${log.reason}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textSecondary
                                        )
                                    }
                                    Text(
                                        text = "Logged: ${dateFormatter.format(Date(log.changeTimestamp))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary.copy(alpha = 0.6f),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
