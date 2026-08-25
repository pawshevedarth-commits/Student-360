@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.Screen
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    repository: StudentRepository,
    viewModel: HomeViewModel = viewModel(),
    onNavigate: (Screen) -> Unit = {}
) {
    val colors = LocalAppColors.current
    val profile by viewModel.profile.collectAsState()
    val stats by viewModel.overallStats.collectAsState()
    val nextLect by viewModel.nextLecture.collectAsState()
    val countdown by viewModel.nextLectureCountdown.collectAsState()
    val tasksCount by viewModel.pendingTasksCount.collectAsState()
    val streak by viewModel.studyStreak.collectAsState()

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val formattedDate = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    val attPct = stats?.percentage ?: 100.0
    val attColor = when {
        attPct >= 75.0 -> colors.success
        attPct >= 70.0 -> colors.warning
        else -> colors.danger
    }
    val attStatusText = when {
        attPct >= 75.0 -> "Safe"
        attPct >= 70.0 -> "At Risk"
        else -> "Critical"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "$greeting, ${profile?.name ?: "Student"} 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }

        // Hero Attendance Card
        StudentCard(
            onClick = { onNavigate(Screen.ATTENDANCE) },
            backgroundColor = colors.card,
            borderColor = attColor.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overall Attendance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stats?.let { "${String.format("%.1f", it.percentage)}%" } ?: "100%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = attColor
                    )
                }
                StatusBadge(
                    text = attStatusText,
                    color = attColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            StudentProgressBar(
                progress = (attPct / 100.0).toFloat(),
                color = attColor,
                trackColor = colors.elevatedCard,
                height = 8.dp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stats?.let { "${it.totalAttended} / ${it.totalConducted} classes" } ?: "0 classes",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Text(
                    text = "Target: 75%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
            }
        }

        // Secondary Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Study Streak Card
            StudentCard(
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.STUDY) },
                backgroundColor = colors.card,
                borderColor = colors.border
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Study Streak", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(colors.warning.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔥", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$streak Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Keep it going!",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }

            // Pending Tasks Card
            StudentCard(
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.MY_DAY) },
                backgroundColor = colors.card,
                borderColor = colors.border
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pending Tasks", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$tasksCount Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "View tasks →",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Next Lecture Card
        StudentCard(
            onClick = { onNavigate(Screen.ATTENDANCE) },
            backgroundColor = colors.card,
            borderColor = colors.border
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Next Lecture",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = nextLect?.second?.name ?: "No lecture right now",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = if (nextLect != null) countdown else "All done",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (nextLect != null) colors.accent else colors.textSecondary
                )
            }
        }

        // Quick Actions Section
        SectionHeader(title = "Quick Actions")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.CheckCircle,
                label = "Attend",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.ATTENDANCE) }
            )
            QuickActionButton(
                icon = Icons.Default.Check,
                label = "Tasks",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.MY_DAY) }
            )
            QuickActionButton(
                icon = Icons.Default.Info,
                label = "Exams",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.EXAMS) }
            )
            QuickActionButton(
                icon = Icons.Default.Edit,
                label = "Assign",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.MY_DAY) }
            )
            QuickActionButton(
                icon = Icons.Default.PlayArrow,
                label = "Study",
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.STUDY) }
            )
        }

        // Attendance Below Target Alert Card
        if (attPct < 75.0) {
            StudentCard(
                backgroundColor = if (colors.isDark) Color(0xFF28181B) else Color(0xFFFFEBEE),
                borderColor = colors.danger.copy(alpha = 0.5f),
                onClick = { onNavigate(Screen.ATTENDANCE) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.danger.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = colors.danger, modifier = Modifier.size(20.dp))
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Attendance below target",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.danger,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Your overall attendance is ${String.format(Locale.US, "%.1f", attPct)}%. Target: 75%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.danger.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, colors.danger.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "View →",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.danger,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(colors.elevatedCard, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = colors.accent,
                    modifier = Modifier.size(17.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
