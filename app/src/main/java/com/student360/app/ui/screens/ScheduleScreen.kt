@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
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
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*

@Composable
fun ScheduleScreen(
    repository: StudentRepository,
    viewModel: ScheduleViewModel = viewModel()
) {
    val timetable by viewModel.timetable.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var selectedDayTab by remember { mutableStateOf(0) } // 0 = Mon, 5 = Sat
    var showAddDialog by remember { mutableStateOf(false) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val fullDaysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    val filteredList = timetable.filter { it.dayOfWeek == selectedDayTab }
        .sortedBy { it.startTime }

    Scaffold(
        containerColor = BgDark,
        floatingActionButton = {
            if (subjects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = PrimaryPurple,
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
                .background(BgDark)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Compact Horizontal Day Pill Selector
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(daysOfWeek) { index, day ->
                        val isSelected = selectedDayTab == index
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedDayTab = index },
                            color = if (isSelected) PrimaryPurple else CardDark,
                            border = BorderStroke(1.dp, if (isSelected) PrimaryPurple else BorderDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else SecondaryText,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                if (subjects.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.DateRange,
                        title = "No Subjects Added",
                        subtitle = "Please add your courses first in the Attendance tab or Onboarding.",
                        modifier = Modifier.weight(1f)
                    )
                } else if (filteredList.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.DateRange,
                        title = "No Lectures on ${fullDaysOfWeek[selectedDayTab]}",
                        subtitle = "Enjoy your day off or tap + to schedule a lecture.",
                        actionText = "+ Add Lecture",
                        onActionClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList) { entry ->
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

            if (showAddDialog && subjects.isNotEmpty()) {
                AddLectureDialog(
                    subjects = subjects,
                    initialDay = selectedDayTab,
                    onDismiss = { showAddDialog = false },
                    onSave = { subjectId, day, start, end, room, faculty, alertMin ->
                        viewModel.addTimetableEntry(subjectId, day, start, end, room, faculty, alertMin)
                        showAddDialog = false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLectureDialog(
    subjects: List<Subject>,
    initialDay: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, String, String, String?, Int?) -> Unit
) {
    var subjectIndex by remember { mutableStateOf(0) }
    var selectedDay by remember { mutableStateOf(initialDay) }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var facultyOverride by remember { mutableStateOf("") }
    var notificationIndex by remember { mutableStateOf(2) } // Default: 10m before

    val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val notificationOptions = listOf("None", "At start", "5m before", "10m before", "15m before", "30m before")
    val notificationMinutes = listOf(null, 0, 5, 10, 15, 30)

    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var dayDropdownExpanded by remember { mutableStateOf(false) }
    var notifyDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Add Lecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectDropdownExpanded,
                    onExpandedChange = { subjectDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = subjects[subjectIndex].name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        subjects.forEachIndexed { index, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name, color = PrimaryText) },
                                onClick = {
                                    subjectIndex = index
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Day Dropdown
                ExposedDropdownMenuBox(
                    expanded = dayDropdownExpanded,
                    onExpandedChange = { dayDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = daysOfWeek[selectedDay],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        daysOfWeek.forEachIndexed { index, day ->
                            DropdownMenuItem(
                                text = { Text(day, color = PrimaryText) },
                                onClick = {
                                    selectedDay = index
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:MM)") },
                        placeholder = { Text("09:00") },
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
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:MM)") },
                        placeholder = { Text("10:00") },
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

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Location (e.g. LHC-101)") },
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
                    value = facultyOverride,
                    onValueChange = { facultyOverride = it },
                    label = { Text("Faculty (Optional)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notification Dropdown
                ExposedDropdownMenuBox(
                    expanded = notifyDropdownExpanded,
                    onExpandedChange = { notifyDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = notificationOptions[notificationIndex],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pre-Class Notification") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = notifyDropdownExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = notifyDropdownExpanded,
                        onDismissRequest = { notifyDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        notificationOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option, color = PrimaryText) },
                                onClick = {
                                    notificationIndex = index
                                    notifyDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        subjects[subjectIndex].id,
                        selectedDay,
                        startTime,
                        endTime,
                        room,
                        facultyOverride.ifBlank { null },
                        notificationMinutes[notificationIndex]
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp),
                enabled = startTime.isNotBlank() && endTime.isNotBlank() && room.isNotBlank()
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
