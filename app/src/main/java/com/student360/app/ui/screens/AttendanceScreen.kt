@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.AttendanceRecord
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.StudentRepository
import com.student360.app.data.repository.SubjectStats
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor

enum class AttendanceSubTab {
    TODAY,
    TIMETABLE,
    CALENDAR,
    SUBJECTS
}

@Composable
fun AttendanceScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel = viewModel(),
    scheduleViewModel: ScheduleViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()

    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedDateRecords by viewModel.selectedDateRecords.collectAsState()
    val selectedDateSchedule by viewModel.selectedDateSchedule.collectAsState()

    val currentMonth by viewModel.currentMonth.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val calendarSummary by viewModel.calendarSummary.collectAsState()

    val profile by scheduleViewModel.profile.collectAsState()

    var activeTab by remember { mutableStateOf(AttendanceSubTab.TODAY) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showSimulator by remember { mutableStateOf(false) }
    var overrideSubject by remember { mutableStateOf<Subject?>(null) }
    var selectedSubjectIndexForSim by remember { mutableStateOf(0) }

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = 75

    val overallStatusColor = when {
        overallPct >= 75.0 -> SuccessGreen
        overallPct >= 70.0 -> WarningOrange
        else -> DangerRed
    }

    val overallAttended = overallStats?.totalAttended ?: 0
    val overallConducted = overallStats?.totalConducted ?: 0
    val overallOff = calendarSummary.totalOff
    val overallMissed = (overallConducted - overallAttended).coerceAtLeast(0)

    val overallRecommendation = remember(overallAttended, overallConducted) {
        viewModel.calculateRecommendation(overallAttended, overallConducted, 75.0)
    }

    val dateFormatter = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }

    Scaffold(
        containerColor = BgDark,
        floatingActionButton = {
            if (activeTab == AttendanceSubTab.SUBJECTS) {
                FloatingActionButton(
                    onClick = { showAddSubjectDialog = true },
                    containerColor = PrimaryPurple,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Subject")
                }
            }
        },
        bottomBar = {
            // Persistent Bottom Bar for Today, Timetable, Calendar, Subjects, Settings
            Surface(
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AttendanceBottomItem(
                        icon = Icons.Default.Check,
                        label = "Today",
                        isSelected = activeTab == AttendanceSubTab.TODAY,
                        onClick = { activeTab = AttendanceSubTab.TODAY }
                    )
                    AttendanceBottomItem(
                        icon = Icons.Default.DateRange,
                        label = "Timetable",
                        isSelected = activeTab == AttendanceSubTab.TIMETABLE,
                        onClick = { activeTab = AttendanceSubTab.TIMETABLE }
                    )
                    AttendanceBottomItem(
                        icon = Icons.Default.DateRange,
                        label = "Calendar",
                        isSelected = activeTab == AttendanceSubTab.CALENDAR,
                        onClick = { activeTab = AttendanceSubTab.CALENDAR }
                    )
                    AttendanceBottomItem(
                        icon = Icons.Default.List,
                        label = "Subjects",
                        isSelected = activeTab == AttendanceSubTab.SUBJECTS,
                        onClick = { activeTab = AttendanceSubTab.SUBJECTS }
                    )
                    AttendanceBottomItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        isSelected = false,
                        onClick = onNavigateToSettings
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BgDark)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Persistent Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (activeTab == AttendanceSubTab.TODAY) {
                        Text(
                            text = dateFormatter.format(Date(selectedDate)),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    } else {
                        Text(
                            text = profile?.name?.ifBlank { "Student360" } ?: "Student360",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CardDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Text(
                                text = "${String.format("%.2f", overallPct)} | $targetPct",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = overallStatusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = { showAddSubjectDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add",
                                tint = PrimaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showSimulator = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Simulator",
                                tint = BrandCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // TAB CONTENT SWITCHER
                when (activeTab) {
                    AttendanceSubTab.TODAY -> {
                        TodayAttendanceView(
                            selectedDate = selectedDate,
                            records = selectedDateRecords,
                            schedule = selectedDateSchedule,
                            subjectsWithStats = subjectsWithStats,
                            onPreviousDay = { viewModel.previousDay() },
                            onNextDay = { viewModel.nextDay() },
                            onPickDate = {
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val picked = Calendar.getInstance().apply {
                                            set(year, month, day, 0, 0, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        viewModel.selectDate(picked)
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            onMarkLecture = { subId, status ->
                                viewModel.markLectureAttendance(subId, selectedDate, status)
                            },
                            onMarkAllDay = { status -> viewModel.markAllDay(selectedDate, status) },
                            onClearDay = { viewModel.clearDay(selectedDate) },
                            calculateRecommendation = { att, cond, tgt ->
                                viewModel.calculateRecommendation(att, cond, tgt)
                            }
                        )
                    }

                    AttendanceSubTab.TIMETABLE -> {
                        ScheduleScreen(repository = repository, viewModel = scheduleViewModel)
                    }

                    AttendanceSubTab.CALENDAR -> {
                        CalendarHeatmapView(
                            currentMonth = currentMonth,
                            heatmapData = heatmapData,
                            summary = calendarSummary,
                            selectedDate = selectedDate,
                            onSelectDate = { viewModel.selectDate(it) },
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() }
                        )
                    }

                    AttendanceSubTab.SUBJECTS -> {
                        SubjectsOverviewView(
                            overallPct = overallPct,
                            targetPct = targetPct,
                            overallAttended = overallAttended,
                            overallMissed = overallMissed,
                            overallOff = overallOff,
                            overallConducted = overallConducted,
                            overallRecommendation = overallRecommendation,
                            overallStatusColor = overallStatusColor,
                            subjectsWithStats = subjectsWithStats,
                            onSubjectClick = { sub -> overrideSubject = sub },
                            calculateRecommendation = { att, cond, tgt ->
                                viewModel.calculateRecommendation(att, cond, tgt)
                            }
                        )
                    }
                }
            }

            // Attendance Simulator Dialog
            if (showSimulator && subjectsWithStats.isNotEmpty()) {
                val subjects = subjectsWithStats.map { it.first }
                val statsList = subjectsWithStats.map { it.second }

                AttendanceSimulatorDialog(
                    subjects = subjects,
                    statsList = statsList,
                    initialSelectedIndex = selectedSubjectIndexForSim.coerceIn(0, subjects.size - 1),
                    onDismiss = { showSimulator = false }
                )
            }

            // Manual Override Dialog
            overrideSubject?.let { subject ->
                val stats = subjectsWithStats.find { it.first.id == subject.id }?.second
                ManualOverrideDialog(
                    subject = subject,
                    currentAttended = stats?.attended ?: subject.manualAttended,
                    currentConducted = (stats?.attended ?: subject.manualAttended) + (stats?.missed ?: 0),
                    onDismiss = { overrideSubject = null },
                    onSave = { attended, conducted ->
                        viewModel.updateManualOverrides(subject, attended, conducted)
                        overrideSubject = null
                    },
                    onUpdateTarget = { target ->
                        viewModel.updateSubjectTarget(subject, target)
                    }
                )
            }

            // Add Subject Dialog
            if (showAddSubjectDialog) {
                AddSubjectDialog(
                    onDismiss = { showAddSubjectDialog = false },
                    onSave = { name, code, faculty, target ->
                        viewModel.addSubject(name, code, faculty, target)
                        showAddSubjectDialog = false
                    }
                )
            }
        }
    }
}

// ==========================================
// 1. TODAY ATTENDANCE VIEW (SCREENSHOT 1)
// ==========================================
@Composable
fun TodayAttendanceView(
    selectedDate: Long,
    records: List<AttendanceRecord>,
    schedule: List<TimetableEntry>,
    subjectsWithStats: List<Pair<Subject, SubjectStats>>,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onMarkLecture: (Int, AttendanceStatus?) -> Unit,
    onMarkAllDay: (AttendanceStatus) -> Unit,
    onClearDay: () -> Unit,
    calculateRecommendation: (Int, Int, Double) -> String
) {
    val attendedCount = records.count { it.status == AttendanceStatus.PRESENT }
    val missedCount = records.count { it.status == AttendanceStatus.ABSENT }
    val offCount = records.count { it.status == AttendanceStatus.OFF }
    val totalRecords = records.size

    val dayStatusText = when {
        totalRecords == 0 -> "Not Marked"
        attendedCount > 0 && missedCount == 0 && offCount == 0 -> "Attended"
        missedCount > 0 && attendedCount == 0 && offCount == 0 -> "Missed"
        offCount > 0 && attendedCount == 0 && missedCount == 0 -> "Off"
        else -> "Mixed"
    }

    val dayStatusDotColor = when (dayStatusText) {
        "Attended" -> SuccessGreen
        "Missed" -> DangerRed
        "Off" -> WarningOrange
        "Mixed" -> Color(0xFFC084FC)
        else -> SecondaryText
    }

    val displaySubjects = if (schedule.isNotEmpty()) {
        schedule.mapNotNull { entry ->
            val sub = subjectsWithStats.find { it.first.id == entry.subjectId }
            sub?.let { entry to it }
        }
    } else {
        subjectsWithStats.map { null to it }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Date Switcher Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousDay, modifier = Modifier.size(32.dp)) {
                    Text("◀", color = SecondaryText, fontSize = 14.sp)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, BorderDark),
                    modifier = Modifier.clickable { onPickDate() }
                ) {
                    Text(
                        text = "📅 Jump to Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                IconButton(onClick = onNextDay, modifier = Modifier.size(32.dp)) {
                    Text("▶", color = SecondaryText, fontSize = 14.sp)
                }
            }
        }

        // Day Status Banner
        item {
            StudentCard(
                backgroundColor = Color(0xFF383350),
                borderColor = BorderDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(dayStatusDotColor, CircleShape)
                        )
                        Column {
                            Text("Day status:", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                            Text(dayStatusText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = PrimaryText)
                        }
                    }

                    // Bulk Day Actions: Clear, Off, Miss, Att
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DayActionButton(label = "Clear", icon = "⊘", isSelected = false, onClick = onClearDay)
                        DayActionButton(label = "Off", icon = "—", isSelected = dayStatusText == "Off", onClick = { onMarkAllDay(AttendanceStatus.OFF) })
                        DayActionButton(label = "Miss", icon = "✕", isSelected = dayStatusText == "Missed", onClick = { onMarkAllDay(AttendanceStatus.ABSENT) })
                        DayActionButton(label = "Att", icon = "✓", isSelected = dayStatusText == "Attended", onClick = { onMarkAllDay(AttendanceStatus.PRESENT) })
                    }
                }
            }
        }

        // Lecture Cards Stack
        if (displaySubjects.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.CheckCircle,
                    title = "No Subjects or Classes Scheduled",
                    subtitle = "Add your academic courses in Subjects tab to begin tracking.",
                    actionText = "+ Add Course",
                    onActionClick = {}
                )
            }
        } else {
            items(displaySubjects) { (entry, pair) ->
                val (subject, stats) = pair
                val record = records.find { it.subjectId == subject.id }
                val currentStatus = record?.status

                val diff = stats.percentage - subject.targetPercentage
                val statusColor = when {
                    diff >= 0 -> SuccessGreen
                    diff >= -5.0 -> WarningOrange
                    else -> DangerRed
                }

                val rec = calculateRecommendation(stats.attended, stats.attended + stats.missed, subject.targetPercentage)

                StudentCard(
                    backgroundColor = CardDark,
                    borderColor = BorderDark
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Circular Fraction Badge: 41.67 / 75
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF261D2B))
                                    .border(BorderStroke(1.5.dp, statusColor.copy(alpha = 0.6f)), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = String.format("%.1f", stats.percentage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        lineHeight = 12.sp
                                    )
                                    Box(modifier = Modifier.width(22.dp).height(1.dp).background(statusColor.copy(alpha = 0.5f)))
                                    Text(
                                        text = "${subject.targetPercentage.toInt()}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SecondaryText,
                                        lineHeight = 11.sp
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rec,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText
                                )
                                if (entry != null) {
                                    Text(
                                        text = "⏰ ${entry.startTime} - ${entry.endTime} • Room ${entry.room}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandCyan.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // 4 Interactive Round Buttons: Clear, Off, Miss, Att
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoundStatusButton(
                                symbol = "⊘",
                                isSelected = currentStatus == null,
                                activeColor = SecondaryText,
                                onClick = { onMarkLecture(subject.id, null) }
                            )
                            RoundStatusButton(
                                symbol = "—",
                                isSelected = currentStatus == AttendanceStatus.OFF,
                                activeColor = WarningOrange,
                                onClick = { onMarkLecture(subject.id, AttendanceStatus.OFF) }
                            )
                            RoundStatusButton(
                                symbol = "✕",
                                isSelected = currentStatus == AttendanceStatus.ABSENT,
                                activeColor = DangerRed,
                                onClick = { onMarkLecture(subject.id, AttendanceStatus.ABSENT) }
                            )
                            RoundStatusButton(
                                symbol = "✓",
                                isSelected = currentStatus == AttendanceStatus.PRESENT,
                                activeColor = SuccessGreen,
                                onClick = { onMarkLecture(subject.id, AttendanceStatus.PRESENT) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. ALL SUBJECTS OVERVIEW (SCREENSHOT 4)
// ==========================================
@Composable
fun SubjectsOverviewView(
    overallPct: Double,
    targetPct: Int,
    overallAttended: Int,
    overallMissed: Int,
    overallOff: Int,
    overallConducted: Int,
    overallRecommendation: String,
    overallStatusColor: Color,
    subjectsWithStats: List<Pair<Subject, SubjectStats>>,
    onSubjectClick: (Subject) -> Unit,
    calculateRecommendation: (Int, Int, Double) -> String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overall Card
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Status Strip
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(68.dp)
                            .background(overallStatusColor, RoundedCornerShape(2.dp))
                    )

                    // Fractional Badge: 57.41 / 75
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF261E29))
                            .border(BorderStroke(1.dp, overallStatusColor.copy(alpha = 0.5f)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.2f", overallPct),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = overallStatusColor,
                                lineHeight = 13.sp
                            )
                            Box(modifier = Modifier.width(28.dp).height(1.dp).background(overallStatusColor.copy(alpha = 0.5f)))
                            Text(
                                text = "$targetPct",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SecondaryText,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    // Middle Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Overall",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Text(
                            text = overallRecommendation,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Att: $overallAttended   Miss: $overallMissed   Off: $overallOff   Tot: $overallConducted",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Individual Subject Cards
        items(subjectsWithStats) { (subject, stats) ->
            val totalConducted = stats.attended + stats.missed
            val diff = stats.percentage - subject.targetPercentage
            val statusColor = when {
                diff >= 0 -> SuccessGreen
                diff >= -5.0 -> WarningOrange
                else -> DangerRed
            }
            val rec = calculateRecommendation(stats.attended, totalConducted, subject.targetPercentage)

            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSubjectClick(subject) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Status Strip
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(68.dp)
                            .background(statusColor, RoundedCornerShape(2.dp))
                    )

                    // Fractional Badge: 41.67 / 75
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF261E29))
                            .border(BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.2f", stats.percentage),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                lineHeight = 13.sp
                            )
                            Box(modifier = Modifier.width(28.dp).height(1.dp).background(statusColor.copy(alpha = 0.5f)))
                            Text(
                                text = "${subject.targetPercentage.toInt()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SecondaryText,
                                lineHeight = 12.sp
                            )
                        }
                    }

                    // Middle Details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = subject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = rec,
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Att: ${stats.attended}   Miss: ${stats.missed}   Off: ${stats.off}   Tot: $totalConducted",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. CALENDAR HEATMAP VIEW (SCREENSHOT 3)
// ==========================================
@Composable
fun CalendarHeatmapView(
    currentMonth: Calendar,
    heatmapData: Map<Long, DayAttendanceStats>,
    summary: CalendarSummaryStats,
    selectedDate: Long,
    onSelectDate: (Long) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

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
    // Map Java Calendar Sunday=1..Saturday=7 to Mon=0..Sun=6
    val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

    val selectedDayStats = heatmapData[selectedDate]
    val selectedDateFormatted = remember(selectedDate) {
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Header Switcher: < August 2026 >
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
                    Text("◀", color = SecondaryText, fontSize = 14.sp)
                }
                Text(
                    text = monthFormatter.format(currentMonth.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                    Text("▶", color = SecondaryText, fontSize = 14.sp)
                }
            }
        }

        // Calendar Heatmap Matrix
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Weekday headers
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Calendar Days Grid
                    val totalSlots = startOffset + daysInMonth
                    val rows = (totalSlots + 6) / 7

                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
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
                                        DayStatus.ATTENDED -> SuccessGreen
                                        DayStatus.MISSED -> DangerRed
                                        DayStatus.MIXED -> Color(0xFFC084FC)
                                        DayStatus.OFF -> WarningOrange
                                        else -> null
                                    }

                                    Column(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (isSelected) Modifier.border(1.5.dp, Color.White, CircleShape)
                                                else Modifier
                                            )
                                            .clickable { onSelectDate(dayTime) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else PrimaryText
                                        )
                                        if (dotColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(dotColor, CircleShape)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(36.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Attendance Breakdown Card
        item {
            StudentCard(
                backgroundColor = Color(0xFF1E2640),
                borderColor = BorderDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "📅 $selectedDateFormatted",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Attended: ${selectedDayStats?.attended ?: 0}", color = SuccessGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Missed: ${selectedDayStats?.missed ?: 0}", color = DangerRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Off: ${selectedDayStats?.off ?: 0}", color = WarningOrange, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text("Total: ${selectedDayStats?.total ?: 0}", color = PrimaryText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Summary Metric Badges (Screenshot 3)
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SummaryPill(count = "${summary.notMarkedDays}", label = "Not marked", dotColor = SecondaryText)
                        SummaryPill(count = "${summary.offDays}", label = "Off", dotColor = WarningOrange)
                        SummaryPill(count = "${summary.missedDays}", label = "Missed", dotColor = DangerRed)
                        SummaryPill(count = "${summary.attendedDays}", label = "Attended", dotColor = SuccessGreen)
                        SummaryPill(count = "${summary.mixedDays}", label = "Mixed", dotColor = Color(0xFFC084FC))
                    }
                    Text(
                        text = "Days Summary",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Semester Totals Card
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${summary.totalOff}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text("Off", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${summary.totalMissed}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DangerRed)
                            Text("Missed", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${summary.totalAttended}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SuccessGreen)
                            Text("Attended", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${summary.totalAttended + summary.totalMissed}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText)
                            Text("Total", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${String.format("%.2f", summary.overallPercentage)}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandCyan)
                            Text("Percent", style = MaterialTheme.typography.labelSmall, color = SecondaryText)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// HELPER UI PILLS & BUTTONS
// ==========================================
@Composable
fun AttendanceBottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) Color(0xFF4C3E72) else Color.Transparent,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else SecondaryText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else SecondaryText,
            fontSize = 11.sp
        )
    }
}

@Composable
fun DayActionButton(
    label: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) BrandCyan.copy(alpha = 0.3f) else Color(0xFF261D2B))
                .border(BorderStroke(1.dp, if (isSelected) BrandCyan else BorderDark), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 12.sp, color = if (isSelected) BrandCyan else Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = SecondaryText)
    }
}

@Composable
fun RoundStatusButton(
    symbol: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (isSelected) activeColor.copy(alpha = 0.3f) else ElevatedCardDark)
            .border(BorderStroke(1.dp, if (isSelected) activeColor else BorderDark), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            color = if (isSelected) activeColor else SecondaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SummaryPill(count: String, label: String, dotColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText)
        Spacer(modifier = Modifier.height(2.dp))
        Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = SecondaryText, fontSize = 9.sp)
    }
}

@Composable
fun AttendanceSimulatorDialog(
    subjects: List<Subject>,
    statsList: List<SubjectStats>,
    initialSelectedIndex: Int,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(initialSelectedIndex) }
    var futureAttendStr by remember { mutableStateOf("0") }
    var futureMissStr by remember { mutableStateOf("0") }

    val subject = subjects[selectedIndex]
    val stats = statsList[selectedIndex]

    val fa = futureAttendStr.toIntOrNull() ?: 0
    val fm = futureMissStr.toIntOrNull() ?: 0

    val newAttended = stats.attended + fa
    val newConducted = stats.attended + stats.missed + fa + fm

    val simulatedPercentage = if (newConducted > 0) {
        (newAttended.toDouble() / newConducted.toDouble()) * 100.0
    } else {
        100.0
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Attendance Simulator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Subject Dropdown Selector
                Box {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subject: ${subject.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        subjects.forEachIndexed { index, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name, color = PrimaryText) },
                                onClick = {
                                    selectedIndex = index
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Simulate Future Classes:", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = futureAttendStr,
                        onValueChange = { futureAttendStr = it },
                        label = { Text("Attend (+)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuccessGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = futureMissStr,
                        onValueChange = { futureMissStr = it },
                        label = { Text("Miss (-)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DangerRed,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Projected Comparison Card
                ElevatedStudentCard(
                    backgroundColor = CardDark,
                    borderColor = if (simulatedPercentage >= subject.targetPercentage) SuccessGreen.copy(alpha = 0.4f) else DangerRed.copy(alpha = 0.4f)
                ) {
                    Text(
                        "Projected Attendance",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${String.format("%.1f", stats.percentage)}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = SecondaryText
                        )
                        Text("➔", style = MaterialTheme.typography.titleMedium, color = LightPurple)
                        Text(
                            "${String.format("%.1f", simulatedPercentage)}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (simulatedPercentage >= subject.targetPercentage) SuccessGreen else DangerRed
                        )
                    }
                    Text(
                        "$newAttended attended / $newConducted conducted",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    if (simulatedPercentage < subject.targetPercentage) {
                        val classesNeeded = ceil((subject.targetPercentage / 100.0 * newConducted - newAttended) / (1.0 - subject.targetPercentage / 100.0)).toInt().coerceAtLeast(0)
                        Text(
                            "👉 Attend next $classesNeeded classes consecutively to reach ${subject.targetPercentage.toInt()}%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        val classesCanMiss = floor((newAttended - subject.targetPercentage / 100.0 * newConducted) / (subject.targetPercentage / 100.0)).toInt().coerceAtLeast(0)
                        Text(
                            "👉 Safe! You can miss next $classesCanMiss classes consecutively and stay above target.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualOverrideDialog(
    subject: Subject,
    currentAttended: Int,
    currentConducted: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onUpdateTarget: (Double) -> Unit
) {
    var attendedStr by remember { mutableStateOf(currentAttended.toString()) }
    var conductedStr by remember { mutableStateOf(currentConducted.toString()) }
    var targetVal by remember { mutableStateOf(subject.targetPercentage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Subject Settings & Baseline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Edit baseline totals for ${subject.name}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = attendedStr,
                        onValueChange = { attendedStr = it },
                        label = { Text("Attended") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = conductedStr,
                        onValueChange = { conductedStr = it },
                        label = { Text("Conducted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Target Attendance %:", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(75.0, 80.0, 85.0, 90.0).forEach { target ->
                        val isSel = targetVal == target
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                targetVal = target
                                onUpdateTarget(target)
                            },
                            label = { Text("${target.toInt()}%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White,
                                containerColor = CardDark,
                                labelColor = SecondaryText
                            ),
                            border = FilterChipDefaults.filterChipBorder(borderColor = BorderDark)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val att = attendedStr.toIntOrNull() ?: currentAttended
                    val cond = conductedStr.toIntOrNull() ?: currentConducted
                    onSave(att, cond)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("75") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Add New Subject",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. DBMS)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Subject Code (optional)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty Name (optional)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Attendance %") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, code, faculty, targetStr.toDoubleOrNull() ?: 75.0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}
