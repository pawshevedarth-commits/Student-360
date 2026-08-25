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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.StudentCard
import com.student360.app.ui.theme.*
import java.util.*

@Composable
fun AttendanceScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    scheduleViewModel: ScheduleViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val timetable by scheduleViewModel.timetable.collectAsState()
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var selectedDayTab by remember { mutableStateOf(0) } // 0 = Mon .. 6 = Sun

    var showAddLectureDialog by remember { mutableStateOf(false) }
    var selectedEntryForPicker by remember { mutableStateOf<TimetableEntry?>(null) }
    var selectedEntryForDetail by remember { mutableStateOf<TimetableEntry?>(null) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val fullDaysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = 75

    val overallStatusColor = when {
        overallPct >= 75.0 -> colors.success
        overallPct >= 70.0 -> colors.warning
        else -> colors.danger
    }

    // Determine current day of week from selectedDate
    LaunchedEffect(selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        selectedDayTab = if (calDay == Calendar.SUNDAY) 6 else calDay - 2
    }

    val selectedDayTimetable = timetable.filter { it.dayOfWeek == selectedDayTab }.sortedBy { it.startTime }

    val maxSlots = remember(timetable) {
        val maxInAnyDay = (0..6).maxOfOrNull { dayIdx ->
            timetable.count { it.dayOfWeek == dayIdx }
        } ?: 5
        maxInAnyDay.coerceIn(5, 7)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOP BAR (Student360 | 59.26 | 75 | + | ✏️)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Student360",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Attendance Pill: 57.41 | 75
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", overallPct)} | $targetPct",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = overallStatusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // In Edit Mode: + Button to add a new class
                    if (isEditMode) {
                        IconButton(
                            onClick = { showAddLectureDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Lecture",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Pen / Done Edit Mode Toggle Button
                    IconButton(
                        onClick = {
                            isEditMode = !isEditMode
                            if (!isEditMode) {
                                Toast.makeText(context, "Timetable saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isEditMode) colors.activePill else colors.card)
                            .border(BorderStroke(1.dp, if (isEditMode) colors.accent else colors.border), CircleShape)
                    ) {
                        Icon(
                            if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Done" else "Edit Timetable",
                            tint = if (isEditMode) (if (colors.isDark) Color.White else colors.accent) else colors.textPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. WEEKLY DAY TABS: Mon Tue Wed Thu Fri Sat Sun
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    val isSelected = selectedDayTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedDayTab = index
                                // Sync selected date in viewModel
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                val currentCalDay = cal.get(Calendar.DAY_OF_WEEK)
                                val currentDayIdx = if (currentCalDay == Calendar.SUNDAY) 6 else currentCalDay - 2
                                val diff = index - currentDayIdx
                                cal.add(Calendar.DAY_OF_YEAR, diff)
                                viewModel.selectDate(cal.timeInMillis)
                            }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.accent else colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.5.dp)
                                    .background(colors.accent, RoundedCornerShape(1.dp))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.5.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. MAIN CONTENT: NORMAL MATRIX GRID vs. EDIT MODE LIST
            if (isEditMode) {
                // EDIT MODE: Reorderable list for the selected day (Screenshot 2)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${fullDaysOfWeek[selectedDayTab]} Classes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Tap to change • Reorder below",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }

                    if (selectedDayTimetable.isEmpty()) {
                        StudentCard(
                            backgroundColor = colors.card,
                            borderColor = colors.border,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No lectures on ${fullDaysOfWeek[selectedDayTab]}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                OutlinedButton(
                                    onClick = { showAddLectureDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                                    border = BorderStroke(1.dp, colors.border)
                                ) {
                                    Text("+ Add Lecture", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            itemsIndexed(selectedDayTimetable) { index, entry ->
                                val sub = subjectsWithStats.find { it.first.id == entry.subjectId }?.first
                                val subjectTitle = sub?.name ?: "Subject"

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = colors.card,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedEntryForPicker = entry }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // 6-Dot Drag Handle (Screenshot 2: ⠿)
                                            Icon(
                                                Icons.Default.Menu,
                                                contentDescription = "Drag handle",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )

                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Text(
                                                    text = subjectTitle,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    text = "⏰ ${entry.startTime} – ${entry.endTime}" + if (entry.room.isNotBlank()) " • Room ${entry.room}" else "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.textSecondary,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        // Reorder / Delete Actions
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            IconButton(
                                                onClick = { scheduleViewModel.moveEntryUp(selectedDayTab, index) },
                                                enabled = index > 0,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("▲", color = if (index > 0) colors.accent else colors.textSecondary.copy(alpha = 0.3f), fontSize = 12.sp)
                                            }
                                            IconButton(
                                                onClick = { scheduleViewModel.moveEntryDown(selectedDayTab, index) },
                                                enabled = index < selectedDayTimetable.size - 1,
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Text("▼", color = if (index < selectedDayTimetable.size - 1) colors.accent else colors.textSecondary.copy(alpha = 0.3f), fontSize = 12.sp)
                                            }
                                            IconButton(
                                                onClick = {
                                                    scheduleViewModel.deleteTimetableEntry(entry)
                                                    Toast.makeText(context, "Removed $subjectTitle", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colors.danger, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // NORMAL MODE: WEEKLY TIMETABLE MATRIX GRID (Screenshot 1)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (slotIdx in 0 until maxSlots) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    for (dayIdx in 0..6) {
                                        val dayEntries = timetable.filter { it.dayOfWeek == dayIdx }.sortedBy { it.startTime }
                                        val entry = dayEntries.getOrNull(slotIdx)

                                        if (entry != null) {
                                            val sub = subjectsWithStats.find { it.first.id == entry.subjectId }?.first
                                            val subTitle = sub?.name ?: "Subject"

                                            // Lecture cell: rounded rectangle with lavender/purple background (Screenshot 1)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(68.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFE8DEFF)) // Soft lavender cell matching reference
                                                    .border(BorderStroke(1.dp, Color(0xFFD4C4FA)), RoundedCornerShape(8.dp))
                                                    .clickable { selectedEntryForDetail = entry }
                                                    .padding(horizontal = 3.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.TopStart
                                            ) {
                                                Text(
                                                    text = subTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF261D45), // Dark readable text on lavender cell
                                                    fontSize = 10.sp,
                                                    lineHeight = 11.sp,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            // Empty cell
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(68.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.card)
                                                    .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedDayTab = dayIdx
                                                        showAddLectureDialog = true
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
        }

        // Add Lecture Dialog
        if (showAddLectureDialog) {
            val subjects = subjectsWithStats.map { it.first }
            if (subjects.isEmpty()) {
                AlertDialog(
                    onDismissRequest = { showAddLectureDialog = false },
                    containerColor = colors.card,
                    title = { Text("No Subjects", color = colors.textPrimary) },
                    text = { Text("Please create a course in the Subjects tab first.", color = colors.textSecondary) },
                    confirmButton = { TextButton(onClick = { showAddLectureDialog = false }) { Text("OK", color = colors.accent) } }
                )
            } else {
                AddLectureDialog(
                    subjects = subjects,
                    initialDay = selectedDayTab,
                    onDismiss = { showAddLectureDialog = false },
                    onSave = { subjectId, day, start, end, room, faculty, alertMin ->
                        scheduleViewModel.addTimetableEntry(subjectId, day, start, end, room, faculty, alertMin)
                        showAddLectureDialog = false
                        Toast.makeText(context, "Lecture added", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }

        // Change Subject / Cell Picker Dialog (Edit Mode Tap)
        selectedEntryForPicker?.let { entry ->
            val subjects = subjectsWithStats.map { it.first }
            AlertDialog(
                onDismissRequest = { selectedEntryForPicker = null },
                containerColor = colors.card,
                titleContentColor = colors.textPrimary,
                title = { Text("Select Subject for Cell", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Choose course to assign to this period:", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(subjects.size) { idx ->
                                val sub = subjects[idx]
                                val isCurrent = sub.id == entry.subjectId
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCurrent) colors.activePill else colors.elevatedCard,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scheduleViewModel.updateEntrySubject(entry, sub.id)
                                            selectedEntryForPicker = null
                                            Toast.makeText(context, "Updated to ${sub.name}", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Text(
                                        text = sub.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) (if (colors.isDark) Color.White else colors.accent) else colors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedEntryForPicker = null }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            scheduleViewModel.deleteTimetableEntry(entry)
                            selectedEntryForPicker = null
                            Toast.makeText(context, "Cleared cell", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Clear Cell", color = colors.danger)
                    }
                }
            )
        }

        // Lecture Detail BottomSheet / Dialog (Normal Mode Tap)
        selectedEntryForDetail?.let { entry ->
            val sub = subjectsWithStats.find { it.first.id == entry.subjectId }?.first
            AlertDialog(
                onDismissRequest = { selectedEntryForDetail = null },
                containerColor = colors.card,
                titleContentColor = colors.textPrimary,
                title = {
                    Text(
                        sub?.name ?: "Lecture Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🗓 Day: ${fullDaysOfWeek[entry.dayOfWeek]}", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text("⏰ Time: ${entry.startTime} – ${entry.endTime}", color = colors.accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        if (entry.room.isNotBlank()) {
                            Text("📍 Room: ${entry.room}", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        val fac = entry.facultyOverride?.ifBlank { null } ?: sub?.faculty?.ifBlank { null }
                        if (fac != null) {
                            Text("👤 Faculty: $fac", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isEditMode = true
                            selectedDayTab = entry.dayOfWeek
                            selectedEntryForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Edit Timetable", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedEntryForDetail = null }) {
                        Text("Close", color = colors.textSecondary)
                    }
                }
            )
        }
    }
}
