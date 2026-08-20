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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = { viewModel.markAllAsRead() }) {
                    Text("Read All")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { viewModel.clearAll() }) {
                    Text("Clear All")
                }
            }
        }

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No notifications logged yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
        AlertType.ATTENDANCE -> CriticalRed
        AlertType.EXAM -> ExamPurple
        AlertType.LECTURE -> MaterialTheme.colorScheme.primary
        AlertType.ASSIGNMENT -> WarningYellow
        AlertType.STUDY -> SafeGreen
        AlertType.ACHIEVEMENT -> SafeGreen
    }

    val icon = when (alert.type) {
        AlertType.ATTENDANCE -> Icons.Default.Warning
        AlertType.EXAM -> Icons.Default.DateRange
        AlertType.LECTURE -> Icons.Default.List
        AlertType.ASSIGNMENT -> Icons.Default.Create
        AlertType.STUDY -> Icons.Default.PlayArrow
        AlertType.ACHIEVEMENT -> Icons.Default.Star
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (alert.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(typeColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = typeColor, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alert.title,
                    fontWeight = if (alert.isRead) FontWeight.Normal else FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (!alert.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
            }
        }
    }
}
