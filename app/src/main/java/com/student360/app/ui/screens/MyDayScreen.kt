package com.student360.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.AssignmentPriority
import com.student360.app.data.local.entity.TaskCategory
import com.student360.app.data.local.entity.TaskPriority
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.theme.SafeGreen
import com.student360.app.ui.theme.SafeGreenLight
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDayScreen(
    repository: StudentRepository,
    viewModel: MyDayViewModel = viewModel()
) {
    val myDayItems by viewModel.myDayItems.collectAsState()
    val progress by viewModel.completionPercentage.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddAssignDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = { showAddTaskDialog = true }, containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Task")
                    }
                }
                if (subjects.isNotEmpty()) {
                    FloatingActionButton(onClick = { showAddAssignDialog = true }, containerColor = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, contentDescription = "Add Assignment")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Assignment")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Today's Progress: ${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = SafeGreen,
                        trackColor = SafeGreenLight
                    )
                }
            }

            if (myDayItems.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Your schedule is clear! Add some tasks or assignments.")
                }
            } else {
                val categories = listOf("COLLEGE", "STUDY", "ASSIGNMENTS", "PERSONAL")
                
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    categories.forEach { cat ->
                        val catItems = myDayItems.filter { it.category == cat }
                        if (catItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = cat,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(catItems) { item ->
                                MyDayChecklistItem(
                                    item = item,
                                    onToggle = { checked -> viewModel.toggleItemCompleted(item, checked) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showAddTaskDialog) {
            AddTaskDialog(
                subjects = subjects,
                onDismiss = { showAddTaskDialog = false },
                onSave = { title, desc, category, subId, priority, duration ->
                    viewModel.addTask(title, desc, category, subId, priority, System.currentTimeMillis(), duration)
                    showAddTaskDialog = false
                }
            )
        }

        if (showAddAssignDialog && subjects.isNotEmpty()) {
            AddAssignmentDialog(
                subjects = subjects,
                onDismiss = { showAddAssignDialog = false },
                onSave = { name, subId, desc, dueDays, priority ->
                    val dueMillis = System.currentTimeMillis() + (dueDays * 24 * 60 * 60 * 1000L)
                    viewModel.addAssignment(name, subId, desc, dueMillis, priority)
                    showAddAssignDialog = false
                }
            )
        }
    }
}

@Composable
fun MyDayChecklistItem(
    item: MyDayItem,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (item.category == "COLLEGE") {
                    Box(modifier = Modifier.width(24.dp))
                } else {
                    Checkbox(
                        checked = item.completed,
                        onCheckedChange = onToggle
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        item.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (item.category != "COLLEGE" && !item.completed) {
                Text(
                    item.priority.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = when (item.priority) {
                        TaskPriority.URGENT -> MaterialTheme.colorScheme.error
                        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (String, String, TaskCategory, Int?, TaskPriority, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.PERSONAL) }
    var subjectIndex by remember { mutableStateOf(-1) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var durationString by remember { mutableStateOf("30") }

    var catDropdownExpanded by remember { mutableStateOf(false) }
    var subDropdownExpanded by remember { mutableStateOf(false) }
    var prioDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Daily Task") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = catDropdownExpanded,
                    onExpandedChange = { catDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false }
                    ) {
                        TaskCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    category = cat
                                    catDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (subjects.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = subDropdownExpanded,
                        onExpandedChange = { subDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (subjectIndex == -1) "None" else subjects[subjectIndex].name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Linked Subject") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subDropdownExpanded,
                            onDismissRequest = { subDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    subjectIndex = -1
                                    subDropdownExpanded = false
                                }
                            )
                            subjects.forEachIndexed { index, sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name) },
                                    onClick = {
                                        subjectIndex = index
                                        subDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = prioDropdownExpanded,
                    onExpandedChange = { prioDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = priority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prioDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = prioDropdownExpanded,
                        onDismissRequest = { prioDropdownExpanded = false }
                    ) {
                        TaskPriority.values().forEach { prio ->
                            DropdownMenuItem(
                                text = { Text(prio.name) },
                                onClick = {
                                    priority = prio
                                    prioDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = durationString,
                    onValueChange = { durationString = it },
                    label = { Text("Duration (Minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val subId = if (subjectIndex == -1) null else subjects[subjectIndex].id
                    onSave(title, desc, category, subId, priority, durationString.toIntOrNull() ?: 30)
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssignmentDialog(
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, Int, AssignmentPriority) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var subjectIndex by remember { mutableStateOf(0) }
    var dueDaysString by remember { mutableStateOf("3") }
    var priority by remember { mutableStateOf(AssignmentPriority.MEDIUM) }

    var subDropdownExpanded by remember { mutableStateOf(false) }
    var prioDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Assignment") },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Assignment Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = subDropdownExpanded,
                    onExpandedChange = { subDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = subjects[subjectIndex].name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subDropdownExpanded,
                        onDismissRequest = { subDropdownExpanded = false }
                    ) {
                        subjects.forEachIndexed { index, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    subjectIndex = index
                                    subDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = prioDropdownExpanded,
                    onExpandedChange = { prioDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = priority.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = prioDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = prioDropdownExpanded,
                        onDismissRequest = { prioDropdownExpanded = false }
                    ) {
                        AssignmentPriority.values().forEach { prio ->
                            DropdownMenuItem(
                                text = { Text(prio.name) },
                                onClick = {
                                    priority = prio
                                    prioDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = dueDaysString,
                    onValueChange = { dueDaysString = it },
                    label = { Text("Due in (Days from now)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, subjects[subjectIndex].id, desc, dueDaysString.toIntOrNull() ?: 3, priority)
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
