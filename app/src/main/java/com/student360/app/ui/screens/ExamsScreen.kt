package com.student360.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.*
import com.student360.app.data.repository.StudentRepository
import com.student360.app.service.ExamEngine
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsScreen(
    repository: StudentRepository,
    viewModel: ExamsViewModel = viewModel()
) {
    val examsWithPrep by viewModel.examsWithPrep.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val examModeTarget = examsWithPrep.filter { it.daysRemaining in 0..7 }
        .minByOrNull { it.daysRemaining }

    Scaffold(
        floatingActionButton = {
            if (subjects.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exam")
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
                
                examModeTarget?.let { target ->
                    val subjectName = subjects.find { it.id == target.exam.subjectId }?.name ?: "Subject"
                    val weakTopics = ExamEngine.getWeakTopics(target.topics)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "🚨 EXAM MODE ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    ExamEngine.getUrgencyText(target.daysRemaining),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Subject: $subjectName (${target.exam.examType.name})",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Preparation: ${target.prepPercentage.toInt()}% complete",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (weakTopics.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Weak Topics: ${weakTopics.take(3).joinToString { it.topicName }}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Text(
                                        "Action recommendation: Study '${weakTopics.first().topicName}' today for 45 minutes.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (subjects.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Please add subjects first in settings or onboarding.")
                    }
                } else if (examsWithPrep.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No exams scheduled. Tap + to add.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(examsWithPrep) { examWithPrep ->
                            val subject = subjects.find { it.id == examWithPrep.exam.subjectId }
                            ExamPrepCard(
                                examWithPrep = examWithPrep,
                                subjectName = subject?.name ?: "Unknown Subject",
                                onAddTopic = { topic -> viewModel.addTopicToExam(examWithPrep.exam.id, topic) },
                                onUpdateTopicStatus = { topic, status -> viewModel.updateTopicStatus(topic, status) },
                                onDelete = { viewModel.deleteExam(examWithPrep.exam) }
                            )
                        }
                    }
                }
            }

            if (showAddDialog && subjects.isNotEmpty()) {
                AddExamDialog(
                    subjects = subjects,
                    onDismiss = { showAddDialog = false },
                    onSave = { subjectId, type, date, time, venue, max, target, topics ->
                        viewModel.addExam(subjectId, type, date, time, venue, max, target, topics)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun ExamPrepCard(
    examWithPrep: ExamWithPrep,
    subjectName: String,
    onAddTopic: (String) -> Unit,
    onUpdateTopicStatus: (ExamTopic, TopicStatus) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var newTopicName by remember { mutableStateOf("") }

    val daysLeft = examWithPrep.daysRemaining
    val urgencyColor = when {
        daysLeft < 0 -> HolidayGrey
        daysLeft <= 1 -> CriticalRed
        daysLeft <= 3 -> CriticalRed
        daysLeft <= 7 -> WarningYellow
        else -> SafeGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "$subjectName (${examWithPrep.exam.examType.name})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Venue: ${examWithPrep.exam.venue} • Time: ${examWithPrep.exam.time}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .background(urgencyColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        ExamEngine.getUrgencyText(daysLeft),
                        color = urgencyColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Preparation: ${examWithPrep.prepPercentage.toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    "Target: ${examWithPrep.exam.targetMarks} / ${examWithPrep.exam.maxMarks} M",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = (examWithPrep.prepPercentage / 100.0).toFloat().coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth(),
                color = urgencyColor,
                trackColor = urgencyColor.copy(alpha = 0.2f)
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Syllabus Checklist:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    
                    if (examWithPrep.topics.isEmpty()) {
                        Text("No topics defined.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp))
                    } else {
                        examWithPrep.topics.forEach { topic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(topic.topicName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TopicStatusButton("Not Started", topic.status == TopicStatus.NOT_STARTED) {
                                        onUpdateTopicStatus(topic, TopicStatus.NOT_STARTED)
                                    }
                                    TopicStatusButton("In Progress", topic.status == TopicStatus.IN_PROGRESS) {
                                        onUpdateTopicStatus(topic, TopicStatus.IN_PROGRESS)
                                    }
                                    TopicStatusButton("Done", topic.status == TopicStatus.COMPLETED) {
                                        onUpdateTopicStatus(topic, TopicStatus.COMPLETED)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTopicName,
                            onValueChange = { newTopicName = it },
                            label = { Text("Add Topic") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            if (newTopicName.isNotBlank()) {
                                onAddTopic(newTopicName)
                                newTopicName = ""
                            }
                        }) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Exam")
                    }
                }
            }
        }
    }
}

@Composable
fun TopicStatusButton(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = bgColor),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        modifier = Modifier.height(28.dp)
    ) {
        Text(label, color = textColor, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExamDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (Int, ExamType, Long, String, String, Int, Int, List<String>) -> Unit
) {
    var subjectIndex by remember { mutableStateOf(0) }
    var examType by remember { mutableStateOf(ExamType.MIDTERM) }
    var dateString by remember { mutableStateOf("") } // YYYY-MM-DD
    var timeString by remember { mutableStateOf("") } // HH:MM
    var venue by remember { mutableStateOf("") }
    var maxMarks by remember { mutableStateOf("") }
    var targetMarks by remember { mutableStateOf("") }
    var topicsString by remember { mutableStateOf("") } // Comma separated topics

    var subjectDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Exam") },
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
                        label = { Text("Subject") },
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
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = examType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Exam Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        ExamType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    examType = type
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dateString,
                    onValueChange = { dateString = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    placeholder = { Text("2026-08-28") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = timeString,
                    onValueChange = { timeString = it },
                    label = { Text("Time (HH:MM)") },
                    placeholder = { Text("10:00") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue") },
                    placeholder = { Text("Hall-A") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxMarks,
                        onValueChange = { maxMarks = it },
                        label = { Text("Max Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetMarks,
                        onValueChange = { targetMarks = it },
                        label = { Text("Target Marks") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = topicsString,
                    onValueChange = { topicsString = it },
                    label = { Text("Topics (Comma separated)") },
                    placeholder = { Text("Arrays, Trees, DP") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val dateParts = dateString.split("-")
                    val year = dateParts.getOrNull(0)?.toIntOrNull() ?: 2026
                    val month = (dateParts.getOrNull(1)?.toIntOrNull() ?: 8) - 1
                    val day = dateParts.getOrNull(2)?.toIntOrNull() ?: 28
                    
                    val cal = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val initialTopicsList = topicsString.split(",").map { it.trim() }

                    onSave(
                        subjects[subjectIndex].id,
                        examType,
                        cal.timeInMillis,
                        timeString,
                        venue,
                        maxMarks.toIntOrNull() ?: 100,
                        targetMarks.toIntOrNull() ?: 75,
                        initialTopicsList
                    )
                },
                enabled = dateString.isNotBlank() && timeString.isNotBlank() && venue.isNotBlank()
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
