@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.student360.app.data.local.entity.AssignmentPriority
import com.student360.app.data.local.entity.TaskCategory
import com.student360.app.data.local.entity.TaskPriority
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import java.util.*

@Composable
fun MyDayScreen(
    repository: StudentRepository,
    viewModel: MyDayViewModel = viewModel()
) {
    val colors = LocalAppColors.current
    val myDayItems by viewModel.myDayItems.collectAsState()
    val progress by viewModel.completionPercentage.collectAsState()
    val subjects by viewModel.subjects.collectAsState()

    var showAddMenu by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddAssignDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = colors.bg,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
            ) {
                if (showAddMenu) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 6.dp,
                        modifier = Modifier.clickable {
                            showAddMenu = false
                            showAddAssignDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Text("Assignment", fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 6.dp,
                        modifier = Modifier.clickable {
                            showAddMenu = false
                            showAddTaskDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Text("Task", fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 6.dp,
                        modifier = Modifier.clickable {
                            showAddMenu = false
                            showAddGoalDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Text("Goal", fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.sp)
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showAddMenu = !showAddMenu },
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(46.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (showAddMenu) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Text(
                            "Add",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colors.bg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Today's Progress Card
            val pctInt = (progress * 100).toInt()
            val progressColor = if (pctInt == 100) SuccessGreen else PrimaryPurple

            StudentCard(
                backgroundColor = CardDark,
                borderColor = BorderDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Today's Progress",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SecondaryText
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$pctInt%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (pctInt == 100) SuccessGreen else PrimaryText
                        )
                    }
                    if (pctInt == 100 && myDayItems.isNotEmpty()) {
                        StatusBadge(text = "All Done 🎉", color = SuccessGreen)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                StudentProgressBar(
                    progress = progress,
                    color = progressColor,
                    trackColor = SurfaceDark,
                    height = 8.dp
                )
            }

            if (myDayItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.CheckCircle,
                    title = "You're all caught up 🎉",
                    subtitle = "No tasks or assignments due today. Add a new item to plan your day.",
                    actionText = "+ Add Task",
                    onActionClick = { showAddTaskDialog = true },
                    modifier = Modifier.weight(1f)
                )
            } else {
                val categories = listOf("COLLEGE", "STUDY", "ASSIGNMENTS", "PERSONAL")

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    categories.forEach { cat ->
                        val catItems = myDayItems.filter { it.category == cat }
                        if (catItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = cat,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LightPurple,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
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

        if (showAddGoalDialog) {
            var goalTitle by remember { mutableStateOf("") }
            var goalTarget by remember { mutableStateOf("10") }
            var goalDueDays by remember { mutableStateOf("14") }

            AlertDialog(
                onDismissRequest = { showAddGoalDialog = false },
                containerColor = colors.card,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textPrimary,
                title = {
                    Text("Add Academic Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = goalTitle,
                            onValueChange = { goalTitle = it },
                            label = { Text("Goal Title (e.g. Complete DBMS Unit 2)") },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = goalTarget,
                            onValueChange = { goalTarget = it },
                            label = { Text("Target count (e.g. 10)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colors.accent,
                                unfocusedBorderColor = colors.border,
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (goalTitle.isNotBlank()) {
                                val dueMillis = System.currentTimeMillis() + ((goalDueDays.toLongOrNull() ?: 14L) * 24 * 60 * 60 * 1000L)
                                viewModel.addGoal(goalTitle, dueMillis, goalTarget.toDoubleOrNull() ?: 10.0)
                                showAddGoalDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(10.dp),
                        enabled = goalTitle.isNotBlank()
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddGoalDialog = false }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
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
    val colors = LocalAppColors.current
    val prioColor = when (item.priority) {
        TaskPriority.URGENT -> colors.danger
        TaskPriority.HIGH -> colors.warning
        TaskPriority.MEDIUM -> colors.accent
        TaskPriority.LOW -> colors.textSecondary
    }

    StudentCard(
        backgroundColor = if (item.completed) colors.card.copy(alpha = 0.5f) else colors.card,
        borderColor = if (item.completed) colors.border.copy(alpha = 0.4f) else colors.border
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (item.category == "COLLEGE") {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(colors.accent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏛", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Checkbox(
                        checked = item.completed,
                        onCheckedChange = onToggle,
                        colors = CheckboxDefaults.colors(
                            checkedColor = colors.success,
                            uncheckedColor = colors.textSecondary,
                            checkmarkColor = Color.White
                        )
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        item.title,
                        fontWeight = if (item.completed) FontWeight.Normal else FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (item.completed) colors.textSecondary else colors.textPrimary,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (item.category != "COLLEGE" && !item.completed) {
                Spacer(modifier = Modifier.width(8.dp))
                StatusBadge(text = item.priority.name, color = prioColor)
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
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Add Daily Task",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
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
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
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
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = catDropdownExpanded,
                        onDismissRequest = { catDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        TaskCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name, color = PrimaryText) },
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
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = BorderDark,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subDropdownExpanded,
                            onDismissRequest = { subDropdownExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            DropdownMenuItem(
                                text = { Text("None", color = PrimaryText) },
                                onClick = {
                                    subjectIndex = -1
                                    subDropdownExpanded = false
                                }
                            )
                            subjects.forEachIndexed { index, sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name, color = PrimaryText) },
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
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = prioDropdownExpanded,
                        onDismissRequest = { prioDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        TaskPriority.values().forEach { prio ->
                            DropdownMenuItem(
                                text = { Text(prio.name, color = PrimaryText) },
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val subId = if (subjectIndex == -1) null else subjects[subjectIndex].id
                    onSave(title, desc, category, subId, priority, durationString.toIntOrNull() ?: 30)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp),
                enabled = title.isNotBlank()
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
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
        containerColor = SurfaceDark,
        titleContentColor = PrimaryText,
        textContentColor = PrimaryText,
        title = {
            Text(
                "Add Assignment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Assignment Name") },
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
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = BorderDark,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText
                    ),
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
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = subDropdownExpanded,
                        onDismissRequest = { subDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        subjects.forEachIndexed { index, sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name, color = PrimaryText) },
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
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = prioDropdownExpanded,
                        onDismissRequest = { prioDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        AssignmentPriority.values().forEach { prio ->
                            DropdownMenuItem(
                                text = { Text(prio.name, color = PrimaryText) },
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, subjects[subjectIndex].id, desc, dueDaysString.toIntOrNull() ?: 3, priority)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}
