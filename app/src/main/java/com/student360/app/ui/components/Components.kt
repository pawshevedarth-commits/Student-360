package com.student360.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import com.student360.app.ui.screens.DayAttendanceState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.student360.app.ui.theme.*
import java.util.Locale

/**
 * Official Scholar / Student360 3x3 Matrix Emblem branding symbol.
 */
@Composable
fun Student360Emblem(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val squircleCorner = size * 0.26f
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(squircleCorner))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF131D36), Color(0xFF090F1C)),
                    start = Offset.Zero,
                    end = Offset.Infinite
                )
            )
            .border(
                width = 1.dp,
                color = Color(0xFF1E293B).copy(alpha = 0.7f),
                shape = RoundedCornerShape(squircleCorner)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.62f)) {
            val totalW = this.size.width

            val gap = totalW * 0.08f
            val cellSize = (totalW - 2 * gap) / 3f
            val cornerRadius = CornerRadius(cellSize * 0.28f, cellSize * 0.28f)

            // 3x3 Matrix Colors matching the Figma Academic App Logo
            val matrixColors = listOf(
                listOf(Color(0xFF141F36), Color(0xFF1C2C4E), Color(0xFF2952E3)),
                listOf(Color(0xFF1C2C4E), Color(0xFF2952E3), Color(0xFF06B6D4)),
                listOf(Color(0xFF2952E3), Color(0xFF06B6D4), Color(0xFF22D3EE))
            )

            for (r in 0..2) {
                for (c in 0..2) {
                    val left = c * (cellSize + gap)
                    val top = r * (cellSize + gap)

                    drawRoundRect(
                        color = matrixColors[r][c],
                        topLeft = Offset(left, top),
                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize),
                        cornerRadius = cornerRadius
                    )

                    // Draw white checkmark in cell [2,2]
                    if (r == 2 && c == 2) {
                        val cellCenterX = left + cellSize / 2f
                        val cellCenterY = top + cellSize / 2f

                        val checkPath = Path().apply {
                            moveTo(cellCenterX - cellSize * 0.25f, cellCenterY + cellSize * 0.02f)
                            lineTo(cellCenterX - cellSize * 0.06f, cellCenterY + cellSize * 0.22f)
                            lineTo(cellCenterX + cellSize * 0.26f, cellCenterY - cellSize * 0.20f)
                        }

                        drawPath(
                            path = checkPath,
                            color = Color.White,
                            style = Stroke(
                                width = cellSize * 0.22f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Scholar Emblem alias for academic branding.
 */
@Composable
fun ScholarEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Student360Emblem(modifier = modifier, size = size)
}

/**
 * Full Student360 Logo with Emblem and Wordmark.
 */
@Composable
fun Student360Logo(
    modifier: Modifier = Modifier,
    emblemSize: Dp = 38.dp,
    showWordmark: Boolean = true,
    tagline: String? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Student360Emblem(size = emblemSize)
        if (showWordmark) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Student",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "360",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = BrandCyan
                    )
                }
                if (tagline != null) {
                    Text(
                        text = tagline,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

/**
 * Standard Student360 Card with 16dp rounded corners and subtle dynamic theme border.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalAppColors.current.card,
    borderColor: Color = LocalAppColors.current.border,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = when {
        onLongClick != null -> modifier.clip(RoundedCornerShape(16.dp)).combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
        onClick != null -> modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() }
        else -> modifier
    }
    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

/**
 * Elevated Student360 Card with 16dp rounded corners and dynamic elevated background.
 */
@Composable
fun ElevatedStudentCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = LocalAppColors.current.elevatedCard,
    borderColor: Color = LocalAppColors.current.border,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    StudentCard(
        modifier = modifier,
        backgroundColor = backgroundColor,
        borderColor = borderColor,
        onClick = onClick,
        content = content
    )
}

/**
 * Modern, clean, text-based Student360 wordmark for main in-app headers.
 * "Student" in dark/navy primary text color, "360" in primary blue accent color.
 */
@Composable
fun Student360Wordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 21.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val colors = LocalAppColors.current
    Text(
        text = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    color = colors.textPrimary,
                    fontWeight = fontWeight
                )
            ) {
                append("Student")
            }
            withStyle(
                style = SpanStyle(
                    color = PrimaryBlue,
                    fontWeight = fontWeight
                )
            ) {
                append("360")
            }
        },
        style = MaterialTheme.typography.titleLarge.copy(
            letterSpacing = (-0.3).sp,
            lineHeight = fontSize
        ),
        fontSize = fontSize,
        maxLines = 1,
        modifier = modifier
    )
}

/**
 * Standardized Contextual Screen Header across screens (e.g. [Title]  85.71% | 75%  [+])
 * Never duplicates the global Student360 wordmark.
 */
@Composable
fun StudentScreenHeader(
    title: String = "",
    overallPercentage: Double = 100.0,
    targetPercentage: Int = 75,
    onAddClick: (() -> Unit)? = null,
    extraAction: (@Composable () -> Unit)? = null
) {
    val colors = LocalAppColors.current
    val statusColor = when {
        overallPercentage >= targetPercentage -> colors.success
        overallPercentage >= targetPercentage - 5.0 -> colors.warning
        else -> colors.danger
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = if (title.isNotBlank()) Arrangement.SpaceBetween else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Pill Badge: 57.41% | 75%
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Text(
                    text = "${String.format(Locale.US, "%.2f", overallPercentage)}% | $targetPercentage%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }

            extraAction?.invoke()

            if (onAddClick != null) {
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(colors.card)
                        .border(BorderStroke(1.dp, colors.border), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Attendance Fractional Circular Badge (Matching reference card: 41.67 / 75)
 */
@Composable
fun AttendanceFractionBadge(
    percentage: Double,
    target: Int = 75,
    statusColor: Color = LocalAppColors.current.success,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(colors.elevatedCard)
            .border(BorderStroke(1.dp, colors.border), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format(Locale.US, "%.2f", percentage),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor,
                fontSize = 11.sp,
                lineHeight = 12.sp
            )
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(1.dp)
                    .background(colors.border)
                    .padding(vertical = 1.dp)
            )
            Text(
                text = "$target",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                fontSize = 10.sp,
                lineHeight = 11.sp
            )
        }
    }
}

/**
 * Standardized Quick Round Attendance Control (⊘, —, ✕, ✓)
 */
@Composable
fun QuickAttendanceRoundButton(
    symbol: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp
) {
    val colors = LocalAppColors.current
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isSelected) activeColor else colors.elevatedCard)
            .border(
                BorderStroke(1.dp, if (isSelected) activeColor else colors.border),
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else colors.textSecondary.copy(alpha = 0.8f),
            fontSize = if (symbol == "—") 14.sp else 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Individual action item (Clear, Off, Miss, Att) inside DayStatusBanner.
 * Displays a round icon button with a label underneath, matching the reference design.
 */
@Composable
private fun DayStatusActionItem(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
    iconContent: @Composable (Color) -> Unit
) {
    val colors = LocalAppColors.current
    val currentIconColor = if (isSelected) activeColor else colors.textSecondary.copy(alpha = 0.8f)
    val buttonBgColor = if (isSelected) {
        activeColor.copy(alpha = if (colors.isDark) 0.20f else 0.12f)
    } else {
        if (colors.isDark) colors.elevatedCard.copy(alpha = 0.5f) else colors.surface
    }
    val buttonBorderColor = if (isSelected) {
        activeColor
    } else {
        colors.border
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = buttonBgColor,
            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, buttonBorderColor),
            modifier = Modifier
                .size(buttonSize)
                .clip(CircleShape)
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                iconContent(currentIconColor)
            }
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else colors.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Standardized Day Status Component matching the reference design.
 * Features a status indicator dot, "Today's status" title, current state ("Not marked", "Attended",
 * "Missed", "Off"), selected date, and 4 circular action buttons (Clear, Off, Miss, Att).
 */
@Composable
fun DayStatusBanner(
    statusTitle: String,
    statusDotColor: Color,
    onClearAll: () -> Unit,
    onMarkAllOff: () -> Unit,
    onMarkAllMissed: () -> Unit,
    onMarkAllAttended: () -> Unit,
    modifier: Modifier = Modifier,
    currentState: DayAttendanceState? = null,
    selectedDateText: String? = null
) {
    val colors = LocalAppColors.current

    // Infer state if not explicitly passed
    val effectiveState = currentState ?: when (statusTitle.trim().lowercase()) {
        "attended" -> DayAttendanceState.ATTENDED
        "missed" -> DayAttendanceState.MISSED
        "off" -> DayAttendanceState.OFF
        "mixed" -> DayAttendanceState.MIXED
        else -> DayAttendanceState.NOT_MARKED
    }

    val effectiveDotColor = when (effectiveState) {
        DayAttendanceState.ATTENDED -> colors.success
        DayAttendanceState.MISSED -> colors.danger
        DayAttendanceState.OFF -> colors.warning
        DayAttendanceState.MIXED -> colors.accent
        DayAttendanceState.NOT_MARKED -> if (statusDotColor != Color.Unspecified) statusDotColor else colors.textSecondary.copy(alpha = 0.45f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = if (colors.isDark) 0.dp else 1.5.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val isNarrow = maxWidth < 340.dp
            val isUltraNarrow = maxWidth < 280.dp
            val buttonSize = if (isNarrow) 36.dp else 40.dp
            val buttonSpacing = if (isNarrow) 6.dp else 10.dp

            if (isUltraNarrow) {
                // Stacked layout for very constrained widths
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(effectiveDotColor, CircleShape)
                        )
                        Column {
                            Text(
                                text = "Today's status",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DayStatusActionItem(
                            label = "Clear",
                            isSelected = false,
                            activeColor = colors.accent,
                            buttonSize = buttonSize,
                            onClick = onClearAll,
                            iconContent = { color ->
                                Canvas(modifier = Modifier.size(16.dp)) {
                                    val strokeW = 1.8.dp.toPx()
                                    val r = size.minDimension / 2 - strokeW / 2
                                    drawCircle(color = color, radius = r, style = Stroke(width = strokeW))
                                    val offset = r * 0.7071f
                                    drawLine(
                                        color = color,
                                        start = Offset(center.x - offset, center.y - offset),
                                        end = Offset(center.x + offset, center.y + offset),
                                        strokeWidth = strokeW,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        )

                        DayStatusActionItem(
                            label = "Off",
                            isSelected = (effectiveState == DayAttendanceState.OFF),
                            activeColor = colors.warning,
                            buttonSize = buttonSize,
                            onClick = onMarkAllOff,
                            iconContent = { color ->
                                Canvas(modifier = Modifier.size(16.dp)) {
                                    val strokeW = 2.dp.toPx()
                                    val halfLen = size.width * 0.34f
                                    drawLine(
                                        color = color,
                                        start = Offset(center.x - halfLen, center.y),
                                        end = Offset(center.x + halfLen, center.y),
                                        strokeWidth = strokeW,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        )

                        DayStatusActionItem(
                            label = "Miss",
                            isSelected = (effectiveState == DayAttendanceState.MISSED),
                            activeColor = colors.danger,
                            buttonSize = buttonSize,
                            onClick = onMarkAllMissed,
                            iconContent = { color ->
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Miss",
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        DayStatusActionItem(
                            label = "Att",
                            isSelected = (effectiveState == DayAttendanceState.ATTENDED),
                            activeColor = colors.success,
                            buttonSize = buttonSize,
                            onClick = onMarkAllAttended,
                            iconContent = { color ->
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Att",
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            } else {
                // Standard responsive horizontal layout matching reference
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Section: Status indicator dot + Status label, Title, and Date
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(effectiveDotColor, CircleShape)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "Today's status",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1
                            )
                            Text(
                                text = statusTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = if (isNarrow) 16.sp else 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!selectedDateText.isNullOrBlank()) {
                                Text(
                                    text = selectedDateText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    fontSize = 10.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Vertical Divider
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(if (isNarrow) 6.dp else 10.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(colors.border.copy(alpha = 0.7f))
                        )
                        Spacer(modifier = Modifier.width(if (isNarrow) 6.dp else 10.dp))
                    }

                    // Right Section: 4 Action Buttons (Clear, Off, Miss, Att)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(buttonSpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DayStatusActionItem(
                            label = "Clear",
                            isSelected = false,
                            activeColor = colors.accent,
                            buttonSize = buttonSize,
                            onClick = onClearAll,
                            iconContent = { color ->
                                Canvas(modifier = Modifier.size(if (isNarrow) 15.dp else 17.dp)) {
                                    val strokeW = 1.8.dp.toPx()
                                    val r = size.minDimension / 2 - strokeW / 2
                                    drawCircle(color = color, radius = r, style = Stroke(width = strokeW))
                                    val offset = r * 0.7071f
                                    drawLine(
                                        color = color,
                                        start = Offset(center.x - offset, center.y - offset),
                                        end = Offset(center.x + offset, center.y + offset),
                                        strokeWidth = strokeW,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        )

                        DayStatusActionItem(
                            label = "Off",
                            isSelected = (effectiveState == DayAttendanceState.OFF),
                            activeColor = colors.warning,
                            buttonSize = buttonSize,
                            onClick = onMarkAllOff,
                            iconContent = { color ->
                                Canvas(modifier = Modifier.size(if (isNarrow) 15.dp else 17.dp)) {
                                    val strokeW = 2.dp.toPx()
                                    val halfLen = size.width * 0.34f
                                    drawLine(
                                        color = color,
                                        start = Offset(center.x - halfLen, center.y),
                                        end = Offset(center.x + halfLen, center.y),
                                        strokeWidth = strokeW,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        )

                        DayStatusActionItem(
                            label = "Miss",
                            isSelected = (effectiveState == DayAttendanceState.MISSED),
                            activeColor = colors.danger,
                            buttonSize = buttonSize,
                            onClick = onMarkAllMissed,
                            iconContent = { color ->
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Miss",
                                    tint = color,
                                    modifier = Modifier.size(if (isNarrow) 16.dp else 18.dp)
                                )
                            }
                        )

                        DayStatusActionItem(
                            label = "Att",
                            isSelected = (effectiveState == DayAttendanceState.ATTENDED),
                            activeColor = colors.success,
                            buttonSize = buttonSize,
                            onClick = onMarkAllAttended,
                            iconContent = { color ->
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Att",
                                    tint = color,
                                    modifier = Modifier.size(if (isNarrow) 16.dp else 18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern Progress Bar with smooth animated progress and rounded ends.
 */
@Composable
fun StudentProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = LocalAppColors.current.accent,
    trackColor: Color = LocalAppColors.current.elevatedCard,
    height: Dp = 8.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/**
 * Status badge pill (e.g. 🟢 Safe, 🟠 At Risk, 🔴 Critical, or Urgent tags).
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color, CircleShape)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Standardized section title with optional trailing action.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Modern empty state presentation.
 */
@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Default.Info,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(colors.elevatedCard, CircleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = actionText, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
