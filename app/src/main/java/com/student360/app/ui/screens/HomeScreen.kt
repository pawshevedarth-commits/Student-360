package com.student360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.theme.SafeGreen
import com.student360.app.ui.theme.SafeGreenLight
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    repository: StudentRepository,
    viewModel: HomeViewModel = viewModel()
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        Column {
            Text(
                text = "$greeting, ${profile?.name ?: "Student"} 👋",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Summary 4-Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "Overall Attendance",
                value = stats?.let { "${String.format("%.1f", it.percentage)}%" } ?: "100%",
                subtitle = stats?.let { "${it.totalAttended} / ${it.totalConducted} classes" } ?: "0 classes",
                icon = Icons.Default.CheckCircle,
                color = SafeGreen,
                modifier = Modifier.weight(1f)
            )
            
            DashboardCard(
                title = "Study Streak",
                value = "$streak Days",
                subtitle = "🔥 Keep it going!",
                icon = Icons.Default.PlayArrow,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "Next Lecture",
                value = nextLect?.second?.name ?: "None scheduled",
                subtitle = if (nextLect != null) countdown else "All classes done!",
                icon = Icons.Default.List,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            DashboardCard(
                title = "Pending Tasks",
                value = "$tasksCount Tasks",
                subtitle = "To-Do checklist",
                icon = Icons.Default.Star,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }

        // Quick Actions Row
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionButton(icon = Icons.Default.Check, label = "Attendance") {
                /* Shortcut to log attendance */
            }
            QuickActionButton(icon = Icons.Default.Add, label = "Task") {
                /* Shortcut to add task */
            }
            QuickActionButton(icon = Icons.Default.DateRange, label = "Exam") {
                /* Shortcut to add exam */
            }
            QuickActionButton(icon = Icons.Default.Create, label = "Assignment") {
                /* Shortcut to add assignment */
            }
            QuickActionButton(icon = Icons.Default.PlayArrow, label = "Study") {
                /* Shortcut to start study session */
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        
        // Safety Warning Banner
        stats?.let {
            if (it.percentage < 75.0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Alert", tint = MaterialTheme.colorScheme.error)
                        Text(
                            "Warning: Your overall attendance has dropped below 75%!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 130.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
