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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*

@Composable
fun AssistantScreen(
    repository: StudentRepository,
    viewModel: AssistantViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val candidates by viewModel.candidates.collectAsState()
    val schedule by viewModel.plannedSchedule.collectAsState()

    var availableHours by remember { mutableStateOf("3.0") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                Text(
                    "Study Assistant",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = colors.textPrimary
                )
                Text(
                    "Your personalized academic planner & recommendation engine",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        }

        // Top Recommendation Priority Hero Card
        if (candidates.isNotEmpty()) {
            val top = candidates.first()
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.accent.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Today's Priority",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.danger.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(colors.danger, CircleShape))
                                Text(
                                    "High Priority",
                                    color = colors.danger,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        top.subject.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        top.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Plan My Day Inputs Card
        item {
            StudentCard(
                backgroundColor = colors.elevatedCard,
                borderColor = colors.border,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Plan My Day",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.textPrimary
                )
                Text(
                    "How many study hours do you have available today?",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = colors.textSecondary
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
                        label = { Text("Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.width(100.dp)
                    )
                    Button(
                        onClick = {
                            val hrs = availableHours.toDoubleOrNull() ?: 3.0
                            viewModel.generateDaySchedule(hrs)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("⚡ Generate My Study Plan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    backgroundColor = if (block.isBreak) colors.elevatedCard else colors.card,
                    borderColor = if (block.isBreak) colors.border.copy(alpha = 0.5f) else colors.success.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
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
                                        if (block.isBreak) colors.border.copy(alpha = 0.2f) else colors.success.copy(alpha = 0.15f),
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
                                    color = if (block.isBreak) colors.textSecondary else colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${block.startTime} - ${block.endTime}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary
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
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                candidate.subject.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                candidate.reason,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val scorePct = (candidate.priorityScore / 100.0).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = scorePct,
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (candidate.priorityScore > 40) colors.warning else colors.accent,
                                trackColor = colors.border.copy(alpha = 0.4f)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.elevatedCard,
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Text(
                                text = "Score ${candidate.priorityScore.toInt()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (candidate.priorityScore > 40) colors.warning else colors.accent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
