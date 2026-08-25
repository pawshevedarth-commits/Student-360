@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.Goal
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StudyScreen(
    repository: StudentRepository,
    viewModel: StudyViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val subjects by viewModel.subjects.collectAsState()
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

    val formattedTime = remember(timerSeconds) {
        val h = timerSeconds / 3600
        val m = (timerSeconds % 3600) / 60
        val s = timerSeconds % 60
        String.format("%02d:%02d:%02d", h, m, s)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Study Timer Stopwatch Hero Widget
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = if (timerRunning) PrimaryPurple.copy(alpha = 0.6f) else BorderDark
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "STUDY TIMER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = LightPurple
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (timerRunning) LightPurple else PrimaryText
                    )

                    // Active subject badge
                    if (timerSubjectId != null) {
                        val subName = subjects.find { it.id == timerSubjectId }?.name ?: "Subject"
                        val topicText = if (!timerTopic.isNullOrBlank()) " • $timerTopic" else ""
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ElevatedCardDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Text(
                                "📚 $subName$topicText",
                                style = MaterialTheme.typography.labelSmall,
                                color = LightPurple,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    if (!timerRunning && timerSeconds == 0) {
                        if (subjects.isNotEmpty()) {
                            Button(
                                onClick = { showTimerSetup = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Session", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                "Add subjects first in Attendance to track study sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (timerRunning) {
                                    Button(
                                        onClick = { viewModel.pauseTimer() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardDark),
                                        border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("Pause", color = WarningOrange, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.resumeTimer() },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Resume", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = { viewModel.stopAndSaveTimer() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Session", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            TextButton(
                                onClick = { viewModel.discardTimer() },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Discard Session", color = DangerRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        // Live stats aggregates (Today, Week, Month)
        item {
            SectionHeader(title = "Study Statistics")
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudyStatCard("Today", "${stats.todayMins} min", modifier = Modifier.weight(1f))
                StudyStatCard("This Week", "${String.format("%.1f", stats.weekHours)} hrs", modifier = Modifier.weight(1f))
                StudyStatCard("This Month", "${String.format("%.1f", stats.monthHours)} hrs", modifier = Modifier.weight(1f))
            }
        }

        // Academic Goals list
        item {
            SectionHeader(
                title = "My Goals",
                actionText = "+ Add Goal",
                onActionClick = { showAddGoalDialog = true }
            )
        }

        if (goals.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.Default.Star,
                    title = "No Academic Goals",
                    subtitle = "Set study goals like 'Complete DBMS Unit 2' to stay focused and track progress.",
                    actionText = "+ Add Goal",
                    onActionClick = { showAddGoalDialog = true }
                )
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
            containerColor = SurfaceDark,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = {
                Text(
                    "Configure Study Session",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
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
                        label = { Text("Topic Name (e.g. Unit 2 - SQL Queries)") },
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
                        viewModel.startTimer(subjects[selectedSubjectIndex].id, topicNameInput)
                        showTimerSetup = false
                        topicNameInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Start", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimerSetup = false }) {
                    Text("Cancel", color = SecondaryText)
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
            containerColor = SurfaceDark,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = {
                Text(
                    "Add Academic Goal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal Title (e.g. Complete DBMS Unit 2)") },
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
                        value = goalTarget,
                        onValueChange = { goalTarget = it },
                        label = { Text("Target count (e.g. 100)") },
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
                    OutlinedTextField(
                        value = goalDueDays,
                        onValueChange = { goalDueDays = it },
                        label = { Text("Time limit (Days)") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(10.dp),
                    enabled = goalTitle.isNotBlank()
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel", color = SecondaryText)
                }
            }
        )
    }

    // Edit Goal Progress Dialog
    editGoalProgressTarget?.let { goal ->
        var progressInput by remember { mutableStateOf(goal.currentProgress.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { editGoalProgressTarget = null },
            containerColor = SurfaceDark,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText,
            title = {
                Text(
                    "Update Progress",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Goal: ${goal.title}", style = MaterialTheme.typography.bodyMedium, color = SecondaryText)
                    OutlinedTextField(
                        value = progressInput,
                        onValueChange = { progressInput = it },
                        label = { Text("Current count") },
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
                        val prog = progressInput.toDoubleOrNull() ?: goal.currentProgress
                        viewModel.updateGoalProgress(goal, prog)
                        editGoalProgressTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editGoalProgressTarget = null }) {
                    Text("Cancel", color = SecondaryText)
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
    StudentCard(
        modifier = modifier,
        backgroundColor = CardDark,
        borderColor = BorderDark
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
    val pct = (progressFraction * 100).toInt()
    val formattedDeadline = remember(goal.deadline) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(goal.deadline))
    }

    StudentCard(
        backgroundColor = CardDark,
        borderColor = BorderDark
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    goal.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = PrimaryText,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(
                    text = "$pct%",
                    color = if (pct >= 100) SuccessGreen else PrimaryPurple
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Goal",
                        tint = SecondaryText.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            StudentProgressBar(
                progress = progressFraction,
                color = if (pct >= 100) SuccessGreen else PrimaryPurple,
                trackColor = SurfaceDark,
                height = 8.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${goal.currentProgress.toInt()} / ${goal.target.toInt()} completed • Due $formattedDeadline",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Button(
                    onClick = onUpdateProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardDark),
                    border = BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Update",
                        style = MaterialTheme.typography.labelSmall,
                        color = LightPurple,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
