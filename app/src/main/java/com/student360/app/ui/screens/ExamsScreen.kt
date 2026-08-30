@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.*
import com.student360.app.data.repository.StudentRepository
import com.student360.app.service.ExamEngine
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExamsScreen(
    repository: StudentRepository,
    viewModel: ExamsViewModel = viewModel(),
    onNavigateToStudy: (subjectId: Int, topic: String, durationMins: Int) -> Unit = { _, _, _ -> }
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val examsWithPrep by viewModel.examsWithPrep.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val activeSubjects = remember(subjects) { subjects.filter { !it.isArchived } }

    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var selectedExamToEdit by remember { mutableStateOf<Exam?>(null) }
    var examToDelete by remember { mutableStateOf<Exam?>(null) }

    Scaffold(
        containerColor = colors.bg,
        floatingActionButton = {
            // Requirement 4: Only show FAB if exams already exist
            if (activeSubjects.isNotEmpty() && examsWithPrep.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        selectedExamToEdit = null
                        showAddOrEditDialog = true
                    },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Clean Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Exams",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (examsWithPrep.isEmpty()) "No scheduled exams" else "${examsWithPrep.size} scheduled exam${if (examsWithPrep.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }

                    if (examsWithPrep.isNotEmpty()) {
                        Surface(
                            color = colors.accent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Target & Prep",
                                color = colors.accent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (activeSubjects.isEmpty()) {
                    // No subjects registered yet
                    EmptyStateView(
                        icon = Icons.Default.Info,
                        title = "No Subjects Found",
                        subtitle = "Please add your subjects first in the Subjects tab.",
                        modifier = Modifier.weight(1f)
                    )
                } else if (examsWithPrep.isEmpty()) {
                    // Requirement 5: Standard Clean Empty State with Single Primary Add Action
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        StudentCard(
                            backgroundColor = colors.card,
                            borderColor = colors.border,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(64.dp),
                                    shape = CircleShape,
                                    color = colors.accent.copy(alpha = 0.12f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = colors.accent,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "No upcoming exams",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )

                                Text(
                                    text = "Add your next exam to track\npreparation, targets and syllabus.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = {
                                        selectedExamToEdit = null
                                        showAddOrEditDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Add Exam",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // List of Exam Cards
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(examsWithPrep, key = { it.exam.id }) { item ->
                            val sub = subjects.find { it.id == item.exam.subjectId }
                            CleanExamCard(
                                examWithPrep = item,
                                subjectName = sub?.name ?: "Subject",
                                onToggleTopic = { topic -> viewModel.toggleTopicStatus(topic) },
                                onAddTopic = { name -> viewModel.addTopicToExam(item.exam.id, name) },
                                onDeleteTopic = { topic -> viewModel.deleteTopic(topic) },
                                onEditExam = {
                                    selectedExamToEdit = item.exam
                                    showAddOrEditDialog = true
                                },
                                onDeleteExam = {
                                    examToDelete = item.exam
                                },
                                onPlanStudy = {
                                    val duration = when {
                                        item.daysRemaining <= 3 -> 60
                                        item.daysRemaining <= 7 -> 45
                                        else -> 30
                                    }
                                    Toast.makeText(context, "Starting ${duration}m study for ${sub?.name ?: "Exam"}", Toast.LENGTH_SHORT).show()
                                    onNavigateToStudy(item.exam.subjectId, "${item.exam.examType.displayName} Prep", duration)
                                }
                            )
                        }
                    }
                }
            }

            // Add or Edit Exam Dialog (Responsive, scrollable, native pickers, validation)
            if (showAddOrEditDialog && activeSubjects.isNotEmpty()) {
                AddOrEditExamDialog(
                    existingExam = selectedExamToEdit,
                    subjects = activeSubjects,
                    onDismiss = { showAddOrEditDialog = false },
                    onSave = { examId, subId, type, date, time, venue, maxMarks, targetMarks, newTopics ->
                        if (examId == null) {
                            viewModel.addExam(
                                subjectId = subId,
                                examType = type,
                                date = date,
                                time = time,
                                venue = venue,
                                maxMarks = maxMarks,
                                targetMarks = targetMarks,
                                initialTopics = newTopics
                            )
                            Toast.makeText(context, "Exam scheduled", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateExam(
                                exam = Exam(
                                    id = examId,
                                    subjectId = subId,
                                    examType = type,
                                    date = date,
                                    time = time,
                                    venue = venue,
                                    maxMarks = maxMarks,
                                    targetMarks = targetMarks
                                ),
                                newTopics = newTopics
                            )
                            Toast.makeText(context, "Exam updated", Toast.LENGTH_SHORT).show()
                        }
                        showAddOrEditDialog = false
                    }
                )
            }

            // Confirm Delete Dialog
            examToDelete?.let { exam ->
                val subName = subjects.find { it.id == exam.subjectId }?.name ?: "this exam"
                AlertDialog(
                    onDismissRequest = { examToDelete = null },
                    containerColor = colors.card,
                    title = { Text("Delete Exam?", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            "Are you sure you want to remove the ${exam.examType.displayName} exam for $subName? All syllabus progress will be removed.",
                            color = colors.textSecondary
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteExam(exam)
                                examToDelete = null
                                Toast.makeText(context, "Exam removed", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { examToDelete = null }) {
                            Text("Cancel", color = colors.textSecondary)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Requirement 6 & 12: Detailed, clean Exam Card with Countdown, Target Marks,
 * Preparation progress bar, and Syllabus checklist.
 */
@Composable
fun CleanExamCard(
    examWithPrep: ExamWithPrep,
    subjectName: String,
    onToggleTopic: (ExamTopic) -> Unit,
    onAddTopic: (String) -> Unit,
    onDeleteTopic: (ExamTopic) -> Unit,
    onEditExam: () -> Unit,
    onDeleteExam: () -> Unit,
    onPlanStudy: () -> Unit
) {
    val colors = LocalAppColors.current
    val exam = examWithPrep.exam
    val daysLeft = examWithPrep.daysRemaining
    var isExpanded by remember { mutableStateOf(false) }
    var newTopicText by remember { mutableStateOf("") }

    val countdownText = ExamEngine.getCountdownText(daysLeft)
    val countdownColor = when {
        daysLeft < 0 -> colors.textSecondary
        daysLeft == 0 -> colors.danger
        daysLeft <= 3 -> colors.danger
        daysLeft <= 7 -> colors.warning
        else -> colors.accent
    }

    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(exam.date) { dateFormatter.format(Date(exam.date)) }

    val prepInt = examWithPrep.prepPercentage.toInt()
    val prepColor = when {
        prepInt >= 75 -> colors.success
        prepInt >= 40 -> colors.warning
        else -> colors.accent
    }

    val recommendedDailyStudy = when {
        daysLeft <= 3 -> "60 min/day"
        daysLeft <= 7 -> "45 min/day"
        else -> "30 min/day"
    }

    StudentCard(
        backgroundColor = colors.card,
        borderColor = if (daysLeft in 0..3) colors.danger.copy(alpha = 0.5f) else colors.border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Subject, Exam Type Pill, and Countdown Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = colors.accent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = exam.examType.displayName.uppercase(Locale.getDefault()),
                                color = colors.accent,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        if (exam.time.isNotBlank()) {
                            Text("•", color = colors.textSecondary.copy(alpha = 0.5f), fontSize = 12.sp)
                            Text(
                                text = exam.time,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        if (exam.venue.isNotBlank()) {
                            Text("•", color = colors.textSecondary.copy(alpha = 0.5f), fontSize = 12.sp)
                            Text(
                                text = exam.venue,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Countdown Pill
                Surface(
                    color = countdownColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, countdownColor.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = countdownText,
                        color = countdownColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(color = colors.border.copy(alpha = 0.6f))

            // Targets & Preparation Metric
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target: ${exam.targetMarks} / ${exam.maxMarks} M (${if (exam.maxMarks > 0) ((exam.targetMarks.toDouble() / exam.maxMarks) * 100).toInt() else 0}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                Text(
                    text = "Preparation: $prepInt%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = prepColor
                )
            }

            // Preparation Progress Bar
            StudentProgressBar(
                progress = (examWithPrep.prepPercentage / 100.0).toFloat().coerceIn(0f, 1f),
                color = prepColor,
                trackColor = colors.border.copy(alpha = 0.4f),
                height = 6.dp
            )

            // Topics Syllabus Checklist Summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Topics (${examWithPrep.completedTopicsCount}/${examWithPrep.totalTopicsCount} completed)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = if (isExpanded) "Hide" else "View Syllabus",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Expanded Syllabus Topics
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (examWithPrep.topics.isEmpty()) {
                        Text(
                            text = "No topics added yet. Add key syllabus units below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary.copy(alpha = 0.8f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        examWithPrep.topics.forEach { topic ->
                            val isCompleted = topic.status == TopicStatus.COMPLETED
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleTopic(topic) }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Custom Circular / Check Icon
                                    Surface(
                                        modifier = Modifier.size(20.dp),
                                        shape = CircleShape,
                                        color = if (isCompleted) colors.success else Color.Transparent,
                                        border = BorderStroke(1.5.dp, if (isCompleted) colors.success else colors.textSecondary.copy(alpha = 0.5f))
                                    ) {
                                        if (isCompleted) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = "Completed",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = topic.topicName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isCompleted) colors.textSecondary else colors.textPrimary,
                                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteTopic(topic) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete topic",
                                        tint = colors.textSecondary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Add Topic Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newTopicText,
                            onValueChange = { newTopicText = it },
                            placeholder = { Text("Add topic (e.g. Unit 3 Trees)") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = {
                                if (newTopicText.isNotBlank()) {
                                    onAddTopic(newTopicText.trim())
                                    newTopicText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(8.dp),
                            enabled = newTopicText.isNotBlank()
                        ) {
                            Text("+ Add", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Study Plan Recommendation & Action Row
            if (daysLeft >= 0) {
                Surface(
                    color = colors.bg,
                    border = BorderStroke(1.dp, colors.border.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 $recommendedDailyStudy prep",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1
                        )

                        TextButton(
                            onClick = onPlanStudy,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Start Session",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Card Footer: Edit and Delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onEditExam,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit Exam", color = colors.accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteExam,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Exam",
                        tint = colors.danger.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Requirements 2, 3, 7, 8: Responsive Add/Edit Exam Dialog with:
 * - Native Android DatePickerDialog and TimePickerDialog
 * - Subject & ExamType dropdowns
 * - Max & Target marks numeric validation (Target <= Max)
 * - Dynamic topic additions
 * - Fixed/Sticky Save and Cancel buttons that never overflow or get hidden by the keyboard.
 */
@Composable
fun AddOrEditExamDialog(
    existingExam: Exam?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (examId: Int?, subjectId: Int, type: ExamType, date: Long, time: String, venue: String, maxMarks: Int, targetMarks: Int, newTopics: List<String>) -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    val isEditing = existingExam != null

    var selectedSubjectId by remember {
        mutableStateOf(existingExam?.subjectId ?: subjects.firstOrNull()?.id ?: 0)
    }
    var selectedExamType by remember {
        mutableStateOf(existingExam?.examType ?: ExamType.MIDTERM)
    }
    var selectedDateMillis by remember {
        mutableStateOf(existingExam?.date ?: (System.currentTimeMillis() + 7 * 24 * 3600 * 1000L))
    }
    var timeString by remember { mutableStateOf(existingExam?.time ?: "10:00") }
    var venueString by remember { mutableStateOf(existingExam?.venue ?: "") }
    var maxMarksString by remember { mutableStateOf(existingExam?.maxMarks?.toString() ?: "100") }
    var targetMarksString by remember { mutableStateOf(existingExam?.targetMarks?.toString() ?: "75") }

    var topicInput by remember { mutableStateOf("") }
    val initialTopics = remember { mutableStateListOf<String>() }

    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var showDatePickerModal by remember { mutableStateOf(false) }
    var showTimePickerModal by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val friendlyDateText = remember(selectedDateMillis) {
        if (selectedDateMillis > 0) dateFormatter.format(Date(selectedDateMillis)) else "Select Date"
    }

    // Validation
    val maxMarksInt = maxMarksString.toIntOrNull()
    val targetMarksInt = targetMarksString.toIntOrNull()

    val isMaxMarksValid = maxMarksInt != null && maxMarksInt > 0
    val isTargetMarksValid = targetMarksInt != null && targetMarksInt >= 0 && (maxMarksInt == null || targetMarksInt <= maxMarksInt)

    val maxMarksError = when {
        maxMarksString.isNotBlank() && !isMaxMarksValid -> "Max marks must be greater than 0"
        else -> null
    }

    val targetMarksError = when {
        targetMarksString.isNotBlank() && maxMarksInt != null && targetMarksInt != null && targetMarksInt > maxMarksInt -> "Target marks cannot exceed max marks ($maxMarksInt)"
        targetMarksString.isNotBlank() && targetMarksInt != null && targetMarksInt < 0 -> "Target marks cannot be negative"
        else -> null
    }

    val canSave = selectedSubjectId > 0 && selectedDateMillis > 0 && isMaxMarksValid && isTargetMarksValid
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.card,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
                // Dialog Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Exam" else "Schedule Exam",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.textSecondary)
                    }
                }

                Divider(color = colors.border)

                // Scrollable Form Body (Responsive across all screen sizes)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Subject Dropdown
                    val currentSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.firstOrNull()
                    ExposedDropdownMenuBox(
                        expanded = subjectMenuExpanded,
                        onExpandedChange = { subjectMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = currentSubject?.name ?: "Select Subject",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subjectMenuExpanded,
                            onDismissRequest = { subjectMenuExpanded = false },
                            modifier = Modifier.background(colors.card)
                        ) {
                            subjects.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name, color = colors.textPrimary) },
                                    onClick = {
                                        selectedSubjectId = sub.id
                                        subjectMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Exam Type Dropdown
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedExamType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Exam Type *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuExpanded) },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false },
                            modifier = Modifier.background(colors.card)
                        ) {
                            ExamType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName, color = colors.textPrimary) },
                                    onClick = {
                                        selectedExamType = type
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Date & Time Row (Native pickers on tap)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date Picker Field
                        Surface(
                            onClick = {
                                android.util.Log.d("STUDENT360_DEBUG", "Date Surface clicked!")
                                showDatePickerModal = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = friendlyDateText,
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Date *") },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        android.util.Log.d("STUDENT360_DEBUG", "Date Icon clicked!")
                                        showDatePickerModal = true
                                    }) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = "Pick Date",
                                            tint = colors.accent
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = colors.border,
                                    disabledTextColor = colors.textPrimary,
                                    disabledLabelColor = colors.textSecondary,
                                    disabledTrailingIconColor = colors.accent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Time Picker Field (Optional)
                        Surface(
                            onClick = { showTimePickerModal = true },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            modifier = Modifier.weight(0.9f)
                        ) {
                            OutlinedTextField(
                                value = timeString.ifBlank { "Anytime" },
                                onValueChange = {},
                                enabled = false,
                                label = { Text("Time") },
                                trailingIcon = {
                                    IconButton(onClick = { showTimePickerModal = true }) {
                                        Icon(
                                            Icons.Default.Notifications,
                                            contentDescription = "Pick Time",
                                            tint = colors.accent
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledBorderColor = colors.border,
                                    disabledTextColor = colors.textPrimary,
                                    disabledLabelColor = colors.textSecondary,
                                    disabledTrailingIconColor = colors.accent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 4. Venue Field
                    OutlinedTextField(
                        value = venueString,
                        onValueChange = { venueString = it },
                        label = { Text("Venue / Room (Optional)") },
                        placeholder = { Text("e.g. Hall-A, Lab 2") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 5. Max Marks and Target Marks with Validation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = maxMarksString,
                            onValueChange = { maxMarksString = it.filter { char -> char.isDigit() } },
                            label = { Text("Max Marks *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = maxMarksError != null,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = targetMarksString,
                            onValueChange = { targetMarksString = it.filter { char -> char.isDigit() } },
                            label = { Text("Target Marks *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = targetMarksError != null,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Inline Validation Errors
                    if (maxMarksError != null) {
                        Text(
                            text = maxMarksError,
                            color = colors.danger,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (targetMarksError != null) {
                        Text(
                            text = targetMarksError,
                            color = colors.danger,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // 6. Topics (Syllabus)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Syllabus / Topics (Optional)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = topicInput,
                                onValueChange = { topicInput = it },
                                placeholder = { Text("e.g. Unit 1: Arrays") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.border,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    if (topicInput.isNotBlank()) {
                                        initialTopics.add(topicInput.trim())
                                        topicInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = RoundedCornerShape(10.dp),
                                enabled = topicInput.isNotBlank()
                            ) {
                                Text("+ Add", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // List of queued topics to be added
                        if (initialTopics.isNotEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                initialTopics.forEachIndexed { index, topic ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.bg, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• $topic",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.textPrimary
                                        )
                                        IconButton(
                                            onClick = { initialTopics.removeAt(index) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove topic",
                                                tint = colors.danger,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = colors.border)

                // Sticky Dialog Footer (Cancel & Save buttons always reachable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            if (canSave) {
                                onSave(
                                    existingExam?.id,
                                    selectedSubjectId,
                                    selectedExamType,
                                    selectedDateMillis,
                                    timeString.trim(),
                                    venueString.trim(),
                                    maxMarksInt ?: 100,
                                    targetMarksInt ?: 75,
                                    initialTopics.toList()
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(10.dp),
                        enabled = canSave
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Save Exam",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

    if (showDatePickerModal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (selectedDateMillis > 0) selectedDateMillis else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerModal = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePickerModal = false
                    }
                ) {
                    Text("OK", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerModal = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = colors.card
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = colors.textPrimary,
                    headlineContentColor = colors.textPrimary,
                    selectedDayContainerColor = colors.accent,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = colors.accent,
                    todayContentColor = colors.accent
                )
            )
        }
    }

    if (showTimePickerModal) {
        val cal = Calendar.getInstance()
        var hour = cal.get(Calendar.HOUR_OF_DAY)
        var minute = cal.get(Calendar.MINUTE)
        if (timeString.contains(":")) {
            val parts = timeString.split(":")
            hour = parts.getOrNull(0)?.toIntOrNull() ?: hour
            minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: minute
        }
        val timePickerState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePickerModal = false },
            containerColor = colors.card,
            title = { Text("Select Time", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = colors.bg,
                            selectorColor = colors.accent,
                            periodSelectorBorderColor = colors.border,
                            periodSelectorSelectedContainerColor = colors.accent.copy(alpha = 0.2f),
                            periodSelectorSelectedContentColor = colors.accent,
                            timeSelectorSelectedContainerColor = colors.accent.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = colors.accent
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeString = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                        showTimePickerModal = false
                    }
                ) {
                    Text("OK", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerModal = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
    }
}
