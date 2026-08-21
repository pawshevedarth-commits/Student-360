@file:OptIn(ExperimentalMaterial3Api::class)

package com.student360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.student360.app.data.local.entity.StudentProfile
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
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
            .background(BgDark)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StudentProgressBar(
            progress = step.toFloat() / 3f,
            color = PrimaryPurple,
            trackColor = SurfaceDark,
            height = 6.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "STEP $step OF 3",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = LightPurple
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (step) {
            1 -> {
                // Step 1 UI: Profile Setup
                Student360Logo(emblemSize = 48.dp, showWordmark = true)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Welcome to Student360 👋",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    "Let's get your academic profile set up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(16.dp))

                StudentCard(
                    backgroundColor = CardDark,
                    borderColor = BorderDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                            label = { Text("Branch / Major") },
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
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { if (name.isNotBlank()) step = 2 },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = name.isNotBlank() && collegeName.isNotBlank()
                ) {
                    Text("Next: Add Subjects →", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            2 -> {
                // Step 2 UI: Add Subjects
                Text(
                    "Add Academic Subjects 📚",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    "Add your course subjects for attendance and study tracking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(16.dp))

                StudentCard(
                    backgroundColor = CardDark,
                    borderColor = BorderDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = subName,
                            onValueChange = { subName = it },
                            label = { Text("Subject Name (e.g. Data Structures)") },
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
                                value = subCode,
                                onValueChange = { subCode = it },
                                label = { Text("Code (e.g. CS201)") },
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
                                value = subFaculty,
                                onValueChange = { subFaculty = it },
                                label = { Text("Faculty (optional)") },
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
                            colors = ButtonDefaults.buttonColors(containerColor = ElevatedCardDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            enabled = subName.isNotBlank()
                        ) {
                            Text("+ Add Subject to List", color = LightPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                SectionHeader(title = "Subjects Added (${addedSubjects.size})")

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(addedSubjects) { sub ->
                        StudentCard(
                            backgroundColor = SurfaceDark,
                            borderColor = BorderDark
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.name, fontWeight = FontWeight.Bold, color = PrimaryText)
                                    if (sub.code.isNotBlank()) {
                                        Text(sub.code, style = MaterialTheme.typography.bodySmall, color = SecondaryText)
                                    }
                                }
                                TextButton(onClick = { addedSubjects.remove(sub) }) {
                                    Text("Remove", color = DangerRed, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { if (addedSubjects.isNotEmpty()) step = 3 },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = addedSubjects.isNotEmpty()
                ) {
                    Text("Next: Starting Baseline →", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            3 -> {
                // Step 3 UI: Attendance Overrides
                Text(
                    "Starting Attendance Baseline 📊",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Text(
                    "Starting midway in the semester? Enter current attendance count. Otherwise leave as 0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(addedSubjects.size) { index ->
                        val sub = addedSubjects[index]
                        StudentCard(
                            backgroundColor = CardDark,
                            borderColor = BorderDark
                        ) {
                            Text(sub.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PrimaryText)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manualAttendedMap[index] ?: "",
                                    onValueChange = { manualAttendedMap[index] = it },
                                    label = { Text("Attended") },
                                    placeholder = { Text("0") },
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
                                    value = manualConductedMap[index] ?: "",
                                    onValueChange = { manualConductedMap[index] = it },
                                    label = { Text("Conducted") },
                                    placeholder = { Text("0") },
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🚀 Finish Setup & Enter Student360", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
