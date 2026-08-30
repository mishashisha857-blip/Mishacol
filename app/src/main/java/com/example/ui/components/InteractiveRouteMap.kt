package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LatLngPoint
import com.example.data.model.MapsPlace
import com.example.ui.theme.*

@Composable
fun InteractiveRouteMap(
    currentLocation: LatLngPoint?,
    routePoints: List<LatLngPoint>,
    nearbyPlaces: List<MapsPlace>,
    isTracking: Boolean,
    onPlaceSelected: (MapsPlace) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedPlace by remember { mutableStateOf<MapsPlace?>(null) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val isDark = MaterialTheme.colorScheme.background == BackgroundDark

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDark) Color(0xFF0F1B15) else Color(0xFFE8F1EC))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 3.0f)
                    panOffset += pan
                }
            }
            .testTag("interactive_map_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f) + panOffset

            // Draw stylized vector map background (blocks, grid roads, parks, rivers)
            drawMapBackground(isDark, panOffset, scale)

            val baseLat = currentLocation?.latitude ?: 55.7558
            val baseLng = currentLocation?.longitude ?: 37.6173

            // Coordinate to screen mapping function
            // 0.001 deg lat is approx 111m -> scale to pixels
            val pixelPerDegreeLat = 35000f * scale
            val pixelPerDegreeLng = 22000f * scale

            fun toScreenOffset(point: LatLngPoint): Offset {
                val dx = ((point.longitude - baseLng) * pixelPerDegreeLng).toFloat()
                val dy = -((point.latitude - baseLat) * pixelPerDegreeLat).toFloat()
                return center + Offset(dx, dy)
            }

            // Draw route polyline
            if (routePoints.size > 1) {
                val path = Path()
                val firstScreen = toScreenOffset(routePoints.first())
                path.moveTo(firstScreen.x, firstScreen.y)

                for (i in 1 until routePoints.size) {
                    val pt = toScreenOffset(routePoints[i])
                    path.lineTo(pt.x, pt.y)
                }

                // Glowing outer stroke
                drawPath(
                    path = path,
                    color = primaryColor.copy(alpha = 0.35f),
                    style = Stroke(width = 12f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Solid inner stroke
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Start Pin
                val startPos = toScreenOffset(routePoints.first())
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 8f * scale,
                    center = startPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f * scale,
                    center = startPos
                )
            }

            // Draw Nearby Grounded Places Markers
            for (place in nearbyPlaces) {
                val placePos = toScreenOffset(LatLngPoint(place.latitude, place.longitude))

                // Place pin
                val pinColor = if (place.category.contains("Парк")) Color(0xFF10B981)
                else if (place.category.contains("питание")) Color(0xFFF59E0B)
                else Color(0xFF3B82F6)

                drawCircle(
                    color = pinColor.copy(alpha = 0.25f),
                    radius = 16f * scale,
                    center = placePos
                )
                drawCircle(
                    color = pinColor,
                    radius = 7f * scale,
                    center = placePos
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f * scale,
                    center = placePos
                )
            }

            // Draw Live User Location Pin with pulsing indicator
            val userPos = toScreenOffset(currentLocation ?: LatLngPoint(baseLat, baseLng))

            // Outer pulse
            drawCircle(
                color = tertiaryColor.copy(alpha = if (isTracking) 0.3f else 0.15f),
                radius = 22f * scale,
                center = userPos
            )
            // Pin circle
            drawCircle(
                color = tertiaryColor,
                radius = 9f * scale,
                center = userPos
            )
            drawCircle(
                color = Color.White,
                radius = 4f * scale,
                center = userPos
            )
        }

        // Map Control Floating Badges (Recenter, Zoom In, Zoom Out)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = {
                    panOffset = Offset.Zero
                    scale = 1.0f
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .shadow(2.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Мое местоположение",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = { scale = (scale * 1.25f).coerceAtMost(3.0f) },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .shadow(2.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Приблизить",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = { scale = (scale / 1.25f).coerceAtLeast(0.6f) },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .shadow(2.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Отдалить",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Live HUD Overlay Pill at bottom
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isTracking) Color(0xFF10B981) else AmberSecondary)
                )
                Text(
                    text = if (isTracking) "GPS запись маршрута активна" else "Интерактивная карта шагов",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun DrawScope.drawMapBackground(isDark: Boolean, pan: Offset, scale: Float) {
    val roadColor = if (isDark) Color(0xFF1C2C24) else Color(0xFFFFFFFF)
    val parkColor = if (isDark) Color(0xFF13281E) else Color(0xFFD4E8DC)
    val waterColor = if (isDark) Color(0xFF12222A) else Color(0xFFCCE4F0)

    val width = size.width
    val height = size.height

    // Draw stylized park green polygon
    drawRoundRect(
        color = parkColor,
        topLeft = Offset(width * 0.1f + pan.x * 0.3f, height * 0.15f + pan.y * 0.3f),
        size = androidx.compose.ui.geometry.Size(width * 0.38f, height * 0.45f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
    )

    // Draw stylized river/water canal
    val waterPath = Path().apply {
        moveTo(-100f + pan.x * 0.2f, height * 0.75f + pan.y * 0.2f)
        cubicTo(
            width * 0.3f + pan.x * 0.2f, height * 0.85f + pan.y * 0.2f,
            width * 0.6f + pan.x * 0.2f, height * 0.65f + pan.y * 0.2f,
            width + 100f + pan.x * 0.2f, height * 0.72f + pan.y * 0.2f
        )
    }
    drawPath(
        path = waterPath,
        color = waterColor,
        style = Stroke(width = 36f * scale, cap = StrokeCap.Round)
    )

    // Draw road grid lines
    val gridStep = 80f * scale
    val startX = (pan.x % gridStep)
    val startY = (pan.y % gridStep)

    var x = startX - gridStep
    while (x < width + gridStep) {
        drawLine(
            color = roadColor.copy(alpha = 0.7f),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 3f * scale
        )
        x += gridStep
    }

    var y = startY - gridStep
    while (y < height + gridStep) {
        drawLine(
            color = roadColor.copy(alpha = 0.7f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 3f * scale
        )
        y += gridStep
    }
}
