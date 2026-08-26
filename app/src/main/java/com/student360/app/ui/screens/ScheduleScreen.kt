@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.util.Locale

@Composable
fun ScheduleScreen(
    repository: StudentRepository,
    viewModel: ScheduleViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val timetable by viewModel.timetable.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var selectedDayTab by remember { mutableStateOf(0) } // Default: Monday (0) like edit screenshot
    var showAddDialog by remember { mutableStateOf(false) }
    var addInitialDay by remember { mutableStateOf(0) }
    var selectedLectureDetail by remember { mutableStateOf<TimetableEntry?>(null) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val fullDaysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = 75

    // Calculate max slots across the 7 days (at least 5 rows)
    val maxSlots = remember(timetable) {
        val maxInAnyDay = (0..6).maxOfOrNull { dayIdx ->
            timetable.count { it.dayOfWeek == dayIdx }
        } ?: 5
        maxInAnyDay.coerceIn(5, 7)
    }

    val selectedDayList = timetable.filter { it.dayOfWeek == selectedDayTab }.sortedBy { it.startTime }

    Scaffold(
        containerColor = colors.bg,
        floatingActionButton = {
            if (subjects.isNotEmpty() && !isEditMode) {
                FloatingActionButton(
                    onClick = {
                        addInitialDay = selectedDayTab
                        showAddDialog = true
                    },
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Lecture")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.bg)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row: Student Name & Attendance Pill + Add + Edit
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile?.name?.ifBlank { "Student" } ?: "Student",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))

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
                                    text = "${String.format(Locale.US, "%.2f", overallPct)} | $targetPct",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    addInitialDay = selectedDayTab
                                    showAddDialog = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Subject",
                                    tint = PrimaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { isEditMode = !isEditMode },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Toggle Edit Mode",
                                    tint = if (isEditMode) BrandCyan else SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Days of Week Header Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        daysOfWeek.forEachIndexed { index, day ->
                            val isSelected = selectedDayTab == index
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedDayTab = index }
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else SecondaryText,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(18.dp)
                                            .height(2.dp)
                                            .background(BrandCyan, RoundedCornerShape(1.dp))
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                }

                if (isEditMode) {
                    // EDIT MODE: Reorderable Subject Cards for Selected Day
                    if (selectedDayList.isEmpty()) {
                        item {
                            StudentCard(
                                backgroundColor = SurfaceDark,
                                borderColor = BorderDark
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "No subjects scheduled for ${fullDaysOfWeek[selectedDayTab]}.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SecondaryText
                                    )
                                    Button(
                                        onClick = {
                                            addInitialDay = selectedDayTab
                                            showAddDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("+ Add Subject to ${daysOfWeek[selectedDayTab]}", color = Color.White)
                                    }
                                }
                            }
                        }
                    } else {
                        items(selectedDayList.size) { index ->
                            val entry = selectedDayList[index]
                            val subject = subjects.find { it.id == entry.subjectId }
                            val subName = subject?.name ?: "Unknown Subject"

                            StudentCard(
                                backgroundColor = CardDark,
                                borderColor = BorderDark
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // 6-dot drag gripper icon
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            modifier = Modifier.padding(start = 4.dp)
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Box(modifier = Modifier.size(3.dp).background(SecondaryText, CircleShape))
                                                Box(modifier = Modifier.size(3.dp).background(SecondaryText, CircleShape))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Box(modifier = Modifier.size(3.dp).background(SecondaryText, CircleShape))
                                                Box(modifier = Modifier.size(3.dp).background(SecondaryText, CircleShape))
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Box(modifier = Modifier.size(3.dp).background(SecondaryText, CircleShape))
                                                Box(modifier = Modifier.size(3.dp).background(SecondaryText, CircleShape))
                                            }
                                        }

                                        Text(
                                            text = subName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Up, Down, Delete buttons
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = { viewModel.moveEntryUp(selectedDayTab, index) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("▲", color = if (index > 0) PrimaryText else SecondaryText.copy(alpha = 0.3f), fontSize = 12.sp)
                                        }
                                        IconButton(
                                            onClick = { viewModel.moveEntryDown(selectedDayTab, index) },
                                            enabled = index < selectedDayList.size - 1,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Text("▼", color = if (index < selectedDayList.size - 1) PrimaryText else SecondaryText.copy(alpha = 0.3f), fontSize = 12.sp)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteTimetableEntry(entry) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = DangerRed.copy(alpha = 0.8f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    addInitialDay = selectedDayTab
                                    showAddDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardDark),
                                border = BorderStroke(1.dp, BorderDark),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+ Add Another Subject to ${daysOfWeek[selectedDayTab]}", color = BrandCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // NORMAL MODE: Weekly Timetable Matrix Card
                    item {
                        StudentCard(
                            backgroundColor = Color(0xFF0F172A),
                            borderColor = BorderDark,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Matrix Slots Rows
                                for (slotIdx in 0 until maxSlots) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        for (dayIdx in 0..6) {
                                            val dayEntries = timetable.filter { it.dayOfWeek == dayIdx }.sortedBy { it.startTime }
                                            val entry = dayEntries.getOrNull(slotIdx)

                                            if (entry != null) {
                                                val subject = subjects.find { it.id == entry.subjectId }
                                                val subTitle = subject?.name ?: "Subject"

                                                // Occupied Purple/Navy Lecture Tile
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(64.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF4C3E72))
                                                        .border(BorderStroke(1.dp, Color(0xFF6B599C).copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                                        .clickable { selectedLectureDetail = entry }
                                                        .padding(4.dp),
                                                    contentAlignment = Alignment.TopStart
                                                ) {
                                                    Text(
                                                        text = subTitle,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        lineHeight = 12.sp,
                                                        maxLines = 3,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else {
                                                // Empty Dark Grid Slot
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(64.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF161F33))
                                                        .border(BorderStroke(0.5.dp, BorderDark.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                                                        .clickable {
                                                            addInitialDay = dayIdx
                                                            showAddDialog = true
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Day Detailed Lecture List Header
                    item {
                        SectionHeader(
                            title = "${fullDaysOfWeek[selectedDayTab]} Schedule",
                            actionText = "Edit / Reorder",
                            onActionClick = { isEditMode = true }
                        )
                    }

                    if (subjects.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Default.DateRange,
                                title = "No Academic Subjects Added",
                                subtitle = "Please add course subjects in Attendance to build your timetable.",
                                actionText = "+ Add Subject",
                                onActionClick = { showAddDialog = true }
                            )
                        }
                    } else if (selectedDayList.isEmpty()) {
                        item {
                            StudentCard(
                                backgroundColor = SurfaceDark,
                                borderColor = BorderDark
                            ) {
                                Text(
                                    text = "No classes scheduled for ${fullDaysOfWeek[selectedDayTab]}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SecondaryText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                                )
                            }
                        }
                    } else {
                        items(selectedDayList.size) { index ->
                            val entry = selectedDayList[index]
                            val subject = subjects.find { it.id == entry.subjectId }
                            LectureCard(
                                entry = entry,
                                subjectName = subject?.name ?: "Unknown Subject",
                                subjectFaculty = subject?.faculty ?: "",
                                onDelete = { viewModel.deleteTimetableEntry(entry) }
                            )
                        }
                    }
                }
            }

            // Lecture Detail / Options Bottom Dialog
            selectedLectureDetail?.let { entry ->
                val subject = subjects.find { it.id == entry.subjectId }
                AlertDialog(
                    onDismissRequest = { selectedLectureDetail = null },
                    containerColor = SurfaceDark,
                    titleContentColor = PrimaryText,
                    textContentColor = PrimaryText,
                    title = {
                        Text(
                            subject?.name ?: "Lecture Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🗓 Day: ${fullDaysOfWeek[entry.dayOfWeek]}", color = PrimaryText, style = MaterialTheme.typography.bodyMedium)
                            Text("⏰ Time: ${entry.startTime} - ${entry.endTime}", color = BrandCyan, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("📍 Room: ${entry.room}", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                            val faculty = entry.facultyOverride ?: subject?.faculty
                            if (!faculty.isNullOrBlank()) {
                                Text("👤 Faculty: $faculty", color = SecondaryText, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { selectedLectureDetail = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteTimetableEntry(entry)
                                selectedLectureDetail = null
                            }
                        ) {
                            Text("Delete Lecture", color = DangerRed)
                        }
                    }
                )
            }

            // Add Lecture Dialog (Save Class saves and stays open)
            if (showAddDialog && subjects.isNotEmpty()) {
                AddLectureDialog(
                    subjects = subjects,
                    initialDay = addInitialDay,
                    onDismiss = { showAddDialog = false },
                    onSave = { subjectId, day, start, end, room, faculty, alertMin ->
                        viewModel.addTimetableEntry(subjectId, day, start, end, room, faculty, alertMin)
                    }
                )
            }
        }
    }
}

@Composable
fun LectureCard(
    entry: TimetableEntry,
    subjectName: String,
    subjectFaculty: String,
    onDelete: () -> Unit
) {
    StudentCard(
        backgroundColor = CardDark,
        borderColor = BorderDark
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElevatedCardDark,
                        border = BorderStroke(1.dp, BorderDark)
                    ) {
                        Text(
                            text = "${entry.startTime} - ${entry.endTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LightPurple,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "📍 Room ${entry.room}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    val facultyText = entry.facultyOverride?.ifBlank { null } ?: subjectFaculty.ifBlank { null }
                    if (facultyText != null) {
                        Text(
                            "👤 $facultyText",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Lecture",
                    tint = SecondaryText,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
