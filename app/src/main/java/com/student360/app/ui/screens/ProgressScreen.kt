@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.*
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProgressScreen(
    repository: StudentRepository,
    viewModel: ProgressViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val records by viewModel.attendanceRecords.collectAsState()
    val collegeDays by viewModel.collegeDays.collectAsState()
    val trendsText by viewModel.trendsText.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    val studySessions by repository.studySessionsFlow.collectAsState(initial = emptyList())
    val allAssignments by repository.assignmentsFlow.collectAsState(initial = emptyList())
    val allGoals by repository.goalsFlow.collectAsState(initial = emptyList())

    val oneWeekAgo = remember { System.currentTimeMillis() - (7 * 24 * 3600 * 1000L) }

    val weeklyStudyMins = remember(studySessions, oneWeekAgo) {
        studySessions.filter { it.dateCompleted >= oneWeekAgo }.sumOf { it.duration }
    }
    val weeklyStudyStr = remember(weeklyStudyMins) {
        val h = weeklyStudyMins / 60
        val m = weeklyStudyMins % 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    val weeklyRecords = remember(records, oneWeekAgo) {
        records.filter { it.date >= oneWeekAgo }
    }
    val weeklyAttended = remember(weeklyRecords) {
        weeklyRecords.count { it.status == AttendanceStatus.PRESENT }
    }
    val weeklyConducted = remember(weeklyRecords) {
        weeklyRecords.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.ABSENT }
    }
    val weeklyAttendancePct = remember(weeklyAttended, weeklyConducted) {
        if (weeklyConducted > 0) (weeklyAttended.toDouble() / weeklyConducted * 100.0) else 100.0
    }

    val weeklyCompletedAssigns = remember(allAssignments) {
        allAssignments.count { it.status == AssignmentStatus.COMPLETED }
    }

    val activeGoalsCount = remember(allGoals) {
        allGoals.count { it.status == GoalStatus.ACTIVE }
    }

    // Month Navigation State for the Heatmap
    var displayedCalendar by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    // Selected Day in Heatmap for inline detail panel (defaults to today normalized to midnight)
    val todayMidnight = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    var selectedDayMillis by remember { mutableStateOf(todayMidnight) }

    var showConfigDialog by remember { mutableStateOf(false) }
    var configDate by remember { mutableStateOf(todayMidnight) }
    var selectedDayStatus by remember { mutableStateOf(DayStatus.HOLIDAY) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Weekly Overview Card (Section 10 & 2)
        item {
            StudentCard(
                backgroundColor = colors.card,
                borderColor = colors.border,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Weekly Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 17.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.accent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.25f))
                        ) {
                            Text(
                                "Past 7 Days",
                                color = colors.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Grid of 5 clean metrics: Study Time, Classes, Assignments, Attendance, Goals
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(weeklyStudyStr, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.accent)
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${weeklyRecords.size}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("${weeklyCompletedAssigns}/${allAssignments.size}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                Text("Assignments", fontSize = 11.sp, color = colors.textSecondary)
                            }
                        }
                    }

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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val attColor = if (weeklyAttendancePct >= 75.0) colors.success else colors.danger
                                Text("${String.format(Locale.US, "%.1f", weeklyAttendancePct)}%", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = attColor)
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
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("$activeGoalsCount", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
                                Text("Active Goals", fontSize = 11.sp, color = colors.textSecondary)
                            }
                        }
                    }
                }
            }
        }

        // 2. Attendance Trend Summary Card (Section 3)
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Attendance Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        fontSize = 16.sp
                    )
                    // Trend Indicator icon/badge
                    val isIncreased = trendsText.contains("increased", ignoreCase = true)
                    val isDecreased = trendsText.contains("decreased", ignoreCase = true)
                    val badgeColor = when {
                        isIncreased -> SuccessGreen
                        isDecreased -> DangerRed
                        else -> LightPurple
                    }
                    val badgeIcon = when {
                        isIncreased -> "▲"
                        isDecreased -> "▼"
                        else -> "●"
                    }
                    Text(
                        text = badgeIcon,
                        color = badgeColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = trendsText.ifBlank { "Attendance increased by 0.0% this month compared to last month." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    lineHeight = 20.sp
                )
            }
        }

        // 3. Natural Heat-Map Calendar Card (Section 4–7, 10–12)
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
                val monthTitle = remember(displayedCalendar) { monthFormatter.format(displayedCalendar.time) }

                // Month Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            fontSize = 16.5.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                val prev = displayedCalendar.clone() as Calendar
                                prev.add(Calendar.MONTH, -1)
                                displayedCalendar = prev
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = SecondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val next = displayedCalendar.clone() as Calendar
                                next.add(Calendar.MONTH, 1)
                                displayedCalendar = next
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = SecondaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Label Day quick action button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LightPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, LightPurple.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                configDate = selectedDayMillis
                                showConfigDialog = true
                            }
                    ) {
                        Text(
                            text = "🏷 Label Day",
                            color = LightPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Heatmap Calendar Grid
                AttendanceHeatmapGrid(
                    displayedCalendar = displayedCalendar,
                    records = records,
                    collegeDays = collegeDays,
                    selectedDayMillis = selectedDayMillis,
                    onDayClick = { dayTime ->
                        selectedDayMillis = dayTime
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = BorderDark.copy(alpha = 0.7f), thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Legend (Section 11)
                HeatmapLegend()
            }
        }

        // 4. Selected Day Details Panel (Section 8 & 9)
        item {
            SelectedDayDetailCard(
                selectedDateMillis = selectedDayMillis,
                records = records,
                collegeDays = collegeDays,
                subjects = subjects,
                normalizeToMidnight = { viewModel.normalizeToMidnight(it) },
                onLabelDay = {
                    configDate = selectedDayMillis
                    showConfigDialog = true
                }
            )
        }

        // 5. Subject-wise Performance Section Header
        item {
            SectionHeader(title = "Subject-wise Performance")
        }

        if (subjects.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Subjects Found",
                    subtitle = "Add subjects in the Subjects tab to view detailed academic performance breakdown."
                )
            }
        } else {
            items(subjects, key = { it.id }) { subject ->
                val subRecords = records.filter { it.subjectId == subject.id }
                val att = subject.manualAttended + subRecords.count { it.status == AttendanceStatus.PRESENT }
                val cond = subject.manualConducted + subRecords.count { it.status == AttendanceStatus.PRESENT } + subRecords.count { it.status == AttendanceStatus.ABSENT }
                val pct = if (cond > 0) (att.toDouble() / cond.toDouble()) * 100.0 else 100.0
                val isSafe = pct >= subject.targetPercentage

                StudentCard(
                    backgroundColor = CardDark,
                    borderColor = BorderDark
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            subject.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "${String.format(Locale.US, "%.1f", pct)}%",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSafe) SuccessGreen else DangerRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    StudentProgressBar(
                        progress = (pct / 100.0).toFloat().coerceIn(0f, 1f),
                        color = if (isSafe) SuccessGreen else DangerRed,
                        trackColor = SurfaceDark,
                        height = 6.dp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$att attended / $cond conducted (Target: ${subject.targetPercentage.toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                }
            }
        }
    }

    // Configure Day Status Dialog (for custom holiday/exam labels)
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            containerColor = SurfaceDark,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = {
                val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(configDate))
                Text(
                    "Configure Status for $dateLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Select a day status:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    DayStatus.values().forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedDayStatus = status }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedDayStatus == status,
                                onClick = { selectedDayStatus = status },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryPurple,
                                    unselectedColor = SecondaryText
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                status.name.replace("_", " "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryText
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCollegeDayStatus(configDate, selectedDayStatus, null)
                        showConfigDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }
}

@Composable
fun AttendanceHeatmapGrid(
    displayedCalendar: Calendar,
    records: List<AttendanceRecord>,
    collegeDays: List<CollegeDay>,
    selectedDayMillis: Long,
    onDayClick: (Long) -> Unit
) {
    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")

    val cal = (displayedCalendar.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val firstDayOffset = when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalCells = firstDayOffset + maxDays
    val numRows = (totalCells + 6) / 7

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Weekday header row: M T W T F S S
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            daysOfWeek.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryText,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Month Days Grid
        var dayCounter = 1
        for (row in 0 until numRows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (col in 0 until 7) {
                    val gridIndex = row * 7 + col
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (gridIndex >= firstDayOffset && dayCounter <= maxDays) {
                            val currentDay = dayCounter
                            val dayCal = (displayedCalendar.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, currentDay)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val timeMillis = dayCal.timeInMillis
                            val isSelected = (selectedDayMillis == timeMillis)

                            val dayStatus = collegeDays.find { it.date == timeMillis }?.status
                            val dayRecords = records.filter {
                                val rCal = Calendar.getInstance().apply { timeInMillis = it.date }
                                rCal.set(Calendar.HOUR_OF_DAY, 0)
                                rCal.set(Calendar.MINUTE, 0)
                                rCal.set(Calendar.SECOND, 0)
                                rCal.set(Calendar.MILLISECOND, 0)
                                rCal.timeInMillis == timeMillis
                            }

                            // Attendance intensity & status color mapping
                            val attendedCount = dayRecords.count { it.status == AttendanceStatus.PRESENT }
                            val missedCount = dayRecords.count { it.status == AttendanceStatus.ABSENT }
                            val conductedCount = attendedCount + missedCount

                            val cellColor = when {
                                dayStatus == DayStatus.HOLIDAY || dayStatus == DayStatus.NO_CLASSES -> HolidayGrey
                                dayStatus == DayStatus.EXAM || dayStatus == DayStatus.STUDY_LEAVE -> ExamPurple
                                dayRecords.isEmpty() -> Color(0xFF1E2033)
                                dayRecords.all { it.status == AttendanceStatus.OFF } -> HolidayGrey
                                conductedCount == 0 -> Color(0xFF1E2033)
                                missedCount == conductedCount -> DangerRed
                                attendedCount == conductedCount -> SuccessGreen // 100% attendance
                                else -> {
                                    val pct = (attendedCount.toDouble() / conductedCount.toDouble()) * 100.0
                                    when {
                                        pct >= 75.0 -> Color(0xFF40B87A) // High strong green
                                        pct >= 50.0 -> Color(0xFF2E8B57) // Medium green
                                        pct > 0.0 -> WarningOrange       // Partial attendance amber
                                        else -> DangerRed
                                    }
                                }
                            }

                            val isNeutral = (cellColor == Color(0xFF1E2033))
                            val cellBorder = when {
                                isSelected -> BorderStroke(2.dp, Color.White)
                                isNeutral -> BorderStroke(1.dp, BorderDark.copy(alpha = 0.5f))
                                else -> BorderStroke(1.dp, cellColor.copy(alpha = 0.3f))
                            }

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(cellColor)
                                    .border(cellBorder, RoundedCornerShape(7.dp))
                                    .clickable { onDayClick(timeMillis) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$currentDay",
                                    color = if (isNeutral) SecondaryText else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            dayCounter++
                        } else {
                            Spacer(modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeatmapLegend() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Intensity scale: Less attendance ▫ ▫ ▫ ▫ ▫ More attendance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Less attendance",
                fontSize = 11.sp,
                color = SecondaryText,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(8.dp))
            listOf(
                Color(0xFF1E2033),
                Color(0xFF1E4D38),
                Color(0xFF2E8B57),
                Color(0xFF40B87A),
                SuccessGreen
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(11.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                        .then(
                            if (color == Color(0xFF1E2033)) Modifier.border(0.5.dp, BorderDark, RoundedCornerShape(3.dp))
                            else Modifier
                        )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "More attendance",
                fontSize = 11.sp,
                color = SecondaryText,
                fontWeight = FontWeight.Medium
            )
        }

        // Status indicator dots: Full, Partial, Absent, Holiday, Exam
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("Full", SuccessGreen)
            LegendItem("Partial", WarningOrange)
            LegendItem("Absent", DangerRed)
            LegendItem("Holiday", HolidayGrey)
            LegendItem("Exam", ExamPurple)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = SecondaryText,
            fontSize = 11.sp
        )
    }
}

@Composable
fun SelectedDayDetailCard(
    selectedDateMillis: Long,
    records: List<AttendanceRecord>,
    collegeDays: List<CollegeDay>,
    subjects: List<Subject>,
    normalizeToMidnight: (Long) -> Long,
    onLabelDay: () -> Unit
) {
    val normalizedDate = remember(selectedDateMillis) { normalizeToMidnight(selectedDateMillis) }
    val dateFormatter = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val dateStr = remember(selectedDateMillis) { dateFormatter.format(Date(selectedDateMillis)) }

    val dayStatus = remember(collegeDays, normalizedDate) {
        collegeDays.find { it.date == normalizedDate }?.status
    }
    val dayRecords = remember(records, normalizedDate) {
        records.filter { normalizeToMidnight(it.date) == normalizedDate }
    }

    val attended = remember(dayRecords) { dayRecords.count { it.status == AttendanceStatus.PRESENT } }
    val missed = remember(dayRecords) { dayRecords.count { it.status == AttendanceStatus.ABSENT } }
    val off = remember(dayRecords) { dayRecords.count { it.status == AttendanceStatus.OFF } }
    val conducted = attended + missed

    val attendancePct = if (conducted > 0) (attended.toDouble() / conducted.toDouble()) * 100.0 else 100.0

    val statusBadgeTitle: String
    val statusBadgeColor: Color
    when {
        dayStatus == DayStatus.HOLIDAY -> {
            statusBadgeTitle = "Holiday"
            statusBadgeColor = HolidayGrey
        }
        dayStatus == DayStatus.NO_CLASSES -> {
            statusBadgeTitle = "No Classes"
            statusBadgeColor = HolidayGrey
        }
        dayStatus == DayStatus.EXAM -> {
            statusBadgeTitle = "Exam Day"
            statusBadgeColor = ExamPurple
        }
        dayStatus == DayStatus.STUDY_LEAVE -> {
            statusBadgeTitle = "Study Leave"
            statusBadgeColor = ExamPurple
        }
        dayRecords.isEmpty() -> {
            statusBadgeTitle = "No Logs"
            statusBadgeColor = SecondaryText
        }
        conducted == 0 && off > 0 -> {
            statusBadgeTitle = "Classes Off"
            statusBadgeColor = WarningOrange
        }
        attended == conducted && conducted > 0 -> {
            statusBadgeTitle = "Full Attendance"
            statusBadgeColor = SuccessGreen
        }
        missed == conducted && conducted > 0 -> {
            statusBadgeTitle = "Missed All"
            statusBadgeColor = DangerRed
        }
        attended > 0 && missed > 0 -> {
            statusBadgeTitle = "Partial Attendance"
            statusBadgeColor = WarningOrange
        }
        else -> {
            statusBadgeTitle = "Recorded"
            statusBadgeColor = SuccessGreen
        }
    }

    StudentCard(
        backgroundColor = CardDark,
        borderColor = BorderDark,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row: Date & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBadgeColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = statusBadgeTitle,
                        color = statusBadgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Metrics Summary Row: e.g. "5 / 5 Classes Attended" • "100% Attendance"
            if (dayRecords.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedCardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$attended / ${dayRecords.size} Classes",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (conducted > 0) "${String.format(Locale.US, "%.0f", attendancePct)}% Attendance" else "All Off",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (attendancePct >= 75.0) SuccessGreen else DangerRed,
                            fontSize = 13.sp
                        )
                    }
                }

                // Listing of individual lecture logs
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayRecords.forEach { record ->
                        val subName = subjects.find { it.id == record.subjectId }?.name ?: "Subject"
                        val (statusText, statColor) = when (record.status) {
                            AttendanceStatus.PRESENT -> "Attended" to SuccessGreen
                            AttendanceStatus.ABSENT -> "Missed" to DangerRed
                            AttendanceStatus.OFF -> "Off / Cancelled" to WarningOrange
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SurfaceDark.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = subName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = statColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = if (dayStatus != null) "Marked as ${dayStatus.name.replace("_", " ")} on college calendar."
                    else "No attendance classes were logged on this date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
