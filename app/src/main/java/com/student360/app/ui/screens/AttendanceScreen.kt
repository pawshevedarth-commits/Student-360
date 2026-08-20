package com.student360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.data.repository.SubjectStats
import com.student360.app.ui.theme.*
import kotlin.math.ceil
import kotlin.math.floor

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSubjectDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Attendance Tracker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Simulate and view statistics",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Button(onClick = { showSimulator = true }) {
                            Text("Simulator")
                        }
                    }
                }

                if (subjectsWithStats.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No subjects found. Tap + to add a subject.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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

            if (showSimulator && subjectsWithStats.isNotEmpty()) {
                val subjects = subjectsWithStats.map { it.first }
                val statsList = subjectsWithStats.map { it.second }
                
                AttendanceSimulatorDialog(
                    subjects = subjects,
                    statsList = statsList,
                    initialSelectedIndex = selectedSubjectIndexForSim,
                    onDismiss = { showSimulator = false }
                )
            }

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
        diff >= 0 -> SafeGreen
        diff >= -5.0 -> WarningYellow
        else -> CriticalRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Code: ${subject.code} • Faculty: ${subject.faculty}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Baseline Override",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "${String.format("%.1f", stats.percentage)}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        "Target: ${subject.targetPercentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${stats.attended} / ${(stats.attended + stats.missed)} classes",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Cancelled/Off: ${stats.off}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onMark(AttendanceStatus.PRESENT) },
                    colors = ButtonDefaults.buttonColors(containerColor = SafeGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Present", color = Color.White)
                }
                Button(
                    onClick = { onMark(AttendanceStatus.ABSENT) },
                    colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Absent", color = Color.White)
                }
                Button(
                    onClick = { onMark(AttendanceStatus.OFF) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
        title = { Text("Attendance Simulator") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box {
                    OutlinedButton(onClick = { dropdownExpanded = true }) {
                        Text("Subject: ${subject.name}")
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        subjects.forEachIndexed { index, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    selectedIndex = index
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Simulate Future Classes:", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = futureAttendStr,
                        onValueChange = { futureAttendStr = it },
                        label = { Text("Attend") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = futureMissStr,
                        onValueChange = { futureMissStr = it },
                        label = { Text("Miss") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Simulated Percentage: ${String.format("%.1f", simulatedPercentage)}%",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (simulatedPercentage >= subject.targetPercentage) SafeGreen else CriticalRed
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Projected classes: $newAttended attended / $newConducted conducted",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        val currentConducted = stats.attended + stats.missed
                        if (simulatedPercentage < subject.targetPercentage) {
                            val classesNeeded = ceil((subject.targetPercentage / 100.0 * newConducted - newAttended) / (1.0 - subject.targetPercentage / 100.0)).toInt().coerceAtLeast(0)
                            Text(
                                "👉 Attend next $classesNeeded classes consecutively to reach ${subject.targetPercentage.toInt()}%.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            val classesCanMiss = floor((newAttended - subject.targetPercentage / 100.0 * newConducted) / (subject.targetPercentage / 100.0)).toInt().coerceAtLeast(0)
                            Text(
                                "👉 Safe! You can miss next $classesCanMiss classes consecutively and remain above target.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
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
        title = { Text("Subject Settings & Baseline") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Edit Baseline Override totals for ${subject.name}:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = attendedStr,
                        onValueChange = { attendedStr = it },
                        label = { Text("Attended") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = conductedStr,
                        onValueChange = { conductedStr = it },
                        label = { Text("Conducted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Set Attendance Target (%):", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(75.0, 80.0, 85.0, 90.0).forEach { target ->
                        FilterChip(
                            selected = targetVal == target,
                            onClick = {
                                targetVal = target
                                onUpdateTarget(target)
                            },
                            label = { Text("${target.toInt()}%") }
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
                }
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
        title = { Text("Add New Subject") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Subject Code") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = faculty, onValueChange = { faculty = it }, label = { Text("Faculty Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = targetStr, onValueChange = { targetStr = it }, label = { Text("Target Attendance %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, code, faculty, targetStr.toDoubleOrNull() ?: 75.0)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
