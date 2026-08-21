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
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*

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
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Study Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    "Your personalized academic planner & recommendation engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        }

        // Top Recommendation Priority Hero Card
        if (candidates.isNotEmpty()) {
            val top = candidates.first()
            item {
                StudentCard(
                    backgroundColor = CardDark,
                    borderColor = PrimaryPurple.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Today's Priority",
                            style = MaterialTheme.typography.labelSmall,
                            color = LightPurple,
                            fontWeight = FontWeight.SemiBold
                        )
                        StatusBadge(
                            text = "High Priority",
                            color = if (top.priorityScore > 50) DangerRed else WarningOrange
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        top.subject.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Reason: ${top.reason}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SecondaryText
                    )
                }
            }
        }

        // Plan My Day Inputs Card
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Text(
                    "Plan My Day Scheduler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    "How many study hours do you have available today?",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = availableHours,
                        onValueChange = { availableHours = it },
                        label = { Text("Available Hours") },
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
                    Button(
                        onClick = {
                            val hrs = availableHours.toDoubleOrNull() ?: 3.0
                            viewModel.generateDaySchedule(hrs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text("⚡ Generate", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Planned Schedule list
        if (schedule.isNotEmpty()) {
            item {
                SectionHeader(title = "Generated Study Schedule")
            }

            items(schedule) { block ->
                StudentCard(
                    backgroundColor = if (block.isBreak) SurfaceDark else CardDark,
                    borderColor = if (block.isBreak) BorderDark.copy(alpha = 0.5f) else SuccessGreen.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (block.isBreak) ElevatedCardDark else SuccessGreen.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (block.isBreak) "☕" else "📚", style = MaterialTheme.typography.bodyLarge)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    block.label,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (block.isBreak) SecondaryText else PrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${block.startTime} - ${block.endTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }

        // Rest of subject priorities
        if (candidates.size > 1) {
            item {
                SectionHeader(title = "Subject Priority Breakdown")
            }
            items(candidates.drop(1)) { candidate ->
                StudentCard(
                    backgroundColor = CardDark,
                    borderColor = BorderDark
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
                                candidate.subject.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = PrimaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                candidate.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = SecondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        StatusBadge(
                            text = "Score ${candidate.priorityScore.toInt()}",
                            color = if (candidate.priorityScore > 40) WarningOrange else LightPurple
                        )
                    }
                }
            }
        }
    }
}
