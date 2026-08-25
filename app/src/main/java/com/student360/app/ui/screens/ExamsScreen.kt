@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.*
import com.student360.app.data.repository.StudentRepository
import com.student360.app.service.ExamEngine
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.util.*

@Composable
fun ExamsScreen(
    repository: StudentRepository,
    viewModel: ExamsViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val examsWithPrep by viewModel.examsWithPrep.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    val examModeTarget = examsWithPrep.filter { it.daysRemaining in 0..7 }
        .minByOrNull { it.daysRemaining }

    Scaffold(
        containerColor = colors.bg,
        floatingActionButton = {
            if (subjects.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exam")
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Exam Mode Active Urgent Banner
                examModeTarget?.let { target ->
                    val subjectName = subjects.find { it.id == target.exam.subjectId }?.name ?: "Subject"
                    val weakTopics = ExamEngine.getWeakTopics(target.topics)

                    StudentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        backgroundColor = Color(0xFF28161A),
                        borderColor = DangerRed.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(DangerRed.copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🚨", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    "EXAM MODE ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DangerRed,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            StatusBadge(
                                text = ExamEngine.getUrgencyText(target.daysRemaining),
                                color = DangerRed
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "$subjectName (${target.exam.examType.name})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Preparation: ${target.prepPercentage.toInt()}% complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        StudentProgressBar(
                            progress = (target.prepPercentage / 100.0).toFloat().coerceIn(0f, 1f),
                            color = DangerRed,
                            trackColor = SurfaceDark,
                            height = 6.dp
                        )

                        if (weakTopics.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = SurfaceDark,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderDark)
                            ) {
                                Text(
                                    "💡 Recommended Focus: Study '${weakTopics.first().topicName}' today for 45 mins.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PrimaryText,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }

                if (subjects.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.Info,
                        title = "No Subjects Found",
                        subtitle = "Please add subjects first in Attendance tab or Onboarding.",
                        modifier = Modifier.weight(1f)
                    )
                } else if (examsWithPrep.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.DateRange,
                        title = "No Upcoming Exams",
                        subtitle = "Add an exam to set targets and track syllabus preparation.",
                        actionText = "+ Add Exam",
                        onActionClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
        daysLeft <= 1 -> DangerRed
        daysLeft <= 3 -> DangerRed
        daysLeft <= 7 -> WarningOrange
        else -> SuccessGreen
    }

    StudentCard(
        backgroundColor = CardDark,
        borderColor = BorderDark,
        onClick = { expanded = !expanded }
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
                    "$subjectName (${examWithPrep.exam.examType.name})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "📍 ${examWithPrep.exam.venue} • ⏰ ${examWithPrep.exam.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(
                text = ExamEngine.getUrgencyText(daysLeft),
                color = urgencyColor
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Preparation: ${examWithPrep.prepPercentage.toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
            Text(
                "Target: ${examWithPrep.exam.targetMarks} / ${examWithPrep.exam.maxMarks} M",
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        StudentProgressBar(
            progress = (examWithPrep.prepPercentage / 100.0).toFloat().coerceIn(0f, 1f),
            color = urgencyColor,
            trackColor = SurfaceDark,
            height = 6.dp
        )

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Divider(color = BorderDark)

                Text(
                    "Syllabus Checklist:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = LightPurple
                )

                if (examWithPrep.topics.isEmpty()) {
                    Text(
                        "No topics defined yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    examWithPrep.topics.forEach { topic ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                topic.topicName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TopicStatusButton(
                                    label = "Not Started",
                                    active = topic.status == TopicStatus.NOT_STARTED,
                                    activeColor = ElevatedCardDark,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onUpdateTopicStatus(topic, TopicStatus.NOT_STARTED)
                                }
                                TopicStatusButton(
                                    label = "In Progress",
                                    active = topic.status == TopicStatus.IN_PROGRESS,
                                    activeColor = WarningOrange,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onUpdateTopicStatus(topic, TopicStatus.IN_PROGRESS)
                                }
                                TopicStatusButton(
                                    label = "Done",
                                    active = topic.status == TopicStatus.COMPLETED,
                                    activeColor = SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onUpdateTopicStatus(topic, TopicStatus.COMPLETED)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newTopicName,
                        onValueChange = { newTopicName = it },
                        label = { Text("Add Topic") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newTopicName.isNotBlank()) {
                                onAddTopic(newTopicName)
                                newTopicName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardDark),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Exam", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TopicStatusButton(
    label: String,
    active: Boolean,
    activeColor: Color = PrimaryPurple,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (active) activeColor else SurfaceDark,
        border = BorderStroke(1.dp, if (active) activeColor else BorderDark),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (active) Color.White else SecondaryText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Schedule Exam",
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
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        ExamType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name, color = PrimaryText) },
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
                    value = timeString,
                    onValueChange = { timeString = it },
                    label = { Text("Time (HH:MM)") },
                    placeholder = { Text("10:00") },
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
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue (e.g. Hall-A)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = maxMarks,
                        onValueChange = { maxMarks = it },
                        label = { Text("Max Marks") },
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
                        value = targetMarks,
                        onValueChange = { targetMarks = it },
                        label = { Text("Target Marks") },
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

                OutlinedTextField(
                    value = topicsString,
                    onValueChange = { topicsString = it },
                    label = { Text("Topics (Comma separated)") },
                    placeholder = { Text("Arrays, Trees, DP") },
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp),
                enabled = dateString.isNotBlank() && timeString.isNotBlank() && venue.isNotBlank()
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
