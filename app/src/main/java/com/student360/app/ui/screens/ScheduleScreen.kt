package com.student360.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.StudentRepository

@OptIn(ExperimentalMaterial3Api::class)
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

    val filteredList = timetable.filter { it.dayOfWeek == selectedDayTab }
        .sortedBy { it.startTime }

    Scaffold(
        floatingActionButton = {
            if (subjects.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Lecture")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(selectedTabIndex = selectedDayTab) {
                    daysOfWeek.forEachIndexed { index, day ->
                        Tab(
                            selected = selectedDayTab == index,
                            onClick = { selectedDayTab = index },
                            text = { Text(day) }
                        )
                    }
                }

                if (subjects.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Please add subjects first in settings or onboarding.")
                    }
                } else if (filteredList.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No classes scheduled for ${daysOfWeek[selectedDayTab]}")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    subjectName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "⏰ ${entry.startTime} - ${entry.endTime}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "📍 Room: ${entry.room}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!entry.facultyOverride.isNullOrBlank() || subjectFaculty.isNotBlank()) {
                    Text(
                        "👤 Faculty: ${entry.facultyOverride ?: subjectFaculty}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Lecture")
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
        title = { Text("Add Lecture") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        subjects.forEachIndexed { index, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    subjectIndex = index
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = dayDropdownExpanded,
                        onDismissRequest = { dayDropdownExpanded = false }
                    ) {
                        daysOfWeek.forEachIndexed { index, day ->
                            DropdownMenuItem(
                                text = { Text(day) },
                                onClick = {
                                    selectedDay = index
                                    dayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time (HH:MM)") },
                    placeholder = { Text("09:00") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    label = { Text("End Time (HH:MM)") },
                    placeholder = { Text("10:00") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Location") },
                    placeholder = { Text("LHC-101") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = facultyOverride,
                    onValueChange = { facultyOverride = it },
                    label = { Text("Faculty (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = notifyDropdownExpanded,
                        onDismissRequest = { notifyDropdownExpanded = false }
                    ) {
                        notificationOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(option) },
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
                enabled = startTime.isNotBlank() && endTime.isNotBlank() && room.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
