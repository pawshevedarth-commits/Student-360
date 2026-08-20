package com.student360.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.repository.StudentRepository

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Edit Profile Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rollNumber, onValueChange = { rollNumber = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = semesterString, onValueChange = { semesterString = it }, label = { Text("Semester") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = division, onValueChange = { division = it }, label = { Text("Division") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = collegeName, onValueChange = { collegeName = it }, label = { Text("College Name") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(8.dp))
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Profile Changes")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🛡️ Data Safety Guarantee", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your personal data (profile, attendance logs, timetables, and notes) stays entirely on this device. No data is sent to external servers or cloud services.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Backups & Migrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Transfer your data to another device or create offline recovery points.", style = MaterialTheme.typography.bodySmall)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { createDocLauncher.launch("Student360_Backup_v1.json") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Export Backup File")
                    }
                    Button(
                        onClick = { openDocLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Import Backup File")
                    }
                }
            }
        }
    }
}
