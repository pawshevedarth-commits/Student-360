@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val colors = LocalAppColors.current
    val alerts by viewModel.alerts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
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
                color = colors.textPrimary
            )
            Row {
                TextButton(onClick = { viewModel.markAllAsRead() }) {
                    Text("Read All", color = colors.accent, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { viewModel.clearAll() }) {
                    Text("Clear All", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
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
    val colors = LocalAppColors.current
    val dateStr = remember(alert.timestamp) {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(alert.timestamp))
    }

    val typeColor = when (alert.type) {
        AlertType.ATTENDANCE -> colors.danger
        AlertType.EXAM -> colors.warning
        AlertType.LECTURE -> colors.accent
        AlertType.ASSIGNMENT -> colors.warning
        AlertType.STUDY -> colors.accent
        AlertType.ACHIEVEMENT -> colors.success
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = colors.card,
        border = BorderStroke(1.dp, if (!alert.isRead) colors.accent.copy(alpha = 0.5f) else colors.border),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(typeColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (alert.type) {
                        AlertType.ATTENDANCE -> Icons.Default.Warning
                        AlertType.EXAM -> Icons.Default.Info
                        AlertType.LECTURE -> Icons.Default.DateRange
                        AlertType.ASSIGNMENT -> Icons.Default.Edit
                        AlertType.STUDY -> Icons.Default.PlayArrow
                        AlertType.ACHIEVEMENT -> Icons.Default.Star
                    },
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }

            if (!alert.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(colors.accent, CircleShape)
                )
            }
        }
    }
}
