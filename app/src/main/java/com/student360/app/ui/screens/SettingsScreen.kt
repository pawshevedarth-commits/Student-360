@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*

@Composable
fun SettingsScreen(
    repository: StudentRepository,
    viewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()

    var name by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var semesterString by remember { mutableStateOf("") }
    var division by remember { mutableStateOf("") }
    var collegeName by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            rollNumber = it.rollNumber
            branch = it.branch
            semesterString = it.semester.toString()
            division = it.division
            collegeName = it.collegeName
        }
    }

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Edit Profile Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = rollNumber,
                        onValueChange = { rollNumber = it },
                        label = { Text("Roll Number") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = { Text("Branch / Department") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = semesterString,
                            onValueChange = { semesterString = it },
                            label = { Text("Semester") },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = division,
                            onValueChange = { division = it },
                            label = { Text("Division / Sec") },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = collegeName,
                        onValueChange = { collegeName = it },
                        label = { Text("College / University") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                name = name,
                                rollNumber = rollNumber,
                                branch = branch,
                                semester = semesterString.toIntOrNull() ?: 1,
                                division = division,
                                collegeName = collegeName
                            )
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Profile Changes", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            StudentCard(
                backgroundColor = SurfaceDark,
                borderColor = SuccessGreen.copy(alpha = 0.35f)
            ) {
                Text(
                    "🛡️ 100% Offline & Private",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = SuccessGreen
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Your personal data (profile, attendance logs, timetables, and notes) stays entirely on this device. No data is sent to external servers or cloud services.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
            }
        }

        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Backups & Migrations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                    Text(
                        "Transfer your data to another device or create offline recovery points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { createDocLauncher.launch("Student360_Backup_v1.json") },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export Backup File (.json)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { openDocLauncher.launch(arrayOf("application/json")) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardDark),
                        border = BorderStroke(1.dp, BorderDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import Backup File", color = LightPurple, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // About Student360 Section
        item {
            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Student360Logo(
                        emblemSize = 40.dp,
                        showWordmark = true,
                        tagline = "Version 1.0.0 • Academic Command Center"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Student360 is an offline-first academic management platform designed to help students track attendance, manage class schedules, time focus study blocks, and prepare for examinations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElevatedCardDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Text(
                                "🔒 100% Offline",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElevatedCardDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Text(
                                "⚡ Jetpack Compose",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElevatedCardDark,
                            border = BorderStroke(1.dp, BorderDark)
                        ) {
                            Text(
                                "📄 MIT License",
                                style = MaterialTheme.typography.labelSmall,
                                color = SecondaryText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
