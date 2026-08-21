@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

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
        attPct >= 75.0 -> SuccessGreen
        attPct >= 70.0 -> WarningOrange
        else -> DangerRed
    }
    val attStatusText = when {
        attPct >= 75.0 -> "Safe"
        attPct >= 70.0 -> "At Risk"
        else -> "Critical"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                color = PrimaryText
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = SecondaryText
            )
        }

        // Hero Attendance Card
        StudentCard(
            onClick = { onNavigate(Screen.ATTENDANCE) },
            backgroundColor = CardDark,
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
                        color = SecondaryText
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
                trackColor = SurfaceDark,
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
                    color = SecondaryText
                )
                Text(
                    text = "Target: 75%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = SecondaryText
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
                onClick = { onNavigate(Screen.STUDY) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Study Streak", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(WarningOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
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
                    color = PrimaryText
                )
                Text(
                    text = "Keep it going!",
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText
                )
            }

            // Pending Tasks Card
            StudentCard(
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(Screen.MY_DAY) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pending Tasks", style = MaterialTheme.typography.labelMedium, color = SecondaryText)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(LightPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = LightPurple, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$tasksCount Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    text = "View tasks →",
                    style = MaterialTheme.typography.labelSmall,
                    color = LightPurple,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Next Lecture Card
        StudentCard(
            onClick = { onNavigate(Screen.SCHEDULE) }
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
                            .background(PrimaryPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = LightPurple, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text(
                            text = "Next Lecture",
                            style = MaterialTheme.typography.labelSmall,
                            color = SecondaryText
                        )
                        Text(
                            text = nextLect?.second?.name ?: "No lecture right now",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = if (nextLect != null) countdown else "All done",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (nextLect != null) LightPurple else SecondaryText
                )
            }
        }

        // Quick Actions Section
        SectionHeader(title = "Quick Actions")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.CheckCircle,
                label = "Attendance",
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
                backgroundColor = Color(0xFF28181B),
                borderColor = DangerRed.copy(alpha = 0.5f),
                onClick = { onNavigate(Screen.ATTENDANCE) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(DangerRed.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendance below target",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                        Text(
                            text = "Your overall attendance is ${String.format("%.1f", attPct)}%. Target: 75%.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryText
                        )
                    }
                    Text(
                        text = "View →",
                        style = MaterialTheme.typography.labelSmall,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
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
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = CardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(SurfaceDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = LightPurple,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = PrimaryText,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
