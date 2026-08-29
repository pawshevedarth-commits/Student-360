@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
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
    viewModel: StudyViewModel = viewModel(),
    onNavigateToProgress: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val subjects by viewModel.subjects.collectAsState()
    val goals by viewModel.goals.collectAsState()

    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val timerRunning by viewModel.timerRunning.collectAsState()
    val timerSubjectId by viewModel.timerSubjectId.collectAsState()
    val timerTopic by viewModel.timerTopic.collectAsState()

    var selectedSubjectIndex by remember { mutableStateOf(0) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showCustomDurationDialog by remember { mutableStateOf(false) }
    var customDurationInput by remember { mutableStateOf("30") }
    var editGoalProgressTarget by remember { mutableStateOf<Goal?>(null) }
    var sessionCompletionInfo by remember { mutableStateOf<Pair<String, Int>?>(null) }

    val stats = viewModel.getStudyStats()

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
                backgroundColor = colors.card,
                borderColor = if (timerRunning) colors.accent.copy(alpha = 0.7f) else colors.border,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Subject Selector & Duration Before Starting
                    if (!timerRunning && timerSeconds == 0) {
                        if (subjects.isNotEmpty()) {
                            Text(
                                "What are you studying?",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.elevatedCard,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.clickable { subjectDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val selectedSubName = subjects[selectedSubjectIndex.coerceIn(0, subjects.lastIndex)].name
                                        Text(
                                            text = selectedSubName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary,
                                            fontSize = 14.sp
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Select Subject",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                DropdownMenu(
                                    expanded = subjectDropdownExpanded,
                                    onDismissRequest = { subjectDropdownExpanded = false },
                                    modifier = Modifier.background(colors.card)
                                ) {
                                    subjects.forEachIndexed { index, sub ->
                                        DropdownMenuItem(
                                            text = { Text(sub.name, color = colors.textPrimary) },
                                            onClick = {
                                                selectedSubjectIndex = index
                                                subjectDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "Duration:",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val targetDuration by viewModel.targetDurationMins.collectAsState()
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(25, 45, 60).forEach { mins ->
                                    val isSelected = targetDuration == mins
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) colors.accent else colors.elevatedCard,
                                        border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                                        modifier = Modifier.clickable { viewModel.setTargetDuration(mins) }
                                    ) {
                                        Text(
                                            text = "$mins min",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else colors.textPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                val isCustom = targetDuration !in listOf(25, 45, 60)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCustom) colors.accent else colors.elevatedCard,
                                    border = BorderStroke(1.dp, if (isCustom) colors.accent else colors.border),
                                    modifier = Modifier.clickable { showCustomDurationDialog = true }
                                ) {
                                    Text(
                                        text = if (isCustom) "$targetDuration min" else "Custom",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCustom) Color.White else colors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    } else if (timerSubjectId != null) {
                        val subName = subjects.find { it.id == timerSubjectId }?.name ?: "Subject"
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.accent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.3f))
                        ) {
                            Text(
                                "📚 $subName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        "STUDY TIMER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        letterSpacing = 1.sp
                    )
                    if (timerTopic.isNotBlank()) {
                        Text(
                            text = timerTopic,
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 38.sp,
                        color = if (timerRunning) colors.accent else colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!timerRunning && timerSeconds == 0) {
                        if (subjects.isNotEmpty()) {
                            Button(
                                onClick = {
                                    val sub = subjects[selectedSubjectIndex.coerceIn(0, subjects.lastIndex)]
                                    viewModel.startTimer(sub.id, "")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Start Session", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            Text(
                                "Add subjects first in Attendance to track study sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
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
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.elevatedCard),
                                        border = BorderStroke(1.dp, colors.warning.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("⏸ Pause", color = colors.warning, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.resumeTimer() },
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 12.dp)
                                    ) {
                                        Text("▶ Resume", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = {
                                        val lastSecs = timerSeconds
                                        val lastSubId = timerSubjectId
                                        val subName = subjects.find { it.id == lastSubId }?.name ?: "Subject"
                                        val mins = (lastSecs / 60).coerceAtLeast(1)
                                        viewModel.stopAndSaveTimer()
                                        sessionCompletionInfo = subName to mins
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Text("■ End Session", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live stats aggregates (Today, Week, Month)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Study Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                if (onNavigateToProgress != null) {
                    Text(
                        "View Analytics →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accent,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onNavigateToProgress() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StudyStatCard("Today", "${stats.todayMins} min", modifier = Modifier.weight(1f))
                StudyStatCard("This Week", "${String.format(Locale.US, "%.1f", stats.weekHours)} hrs", modifier = Modifier.weight(1f))
                StudyStatCard("This Month", "${String.format(Locale.US, "%.1f", stats.monthHours)} hrs", modifier = Modifier.weight(1f))
            }
        }

        // Subject-wise Study Time Breakdown
        if (subjects.isNotEmpty()) {
            item {
                SectionHeader(title = "Subject-wise Study Time")
            }
            items(subjects) { sub ->
                val subMins = viewModel.getStudyTimeForSubject(sub.id)
                val hours = subMins / 60
                val mins = subMins % 60
                val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }
                }
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
                StudentCard(
                    backgroundColor = colors.elevatedCard,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🎯", fontSize = 28.sp)
                        Text(
                            "Set your first academic goal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            "Complete DBMS Unit 2",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { showAddGoalDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Goal", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        }
                    }
                }
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

    // Session Completion Dialog
    if (sessionCompletionInfo != null) {
        val (subName, mins) = sessionCompletionInfo!!
        AlertDialog(
            onDismissRequest = { sessionCompletionInfo = null },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textSecondary,
            title = {
                Text(
                    "Session Complete",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "$subName · $mins min",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        fontSize = 16.sp
                    )
                    Text(
                        "Your study statistics have been updated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { sessionCompletionInfo = null },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Custom Duration Dialog
    if (showCustomDurationDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDurationDialog = false },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            title = {
                Text(
                    "Custom Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter duration in minutes:",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = customDurationInput,
                        onValueChange = { customDurationInput = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = customDurationInput.toIntOrNull() ?: 30
                        if (mins > 0) {
                            viewModel.setTargetDuration(mins)
                        }
                        showCustomDurationDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Set Duration", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDurationDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var goalTitle by remember { mutableStateOf("") }
        var goalTarget by remember { mutableStateOf("10") }
        var goalDueDays by remember { mutableStateOf("14") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textPrimary,
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
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = goalTarget,
                        onValueChange = { goalTarget = it },
                        label = { Text("Target count (e.g. 10)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
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
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
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
                                goalTarget.toDoubleOrNull() ?: 10.0,
                                0.0,
                                goalDueDays.toIntOrNull() ?: 14
                            )
                            showAddGoalDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(10.dp),
                    enabled = goalTitle.isNotBlank()
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Edit Goal Progress Target Dialog
    if (editGoalProgressTarget != null) {
        val g = editGoalProgressTarget!!
        var progressInput by remember { mutableStateOf(g.currentProgress.toInt().toString()) }

        AlertDialog(
            onDismissRequest = { editGoalProgressTarget = null },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textPrimary,
            title = {
                Text("Update Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(g.title, fontWeight = FontWeight.SemiBold, color = colors.accent, fontSize = 14.sp)
                    OutlinedTextField(
                        value = progressInput,
                        onValueChange = { progressInput = it },
                        label = { Text("Completed (out of ${g.target.toInt()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newProgress = progressInput.toDoubleOrNull() ?: g.currentProgress
                        viewModel.updateGoalProgress(g, newProgress)
                        editGoalProgressTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Update", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editGoalProgressTarget = null }) {
                    Text("Cancel", color = colors.textSecondary)
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
    val colors = LocalAppColors.current
    StudentCard(
        modifier = modifier,
        backgroundColor = colors.card,
        borderColor = colors.border
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 16.sp,
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
    val colors = LocalAppColors.current
    val progressFraction = (goal.currentProgress / goal.target).toFloat().coerceIn(0f, 1f)
    val pct = (progressFraction * 100).toInt()
    val formattedDeadline = remember(goal.deadline) {
        SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(goal.deadline))
    }

    StudentCard(
        backgroundColor = colors.card,
        borderColor = colors.border,
        modifier = Modifier.fillMaxWidth()
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
                    fontSize = 16.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(
                    text = "$pct%",
                    color = if (pct >= 100) colors.success else colors.accent
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Goal",
                        tint = colors.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            StudentProgressBar(
                progress = progressFraction,
                color = if (pct >= 100) colors.success else colors.accent,
                trackColor = colors.border.copy(alpha = 0.4f),
                height = 8.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${goal.currentProgress.toInt()} / ${goal.target.toInt()} tasks completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        "Due $formattedDeadline",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        fontSize = 11.sp
                    )
                }
                Button(
                    onClick = onUpdateProgress,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.elevatedCard),
                    border = BorderStroke(1.dp, colors.border),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "Update",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
