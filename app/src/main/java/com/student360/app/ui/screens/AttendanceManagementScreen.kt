@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.data.repository.SubjectStats
import com.student360.app.ui.components.DayStatusBanner
import com.student360.app.ui.components.StudentCard
import com.student360.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Dedicated Attendance Management Page
 * Focuses exclusively on attendance tracking, day status marking,
 * subject-wise attendance analytics, calculations, and what-if simulation.
 */
@Composable
fun AttendanceManagementScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    onNavigateToSubjectDetail: (Subject) -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayLectures by viewModel.todayLectures.collectAsState()
    val targetPercentage by viewModel.targetPercentage.collectAsState()

    var showAddExtraDialog by remember { mutableStateOf(false) }
    var showSimulatorDialog by remember { mutableStateOf(false) }
    var simulatorSelectedSubjectIndex by remember { mutableStateOf(0) }

    val formattedDate = remember(selectedDate) {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = targetPercentage.toInt()

    val overallStatusColor = when {
        overallPct >= targetPercentage -> colors.success
        overallPct >= (targetPercentage - 5.0) -> colors.warning
        else -> colors.danger
    }

    // Determine day-level attendance status from today's lecture records
    val dayStatus: DayAttendanceState = remember(todayLectures) {
        if (todayLectures.isEmpty()) {
            DayAttendanceState.NOT_MARKED
        } else {
            val marked = todayLectures.mapNotNull { it.attendanceRecord?.status }
            when {
                marked.isEmpty() -> DayAttendanceState.NOT_MARKED
                marked.all { it == AttendanceStatus.PRESENT } -> DayAttendanceState.ATTENDED
                marked.all { it == AttendanceStatus.ABSENT } -> DayAttendanceState.MISSED
                marked.all { it == AttendanceStatus.OFF } -> DayAttendanceState.OFF
                else -> DayAttendanceState.MIXED
            }
        }
    }

    val dayStatusTitle = when (dayStatus) {
        DayAttendanceState.ATTENDED -> "Attended"
        DayAttendanceState.MISSED -> "Missed"
        DayAttendanceState.OFF -> "Off"
        DayAttendanceState.MIXED -> "Mixed"
        DayAttendanceState.NOT_MARKED -> "Not marked"
    }

    // Real active subjects list
    val activeSubjectsWithStats = remember(subjectsWithStats) {
        subjectsWithStats.filter { !it.first.isArchived }
    }

    // Attendance Simulator calculations (non-mutating, real formula)
    val totalAttended = overallStats?.totalAttended ?: 0
    val totalConducted = overallStats?.totalConducted ?: 0
    
    val ifAttend1 = if (totalConducted + 1 > 0) {
        ((totalAttended + 1).toDouble() / (totalConducted + 1).toDouble()) * 100.0
    } else 100.0

    val ifAttend3 = if (totalConducted + 3 > 0) {
        ((totalAttended + 3).toDouble() / (totalConducted + 3).toDouble()) * 100.0
    } else 100.0

    val ifMiss1 = if (totalConducted + 1 > 0) {
        (totalAttended.toDouble() / (totalConducted + 1).toDouble()) * 100.0
    } else 0.0

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
            // 1. OVERALL ATTENDANCE HERO
            // --------------------------------------------------
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                shape = RoundedCornerShape(8.dp),
                                color = overallStatusColor.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, overallStatusColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = if (overallPct >= targetPercentage) "Safe" else "Action Needed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = overallStatusColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = "Overall Attendance",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", overallPct)}%",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontSize = 28.sp
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
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }

                        // Aggregate conducted stats
                        Text(
                            text = "$totalAttended attended / $totalConducted conducted",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // --------------------------------------------------
            // 2. DATE NAVIGATION
            // --------------------------------------------------
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.previousDay() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Day", tint = colors.textPrimary)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(y, m, d, 0, 0, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            viewModel.selectDate(newCal.timeInMillis)
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Calendar", tint = colors.accent, modifier = Modifier.size(16.dp))
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                        }

                        IconButton(
                            onClick = { viewModel.nextDay() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Day", tint = colors.textPrimary)
                        }
                    }
                }
            }

            // --------------------------------------------------
            // 3. DAY STATUS (Clear, Off, Miss, Att)
            // --------------------------------------------------
            item {
                DayStatusBanner(
                    statusTitle = dayStatusTitle,
                    statusDotColor = when (dayStatus) {
                        DayAttendanceState.ATTENDED -> colors.success
                        DayAttendanceState.MISSED -> colors.danger
                        DayAttendanceState.OFF -> colors.warning
                        DayAttendanceState.MIXED -> colors.accent
                        DayAttendanceState.NOT_MARKED -> colors.textSecondary.copy(alpha = 0.5f)
                    },
                    currentState = dayStatus,
                    selectedDateText = formattedDate,
                    onClearAll = {
                        viewModel.clearAllForDate(selectedDate)
                        Toast.makeText(context, "Attendance cleared for $formattedDate", Toast.LENGTH_SHORT).show()
                    },
                    onMarkAllOff = {
                        viewModel.markAllForDate(selectedDate, AttendanceStatus.OFF)
                        Toast.makeText(context, "Marked day as Off", Toast.LENGTH_SHORT).show()
                    },
                    onMarkAllMissed = {
                        viewModel.markAllForDate(selectedDate, AttendanceStatus.ABSENT)
                        Toast.makeText(context, "Marked day as Missed", Toast.LENGTH_SHORT).show()
                    },
                    onMarkAllAttended = {
                        viewModel.markAllForDate(selectedDate, AttendanceStatus.PRESENT)
                        Toast.makeText(context, "Marked day as Attended", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // --------------------------------------------------
            // 4. SUBJECT ATTENDANCE HEADER + ADD EXTRA LECTURE
            // --------------------------------------------------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SUBJECT ATTENDANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )

                    TextButton(
                        onClick = { showAddExtraDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Extra Lecture", style = MaterialTheme.typography.labelSmall, color = colors.accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // --------------------------------------------------
            // 5. SUBJECT ATTENDANCE CARDS
            // --------------------------------------------------
            if (activeSubjectsWithStats.isEmpty()) {
                item {
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
                            Text(
                                text = "No active subjects registered",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Add subjects in the Subjects tab to start tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(activeSubjectsWithStats) { (subject, stats) ->
                    val totalConductedSub = stats.attended + stats.missed
                    val diff = stats.percentage - subject.targetPercentage
                    val subStatusColor = when {
                        diff >= 0 -> colors.success
                        diff >= -5.0 -> colors.warning
                        else -> colors.danger
                    }

                    // Real status calculation message: "Need to attend X lectures" or "Can safely miss X lectures"
                    val statusMessage = remember(stats, subject.targetPercentage) {
                        if (totalConductedSub == 0) {
                            "No lectures conducted yet"
                        } else if (stats.percentage >= subject.targetPercentage) {
                            val maxMiss = floor((stats.attended * 100.0 / subject.targetPercentage) - totalConductedSub).toInt()
                            when {
                                maxMiss > 1 -> "Can safely miss $maxMiss lectures"
                                maxMiss == 1 -> "Can safely miss 1 lecture"
                                else -> "Can't miss the next lecture"
                            }
                        } else {
                            val needed = ceil((subject.targetPercentage * totalConductedSub - 100.0 * stats.attended) / (100.0 - subject.targetPercentage)).toInt().coerceAtLeast(1)
                            if (needed == 1) "Need to attend 1 lecture" else "Need to attend $needed lectures"
                        }
                    }

                    // Check if this subject has lectures on the currently selected date
                    val lecturesForThisSubject = todayLectures.filter { it.subject.id == subject.id }

                    SubjectAttendanceCard(
                        subject = subject,
                        stats = stats,
                        targetPercentage = subject.targetPercentage,
                        statusMessage = statusMessage,
                        statusColor = subStatusColor,
                        lecturesOnDate = lecturesForThisSubject,
                        onViewDetails = { onNavigateToSubjectDetail(subject) },
                        onMarkLectureStatus = { lectureItem, newStatus ->
                            viewModel.markLectureAttendance(
                                subjectId = subject.id,
                                date = selectedDate,
                                status = newStatus,
                                isExtra = lectureItem.isExtra,
                                timetableId = lectureItem.timetableEntry?.id
                            )
                        }
                    )
                }
            }

            // --------------------------------------------------
            // 6. ATTENDANCE SIMULATOR SECTION
            // --------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(6.dp))
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
                                text = "ATTENDANCE SIMULATOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = colors.accent.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = "What-If",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Simulation only — does not modify real attendance records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            fontSize = 11.5.sp
                        )

                        // 4 simulation outcomes in clean 2x2 grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SimulatorOutcomePill(
                                label = "Current",
                                value = "${String.format(Locale.US, "%.2f", overallPct)}%",
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            SimulatorOutcomePill(
                                label = "If attend next",
                                value = "${String.format(Locale.US, "%.2f", ifAttend1)}%",
                                color = colors.success,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SimulatorOutcomePill(
                                label = "If attend next 3",
                                value = "${String.format(Locale.US, "%.2f", ifAttend3)}%",
                                color = colors.success,
                                modifier = Modifier.weight(1f)
                            )
                            SimulatorOutcomePill(
                                label = "If miss next",
                                value = "${String.format(Locale.US, "%.2f", ifMiss1)}%",
                                color = colors.danger,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedButton(
                            onClick = { showSimulatorDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Subject What-If Simulator", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Add Extra Lecture Dialog
        if (showAddExtraDialog) {
            AddExtraLectureDialog(
                subjects = activeSubjectsWithStats.map { it.first },
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
                    Toast.makeText(context, "Extra lecture recorded", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Per-Subject What-If Simulator Dialog
        if (showSimulatorDialog && activeSubjectsWithStats.isNotEmpty()) {
            AttendanceSimulatorDialog(
                subjects = activeSubjectsWithStats.map { it.first },
                statsList = activeSubjectsWithStats.map { it.second },
                initialSelectedIndex = simulatorSelectedSubjectIndex,
                onDismiss = { showSimulatorDialog = false }
            )
        }
    }
}

/**
 * Subject Attendance Card dedicated solely to attendance tracking.
 * Strictly adheres to Section 3 & 4:
 * - Subject name
 * - Current attendance & Target percentage
 * - Status message: "Need to attend X lectures" / "Can safely miss X lectures"
 * - [ View Details ] button
 * - Optional scheduled class time & direct attendance marking if present on date
 */
@Composable
fun SubjectAttendanceCard(
    subject: Subject,
    stats: SubjectStats,
    targetPercentage: Double,
    statusMessage: String,
    statusColor: Color,
    lecturesOnDate: List<LectureItem>,
    onViewDetails: () -> Unit,
    onMarkLectureStatus: (LectureItem, AttendanceStatus?) -> Unit
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
            // Top Row: Subject Name + Percentage Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Target ${targetPercentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", stats.percentage)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Status Message Row: "Need to attend X lectures" / "Can safely miss X lectures"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (stats.percentage < targetPercentage) colors.danger else colors.success,
                    fontSize = 13.5.sp
                )

                Text(
                    text = "${stats.attended}/${stats.attended + stats.missed} attended",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }

            // Scheduled class on this date (if any) with marking controls
            if (lecturesOnDate.isNotEmpty()) {
                Divider(color = colors.border.copy(alpha = 0.6f))

                lecturesOnDate.forEach { lecture ->
                    val currentStatus = lecture.attendanceRecord?.status

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.elevatedCard, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (lecture.startTime.isNotBlank()) "${lecture.startTime} – ${lecture.endTime}" else "Lecture scheduled",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            if (lecture.room.isNotBlank()) {
                                Text(
                                    text = lecture.room,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        // 4 semantic marking chips: Clear, Off, Miss, Att
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LectureStatusChip(
                                label = "Clear",
                                isSelected = (currentStatus == null),
                                activeColor = colors.textSecondary,
                                onClick = { onMarkLectureStatus(lecture, null) },
                                modifier = Modifier.weight(1f)
                            )
                            LectureStatusChip(
                                label = "Off",
                                isSelected = (currentStatus == AttendanceStatus.OFF),
                                activeColor = colors.warning,
                                onClick = { onMarkLectureStatus(lecture, AttendanceStatus.OFF) },
                                modifier = Modifier.weight(1f)
                            )
                            LectureStatusChip(
                                label = "Miss",
                                isSelected = (currentStatus == AttendanceStatus.ABSENT),
                                activeColor = colors.danger,
                                onClick = { onMarkLectureStatus(lecture, AttendanceStatus.ABSENT) },
                                modifier = Modifier.weight(1f)
                            )
                            LectureStatusChip(
                                label = "Att",
                                isSelected = (currentStatus == AttendanceStatus.PRESENT),
                                activeColor = colors.success,
                                onClick = { onMarkLectureStatus(lecture, AttendanceStatus.PRESENT) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Bottom Action: View Details Button
            OutlinedButton(
                onClick = onViewDetails,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                border = BorderStroke(1.dp, colors.border),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View Details", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun SimulatorOutcomePill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.elevatedCard,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, fontSize = 11.sp)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun LectureStatusChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) activeColor.copy(alpha = 0.15f) else colors.card,
        border = BorderStroke(1.dp, if (isSelected) activeColor else colors.border),
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) activeColor else colors.textSecondary,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
fun AddExtraLectureDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (subjectId: Int, startTime: String, endTime: String, room: String, faculty: String) -> Unit
) {
    val colors = LocalAppColors.current
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var room by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val currentSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.firstOrNull()

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Subject Dropdown
                Box {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            currentSubject?.name ?: "Select Course",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
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
                        label = { Text("Start") },
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
                        label = { Text("End") },
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

