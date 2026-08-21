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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
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
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.data.repository.SubjectStats
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun AttendanceScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel = viewModel()
) {
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()

    var showSimulator by remember { mutableStateOf(false) }
    var selectedSubjectIndexForSim by remember { mutableStateOf(0) }
    var overrideSubject by remember { mutableStateOf<Subject?>(null) }
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    // Aggregate overall attendance
    val totalAttended = remember(subjectsWithStats) { subjectsWithStats.sumOf { it.second.attended } }
    val totalConducted = remember(subjectsWithStats) { subjectsWithStats.sumOf { it.second.attended + it.second.missed } }
    val overallPercentage = remember(totalAttended, totalConducted) {
        if (totalConducted > 0) (totalAttended.toDouble() / totalConducted.toDouble()) * 100.0 else 100.0
    }
    val overallStatusColor = when {
        overallPercentage >= 75.0 -> SuccessGreen
        overallPercentage >= 70.0 -> WarningOrange
        else -> DangerRed
    }

    Scaffold(
        containerColor = BgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSubjectDialog = true },
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
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
                // Top Attendance Overview Hero Card
                StudentCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    backgroundColor = CardDark,
                    borderColor = overallStatusColor.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Attendance Overview",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SecondaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${String.format("%.1f", overallPercentage)}%",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = overallStatusColor
                            )
                        }
                        Button(
                            onClick = { showSimulator = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardDark),
                            border = BorderStroke(1.dp, BorderDark),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Simulator ⚡", color = LightPurple, style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    StudentProgressBar(
                        progress = (overallPercentage / 100.0).toFloat(),
                        color = overallStatusColor,
                        trackColor = SurfaceDark,
                        height = 8.dp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$totalAttended / $totalConducted total classes",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                        Text(
                            "Target: 75%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = SecondaryText
                        )
                    }
                }

                // Subject Cards List or Empty State
                if (subjectsWithStats.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.CheckCircle,
                        title = "No Subjects Added Yet",
                        subtitle = "Track your attendance by adding your courses and academic subjects.",
                        actionText = "+ Add First Subject",
                        onActionClick = { showAddSubjectDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(subjectsWithStats) { (subject, stats) ->
                            SubjectAttendanceCard(
                                subject = subject,
                                stats = stats,
                                onMark = { status -> viewModel.markAttendance(subject.id, status) },
                                onEdit = { overrideSubject = subject }
                            )
                        }
                    }
                }
            }

            // Attendance Simulator Dialog
            if (showSimulator && subjectsWithStats.isNotEmpty()) {
                val subjects = subjectsWithStats.map { it.first }
                val statsList = subjectsWithStats.map { it.second }

                AttendanceSimulatorDialog(
                    subjects = subjects,
                    statsList = statsList,
                    initialSelectedIndex = selectedSubjectIndexForSim.coerceIn(0, subjects.size - 1),
                    onDismiss = { showSimulator = false }
                )
            }

            // Manual Override Dialog
            overrideSubject?.let { subject ->
                val stats = subjectsWithStats.find { it.first.id == subject.id }?.second
                ManualOverrideDialog(
                    subject = subject,
                    currentAttended = stats?.attended ?: subject.manualAttended,
                    currentConducted = (stats?.attended ?: subject.manualAttended) + (stats?.missed ?: 0),
                    onDismiss = { overrideSubject = null },
                    onSave = { attended, conducted ->
                        viewModel.updateManualOverrides(subject, attended, conducted)
                        overrideSubject = null
                    },
                    onUpdateTarget = { target ->
                        viewModel.updateSubjectTarget(subject, target)
                    }
                )
            }

            // Add Subject Dialog
            if (showAddSubjectDialog) {
                AddSubjectDialog(
                    onDismiss = { showAddSubjectDialog = false },
                    onSave = { name, code, faculty, target ->
                        viewModel.addSubject(name, code, faculty, target)
                        showAddSubjectDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun SubjectAttendanceCard(
    subject: Subject,
    stats: SubjectStats,
    onMark: (AttendanceStatus) -> Unit,
    onEdit: () -> Unit
) {
    val diff = stats.percentage - subject.targetPercentage
    val statusColor = when {
        diff >= 0 -> SuccessGreen
        diff >= -5.0 -> WarningOrange
        else -> DangerRed
    }
    val statusText = when {
        diff >= 0 -> "Safe"
        diff >= -5.0 -> "At Risk"
        else -> "Critical"
    }

    StudentCard(
        backgroundColor = CardDark,
        borderColor = BorderDark
    ) {
        // Top Header: Subject info & status badge + edit button
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
                    subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Code: ${subject.code.ifBlank { "N/A" }} • ${subject.faculty.ifBlank { "Faculty" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                StatusBadge(text = statusText, color = statusColor)
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Baseline Override",
                        tint = SecondaryText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Middle Row: Percentage & Class counts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "${String.format("%.1f", stats.percentage)}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Text(
                    "Target ${subject.targetPercentage.toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text(
                "${stats.attended} / ${(stats.attended + stats.missed)} classes",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        StudentProgressBar(
            progress = (stats.percentage / 100.0).toFloat(),
            color = statusColor,
            trackColor = SurfaceDark,
            height = 6.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom Controls: Clean non-wrapping Present / Absent / Cancelled
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onMark(AttendanceStatus.PRESENT) },
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Text("✓ Present", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button(
                onClick = { onMark(AttendanceStatus.ABSENT) },
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Text("× Absent", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Button(
                onClick = { onMark(AttendanceStatus.OFF) },
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardDark),
                border = BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Text("— Off", color = SecondaryText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

@Composable
fun AttendanceSimulatorDialog(
    subjects: List<Subject>,
    statsList: List<SubjectStats>,
    initialSelectedIndex: Int,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember { mutableStateOf(initialSelectedIndex) }
    var futureAttendStr by remember { mutableStateOf("0") }
    var futureMissStr by remember { mutableStateOf("0") }

    val subject = subjects[selectedIndex]
    val stats = statsList[selectedIndex]

    val fa = futureAttendStr.toIntOrNull() ?: 0
    val fm = futureMissStr.toIntOrNull() ?: 0

    val newAttended = stats.attended + fa
    val newConducted = stats.attended + stats.missed + fa + fm

    val simulatedPercentage = if (newConducted > 0) {
        (newAttended.toDouble() / newConducted.toDouble()) * 100.0
    } else {
        100.0
    }

    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Attendance Simulator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Subject Dropdown Selector
                Box {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryText),
                        border = BorderStroke(1.dp, BorderDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subject: ${subject.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        subjects.forEachIndexed { index, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name, color = PrimaryText) },
                                onClick = {
                                    selectedIndex = index
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Simulate Future Classes:", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = futureAttendStr,
                        onValueChange = { futureAttendStr = it },
                        label = { Text("Attend (+)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SuccessGreen,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = futureMissStr,
                        onValueChange = { futureMissStr = it },
                        label = { Text("Miss (-)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DangerRed,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Projected Comparison Card
                ElevatedStudentCard(
                    backgroundColor = CardDark,
                    borderColor = if (simulatedPercentage >= subject.targetPercentage) SuccessGreen.copy(alpha = 0.4f) else DangerRed.copy(alpha = 0.4f)
                ) {
                    Text(
                        "Projected Attendance",
                        style = MaterialTheme.typography.labelSmall,
                        color = SecondaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${String.format("%.1f", stats.percentage)}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = SecondaryText
                        )
                        Text("➔", style = MaterialTheme.typography.titleMedium, color = LightPurple)
                        Text(
                            "${String.format("%.1f", simulatedPercentage)}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (simulatedPercentage >= subject.targetPercentage) SuccessGreen else DangerRed
                        )
                    }
                    Text(
                        "$newAttended attended / $newConducted conducted",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    if (simulatedPercentage < subject.targetPercentage) {
                        val classesNeeded = ceil((subject.targetPercentage / 100.0 * newConducted - newAttended) / (1.0 - subject.targetPercentage / 100.0)).toInt().coerceAtLeast(0)
                        Text(
                            "👉 Attend next $classesNeeded classes consecutively to reach ${subject.targetPercentage.toInt()}%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DangerRed,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        val classesCanMiss = floor((newAttended - subject.targetPercentage / 100.0 * newConducted) / (subject.targetPercentage / 100.0)).toInt().coerceAtLeast(0)
                        Text(
                            "👉 Safe! You can miss next $classesCanMiss classes consecutively and stay above target.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualOverrideDialog(
    subject: Subject,
    currentAttended: Int,
    currentConducted: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onUpdateTarget: (Double) -> Unit
) {
    var attendedStr by remember { mutableStateOf(currentAttended.toString()) }
    var conductedStr by remember { mutableStateOf(currentConducted.toString()) }
    var targetVal by remember { mutableStateOf(subject.targetPercentage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Subject Settings & Baseline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Edit baseline totals for ${subject.name}:",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = attendedStr,
                        onValueChange = { attendedStr = it },
                        label = { Text("Attended") },
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
                        value = conductedStr,
                        onValueChange = { conductedStr = it },
                        label = { Text("Conducted") },
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

                Text("Target Attendance %:", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(75.0, 80.0, 85.0, 90.0).forEach { target ->
                        val isSel = targetVal == target
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                targetVal = target
                                onUpdateTarget(target)
                            },
                            label = { Text("${target.toInt()}%") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = Color.White,
                                containerColor = CardDark,
                                labelColor = SecondaryText
                            ),
                            border = FilterChipDefaults.filterChipBorder(borderColor = BorderDark)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val att = attendedStr.toIntOrNull() ?: currentAttended
                    val cond = conductedStr.toIntOrNull() ?: currentConducted
                    onSave(att, cond)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp)
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

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("75") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Add New Subject",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. DBMS)") },
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
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Subject Code (optional)") },
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
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty Name (optional)") },
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
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Attendance %") },
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
                    if (name.isNotBlank()) {
                        onSave(name, code, faculty, targetStr.toDoubleOrNull() ?: 75.0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}
