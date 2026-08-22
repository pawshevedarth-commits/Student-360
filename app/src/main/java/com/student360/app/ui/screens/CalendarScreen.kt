@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

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
    val currentMonth by viewModel.currentMonth.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val summary by viewModel.calendarSummary.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val todayLectures by viewModel.todayLectures.collectAsState()

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = 75

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val cal = remember(currentMonth) {
        (currentMonth.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val startOffset = firstDayOfWeek - 1

    val selectedDateFormatted = remember(selectedDate) {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header: Student360 (Left) and 57.41 | 75 (Right)
            item {
                StudentScreenHeader(
                    title = "Calendar",
                    overallPercentage = overallPct,
                    targetPercentage = targetPct
                )
            }

            // Main Rounded Calendar Card
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Month Selector: ‹ August 2026 ›
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

                            Text(
                                text = monthFormatter.format(currentMonth.time),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 16.sp
                            )

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
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            daysOfWeek.forEach { day ->
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(38.dp),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Days Grid Matrix
                        val totalSlots = startOffset + daysInMonth
                        val rows = (totalSlots + 6) / 7

                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (col in 0..6) {
                                    val dayNum = (row * 7 + col) - startOffset + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val dayCal = (cal.clone() as Calendar).apply {
                                            set(Calendar.DAY_OF_MONTH, dayNum)
                                        }
                                        val dayTime = dayCal.timeInMillis
                                        val isSelected = dayTime == selectedDate
                                        val dayStats = heatmapData[dayTime]

                                        val dotColor = when (dayStats?.status) {
                                            DayAttendanceState.ATTENDED -> colors.success
                                            DayAttendanceState.MISSED -> colors.danger
                                            DayAttendanceState.MIXED -> colors.accent
                                            DayAttendanceState.OFF -> colors.warning
                                            else -> null
                                        }

                                        Column(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
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
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) (if (colors.isDark) Color.White else colors.accent) else colors.textPrimary,
                                                fontSize = 13.sp
                                            )
                                            if (dotColor != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .background(
                                                            if (isSelected) (if (colors.isDark) Color(0xFF1F1B2E) else Color.White) else dotColor,
                                                            CircleShape
                                                        )
                                                )
                                            } else {
                                                Spacer(modifier = Modifier.height(5.dp))
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(40.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Breakdown Pills Row: Not marked: 6, Off: 9, Missed: 1, Attended: 11, Mixed: 4
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPillBadge(label = "Not marked: ${summary.notMarkedDays}", dotColor = colors.textSecondary)
                    StatusPillBadge(label = "Off: ${summary.offDays}", dotColor = colors.warning)
                    StatusPillBadge(label = "Missed: ${summary.missedDays}", dotColor = colors.danger)
                    StatusPillBadge(label = "Attended: ${summary.attendedDays}", dotColor = colors.success)
                    StatusPillBadge(label = "Mixed: ${summary.mixedDays}", dotColor = colors.accent)
                }
            }

            // Summary Stats Bar Card (Off: 10 | Missed: 9 | Attended: 31 | Total: 40 | Percent: 77.50%)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.elevatedCard,
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatColumnItem(
                            label = "Off",
                            value = "${summary.totalOff}",
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
                            label = "Attended",
                            value = "${summary.totalAttended}",
                            modifier = Modifier.weight(1f),
                            showDivider = true
                        )
                        StatColumnItem(
                            label = "Total",
                            value = "${summary.totalAttended + summary.totalMissed}",
                            modifier = Modifier.weight(1f),
                            showDivider = true
                        )
                        StatColumnItem(
                            label = "Percent",
                            value = "${String.format(Locale.US, "%.2f", summary.overallPercentage)}%",
                            modifier = Modifier.weight(1.2f),
                            showDivider = false
                        )
                    }
                }
            }

            // Selected Day Classes
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Classes on $selectedDateFormatted",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

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

            if (todayLectures.isEmpty()) {
                item {
                    StudentCard(backgroundColor = colors.card, borderColor = colors.border) {
                        Text(
                            text = "No classes recorded for $selectedDateFormatted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
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
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.subject.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                if (item.startTime.isNotBlank()) {
                                    Text(
                                        text = "⏰ ${item.startTime} – ${item.endTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            // 4 Interactive Status Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                QuickAttendanceRoundButton(
                                    symbol = "⊘",
                                    isSelected = currentStatus == null,
                                    activeColor = colors.textSecondary,
                                    onClick = {
                                        viewModel.markLectureAttendance(item.subject.id, selectedDate, null)
                                    }
                                )
                                QuickAttendanceRoundButton(
                                    symbol = "—",
                                    isSelected = currentStatus == AttendanceStatus.OFF,
                                    activeColor = colors.warning,
                                    onClick = {
                                        viewModel.markLectureAttendance(item.subject.id, selectedDate, AttendanceStatus.OFF)
                                    }
                                )
                                QuickAttendanceRoundButton(
                                    symbol = "✕",
                                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                                    activeColor = colors.danger,
                                    onClick = {
                                        viewModel.markLectureAttendance(item.subject.id, selectedDate, AttendanceStatus.ABSENT)
                                    }
                                )
                                QuickAttendanceRoundButton(
                                    symbol = "✓",
                                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                                    activeColor = colors.success,
                                    onClick = {
                                        viewModel.markLectureAttendance(item.subject.id, selectedDate, AttendanceStatus.PRESENT)
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
