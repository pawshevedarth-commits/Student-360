@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.AttendanceRecord
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.CollegeDay
import com.student360.app.data.local.entity.DayStatus
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

    var showConfigDialog by remember { mutableStateOf(false) }
    var configDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedDayStatus by remember { mutableStateOf(DayStatus.HOLIDAY) }

    var selectedDayDetail by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Attendance Trend Summary Card
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Text(
                    "Attendance Trends",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    trendsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText
                )
            }
        }

        // Heatmap Calendar Section
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
                        text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    TextButton(
                        onClick = {
                            configDate = System.currentTimeMillis()
                            showConfigDialog = true
                        }
                    ) {
                        Text("🏷 Label Day", color = LightPurple, style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Heatmap grid
                AttendanceHeatmapGrid(
                    records = records,
                    collegeDays = collegeDays,
                    onDayClick = { dayTime -> selectedDayDetail = dayTime }
                )
            }
        }

        // Heatmap Legend Section
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
                    LegendItem("Full", SuccessGreen)
                    LegendItem("Partial", WarningOrange)
                    LegendItem("Absent", DangerRed)
                    LegendItem("Off/Holi", HolidayGrey)
                    LegendItem("Exam", ExamPurple)
                }
            }
        }

        // Subject Trends progress listings
        item {
            SectionHeader(title = "Subject-wise Performance")
        }

        if (subjects.isEmpty()) {
            item {
                EmptyStateView(
                    title = "No Subjects Found",
                    subtitle = "Add subjects to view detailed academic performance breakdown."
                )
            }
        } else {
            items(subjects.size) { index ->
                val subject = subjects[index]
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
                            "${String.format("%.1f", pct)}%",
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

    // Configure Day Status Dialog
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            containerColor = SurfaceDark,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = {
                Text(
                    "Configure Day Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Select a status label for today:",
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
                                .padding(vertical = 6.dp, horizontal = 4.dp)
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

    // Day detail Dialog
    selectedDayDetail?.let { dateMillis ->
        val dateStr = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(dateMillis))
        val normalizedDate = viewModel.normalizeToMidnight(dateMillis)

        val dayStatus = collegeDays.find { it.date == normalizedDate }?.status
        val dayRecords = records.filter {
            viewModel.normalizeToMidnight(it.date) == normalizedDate
        }

        val attended = dayRecords.count { it.status == AttendanceStatus.PRESENT }
        val missed = dayRecords.count { it.status == AttendanceStatus.ABSENT }
        val off = dayRecords.count { it.status == AttendanceStatus.OFF }

        AlertDialog(
            onDismissRequest = { selectedDayDetail = null },
            containerColor = SurfaceDark,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = {
                Text(
                    "Day Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dateStr, fontWeight = FontWeight.Bold, color = PrimaryText)
                    Divider(color = BorderDark)

                    if (dayStatus != null) {
                        Text(
                            "Status: ${dayStatus.name.replace("_", " ")}",
                            color = LightPurple,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Text("Total Lectures logged: ${dayRecords.size}", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                    Text("✅ Attended: $attended", color = SuccessGreen, style = MaterialTheme.typography.bodySmall)
                    Text("❌ Missed: $missed", color = DangerRed, style = MaterialTheme.typography.bodySmall)
                    Text("⚪ Canceled/Off: $off", color = SecondaryText, style = MaterialTheme.typography.bodySmall)

                    if (dayRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Logs Detail:", fontWeight = FontWeight.Bold, color = PrimaryText, style = MaterialTheme.typography.bodySmall)
                        dayRecords.forEach { record ->
                            val subName = subjects.find { it.id == record.subjectId }?.name ?: "Subject"
                            Text("• $subName: ${record.status.name}", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedDayDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AttendanceHeatmapGrid(
    records: List<AttendanceRecord>,
    collegeDays: List<CollegeDay>,
    onDayClick: (Long) -> Unit
) {
    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S")

    val calendar = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val firstDayOffset = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Week Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            daysOfWeek.forEach { day ->
                Text(
                    day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = SecondaryText
                )
            }
        }

        // Month Grid Layout
        var dayCounter = 1
        val gridRows = 6

        for (row in 0 until gridRows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0 until 6) {
                    val gridIndex = row * 6 + col
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (gridIndex >= firstDayOffset && dayCounter <= maxDays) {
                            val currentDay = dayCounter
                            val dayCal = Calendar.getInstance().apply {
                                set(Calendar.DAY_OF_MONTH, currentDay)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val timeMillis = dayCal.timeInMillis

                            val dayStatus = collegeDays.find { it.date == timeMillis }?.status
                            val dayRecords = records.filter {
                                val rCal = Calendar.getInstance().apply { timeInMillis = it.date }
                                rCal.set(Calendar.HOUR_OF_DAY, 0)
                                rCal.set(Calendar.MINUTE, 0)
                                rCal.set(Calendar.SECOND, 0)
                                rCal.set(Calendar.MILLISECOND, 0)
                                rCal.timeInMillis == timeMillis
                            }

                            val cellColor = when {
                                dayStatus == DayStatus.HOLIDAY || dayStatus == DayStatus.NO_CLASSES -> HolidayGrey
                                dayStatus == DayStatus.EXAM || dayStatus == DayStatus.STUDY_LEAVE -> ExamPurple
                                dayRecords.isEmpty() -> SurfaceDark
                                dayRecords.all { it.status == AttendanceStatus.OFF } -> HolidayGrey
                                dayRecords.all { it.status == AttendanceStatus.PRESENT } -> SuccessGreen
                                dayRecords.all { it.status == AttendanceStatus.ABSENT } -> DangerRed
                                else -> WarningOrange
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(cellColor, RoundedCornerShape(6.dp))
                                    .clickable { onDayClick(timeMillis) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$currentDay",
                                    color = if (cellColor == SurfaceDark) SecondaryText else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SecondaryText)
    }
}
