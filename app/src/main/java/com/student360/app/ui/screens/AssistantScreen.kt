package com.student360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.theme.SafeGreen
import com.student360.app.ui.theme.SafeGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    repository: StudentRepository,
    viewModel: AssistantViewModel = viewModel()
) {
    val candidates by viewModel.candidates.collectAsState()
    val schedule by viewModel.plannedSchedule.collectAsState()

    var availableHours by remember { mutableStateOf("3.0") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Offline Study Assistant",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Rule-based recommendation engine prioritizing based on exams, prep progress, and attendance safety limits.",
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Top Recommendation Priority
        if (candidates.isNotEmpty()) {
            val top = candidates.first()
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Today's Top Study Priority:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            top.subject.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Reason:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            top.reason,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Plan My Day Inputs
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Plan My Day Scheduler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("How many study hours do you have available today?", style = MaterialTheme.typography.bodySmall)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = availableHours,
                            onValueChange = { availableHours = it },
                            label = { Text("Available Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val hrs = availableHours.toDoubleOrNull() ?: 3.0
                                viewModel.generateDaySchedule(hrs)
                            }
                        ) {
                            Text("Generate")
                        }
                    }
                }
            }
        }

        // Planned Schedule list
        if (schedule.isNotEmpty()) {
            item {
                Text("Your Schedule for Today:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            items(schedule) { block ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (block.isBreak) MaterialTheme.colorScheme.surfaceVariant else SafeGreenLight
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                block.label,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (block.isBreak) MaterialTheme.colorScheme.onSurfaceVariant else SafeGreen
                            )
                            Text(
                                "${block.startTime} - ${block.endTime}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (block.isBreak) {
                            Text("☕", style = MaterialTheme.typography.titleLarge)
                        } else {
                            Text("📚", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }

        // Rest of subject priorities
        if (candidates.size > 1) {
            item {
                Text("Subject Priorities Breakdown", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            items(candidates.drop(1)) { candidate ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(candidate.subject.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(candidate.reason, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Score: ${candidate.priorityScore.toInt()}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}
