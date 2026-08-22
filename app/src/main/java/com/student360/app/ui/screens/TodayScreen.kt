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
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayLectures by viewModel.todayLectures.collectAsState()
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val targetPercentage by viewModel.targetPercentage.collectAsState()

    var showAddExtraDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(selectedDate) { dateFormatter.format(Date(selectedDate)) }

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = targetPercentage.toInt()

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Header: Student360 | 57.41 | 75 | +
            item {
                StudentScreenHeader(
                    title = "Student360",
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
                        fontSize = 20.sp
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

            // 3. Day Status Banner
            item {
                DayStatusBanner(
                    statusTitle = dayStatusInfo.first,
                    statusDotColor = dayStatusInfo.second,
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
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                            fontSize = 17.sp,
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
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    if (startTime.isNotBlank()) {
                        Text(
                            text = "⏰ $startTime" + (if (endTime.isNotBlank()) " – $endTime" else "") + (if (room.isNotBlank()) " • $room" else ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            fontSize = 11.sp
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
