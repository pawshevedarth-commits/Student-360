@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.SubjectStats
import com.student360.app.ui.components.ElevatedStudentCard
import com.student360.app.ui.theme.*
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    val colors = LocalAppColors.current
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("75") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
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
                    label = { Text("Subject Name (e.g. DMGT)") },
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
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Subject Code (optional)") },
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
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty Name (optional)") },
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
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Attendance %") },
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
                    if (name.isNotBlank()) {
                        onSave(name, code, faculty, targetStr.toDoubleOrNull() ?: 75.0)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Add Subject", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun ManualOverrideDialog(
    subject: Subject,
    currentAttended: Int,
    currentConducted: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
    onUpdateTarget: (Double) -> Unit
) {
    val colors = LocalAppColors.current
    var attendedStr by remember { mutableStateOf(currentAttended.toString()) }
    var conductedStr by remember { mutableStateOf(currentConducted.toString()) }
    var targetVal by remember { mutableStateOf(subject.targetPercentage) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                "Baseline & Target (${subject.name})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Set baseline lecture counts if you have past offline attendance:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = attendedStr,
                        onValueChange = { attendedStr = it },
                        label = { Text("Attended") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                        value = conductedStr,
                        onValueChange = { conductedStr = it },
                        label = { Text("Conducted") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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

                Text("Target Attendance %:", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                selectedContainerColor = colors.activePill,
                                selectedLabelColor = if (colors.isDark) Color.White else colors.accent,
                                containerColor = colors.elevatedCard,
                                labelColor = colors.textSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(borderColor = colors.border)
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
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Baseline", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun AttendanceSimulatorDialog(
    subjects: List<Subject>,
    statsList: List<SubjectStats>,
    initialSelectedIndex: Int,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    var selectedIndex by remember { mutableStateOf(initialSelectedIndex.coerceIn(0, (subjects.size - 1).coerceAtLeast(0))) }
    var futureAttendStr by remember { mutableStateOf("0") }
    var futureMissStr by remember { mutableStateOf("0") }

    if (subjects.isEmpty() || statsList.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = colors.card,
            title = { Text("Simulator", color = colors.textPrimary) },
            text = { Text("No subjects registered to simulate.", color = colors.textSecondary) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = colors.accent) } }
        )
        return
    }

    val subject = subjects[selectedIndex.coerceIn(0, subjects.size - 1)]
    val stats = statsList[selectedIndex.coerceIn(0, statsList.size - 1)]

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
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Subject: ${subject.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(colors.card)
                    ) {
                        subjects.forEachIndexed { index, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name, color = colors.textPrimary) },
                                onClick = {
                                    selectedIndex = index
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Simulate Future Classes:", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = futureAttendStr,
                        onValueChange = { futureAttendStr = it },
                        label = { Text("Attend (+)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.success,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
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
                            focusedBorderColor = colors.danger,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Projected Comparison Card
                ElevatedStudentCard(
                    backgroundColor = colors.elevatedCard,
                    borderColor = if (simulatedPercentage >= subject.targetPercentage) colors.success.copy(alpha = 0.4f) else colors.danger.copy(alpha = 0.4f)
                ) {
                    Text(
                        "Projected Attendance",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${String.format(Locale.US, "%.1f", stats.percentage)}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textSecondary
                        )
                        Text("➔", style = MaterialTheme.typography.titleMedium, color = colors.accent)
                        Text(
                            "${String.format(Locale.US, "%.1f", simulatedPercentage)}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (simulatedPercentage >= subject.targetPercentage) colors.success else colors.danger
                        )
                    }
                    Text(
                        "$newAttended attended / $newConducted conducted",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    if (simulatedPercentage < subject.targetPercentage) {
                        val classesNeeded = ceil((subject.targetPercentage / 100.0 * newConducted - newAttended) / (1.0 - subject.targetPercentage / 100.0)).toInt().coerceAtLeast(0)
                        Text(
                            "👉 Attend next $classesNeeded classes consecutively to reach ${subject.targetPercentage.toInt()}%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.danger,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        val classesCanMiss = floor((newAttended - subject.targetPercentage / 100.0 * newConducted) / (subject.targetPercentage / 100.0)).toInt().coerceAtLeast(0)
                        Text(
                            "👉 Safe! You can miss next $classesCanMiss classes consecutively and stay above target.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.success,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Add Scheduled Timetable Lecture Dialog
 */
@Composable
fun AddLectureDialog(
    subjects: List<Subject>,
    initialDay: Int = 0,
    onDismiss: () -> Unit,
    onSave: (Int, Int, String, String, String, String?, Int?) -> Unit
) {
    val colors = LocalAppColors.current
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: 0) }
    var selectedDay by remember { mutableStateOf(initialDay) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var room by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf("") }
    var notifyMinutes by remember { mutableStateOf<Int?>(10) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedSubject = subjects.find { it.id == selectedSubjectId } ?: subjects.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        title = {
            Text(
                "Add Timetable Lecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Subject Dropdown
                Box {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedSubject?.name ?: "Select Course", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.background(colors.card)
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name, color = colors.textPrimary) },
                                onClick = {
                                    selectedSubjectId = sub.id
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Day of Week Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    days.forEachIndexed { index, day ->
                        val isSel = selectedDay == index
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) colors.activePill else colors.elevatedCard,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedDay = index }
                        ) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) (if (colors.isDark) Color.White else colors.accent) else colors.textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                // Time Inputs
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
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
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
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

                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room / Lab (optional)") },
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
                    value = faculty,
                    onValueChange = { faculty = it },
                    label = { Text("Faculty (optional)") },
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
                    onSave(
                        selectedSubjectId,
                        selectedDay,
                        startTime,
                        endTime,
                        room,
                        faculty.ifBlank { null },
                        notifyMinutes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Class", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
