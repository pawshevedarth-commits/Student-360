@file:OptIn(ExperimentalMaterial3Api::class)

package com.student360.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student360.app.ui.theme.LocalAppColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Format timestamp millis into clean user-friendly date string (e.g. "28 Aug 2026")
 */
fun formatDisplayDate(millis: Long): String {
    if (millis <= 0) return "Select Date"
    return try {
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        sdf.format(Date(millis))
    } catch (e: Exception) {
        "Select Date"
    }
}

/**
 * Format 24-hour "HH:mm" time string into friendly 12-hour "hh:mm AM/PM" string.
 */
fun formatDisplayTime(time24h: String): String {
    if (time24h.isBlank()) return "Select Time"
    return try {
        val parts = time24h.trim().split(":")
        val hour = parts[0].toInt()
        val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
    } catch (e: Exception) {
        time24h
    }
}

/**
 * Suggests end time 1 hour ahead of the given start time (preserving 24-hour HH:mm format).
 */
fun calculateNextHour(startTime24h: String): String {
    return try {
        val parts = startTime24h.trim().split(":")
        val hour = parts[0].toInt()
        val minute = parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
        val nextHour = (hour + 1).coerceAtMost(23)
        String.format(Locale.US, "%02d:%02d", nextHour, minute)
    } catch (e: Exception) {
        "10:00"
    }
}

/**
 * Advances to the next contiguous 1-hour time slot for continuous timetable lecture addition.
 */
fun advanceTimeSlot(currentEnd24h: String): Pair<String, String> {
    return try {
        val parts = currentEnd24h.trim().split(":")
        val endHour = parts[0].toInt()
        val nextStart = String.format(Locale.US, "%02d:00", endHour.coerceIn(0, 23))
        val nextEnd = String.format(Locale.US, "%02d:00", (endHour + 1).coerceIn(1, 23))
        nextStart to nextEnd
    } catch (e: Exception) {
        "09:00" to "10:00"
    }
}

/**
 * Clean, modern Material 3 Date Picker Modal Dialog with Student360 theming.
 */
@Composable
fun StudentDatePickerModal(
    initialDateMillis: Long = System.currentTimeMillis(),
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val validInitial = if (initialDateMillis > 0) initialDateMillis else System.currentTimeMillis()

    // Adjust for UTC offset in DatePickerState
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = validInitial
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    datePickerState.selectedDateMillis?.let { utcMillis ->
                        // Convert UTC midnight from picker to local midnight timestamp
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
                        val localCal = Calendar.getInstance().apply {
                            set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
                        }
                        onDateSelected(localCal.timeInMillis)
                    } ?: onDateSelected(validInitial)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        },
        colors = DatePickerDefaults.colors(
            containerColor = colors.card
        )
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                titleContentColor = colors.textPrimary,
                headlineContentColor = colors.textPrimary,
                selectedDayContainerColor = colors.accent,
                selectedDayContentColor = Color.White,
                todayDateBorderColor = colors.accent,
                todayContentColor = colors.accent,
                weekdayContentColor = colors.textSecondary,
                yearContentColor = colors.textPrimary,
                currentYearContentColor = colors.accent,
                selectedYearContainerColor = colors.accent,
                selectedYearContentColor = Color.White
            )
        )
    }
}

/**
 * Clean, modern Material 3 Time Picker Modal Dialog with Student360 theming.
 */
@Composable
fun StudentTimePickerModal(
    initialTime24h: String = "09:00",
    title: String = "Select Time",
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val cal = Calendar.getInstance()
    var hour = cal.get(Calendar.HOUR_OF_DAY)
    var minute = cal.get(Calendar.MINUTE)

    if (initialTime24h.contains(":")) {
        val parts = initialTime24h.split(":")
        parts.getOrNull(0)?.toIntOrNull()?.let { hour = it.coerceIn(0, 23) }
        parts.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()?.let { minute = it.coerceIn(0, 59) }
    }

    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.card,
        titleContentColor = colors.textPrimary,
        title = {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = colors.bg,
                        selectorColor = colors.accent,
                        periodSelectorBorderColor = colors.border,
                        periodSelectorSelectedContainerColor = colors.accent.copy(alpha = 0.2f),
                        periodSelectorSelectedContentColor = colors.accent,
                        periodSelectorUnselectedContainerColor = colors.elevatedCard,
                        periodSelectorUnselectedContentColor = colors.textSecondary,
                        timeSelectorSelectedContainerColor = colors.accent.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = colors.accent,
                        timeSelectorUnselectedContainerColor = colors.elevatedCard,
                        timeSelectorUnselectedContentColor = colors.textPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted24h = String.format(Locale.US, "%02d:%02d", timePickerState.hour, timePickerState.minute)
                    onTimeSelected(formatted24h)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

/**
 * Standard compact, clickable Date Input Field.
 */
@Composable
fun StudentDateField(
    label: String,
    dateMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.DateRange,
    isOptional: Boolean = false
) {
    val colors = LocalAppColors.current
    val displayDate = if (dateMillis > 0) formatDisplayDate(dateMillis) else if (isOptional) "Optional" else "Select Date"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (dateMillis > 0) colors.textPrimary else colors.textMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Standard compact, clickable Time Input Field.
 */
@Composable
fun StudentTimeField(
    label: String,
    time24h: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Notifications,
    isOptional: Boolean = false
) {
    val colors = LocalAppColors.current
    val displayTime = if (time24h.isNotBlank()) formatDisplayTime(time24h) else if (isOptional) "Optional" else "Select Time"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (time24h.isNotBlank()) colors.textPrimary else colors.textMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Smart Quick Date Shortcut Chips (e.g. [ Today ], [ Tomorrow ], [ In 3 Days ], [ In 1 Week ]).
 */
@Composable
fun SmartDateShortcutsRow(
    selectedDateMillis: Long,
    onShortcutSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    includeToday: Boolean = true
) {
    val colors = LocalAppColors.current

    val shortcuts = remember(includeToday) {
        val list = mutableListOf<Pair<String, Long>>()
        if (includeToday) {
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 0)
            }
            list.add("Today" to todayCal.timeInMillis)
        }
        val tomorrowCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
        }
        list.add("Tomorrow" to tomorrowCal.timeInMillis)

        val in3DaysCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 3)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
        }
        list.add("In 3 Days" to in3DaysCal.timeInMillis)

        val in1WeekCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
        }
        list.add("In 1 Week" to in1WeekCal.timeInMillis)

        list
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        shortcuts.forEach { (title, millis) ->
            val selCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            val itemCal = Calendar.getInstance().apply { timeInMillis = millis }
            val isSelected = selectedDateMillis > 0 &&
                    selCal.get(Calendar.YEAR) == itemCal.get(Calendar.YEAR) &&
                    selCal.get(Calendar.DAY_OF_YEAR) == itemCal.get(Calendar.DAY_OF_YEAR)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) colors.accent else colors.elevatedCard,
                border = BorderStroke(1.dp, if (isSelected) colors.accent else colors.border),
                modifier = Modifier.clickable { onShortcutSelected(millis) }
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
