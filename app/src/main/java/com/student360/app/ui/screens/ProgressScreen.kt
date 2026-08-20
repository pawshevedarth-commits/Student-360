package com.student360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.AttendanceRecord
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.CollegeDay
import com.student360.app.data.local.entity.DayStatus
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    repository: StudentRepository,
    viewModel: ProgressViewModel = viewModel()
) {
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Attendance Trend Summary Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Attendance Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(trendsText, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Heatmap Calendar Section
        item {
            Text("Activity Heatmap", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = {
                            configDate = System.currentTimeMillis()
                            showConfigDialog = true
                        }) {
                            Text("Label Day Status")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Heatmap grid
                    AttendanceHeatmapGrid(
                        records = records,
                        collegeDays = collegeDays,
                        onDayClick = { dayTime -> selectedDayDetail = dayTime }
                    )
                }
            }
        }

        // Heatmap Legend Section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("Full", SafeGreen)
                    LegendItem("Partial", WarningYellow)
                    LegendItem("Absent", CriticalRed)
                    LegendItem("Off/Holi", HolidayGrey)
                    LegendItem("Exam", ExamPurple)
                }
            }
        }

        // Subject Trends progress listings
        item {
            Text("Subject-wise Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (subjects.isEmpty()) {
            item {
                Text("No subjects available.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(subjects.size) { index ->
                val subject = subjects[index]
                val subRecords = records.filter { it.subjectId == subject.id }
                val att = subject.manualAttended + subRecords.count { it.status == AttendanceStatus.PRESENT }
                val cond = subject.manualConducted + subRecords.count { it.status == AttendanceStatus.PRESENT } + subRecords.count { it.status == AttendanceStatus.ABSENT }
                val pct = if (cond > 0) (att.toDouble() / cond.toDouble()) * 100.0 else 100.0

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(subject.name, fontWeight = FontWeight.Bold)
                            Text("${String.format("%.1f", pct)}%")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = (pct / 100.0).toFloat().coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxWidth(),
                            color = if (pct >= subject.targetPercentage) SafeGreen else CriticalRed,
                            trackColor = if (pct >= subject.targetPercentage) SafeGreenLight else CriticalRedLight
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "$att classes attended out of $cond conducted (Target: ${subject.targetPercentage.toInt()}%)",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    // Configure Day Status Dialog
    if (showConfigDialog) {
        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text("Configure Day Status") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a status label for today:")
                    DayStatus.values().forEach { status ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDayStatus = status }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = selectedDayStatus == status,
                                onClick = { selectedDayStatus = status }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(status.name.replace("_", " "))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setCollegeDayStatus(configDate, selectedDayStatus, null)
                    showConfigDialog = false
                }) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Cancel")
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
            title = { Text("Day Summary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dateStr, fontWeight = FontWeight.Bold)
                    Divider()
                    
                    if (dayStatus != null) {
                        Text("Day Status Label: ${dayStatus.name.replace("_", " ")}", color = MaterialTheme.colorScheme.primary)
                    }

                    Text("Total Lectures logged: ${dayRecords.size}")
                    Text("✅ Attended: $attended")
                    Text("❌ Missed: $missed")
                    Text("⚪ Cancelled/Off: $off")

                    if (dayRecords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Logs Detail:", fontWeight = FontWeight.Bold)
                        dayRecords.forEach { record ->
                            val subName = subjects.find { it.id == record.subjectId }?.name ?: "Subject"
                            Text("• $subName: ${record.status.name}")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedDayDetail = null }) {
                    Text("Close")
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
    // Mon to Sat headers
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
        Calendar.SUNDAY -> 6 // Sun offset, we skip sunday columns, but we need grid offset
        else -> 0
    }
    
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Week Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            daysOfWeek.forEach { day ->
                Text(
                    day,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Month Grid Layout
        var dayCounter = 1
        val gridRows = 6 // Up to 6 rows

        for (row in 0 until gridRows) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                // Mon-Sat (6 columns)
                for (col in 0 until 6) {
                    val gridIndex = row * 6 + col
                    if (gridIndex < firstDayOffset || dayCounter > maxDays) {
                        // Empty spacer cell
                        Spacer(modifier = Modifier.size(36.dp))
                    } else {
                        val currentDay = dayCounter
                        val dayCal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, currentDay)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val timeMillis = dayCal.timeInMillis

                        // Color coding calculation
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
                            dayRecords.isEmpty() -> HolidayGreyLight
                            dayRecords.all { it.status == AttendanceStatus.OFF } -> HolidayGrey
                            dayRecords.all { it.status == AttendanceStatus.PRESENT } -> SafeGreen
                            dayRecords.all { it.status == AttendanceStatus.ABSENT } -> CriticalRed
                            else -> WarningYellow
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(cellColor, RoundedCornerShape(4.dp))
                                .clickable { onDayClick(timeMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$currentDay",
                                color = if (cellColor == HolidayGreyLight) Color.Black else Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
