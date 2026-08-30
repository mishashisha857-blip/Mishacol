package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun CalorieDeficitCard(
    consumedCalories: Int,
    burnedCalories: Int,
    targetCalories: Int,
    deficitTarget: Int,
    modifier: Modifier = Modifier
) {
    val netCalories = consumedCalories - burnedCalories
    val remainingCalories = targetCalories - netCalories
    val progress = if (targetCalories > 0) {
        (consumedCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1.3f)
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900),
        label = "calorieProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("calorie_deficit_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Баланс калорий и дефицит",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Целевой дефицит: -$deficitTarget ккал/день",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (remainingCalories >= 0) EmeraldContainer else CoralContainer
                ) {
                    Text(
                        text = if (remainingCalories >= 0) "В дефиците" else "Превышение",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingCalories >= 0) OnEmeraldContainer else OnCoralContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Circular Progress Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                Canvas(modifier = Modifier.size(175.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    // Background track
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )

                    // Foreground progress arc
                    val sweepAngle = (animatedProgress.coerceAtMost(1f)) * 360f
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(primaryColor, secondaryColor, primaryColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${remainingCalories.coerceAtLeast(0)}",
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ккал осталось",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Норма: $targetCalories ккал",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3 Stat Badges: Consumed, Burned, Net
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Consumed
                StatBadge(
                    icon = Icons.Default.Restaurant,
                    iconColor = AmberSecondary,
                    title = "Съедено",
                    value = "$consumedCalories",
                    unit = "ккал",
                    containerColor = AmberContainer.copy(alpha = 0.5f)
                )

                // Burned
                StatBadge(
                    icon = Icons.Default.DirectionsWalk,
                    iconColor = CoralTertiary,
                    title = "Сожжено",
                    value = "$burnedCalories",
                    unit = "ккал",
                    containerColor = CoralContainer.copy(alpha = 0.5f)
                )

                // Net
                StatBadge(
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = EmeraldPrimary,
                    title = "Чистыми",
                    value = "$netCalories",
                    unit = "ккал",
                    containerColor = EmeraldContainer.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun StatBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    unit: String,
    containerColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        modifier = Modifier.width(98.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$title ($unit)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
