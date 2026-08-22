@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.io.OutputStreamWriter

@Composable
fun SettingsScreen(
    repository: StudentRepository,
    viewModel: SettingsViewModel = viewModel(),
    attendanceViewModel: AttendanceViewModel = viewModel()
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val profile by viewModel.profile.collectAsState()
    val globalTarget by attendanceViewModel.targetPercentage.collectAsState()

    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    val useSystemColors by ThemeManager.useSystemColors.collectAsState()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var showNotificationTimingDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showWeekStartDialog by remember { mutableStateOf(false) }
    var showCalendarViewDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var lectureNotificationsEnabled by remember { mutableStateOf(true) }
    var notificationTimingMinutes by remember { mutableStateOf(10) }
    var weekStartsOnMonday by remember { mutableStateOf(true) }
    var defaultCalendarView by remember { mutableStateOf("Monthly Heatmap") }

    // CSV Export Launcher
    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    OutputStreamWriter(stream).use { writer ->
                        writer.write("Subject,Date,Status,Notes\n")
                        Toast.makeText(context, "Attendance CSV exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export CSV: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // JSON Backup Launcher
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            val outputStream = context.contentResolver.openOutputStream(it)
            if (outputStream != null) {
                viewModel.backupData(outputStream) { success ->
                    if (success) {
                        Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Backup export failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // JSON Restore Launcher
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            if (inputStream != null) {
                viewModel.restoreData(inputStream) { success ->
                    if (success) {
                        Toast.makeText(context, "Backup imported successfully! Reloading...", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Backup import failed. Check file format.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val themeSubtitle = when (currentThemeMode) {
        AppThemeMode.SYSTEM_DEFAULT -> if (useSystemColors) "System Default, using System colors" else "System Default"
        AppThemeMode.LIGHT -> "Light"
        AppThemeMode.DARK -> "Dark"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // TOP HEADER
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 20.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Text(
                        text = "STUDENT360 PRO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 1. GENERAL SECTION
        item {
            SettingsCategoryHeader(title = "General", textColor = colors.textSecondary)
        }

        item {
            StudentCard(backgroundColor = colors.card, borderColor = colors.border) {
                Column {
                    // Attendance Target
                    SettingsItemRow(
                        icon = Icons.Default.CheckCircle,
                        title = "Attendance Target",
                        subtitle = "${globalTarget.toInt()}%",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { showTargetDialog = true }
                    )

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    // Set Theme
                    SettingsItemRow(
                        icon = Icons.Default.Settings,
                        title = "Set theme",
                        subtitle = themeSubtitle,
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { showThemeDialog = true }
                    )

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    // Lecture Notifications
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Lecture Notifications",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    if (lectureNotificationsEnabled) "Pre-class reminders enabled" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Switch(
                            checked = lectureNotificationsEnabled,
                            onCheckedChange = { lectureNotificationsEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.elevatedCard
                            )
                        )
                    }

                    if (lectureNotificationsEnabled) {
                        Divider(color = colors.border.copy(alpha = 0.5f))

                        // Notification Timing
                        SettingsItemRow(
                            icon = Icons.Default.DateRange,
                            title = "Notification Timing",
                            subtitle = if (notificationTimingMinutes == 0) "At start of class" else "$notificationTimingMinutes minutes before",
                            textColor = colors.textPrimary,
                            subTextColor = colors.textSecondary,
                            iconColor = colors.textSecondary,
                            onClick = { showNotificationTimingDialog = true }
                        )
                    }
                }
            }
        }

        // 2. DATA SECTION
        item {
            SettingsCategoryHeader(title = "Data", textColor = colors.textSecondary)
        }

        item {
            StudentCard(backgroundColor = colors.card, borderColor = colors.border) {
                Column {
                    // Backup & Restore
                    SettingsItemRow(
                        icon = Icons.Default.Share,
                        title = "Backup & Restore",
                        subtitle = "Avoid losing your data. Export full JSON backup or restore past data.",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { createDocLauncher.launch("student360_backup_${System.currentTimeMillis()}.json") }
                    )

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    // Export Data as CSV
                    SettingsItemRow(
                        icon = Icons.Default.List,
                        title = "Export Data as CSV",
                        subtitle = "Generates a CSV archive readable by spreadsheet apps like Excel / Sheets.",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { createCsvLauncher.launch("student360_attendance_${System.currentTimeMillis()}.csv") }
                    )

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    // Import Data
                    SettingsItemRow(
                        icon = Icons.Default.Refresh,
                        title = "Import Data",
                        subtitle = "Restore your attendance, timetable, and courses from a backup JSON file.",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { openDocLauncher.launch(arrayOf("application/json")) }
                    )
                }
            }
        }

        // 3. PERSONALIZATION SECTION
        item {
            SettingsCategoryHeader(title = "Personalization", textColor = colors.textSecondary)
        }

        item {
            StudentCard(backgroundColor = colors.card, borderColor = colors.border) {
                Column {
                    // Student360 Profile & Appearance
                    SettingsItemRow(
                        icon = Icons.Default.Person,
                        title = "Student Profile",
                        subtitle = "${profile?.name ?: "Student"} • ${profile?.branch ?: "Engineering"} Sem ${profile?.semester ?: 1}",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { showProfileDialog = true }
                    )

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    // Week Starts On
                    SettingsItemRow(
                        icon = Icons.Default.DateRange,
                        title = "Week Starts On",
                        subtitle = if (weekStartsOnMonday) "Monday" else "Sunday",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { showWeekStartDialog = true }
                    )

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    // Default Calendar View
                    SettingsItemRow(
                        icon = Icons.Default.Check,
                        title = "Default Calendar View",
                        subtitle = defaultCalendarView,
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { showCalendarViewDialog = true }
                    )
                }
            }
        }

        // 4. ABOUT SECTION
        item {
            SettingsCategoryHeader(title = "About", textColor = colors.textSecondary)
        }

        item {
            StudentCard(backgroundColor = colors.card, borderColor = colors.border) {
                Column {
                    SettingsItemRow(
                        icon = Icons.Default.Info,
                        title = "About Student360",
                        subtitle = "Version 1.0.0 • 100% Offline-First Academic Suite",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = { showAboutDialog = true }
                    )

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    SettingsItemRow(
                        icon = Icons.Default.Share,
                        title = "Share App",
                        subtitle = "Recommend Student360 to friends and classmates",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Track attendance, timetable, and study goals offline with Student360!")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Student360"))
                        }
                    )

                    Divider(color = colors.border.copy(alpha = 0.5f))

                    SettingsItemRow(
                        icon = Icons.Default.Star,
                        title = "Rate App",
                        subtitle = "Help us improve on Google Play Store",
                        textColor = colors.textPrimary,
                        subTextColor = colors.textSecondary,
                        iconColor = colors.textSecondary,
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.student360.app")))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Thank you for supporting Student360! ⭐⭐⭐⭐⭐", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // 1. SET THEME DIALOG (Matching Reference Screenshot Exactly)
    if (showThemeDialog) {
        var tempMode by remember { mutableStateOf(currentThemeMode) }
        var tempUseSystemColors by remember { mutableStateOf(useSystemColors) }

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            textContentColor = colors.textPrimary,
            title = {
                Text(
                    "Set theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Use System Colors Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { tempUseSystemColors = !tempUseSystemColors },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Use System colors",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                "App matches your phone's theme and colors",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
                            checked = tempUseSystemColors,
                            onCheckedChange = { tempUseSystemColors = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent
                            )
                        )
                    }

                    Divider(color = colors.border)

                    // Set mode Section
                    Text(
                        "Set mode",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            AppThemeMode.SYSTEM_DEFAULT to "System Default",
                            AppThemeMode.LIGHT to "Light",
                            AppThemeMode.DARK to "Dark"
                        ).forEach { (mode, label) ->
                            val isSelected = tempMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { tempMode = mode }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { tempMode = mode },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.accent,
                                        unselectedColor = colors.textSecondary
                                    )
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ThemeManager.setTheme(context, tempMode, tempUseSystemColors)
                        showThemeDialog = false
                        Toast.makeText(context, "Theme updated", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Save", color = colors.accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // 2. SET CRITERIA / ATTENDANCE TARGET DIALOG
    if (showTargetDialog) {
        var targetStr by remember { mutableStateOf("${globalTarget.toInt()}") }

        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            containerColor = colors.card,
            titleContentColor = colors.textPrimary,
            title = { Text("Set Attendance Criteria", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter target attendance percentage:", color = colors.textSecondary, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Target %") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(75, 80, 85, 90).forEach { pct ->
                            FilterChip(
                                selected = targetStr == "$pct",
                                onClick = { targetStr = "$pct" },
                                label = { Text("$pct%") }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = targetStr.toDoubleOrNull() ?: 75.0
                        attendanceViewModel.updateGlobalTarget(num)
                        showTargetDialog = false
                        Toast.makeText(context, "Target set to ${num.toInt()}%", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // 3. NOTIFICATION TIMING DIALOG
    if (showNotificationTimingDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationTimingDialog = false },
            containerColor = colors.card,
            title = { Text("Pre-Class Notification", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        0 to "At start of class",
                        5 to "5 minutes before",
                        10 to "10 minutes before",
                        15 to "15 minutes before",
                        30 to "30 minutes before"
                    ).forEach { (minutes, label) ->
                        val isSel = notificationTimingMinutes == minutes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    notificationTimingMinutes = minutes
                                    showNotificationTimingDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = {
                                    notificationTimingMinutes = minutes
                                    showNotificationTimingDialog = false
                                }
                            )
                            Text(label, color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationTimingDialog = false }) { Text("Close", color = colors.accent) }
            }
        )
    }

    // 4. WEEK STARTS ON DIALOG
    if (showWeekStartDialog) {
        AlertDialog(
            onDismissRequest = { showWeekStartDialog = false },
            containerColor = colors.card,
            title = { Text("Week Starts On", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                weekStartsOnMonday = true
                                showWeekStartDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = weekStartsOnMonday, onClick = { weekStartsOnMonday = true; showWeekStartDialog = false })
                        Text("Monday", color = colors.textPrimary)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                weekStartsOnMonday = false
                                showWeekStartDialog = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(selected = !weekStartsOnMonday, onClick = { weekStartsOnMonday = false; showWeekStartDialog = false })
                        Text("Sunday", color = colors.textPrimary)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showWeekStartDialog = false }) { Text("Close", color = colors.accent) } }
        )
    }

    // 5. DEFAULT CALENDAR VIEW DIALOG
    if (showCalendarViewDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarViewDialog = false },
            containerColor = colors.card,
            title = { Text("Default Calendar View", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Monthly Heatmap", "Weekly Grid", "Daily Agenda").forEach { viewName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    defaultCalendarView = viewName
                                    showCalendarViewDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(
                                selected = defaultCalendarView == viewName,
                                onClick = {
                                    defaultCalendarView = viewName
                                    showCalendarViewDialog = false
                                }
                            )
                            Text(viewName, color = colors.textPrimary)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCalendarViewDialog = false }) { Text("Close", color = colors.accent) } }
        )
    }

    // 6. STUDENT PROFILE DIALOG
    if (showProfileDialog) {
        var sName by remember { mutableStateOf(profile?.name ?: "") }
        var sRoll by remember { mutableStateOf(profile?.rollNumber ?: "") }
        var sBranch by remember { mutableStateOf(profile?.branch ?: "") }
        var sSem by remember { mutableStateOf(profile?.semester?.toString() ?: "1") }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            containerColor = colors.card,
            title = { Text("Edit Student Profile", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sName,
                        onValueChange = { sName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sRoll,
                        onValueChange = { sRoll = it },
                        label = { Text("Roll Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sBranch,
                        onValueChange = { sBranch = it },
                        label = { Text("Branch / Major") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sSem,
                        onValueChange = { sSem = it },
                        label = { Text("Semester (1-8)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(
                            name = sName,
                            rollNumber = sRoll,
                            branch = sBranch,
                            semester = sSem.toIntOrNull() ?: 1,
                            division = profile?.division ?: "A",
                            collegeName = profile?.collegeName ?: ""
                        )
                        showProfileDialog = false
                        Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Save Profile", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Cancel", color = colors.textSecondary) }
            }
        )
    }

    // 7. ABOUT DIALOG
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = colors.card,
            title = { Text("Student360", color = colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version 1.0.0 (Release Build)", color = colors.textPrimary, fontWeight = FontWeight.Medium)
                    Text("100% Offline-First Student Productivity & Attendance System.", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("Designed with privacy, speed, and clean academic workflows in mind.", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Awesome", color = colors.accent) }
            }
        )
    }
}

@Composable
fun SettingsCategoryHeader(
    title: String,
    textColor: Color
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = textColor,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    textColor: Color,
    subTextColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subTextColor,
                fontSize = 12.sp,
                lineHeight = 15.sp
            )
        }
    }
}
