@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    scheduleViewModel: ScheduleViewModel? = null,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayLectures by viewModel.todayLectures.collectAsState()
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val allSubjectsWithStats by viewModel.allSubjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val targetPercentage by viewModel.targetPercentage.collectAsState()

    var showAddExtraDialog by remember { mutableStateOf(false) }
    var showSimulatorDialog by remember { mutableStateOf(false) }

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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top Contextual Header: Attendance | 57.41% | 75% | Simulator | + Add Extra Class
            item {
                StudentScreenHeader(
                    title = "Attendance",
                    overallPercentage = overallPct,
                    targetPercentage = targetPct,
                    extraAction = {
                        IconButton(
                            onClick = { showSimulatorDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = "Attendance Simulator",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    onAddClick = { showAddExtraDialog = true }
                )
            }

            // 2. Date Navigation Row: Previous Day | Current Date | Next Day
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
                        fontSize = 19.sp,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { viewModel.previousDay() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Previous Day",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.nextDay() },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = "Next Day",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // 3. Day Status Banner with batch controls (Clear, Off, Missed, Attended)
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

            // 4. List of Daily Lecture Attendance Cards
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
                items(todayLectures, key = { it.timetableEntry?.id ?: it.subject.id }) { item ->
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

        // Attendance Simulator Dialog
        if (showSimulatorDialog) {
            val subs = subjectsWithStats.map { it.first }
            val statsList = subjectsWithStats.map { it.second }
            AttendanceSimulatorDialog(
                subjects = subs,
                statsList = statsList,
                initialSelectedIndex = 0,
                onDismiss = { showSimulatorDialog = false }
            )
        }
    }
}
