@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.Alert
import com.student360.app.data.local.entity.AlertType
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    repository: StudentRepository,
    viewModel: AlertsViewModel = viewModel()
) {
    val alerts by viewModel.alerts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recent Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
            Row {
                TextButton(onClick = { viewModel.markAllAsRead() }) {
                    Text("Read All", color = LightPurple, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { viewModel.clearAll() }) {
                    Text("Clear All", color = SecondaryText, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        if (alerts.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Notifications,
                title = "No Notifications",
                subtitle = "You are all caught up. No unread alerts at the moment.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(alerts) { alert ->
                    AlertRow(
                        alert = alert,
                        onClick = { viewModel.markAsRead(alert.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlertRow(
    alert: Alert,
    onClick: () -> Unit
) {
    val dateStr = remember(alert.timestamp) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(alert.timestamp))
    }

    val typeColor = when (alert.type) {
        AlertType.ATTENDANCE -> DangerRed
        AlertType.EXAM -> ExamPurple
        AlertType.LECTURE -> LightPurple
        AlertType.ASSIGNMENT -> WarningOrange
        AlertType.STUDY -> SuccessGreen
        AlertType.ACHIEVEMENT -> WarningOrange
    }

    val icon = when (alert.type) {
        AlertType.ATTENDANCE -> Icons.Default.Warning
        AlertType.EXAM -> Icons.Default.DateRange
        AlertType.LECTURE -> Icons.Default.List
        AlertType.ASSIGNMENT -> Icons.Default.Create
        AlertType.STUDY -> Icons.Default.PlayArrow
        AlertType.ACHIEVEMENT -> Icons.Default.Star
    }

    StudentCard(
        backgroundColor = if (alert.isRead) CardDark else ElevatedCardDark,
        borderColor = if (alert.isRead) BorderDark else PrimaryPurple.copy(alpha = 0.5f),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(typeColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(18.dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    alert.title,
                    fontWeight = if (alert.isRead) FontWeight.Normal else FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryText
                )
                Text(
                    alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = SecondaryText.copy(alpha = 0.7f)
                )
            }
            if (!alert.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(LightPurple, CircleShape)
                )
            }
        }
    }
}
