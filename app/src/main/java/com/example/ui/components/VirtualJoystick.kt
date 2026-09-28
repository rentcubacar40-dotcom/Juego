package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    onMove: (x: Float, y: Float) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(size)
            .testTag("virtual_joystick")
            .pointerInput(Unit) {
                val radius = (size.toPx()) / 2f
                val maxDistance = radius * 0.75f

                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(radius, radius)
                        val dragVector = offset - center
                        val dist = dragVector.getDistance()
                        val clampedDist = dist.coerceAtMost(maxDistance)
                        val angle = kotlin.math.atan2(dragVector.y, dragVector.x)
                        thumbOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                        onMove(thumbOffset.x / maxDistance, -thumbOffset.y / maxDistance)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nextOffset = thumbOffset + dragAmount
                        val dist = nextOffset.getDistance()
                        val clampedDist = dist.coerceAtMost(maxDistance)
                        val angle = kotlin.math.atan2(nextOffset.y, nextOffset.x)
                        thumbOffset = Offset(cos(angle) * clampedDist, sin(angle) * clampedDist)
                        onMove(thumbOffset.x / maxDistance, -thumbOffset.y / maxDistance)
                    },
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onMove(0f, 0f)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val outerRadius = this.size.width / 2f * 0.85f
            val innerRadius = outerRadius * 0.42f

            // Outer Base Ring
            drawCircle(
                color = Color(0x330F172A),
                radius = outerRadius,
                center = center
            )
            drawCircle(
                color = Color(0x66F59E0B),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Crosshair Guides
            val guideLen = outerRadius * 0.3f
            drawLine(
                color = Color(0x44F59E0B),
                start = Offset(center.x - guideLen, center.y),
                end = Offset(center.x + guideLen, center.y),
                strokeWidth = 1.5f
            )
            drawLine(
                color = Color(0x44F59E0B),
                start = Offset(center.x, center.y - guideLen),
                end = Offset(center.x, center.y + guideLen),
                strokeWidth = 1.5f
            )

            // Inner Thumb Knob
            val knobCenter = center + thumbOffset
            drawCircle(
                color = Color(0xAA1E293B),
                radius = innerRadius,
                center = knobCenter
            )
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = innerRadius,
                center = knobCenter,
                style = Stroke(width = 3.0f)
            )
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 5f,
                center = knobCenter
            )
        }
    }
}
