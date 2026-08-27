@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.AttendanceFractionBadge
import com.student360.app.ui.components.EmptyStateView
import com.student360.app.ui.components.StudentCard
import com.student360.app.ui.components.StudentScreenHeader
import com.student360.app.ui.theme.*
import java.util.Locale

@Composable
fun SubjectsScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    onNavigateToSubjectDetail: (Subject) -> Unit
) {
    val colors = LocalAppColors.current
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val archivedSubjectsWithStats by viewModel.archivedSubjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val calendarSummary by viewModel.calendarSummary.collectAsState()

    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showSimulatorDialog by remember { mutableStateOf(false) }
    var subjectToDelete by remember { mutableStateOf<Pair<Subject, com.student360.app.data.repository.SubjectStats>?>(null) }

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = 75

    val overallStatusColor = when {
        overallPct >= 75.0 -> colors.success
        overallPct >= 70.0 -> colors.warning
        else -> colors.danger
    }

    val overallAttended = overallStats?.totalAttended ?: 0
    val overallConducted = overallStats?.totalConducted ?: 0
    val overallOff = calendarSummary.totalOff
    val overallMissed = (overallConducted - overallAttended).coerceAtLeast(0)

    val overallRecommendation = remember(overallAttended, overallConducted) {
        viewModel.calculateRecommendation(overallAttended, overallConducted, 75.0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header: Title + Overall Pill Badge + Add & Simulator actions
            item {
                StudentScreenHeader(
                    title = "Subjects",
                    overallPercentage = overallPct,
                    targetPercentage = targetPct,
                    onAddClick = { showAddSubjectDialog = true },
                    extraAction = {
                        IconButton(
                            onClick = { showSimulatorDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Simulator",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            }

            // Overall Summary Card
            item {
                StudentCard(
                    backgroundColor = colors.card,
                    borderColor = colors.border,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        AttendanceFractionBadge(
                            percentage = overallPct,
                            target = targetPct,
                            statusColor = overallStatusColor
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Overall Attendance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 16.sp
                            )
                            Text(
                                text = overallRecommendation,
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    overallRecommendation.startsWith("can miss") -> colors.success
                                    overallRecommendation.startsWith("need to attend") -> colors.danger
                                    else -> colors.accent
                                },
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Att: $overallAttended • Miss: $overallMissed • Off: $overallOff • Tot: $overallConducted",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Individual Subject Cards
            if (subjectsWithStats.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "No Subjects Added",
                        subtitle = "Add your academic courses to start tracking attendance and predictions.",
                        actionText = "+ Add Subject",
                        onActionClick = { showAddSubjectDialog = true }
                    )
                }
            } else {
                items(subjectsWithStats) { (subject, stats) ->
                    val totalConducted = stats.attended + stats.missed
                    val diff = stats.percentage - subject.targetPercentage
                    val statusColor = when {
                        diff >= 0 -> colors.success
                        diff >= -5.0 -> colors.warning
                        else -> colors.danger
                    }

                    val rec = viewModel.calculateRecommendation(
                        stats.attended,
                        totalConducted,
                        subject.targetPercentage
                    )

                    StudentCard(
                        backgroundColor = colors.card,
                        borderColor = colors.border,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            viewModel.selectSubjectForDetail(subject)
                            onNavigateToSubjectDetail(subject)
                        },
                        onLongClick = {
                            subjectToDelete = subject to stats
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            AttendanceFractionBadge(
                                percentage = stats.percentage,
                                target = subject.targetPercentage.toInt(),
                                statusColor = statusColor
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = subject.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = rec,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Att: ${stats.attended} • Miss: ${stats.missed} • Off: ${stats.off} • Tot: $totalConducted",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Archived Subjects Section
            if (archivedSubjectsWithStats.isNotEmpty()) {
                item {
                    var isArchivedExpanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.elevatedCard,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isArchivedExpanded = !isArchivedExpanded }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = colors.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Archived Subjects (${archivedSubjectsWithStats.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }

                                Icon(
                                    if (isArchivedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (isArchivedExpanded) {
                            archivedSubjectsWithStats.forEach { (archivedSub, archivedStats) ->
                                val archivedTotal = archivedStats.attended + archivedStats.missed
                                StudentCard(
                                    backgroundColor = colors.card.copy(alpha = 0.85f),
                                    borderColor = colors.border,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        viewModel.selectSubjectForDetail(archivedSub)
                                        onNavigateToSubjectDetail(archivedSub)
                                    },
                                    onLongClick = {
                                        subjectToDelete = archivedSub to archivedStats
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = archivedSub.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.textPrimary
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = colors.textSecondary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Archived",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.textSecondary,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Preserved: $archivedTotal classes • ${archivedStats.attended} attended • ${String.format(Locale.US, "%.1f", archivedStats.percentage)}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.restoreSubject(archivedSub) },
                                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Restore",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Subject Dialog
        if (showAddSubjectDialog) {
            AddSubjectDialog(
                onDismiss = { showAddSubjectDialog = false },
                onSave = { name, code, faculty, target ->
                    viewModel.addSubject(name, code, faculty, target)
                    showAddSubjectDialog = false
                }
            )
        }

        // Simulator Dialog
        if (showSimulatorDialog) {
            AttendanceSimulatorDialog(
                subjects = subjectsWithStats.map { it.first },
                statsList = subjectsWithStats.map { it.second },
                initialSelectedIndex = 0,
                onDismiss = { showSimulatorDialog = false }
            )
        }

        // Delete Subject Dialog (Safe Delete vs Permanent Delete)
        subjectToDelete?.let { (subject, stats) ->
            DeleteSubjectDialog(
                subject = subject,
                stats = stats,
                onDismiss = { subjectToDelete = null },
                onSafeDelete = {
                    viewModel.safeDeleteSubject(subject)
                    subjectToDelete = null
                },
                onPermanentDelete = {
                    viewModel.permanentlyDeleteSubject(subject)
                    subjectToDelete = null
                }
            )
        }
    }
}
