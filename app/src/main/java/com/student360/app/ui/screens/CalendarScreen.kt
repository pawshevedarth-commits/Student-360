@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student360.app.data.repository.StudentRepository
import com.student360.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Reference Calendar Color Tokens matching the screenshot
private val CalendarBgDark = Color(0xFF161616)
private val CalendarTextPrimary = Color(0xFFF2F2F7)
private val CalendarTextMuted = Color(0xFF8E8E93)
private val CalendarTodayBlue = Color(0xFF4C8DF5)
private val CalendarSelectedCircle = Color(0xFF44546A)
private val CalendarEventDot = Color(0xFF4C8DF5)

@Composable
fun CalendarScreen(
    repository: StudentRepository,
    viewModel: AttendanceViewModel,
    onNavigateToToday: (Long) -> Unit = {}
) {
    val heatmapData by viewModel.heatmapData.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.ENGLISH) }

    val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")

    val cal = currentMonth.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val startOffset = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 for Sunday
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Current date (Today) comparison
    val todayCal = Calendar.getInstance()
    val isCurrentMonthToday = (todayCal.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
            todayCal.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH))
    val todayDayOfMonth = if (isCurrentMonthToday) todayCal.get(Calendar.DAY_OF_MONTH) else -1

    // Week indicator calculation (e.g. "Week 35")
    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val weekNumber = if (selectedCal.get(Calendar.YEAR) == currentMonth.get(Calendar.YEAR) &&
        selectedCal.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH)
    ) {
        selectedCal.get(Calendar.WEEK_OF_YEAR)
    } else {
        cal.get(Calendar.WEEK_OF_YEAR)
    }

    // Standard Indian Calendar Festivals / Holidays for demo & matching reference
    val monthIndex = currentMonth.get(Calendar.MONTH)
    val holidaysMap = remember(monthIndex) {
        when (monthIndex) {
            Calendar.AUGUST -> mapOf(
                15 to "स्वतंत्रता दिवस",
                26 to "ओणम",
                28 to "रक..."
            )
            Calendar.JANUARY -> mapOf(26 to "गणतंत्र दिवस")
            Calendar.MARCH -> mapOf(25 to "होली")
            Calendar.OCTOBER -> mapOf(2 to "गांधी जयंती", 20 to "दशहरा")
            Calendar.NOVEMBER -> mapOf(1 to "दिवाली")
            Calendar.DECEMBER -> mapOf(25 to "क्रिसमस")
            else -> emptyMap()
        }
    }

    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CalendarBgDark)
    ) {
        // 1. CALENDAR HEADER: < August 2026 Week 35       ⋮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = { viewModel.previousMonth() },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = monthFormatter.format(currentMonth.time),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )

                    Text(
                        text = "Week $weekNumber",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CalendarTextMuted,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF222222))
                ) {
                    DropdownMenuItem(
                        text = { Text("Today", color = Color.White) },
                        onClick = {
                            val todayMillis = System.currentTimeMillis()
                            viewModel.selectDate(todayMillis)
                            onNavigateToToday(todayMillis)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Next Month", color = Color.White) },
                        onClick = {
                            viewModel.nextMonth()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Previous Month", color = Color.White) },
                        onClick = {
                            viewModel.previousMonth()
                            showMenu = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. WEEKDAY ROW: S   M   T   W   T   F   S
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CalendarTextMuted,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. CALENDAR GRID: 7 equal columns with exact reference styling
        val totalSlots = startOffset + daysInMonth
        val rows = (totalSlots + 6) / 7

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    for (col in 0..6) {
                        val dayNum = (row * 7 + col) - startOffset + 1

                        if (dayNum in 1..daysInMonth) {
                            val dayCal = (cal.clone() as Calendar).apply {
                                set(Calendar.DAY_OF_MONTH, dayNum)
                            }
                            val dayTime = dayCal.timeInMillis
                            val isSelected = (dayCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                                    dayCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                                    dayCal.get(Calendar.DAY_OF_MONTH) == selectedCal.get(Calendar.DAY_OF_MONTH))

                            val isToday = (dayNum == todayDayOfMonth)
                            val holidayText = holidaysMap[dayNum]
                            val dayStats = heatmapData[dayTime]
                            val hasEvent = (holidayText != null || dayStats != null)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                                    .clickable { viewModel.selectDate(dayTime) },
                                contentAlignment = Alignment.TopCenter
                            ) {
                                if (isSelected) {
                                    // Selected Date: Circular muted blue/gray background with white text & optional text inside
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(CalendarSelectedCircle),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 18.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                                if (holidayText != null) {
                                                    Text(
                                                        text = holidayText,
                                                        color = Color.White.copy(alpha = 0.9f),
                                                        fontSize = 8.sp,
                                                        lineHeight = 9.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }

                                        // Blue event dot below selection
                                        if (hasEvent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(4.5.dp)
                                                    .background(CalendarEventDot, CircleShape)
                                            )
                                        }
                                    }
                                } else {
                                    // Non-selected Date
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            color = when {
                                                isToday -> CalendarTodayBlue
                                                else -> CalendarTextPrimary
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 18.sp,
                                            textAlign = TextAlign.Center
                                        )

                                        if (holidayText != null) {
                                            Text(
                                                text = holidayText,
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontSize = 9.sp,
                                                lineHeight = 10.sp,
                                                fontWeight = FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }

                                        if (hasEvent) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = 1.dp)
                                                    .size(4.5.dp)
                                                    .background(CalendarEventDot, CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // Empty slot before day 1 or after last day
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(72.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
