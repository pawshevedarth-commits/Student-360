@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

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
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.QuickAttendanceRoundButton
import com.student360.app.ui.components.StudentCard
import com.student360.app.ui.components.StudentScreenHeader
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    onNavigateToToday: (Long) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val summary by viewModel.calendarSummary.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todayLectures by viewModel.todayLectures.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val dateHeaderFormatter = remember { SimpleDateFormat("d MMMM", Locale.getDefault()) }

    val todayTime = remember { getStartOfDay(System.currentTimeMillis()) }
    val selectedDateFormatted = remember(selectedDate) { dayFormatter.format(Date(selectedDate)) }
    val selectedDateHeader = remember(selectedDate) {
        val prefix = if (selectedDate == todayTime) "Today • " else ""
        prefix + dateHeaderFormatter.format(Date(selectedDate))
    }

    val isSelectedWeekend = remember(selectedDate) {
        val c = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val d = c.get(Calendar.DAY_OF_WEEK)
        d == Calendar.SATURDAY || d == Calendar.SUNDAY
    }

    val todayCal = remember { Calendar.getInstance() }
    val isCurrentMonth = (currentMonth.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)) &&
            (currentMonth.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH))
    val isTodaySelected = selectedDate == todayTime

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val cal = currentMonth.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val startOffset = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val targetPct = 75

    // Daily statistics calculation
    val attendedClasses = todayLectures.count { it.attendanceRecord?.status == AttendanceStatus.PRESENT }
    val missedClasses = todayLectures.count { it.attendanceRecord?.status == AttendanceStatus.ABSENT }
    val offClasses = todayLectures.count { it.attendanceRecord?.status == AttendanceStatus.OFF }
    val conductedClasses = attendedClasses + missedClasses
    val dayPercentage = if (conductedClasses > 0) (attendedClasses.toDouble() / conductedClasses.toDouble()) * 100.0 else 100.0

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
            // Attendance Summary Badge Pill Row
            item {
                StudentScreenHeader(
                    title = "",
                    overallPercentage = summary.overallPercentage,
                    targetPercentage = targetPct
                )
            }

            // Calendar Card
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
                        // Month Selector Header: < August 2026 >
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.previousMonth() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Previous month",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = monthFormatter.format(currentMonth.time),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontSize = 16.sp
                                )

                                // Quick "Today" Jump Action
                                if (!isCurrentMonth || !isTodaySelected) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colors.activePill,
                                        modifier = Modifier.clickable { viewModel.selectToday() }
                                    ) {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (colors.isDark) Color.White else colors.accent,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { viewModel.nextMonth() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next month",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Weekday Headers: Sun Mon Tue Wed Thu Fri Sat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Days Grid Matrix (7 columns)
                        val totalSlots = startOffset + daysInMonth
                        val rows = (totalSlots + 6) / 7

                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (col in 0..6) {
                                    val dayNum = (row * 7 + col) - startOffset + 1
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (dayNum in 1..daysInMonth) {
                                            val dayCal = (cal.clone() as Calendar).apply {
                                                set(Calendar.DAY_OF_MONTH, dayNum)
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            val dayTime = dayCal.timeInMillis
                                            val isSelected = dayTime == selectedDate
                                            val isToday = dayTime == todayTime
                                            val dayStats = heatmapData[dayTime]

                                            val dotColor = when (dayStats?.status) {
                                                DayAttendanceState.ATTENDED -> colors.success
                                                DayAttendanceState.MISSED -> colors.danger
                                                DayAttendanceState.MIXED -> colors.accent
                                                DayAttendanceState.OFF -> colors.warning
                                                DayAttendanceState.NOT_MARKED -> colors.textSecondary.copy(alpha = 0.5f)
                                                null -> null
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .then(
                                                        if (isToday && !isSelected) {
                                                            Modifier.border(BorderStroke(1.5.dp, colors.accent), CircleShape)
                                                        } else Modifier
                                                    )
                                                    .background(
                                                        if (isSelected) colors.activePill else Color.Transparent
                                                    )
                                                    .clickable { viewModel.selectDate(dayTime) },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) (if (colors.isDark) Color.White else colors.accent) else colors.textPrimary,
                                                    fontSize = 12.sp
                                                )
                                                if (dotColor != null) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.5.dp)
                                                            .background(
                                                                if (isSelected) (if (colors.isDark) Color.White else colors.accent) else dotColor,
                                                                CircleShape
                                                            )
                                                    )
                                                } else {
                                                    Spacer(modifier = Modifier.height(4.5.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Legend / Status Breakdown Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPillBadge(label = "Attended: ${summary.attendedDays}", dotColor = colors.success)
                    StatusPillBadge(label = "Missed: ${summary.missedDays}", dotColor = colors.danger)
                    StatusPillBadge(label = "Mixed: ${summary.mixedDays}", dotColor = colors.accent)
                    StatusPillBadge(label = "Off: ${summary.offDays}", dotColor = colors.warning)
                    StatusPillBadge(label = "Not marked: ${summary.notMarkedDays}", dotColor = colors.textSecondary.copy(alpha = 0.5f))
                }
            }

            // Monthly Attendance Summary Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MONTH ATTENDANCE",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (summary.overallPercentage >= 75.0) colors.success.copy(alpha = 0.15f) else colors.danger.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.2f", summary.overallPercentage)}% Overall",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.overallPercentage >= 75.0) colors.success else colors.danger,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatColumnItem(
                                label = "Attended",
                                value = "${summary.totalAttended}",
                                modifier = Modifier.weight(1f),
                                showDivider = true
                            )
                            StatColumnItem(
                                label = "Missed",
                                value = "${summary.totalMissed}",
                                modifier = Modifier.weight(1f),
                                showDivider = true
                            )
                            StatColumnItem(
                                label = "Off",
                                value = "${summary.totalOff}",
                                modifier = Modifier.weight(1f),
                                showDivider = true
                            )
                            StatColumnItem(
                                label = "Total",
                                value = "${summary.totalAttended + summary.totalMissed}",
                                modifier = Modifier.weight(1f),
                                showDivider = false
                            )
                        }
                    }
                }
            }

            // Selected Date Header & Daily Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = selectedDateHeader,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 16.sp
                        )
                        Text(
                            text = when {
                                isSelectedWeekend && todayLectures.isEmpty() -> "Day Off • No classes scheduled"
                                todayLectures.isEmpty() -> "No classes scheduled"
                                offClasses > 0 -> "${todayLectures.size} Classes • $attendedClasses Attended • $missedClasses Missed • $offClasses Off • ${String.format(Locale.US, "%.1f", dayPercentage)}%"
                                else -> "${todayLectures.size} Classes • $attendedClasses Attended • $missedClasses Missed • ${String.format(Locale.US, "%.1f", dayPercentage)}%"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    TextButton(onClick = { onNavigateToToday(selectedDate) }) {
                        Text(
                            text = "Open Day ➔",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Selected Date Lectures / Day Off Surface
            if (todayLectures.isEmpty()) {
                item {
                    StudentCard(
                        backgroundColor = colors.card,
                        borderColor = colors.border,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSelectedWeekend) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.warning.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "DAY OFF",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.warning,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "No classes scheduled. Enjoy your weekend!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "No classes scheduled on $selectedDateFormatted.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(todayLectures) { item ->
                    val currentStatus = item.attendanceRecord?.status
                    StudentCard(
                        backgroundColor = colors.card,
                        borderColor = colors.border,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                    text = item.subject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.startTime.isNotBlank()) {
                                    Text(
                                        text = "⏰ ${item.startTime} – ${item.endTime}" + if (item.room.isNotBlank()) " • Room ${item.room}" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (item.faculty.isNotBlank()) {
                                    Text(
                                        text = "👤 ${item.faculty}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            // 4 Interactive Status Buttons (Clear, Off, Missed, Attended)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                QuickAttendanceRoundButton(
                                    symbol = "⊘",
                                    isSelected = currentStatus == null,
                                    activeColor = colors.textSecondary,
                                    onClick = {
                                        viewModel.markLectureAttendance(
                                            subjectId = item.subject.id,
                                            date = selectedDate,
                                            status = null,
                                            timetableId = item.timetableEntry?.id
                                        )
                                    }
                                )
                                QuickAttendanceRoundButton(
                                    symbol = "—",
                                    isSelected = currentStatus == AttendanceStatus.OFF,
                                    activeColor = colors.warning,
                                    onClick = {
                                        viewModel.markLectureAttendance(
                                            subjectId = item.subject.id,
                                            date = selectedDate,
                                            status = AttendanceStatus.OFF,
                                            timetableId = item.timetableEntry?.id
                                        )
                                    }
                                )
                                QuickAttendanceRoundButton(
                                    symbol = "✕",
                                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                                    activeColor = colors.danger,
                                    onClick = {
                                        viewModel.markLectureAttendance(
                                            subjectId = item.subject.id,
                                            date = selectedDate,
                                            status = AttendanceStatus.ABSENT,
                                            timetableId = item.timetableEntry?.id
                                        )
                                    }
                                )
                                QuickAttendanceRoundButton(
                                    symbol = "✓",
                                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                                    activeColor = colors.success,
                                    onClick = {
                                        viewModel.markLectureAttendance(
                                            subjectId = item.subject.id,
                                            date = selectedDate,
                                            status = AttendanceStatus.PRESENT,
                                            timetableId = item.timetableEntry?.id
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPillBadge(
    label: String,
    dotColor: Color
) {
    val colors = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun StatColumnItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 14.sp
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(26.dp)
                    .background(colors.border)
            )
        }
    }
}
