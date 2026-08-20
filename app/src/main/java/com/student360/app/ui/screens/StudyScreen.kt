package com.student360.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.Goal
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.theme.SafeGreen
import com.student360.app.ui.theme.SafeGreenLight
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    repository: StudentRepository,
    viewModel: StudyViewModel = viewModel()
) {
    val subjects by viewModel.subjects.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val goals by viewModel.goals.collectAsState()

    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val timerRunning by viewModel.timerRunning.collectAsState()
    val timerSubjectId by viewModel.timerSubjectId.collectAsState()
    val timerTopic by viewModel.timerTopic.collectAsState()

    var showTimerSetup by remember { mutableStateOf(false) }
    var selectedSubjectIndex by remember { mutableStateOf(0) }
    var topicNameInput by remember { mutableStateOf("") }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var editGoalProgressTarget by remember { mutableStateOf<Goal?>(null) }

    val stats = viewModel.getStudyStats()

    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    // Helper: Formats seconds to hh:mm:ss
    val formattedTime = remember(timerSeconds) {
        val h = timerSeconds / 3600
        val m = (timerSeconds % 3600) / 60
        val s = timerSeconds % 60
        String.format("%02d:%02d:%02d", h, m, s)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Study Timer Stopwatch Widget
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Study Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        formattedTime,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (timerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // If active, show subject details
                    if (timerSubjectId != null) {
                        val subName = subjects.find { it.id == timerSubjectId }?.name ?: "Subject"
                        Text(
                            "Studying: $subName • $timerTopic",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!timerRunning && timerSeconds == 0) {
                            if (subjects.isNotEmpty()) {
                                Button(onClick = { showTimerSetup = true }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start Session")
                                }
                            } else {
                                Text("Add subjects first to track study timer.")
                            }
                        } else {
                            if (timerRunning) {
                                Button(onClick = { viewModel.pauseTimer() }) {
                                    Text("Pause")
                                }
                            } else {
                                Button(onClick = { viewModel.resumeTimer() }) {
                                    Text("Resume")
                                }
                            }
                            Button(onClick = { viewModel.stopAndSaveTimer() }, colors = ButtonDefaults.buttonColors(containerColor = SafeGreen)) {
                                Text("Save")
                            }
                            TextButton(onClick = { viewModel.discardTimer() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                Text("Discard")
                            }
                        }
                    }
                }
            }
        }

        // Live stats aggregates (Today, Week, Month)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StudyStatCard("Today", "${stats.todayMins}m", modifier = Modifier.weight(1f))
                StudyStatCard("This Week", "${String.format("%.1f", stats.weekHours)}h", modifier = Modifier.weight(1f))
                StudyStatCard("This Month", "${String.format("%.1f", stats.monthHours)}h", modifier = Modifier.weight(1f))
            }
        }

        // Academic Goals list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAddGoalDialog = true }) {
                    Text("+ Add Goal")
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Text("No goals set yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(goals) { goal ->
                GoalCard(
                    goal = goal,
                    onUpdateProgress = { editGoalProgressTarget = goal },
                    onDelete = { viewModel.deleteGoal(goal) }
                )
            }
        }
    }

    // Start Timer Setup Dialog
    if (showTimerSetup && subjects.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showTimerSetup = false },
            title = { Text("Configure Study Session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = subjectDropdownExpanded,
                        onExpandedChange = { subjectDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = subjects[selectedSubjectIndex].name,
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
                                        selectedSubjectIndex = index
                                        subjectDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = topicNameInput,
                        onValueChange = { topicNameInput = it },
                        label = { Text("Topic Name") },
                        placeholder = { Text("e.g. Binary Trees") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.startTimer(subjects[selectedSubjectIndex].id, topicNameInput)
                        showTimerSetup = false
                        topicNameInput = ""
                    }
                ) {
                    Text("Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimerSetup = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var goalTitle by remember { mutableStateOf("") }
        var goalTarget by remember { mutableStateOf("") }
        var goalDueDays by remember { mutableStateOf("14") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Add Academic Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = goalTitle, onValueChange = { goalTitle = it }, label = { Text("Goal Description") }, placeholder = { Text("Complete 100 DSA Problems") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = goalTarget, onValueChange = { goalTarget = it }, label = { Text("Target count") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = goalDueDays, onValueChange = { goalDueDays = it }, label = { Text("Time limit (Days)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (goalTitle.isNotBlank()) {
                            viewModel.addGoal(
                                goalTitle,
                                goalTarget.toDoubleOrNull() ?: 1.0,
                                0.0,
                                goalDueDays.toIntOrNull() ?: 14
                            )
                            showAddGoalDialog = false
                        }
                    },
                    enabled = goalTitle.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Goal Progress Dialog
    editGoalProgressTarget?.let { goal ->
        var progressInput by remember { mutableStateOf(goal.currentProgress.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { editGoalProgressTarget = null },
            title = { Text("Update Progress") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Goal: ${goal.title}")
                    OutlinedTextField(
                        value = progressInput,
                        onValueChange = { progressInput = it },
                        label = { Text("Current count") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prog = progressInput.toDoubleOrNull() ?: goal.currentProgress
                        viewModel.updateGoalProgress(goal, prog)
                        editGoalProgressTarget = null
                    }
                ) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { editGoalProgressTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudyStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    onUpdateProgress: () -> Unit,
    onDelete: () -> Unit
) {
    val progressFraction = (goal.currentProgress / goal.target).toFloat().coerceIn(0f, 1f)
    val formattedDeadline = remember(goal.deadline) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(goal.deadline))
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(goal.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Goal", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Progress: ${goal.currentProgress.toInt()} / ${goal.target.toInt()}", style = MaterialTheme.typography.bodySmall)
                Text("Due: $formattedDeadline", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progressFraction,
                modifier = Modifier.fillMaxWidth(),
                color = SafeGreen,
                trackColor = SafeGreenLight
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onUpdateProgress, modifier = Modifier.align(Alignment.End)) {
                Text("Update Progress", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
