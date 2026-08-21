package com.student360.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.student360.app.ui.theme.*

/**
 * Official Student360 Emblem branding symbol.
 */
@Composable
fun Student360Emblem(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E3A70), Color(0xFF0B132B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height

            // 360 Orbital Ring Arc
            val strokeWidth = w * 0.10f
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(BrandCyan, BrandBlue, BrandSky, BrandCyan)
                ),
                startAngle = 40f,
                sweepAngle = 290f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Orbital Pulse Dot
            drawCircle(
                color = BrandCyan,
                radius = strokeWidth * 0.7f,
                center = Offset(w * 0.82f, h * 0.24f)
            )

            // Graduation Cap Top (Diamond)
            val capPath = Path().apply {
                moveTo(w * 0.50f, h * 0.28f)
                lineTo(w * 0.82f, h * 0.46f)
                lineTo(w * 0.50f, h * 0.62f)
                lineTo(w * 0.18f, h * 0.46f)
                close()
            }
            drawPath(
                path = capPath,
                brush = Brush.linearGradient(
                    listOf(BrandSky, BrandBlue, Color(0xFF1D4ED8))
                )
            )

            // Cap Skull Base
            val baseCapPath = Path().apply {
                moveTo(w * 0.32f, h * 0.54f)
                lineTo(w * 0.32f, h * 0.70f)
                cubicTo(w * 0.32f, h * 0.82f, w * 0.68f, h * 0.82f, w * 0.68f, h * 0.70f)
                lineTo(w * 0.68f, h * 0.54f)
                close()
            }
            drawPath(
                path = baseCapPath,
                brush = Brush.linearGradient(
                    listOf(BrandBlue, Color(0xFF1E40AF))
                )
            )

            // Tassel
            val tasselPath = Path().apply {
                moveTo(w * 0.50f, h * 0.46f)
                quadraticBezierTo(w * 0.74f, h * 0.48f, w * 0.78f, h * 0.68f)
            }
            drawPath(
                path = tasselPath,
                color = BrandCyan,
                style = Stroke(width = strokeWidth * 0.45f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = BrandCyan,
                radius = strokeWidth * 0.5f,
                center = Offset(w * 0.78f, h * 0.70f)
            )
        }
    }
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
                        color = PrimaryText
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
                        color = SecondaryText
                    )
                }
            }
        }
    }
}

/**
 * Standard Student360 Card with 16dp rounded corners and subtle border.
 */
@Composable
fun StudentCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = CardDark,
    borderColor: Color = BorderDark,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = if (onClick != null) modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() } else modifier,
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
 * Elevated Student360 Card with 16dp rounded corners and higher contrast background.
 */
@Composable
fun ElevatedStudentCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ElevatedCardDark,
    borderColor: Color = BorderDark.copy(alpha = 0.8f),
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
 * Modern Progress Bar with smooth animated progress and rounded ends.
 */
@Composable
fun StudentProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = PrimaryPurple,
    trackColor: Color = SurfaceDark,
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
                maxLines = 1
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )
        if (actionText != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = LightPurple,
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
                .background(SurfaceDark, CircleShape)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LightPurple,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = SecondaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = actionText, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
