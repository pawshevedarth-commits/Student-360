package com.student360.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.student360.app.data.local.entity.StudentProfile
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    repository: StudentRepository,
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()

    // Step 1 Profile State
    var name by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var semesterString by remember { mutableStateOf("1") }
    var division by remember { mutableStateOf("") }
    var collegeName by remember { mutableStateOf("") }

    // Step 2 Subjects State
    var subName by remember { mutableStateOf("") }
    var subCode by remember { mutableStateOf("") }
    var subFaculty by remember { mutableStateOf("") }
    val addedSubjects = remember { mutableStateListOf<Subject>() }

    // Step 3 Overrides State
    val manualAttendedMap = remember { mutableStateMapOf<Int, String>() }
    val manualConductedMap = remember { mutableStateMapOf<Int, String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = step.toFloat() / 3f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        Text(
            text = "Step $step of 3",
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (step) {
            1 -> {
                // Step 1 UI: Profile Setup
                Text("Enter Profile Details", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = rollNumber, onValueChange = { rollNumber = it }, label = { Text("Roll Number") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = branch, onValueChange = { branch = it }, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = semesterString, onValueChange = { semesterString = it }, label = { Text("Semester") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = division, onValueChange = { division = it }, label = { Text("Division") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = collegeName, onValueChange = { collegeName = it }, label = { Text("College Name") }, modifier = Modifier.fillMaxWidth())
                
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { if (name.isNotBlank()) step = 2 },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && collegeName.isNotBlank()
                ) {
                    Text("Next: Add Subjects")
                }
            }
            2 -> {
                // Step 2 UI: Add Subjects
                Text("Add Academic Subjects", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(value = subName, onValueChange = { subName = it }, label = { Text("Subject Name (e.g. DSA)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = subCode, onValueChange = { subCode = it }, label = { Text("Code (e.g. CS201)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = subFaculty, onValueChange = { subFaculty = it }, label = { Text("Faculty Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        if (subName.isNotBlank()) {
                            addedSubjects.add(
                                Subject(
                                    name = subName,
                                    code = subCode,
                                    faculty = subFaculty
                                )
                            )
                            subName = ""
                            subCode = ""
                            subFaculty = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Subject")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Subjects added: ${addedSubjects.size}", style = MaterialTheme.typography.bodyLarge)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(addedSubjects) { sub ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${sub.name} (${sub.code})", modifier = Modifier.weight(1f))
                                TextButton(onClick = { addedSubjects.remove(sub) }) {
                                    Text("Remove", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { if (addedSubjects.isNotEmpty()) step = 3 },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = addedSubjects.isNotEmpty()
                ) {
                    Text("Next: Attendance Baseline")
                }
            }
            3 -> {
                // Step 3 UI: Attendance Overrides
                Text("Enter Starting Attendance (Optional)", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("If you are starting midway through the semester, specify your current counts. Otherwise skip.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(addedSubjects.size) { index ->
                        val sub = addedSubjects[index]
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(sub.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = manualAttendedMap[index] ?: "",
                                        onValueChange = { manualAttendedMap[index] = it },
                                        label = { Text("Attended") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = manualConductedMap[index] ?: "",
                                        onValueChange = { manualConductedMap[index] = it },
                                        label = { Text("Conducted") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val sem = semesterString.toIntOrNull() ?: 1
                            val profile = StudentProfile(
                                id = 1,
                                name = name,
                                rollNumber = rollNumber,
                                branch = branch,
                                semester = sem,
                                division = division,
                                collegeName = collegeName,
                                onboarded = true
                            )
                            repository.saveProfile(profile)

                            addedSubjects.forEachIndexed { index, sub ->
                                val attended = manualAttendedMap[index]?.toIntOrNull() ?: 0
                                val conducted = manualConductedMap[index]?.toIntOrNull() ?: 0
                                repository.insertSubject(
                                    sub.copy(
                                        manualAttended = attended,
                                        manualConducted = conducted
                                    )
                                )
                            }
                            onFinished()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Finish Setup")
                }
            }
        }
    }
}
