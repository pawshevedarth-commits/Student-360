@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.student360.app.data.local.entity.TimetableEntry
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.components.*
import com.student360.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun TimetableScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    scheduleViewModel: ScheduleViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val timetable by scheduleViewModel.timetable.collectAsState()
    val subjectsWithStats by viewModel.subjectsWithStats.collectAsState()
    val allSubjectsWithStats by viewModel.allSubjectsWithStats.collectAsState()
    val overallStats by viewModel.overallStats.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var isEditMode by remember { mutableStateOf(false) }
    var selectedDayTab by remember { mutableStateOf(0) } // 0 = Mon .. 6 = Sun

    var showAddLectureDialog by remember { mutableStateOf(false) }
    var showAddMultipleSubjectsSheet by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<TimetableEntry?>(null) }
    var selectedEntryForEdit by remember { mutableStateOf<TimetableEntry?>(null) }
    var selectedEntryForDetail by remember { mutableStateOf<TimetableEntry?>(null) }

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val fullDaysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    val overallPct = overallStats?.percentage ?: 100.0
    val targetPct = 75

    val overallStatusColor = when {
        overallPct >= 75.0 -> colors.success
        overallPct >= 70.0 -> colors.warning
        else -> colors.danger
    }

    // Determine current day of week from selectedDate
    LaunchedEffect(selectedDate) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val calDay = cal.get(Calendar.DAY_OF_WEEK)
        selectedDayTab = if (calDay == Calendar.SUNDAY) 6 else calDay - 2
    }

    val selectedDayTimetable = timetable.filter { it.dayOfWeek == selectedDayTab }.sortedBy { it.startTime }

    var draggingEntryId by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    var currentDayList by remember(selectedDayTimetable) { mutableStateOf(selectedDayTimetable) }

    LaunchedEffect(selectedDayTimetable) {
        if (draggingEntryId == null) {
            currentDayList = selectedDayTimetable
        }
    }

    val maxSlots = remember(timetable) {
        val maxInAnyDay = (0..6).maxOfOrNull { dayIdx ->
            timetable.count { it.dayOfWeek == dayIdx }
        } ?: 5
        maxInAnyDay.coerceIn(5, 7)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. TOP BAR (Timetable | 59.26% | 75% | + | ✏️ or SAVE)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timetable",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Attendance Pill: 57.41% | 75%
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", overallPct)}% | $targetPct%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = overallStatusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }

                    // In Edit Mode: + Button to add lectures (Save and Stay)
                    if (isEditMode) {
                        IconButton(
                            onClick = { showAddLectureDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Lecture",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Prominent SAVE Button in Edit Mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colors.accent,
                            modifier = Modifier
                                .clickable {
                                    isEditMode = false
                                    Toast.makeText(context, "Timetable saved", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Save",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Save",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        // Pencil Button in Normal Mode
                        IconButton(
                            onClick = { isEditMode = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.card)
                                .border(BorderStroke(1.dp, colors.border), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Timetable",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. WEEKLY DAY TABS: Mon Tue Wed Thu Fri Sat Sun
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    val isSelected = selectedDayTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedDayTab = index
                                // Sync selected date in viewModel
                                val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                                val currentCalDay = cal.get(Calendar.DAY_OF_WEEK)
                                val currentDayIdx = if (currentCalDay == Calendar.SUNDAY) 6 else currentCalDay - 2
                                val diff = index - currentDayIdx
                                cal.add(Calendar.DAY_OF_YEAR, diff)
                                viewModel.selectDate(cal.timeInMillis)
                            }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.accent else colors.textSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.5.dp)
                                    .background(colors.accent, RoundedCornerShape(1.dp))
                            )
                        } else {
                            Spacer(modifier = Modifier.height(2.5.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. MAIN CONTENT: NORMAL MATRIX GRID vs. EDIT MODE LIST
            if (isEditMode) {
                // EDIT MODE: Reorderable list for the selected day
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${fullDaysOfWeek[selectedDayTab]} Classes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Drag ☰ to reorder • Long-press card to delete",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showAddMultipleSubjectsSheet = true },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, colors.accent),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Select Multiple", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { showAddLectureDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Add Lecture", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }

                    if (selectedDayTimetable.isEmpty()) {
                        StudentCard(
                            backgroundColor = colors.card,
                            borderColor = colors.border,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "No lectures scheduled on ${fullDaysOfWeek[selectedDayTab]}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showAddLectureDialog = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Lecture", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    }
                                    OutlinedButton(
                                        onClick = { showAddMultipleSubjectsSheet = true },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, colors.accent),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                                    ) {
                                        Text("Select Multiple", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            itemsIndexed(currentDayList, key = { _, entry -> entry.id }) { _, entry ->
                                val isDragging = draggingEntryId == entry.id
                                val subPair = allSubjectsWithStats.find { it.first.id == entry.subjectId }
                                val sub = subPair?.first
                                val stats = subPair?.second
                                val subjectTitle = sub?.name ?: "Subject"
                                val subjectCode = sub?.code?.ifBlank { null }
                                val faculty = entry.facultyOverride?.ifBlank { null } ?: sub?.faculty?.ifBlank { null }
                                val displayStartTime = formatDisplayTime(entry.startTime)
                                val displayEndTime = formatDisplayTime(entry.endTime)

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDragging) colors.activePill else colors.card,
                                    border = BorderStroke(
                                        if (isDragging) 1.5.dp else 1.dp,
                                        if (isDragging) colors.accent else colors.border
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(if (isDragging) 10f else 1f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffsetY else 0f
                                        }
                                        .shadow(if (isDragging) 10.dp else 0.dp, RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            onClick = { selectedEntryForEdit = entry },
                                            onLongClick = { entryToDelete = entry }
                                        )
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
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Drag Handle
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isDragging) colors.accent.copy(alpha = 0.18f) else colors.elevatedCard)
                                                    .border(BorderStroke(1.dp, if (isDragging) colors.accent else colors.border), RoundedCornerShape(10.dp))
                                                    .pointerInput(entry.id, currentDayList.size) {
                                                        detectDragGestures(
                                                            onDragStart = {
                                                                draggingEntryId = entry.id
                                                                dragOffsetY = 0f
                                                            },
                                                            onDrag = { change, dragAmount ->
                                                                change.consume()
                                                                dragOffsetY += dragAmount.y
                                                                val itemHeightPx = 76.dp.toPx()
                                                                val currentIndex = currentDayList.indexOfFirst { it.id == draggingEntryId }
                                                                if (currentIndex != -1) {
                                                                    if (dragOffsetY > itemHeightPx * 0.6f && currentIndex < currentDayList.size - 1) {
                                                                        val mutable = currentDayList.toMutableList()
                                                                        val target = currentIndex + 1
                                                                        val item = mutable.removeAt(currentIndex)
                                                                        mutable.add(target, item)
                                                                        currentDayList = mutable
                                                                        dragOffsetY -= itemHeightPx
                                                                    } else if (dragOffsetY < -itemHeightPx * 0.6f && currentIndex > 0) {
                                                                        val mutable = currentDayList.toMutableList()
                                                                        val target = currentIndex - 1
                                                                        val item = mutable.removeAt(currentIndex)
                                                                        mutable.add(target, item)
                                                                        currentDayList = mutable
                                                                        dragOffsetY += itemHeightPx
                                                                    }
                                                                }
                                                            },
                                                            onDragEnd = {
                                                                val finalIndex = currentDayList.indexOfFirst { it.id == draggingEntryId }
                                                                val originalIndex = selectedDayTimetable.indexOfFirst { it.id == draggingEntryId }
                                                                if (finalIndex != -1 && originalIndex != -1 && finalIndex != originalIndex) {
                                                                    scheduleViewModel.reorderDayEntries(selectedDayTab, originalIndex, finalIndex)
                                                                }
                                                                draggingEntryId = null
                                                                dragOffsetY = 0f
                                                            },
                                                            onDragCancel = {
                                                                currentDayList = selectedDayTimetable
                                                                draggingEntryId = null
                                                                dragOffsetY = 0f
                                                            }
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Menu,
                                                    contentDescription = "Drag to reorder",
                                                    tint = if (isDragging) colors.accent else colors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                // Title Row with Code / Percentage Badges
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    ) {
                                                        Text(
                                                            text = subjectTitle,
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = colors.textPrimary,
                                                            fontSize = 15.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (!subjectCode.isNullOrBlank()) {
                                                            Surface(
                                                                shape = RoundedCornerShape(6.dp),
                                                                color = colors.elevatedCard,
                                                                border = BorderStroke(1.dp, colors.border)
                                                            ) {
                                                                Text(
                                                                    text = subjectCode,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = colors.textSecondary,
                                                                    fontSize = 10.sp,
                                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Attendance percentage badge if available
                                                    if (stats != null) {
                                                        val pctColor = when {
                                                            stats.percentage >= 75.0 -> colors.success
                                                            stats.percentage >= 70.0 -> colors.warning
                                                            else -> colors.danger
                                                        }
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = pctColor.copy(alpha = 0.12f),
                                                            border = BorderStroke(1.dp, pctColor.copy(alpha = 0.25f))
                                                        ) {
                                                            Text(
                                                                text = "${String.format(Locale.US, "%.0f", stats.percentage)}%",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = pctColor,
                                                                fontSize = 11.sp,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                // Badges Row: Time, Room, Faculty
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Time Badge
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = colors.accent.copy(alpha = 0.12f),
                                                        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.22f))
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                        ) {
                                                            Text(
                                                                text = "⏰ $displayStartTime – $displayEndTime",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = colors.accent,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }

                                                    // Room Badge
                                                    if (entry.room.isNotBlank()) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = colors.elevatedCard,
                                                            border = BorderStroke(1.dp, colors.border)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                            ) {
                                                                Text(
                                                                    text = "📍 ${entry.room}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = colors.textSecondary,
                                                                    fontSize = 11.sp
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Faculty Badge
                                                    if (!faculty.isNullOrBlank()) {
                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = colors.elevatedCard,
                                                            border = BorderStroke(1.dp, colors.border)
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                            ) {
                                                                Text(
                                                                    text = "👤 $faculty",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = colors.textSecondary,
                                                                    fontSize = 11.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Action / Edit Icon
                                        IconButton(
                                            onClick = { selectedEntryForEdit = entry },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit class",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // NORMAL MODE: WEEKLY TIMETABLE MATRIX GRID
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (slotIdx in 0 until maxSlots) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    for (dayIdx in 0..6) {
                                        val dayEntries = timetable.filter { it.dayOfWeek == dayIdx }.sortedBy { it.startTime }
                                        val entry = dayEntries.getOrNull(slotIdx)

                                        if (entry != null) {
                                            val sub = allSubjectsWithStats.find { it.first.id == entry.subjectId }?.first
                                            val subTitle = sub?.name ?: "Subject"

                                            // Lecture cell: rounded rectangle with theme-aware background
                                            val cellBg = if (colors.isDark) colors.accent.copy(alpha = 0.22f) else Color(0xFFE8DEFF)
                                            val cellBorder = if (colors.isDark) colors.accent.copy(alpha = 0.45f) else Color(0xFFD4C4FA)
                                            val cellTextColor = if (colors.isDark) colors.textPrimary else Color(0xFF261D45)

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(68.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(cellBg)
                                                    .border(BorderStroke(1.dp, cellBorder), RoundedCornerShape(8.dp))
                                                    .clickable { if (isEditMode) selectedEntryForEdit = entry else selectedEntryForDetail = entry }
                                                    .padding(horizontal = 3.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.TopStart
                                            ) {
                                                Text(
                                                    text = subTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = cellTextColor,
                                                    fontSize = 10.sp,
                                                    lineHeight = 11.sp,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else {
                                            // Empty cell
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(68.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.card)
                                                    .border(BorderStroke(0.5.dp, colors.border), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedDayTab = dayIdx
                                                        showAddLectureDialog = true
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        // Add Timetable Lecture Dialog (Save Class saves and stays open for continuous additions)
        if (showAddLectureDialog) {
            val subjects = subjectsWithStats.map { it.first }
            if (subjects.isEmpty()) {
                AlertDialog(
                    onDismissRequest = { showAddLectureDialog = false },
                    containerColor = colors.card,
                    title = { Text("No Subjects", color = colors.textPrimary) },
                    text = { Text("Please create a course in the Subjects tab first.", color = colors.textSecondary) },
                    confirmButton = { TextButton(onClick = { showAddLectureDialog = false }) { Text("OK", color = colors.accent) } }
                )
            } else {
                AddLectureDialog(
                    subjects = subjects,
                    initialDay = selectedDayTab,
                    onDismiss = { showAddLectureDialog = false },
                    onSave = { subjectId, day, start, end, room, faculty, alertMin ->
                        scheduleViewModel.addTimetableEntry(subjectId, day, start, end, room, faculty, alertMin)
                        // Note: showAddLectureDialog remains true so the user stays inside the dialog!
                    }
                )
            }
        }

        // Long-Press Delete Confirmation Dialog
        entryToDelete?.let { entry ->
            val sub = allSubjectsWithStats.find { it.first.id == entry.subjectId }?.first
            val subjectTitle = sub?.name ?: "Subject"

            AlertDialog(
                onDismissRequest = { entryToDelete = null },
                containerColor = colors.card,
                titleContentColor = colors.textPrimary,
                title = {
                    Text("Delete this class?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "This class will be removed from your timetable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val deletedEntry = entry
                            scheduleViewModel.deleteTimetableEntry(deletedEntry)
                            entryToDelete = null

                            // Trigger Undo Snackbar
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Removed $subjectTitle",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    scheduleViewModel.restoreTimetableEntry(deletedEntry)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.danger),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { entryToDelete = null }) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                }
            )
        }

        // Edit Existing Timetable Lecture Dialog (In Edit Mode)
        selectedEntryForEdit?.let { entry ->
            val subjects = subjectsWithStats.map { it.first }
            EditLectureDialog(
                entry = entry,
                subjects = subjects,
                onDismiss = { selectedEntryForEdit = null },
                onSave = { updatedEntry ->
                    scheduleViewModel.updateTimetableEntry(updatedEntry)
                    selectedEntryForEdit = null
                    Toast.makeText(context, "Class updated", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Add Multiple Subjects BottomSheet (Batch Subject Selection)
        if (showAddMultipleSubjectsSheet) {
            val subjects = subjectsWithStats.map { it.first }
            val existingIds = selectedDayTimetable.map { it.subjectId }.toSet()
            AddMultipleSubjectsBottomSheet(
                subjects = subjects,
                dayName = fullDaysOfWeek[selectedDayTab],
                existingSubjectIdsInDay = existingIds,
                onDismiss = { showAddMultipleSubjectsSheet = false },
                onAddSubjects = { selectedIds ->
                    scheduleViewModel.addMultipleTimetableEntries(selectedIds, selectedDayTab)
                    showAddMultipleSubjectsSheet = false
                    Toast.makeText(
                        context,
                        "Added ${selectedIds.size} classes to ${fullDaysOfWeek[selectedDayTab]}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }

        // Lecture Detail Dialog (Normal Mode Tap)
        selectedEntryForDetail?.let { entry ->
            val sub = subjectsWithStats.find { it.first.id == entry.subjectId }?.first
            AlertDialog(
                onDismissRequest = { selectedEntryForDetail = null },
                containerColor = colors.card,
                titleContentColor = colors.textPrimary,
                title = {
                    Text(
                        sub?.name ?: "Lecture Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🗓 Day: ${fullDaysOfWeek[entry.dayOfWeek]}", color = colors.textPrimary, style = MaterialTheme.typography.bodyMedium)
                        Text("⏰ Time: ${entry.startTime} – ${entry.endTime}", color = colors.accent, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        if (entry.room.isNotBlank()) {
                            Text("📍 Room: ${entry.room}", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        val fac = entry.facultyOverride?.ifBlank { null } ?: sub?.faculty?.ifBlank { null }
                        if (fac != null) {
                            Text("👤 Faculty: $fac", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isEditMode = true
                            selectedDayTab = entry.dayOfWeek
                            selectedEntryForDetail = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Edit Timetable", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedEntryForDetail = null }) {
                        Text("Close", color = colors.textSecondary)
                    }
                }
            )
        }

        // Bottom Snackbar Host for Delete Undo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
