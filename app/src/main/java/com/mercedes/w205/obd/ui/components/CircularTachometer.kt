package com.mercedes.w205.obd.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercedes.w205.obd.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CircularTachometer(
    rpm: Int,
    speedKmh: Int,
    modifier: Modifier = Modifier
) {
    val maxRpm = 7000f
    val redlineRpm = 6200f
    
    // Smooth animation of RPM needle
    val animatedRpm by animateFloatAsState(
        targetValue = rpm.toFloat().coerceIn(0f, maxRpm),
        animationSpec = tween(durationMillis = 80),
        label = "RpmAnimation"
    )

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 16.dp.toPx()
            val startAngle = 135f
            val sweepAngle = 270f

            // Outer brushed metallic ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF222938), Color(0xFF131722)),
                    center = center,
                    radius = radius + 12.dp.toPx()
                ),
                radius = radius + 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = AmgBorder,
                radius = radius + 12.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner dark carbon face
            drawCircle(
                color = Color(0xFF0C0F17),
                radius = radius,
                center = center
            )

            // Base Gauge Track Arc
            drawArc(
                color = Color(0xFF1E2638),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Redline Arc (6200 to 7000 RPM)
            val redlineStartFraction = redlineRpm / maxRpm
            val redlineStartAngle = startAngle + (sweepAngle * redlineStartFraction)
            val redlineSweep = sweepAngle * (1f - redlineStartFraction)
            drawArc(
                color = AmgRed,
                startAngle = redlineStartAngle,
                sweepAngle = redlineSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Active RPM Sweep Arc
            val activeSweep = (animatedRpm / maxRpm) * sweepAngle
            val activeColor = if (animatedRpm >= redlineRpm) AmgRed else AmgCyan
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(AmgCyan, activeColor),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = activeSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dial Tick Marks (0 to 7)
            for (i in 0..7) {
                val tickFraction = i / 7f
                val tickAngleDeg = startAngle + (sweepAngle * tickFraction)
                val tickAngleRad = Math.toRadians(tickAngleDeg.toDouble())
                val cosVal = cos(tickAngleRad).toFloat()
                val sinVal = sin(tickAngleRad).toFloat()

                val tickInnerR = radius - 16.dp.toPx()
                val tickOuterR = radius - 4.dp.toPx()
                val isRedline = (i * 1000) >= redlineRpm
                val tickColor = if (isRedline) AmgRed else AmgSilverMuted

                val p1 = Offset(center.x + tickInnerR * cosVal, center.y + tickInnerR * sinVal)
                val p2 = Offset(center.x + tickOuterR * cosVal, center.y + tickOuterR * sinVal)
                drawLine(
                    color = tickColor,
                    start = p1,
                    end = p2,
                    strokeWidth = if (i % 2 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Animated Needle
            val needleAngleDeg = startAngle + ((animatedRpm / maxRpm) * sweepAngle)
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
            val cosNeedle = cos(needleAngleRad).toFloat()
            val sinNeedle = sin(needleAngleRad).toFloat()
            val needleLength = radius - 20.dp.toPx()
            val needleTip = Offset(center.x + needleLength * cosNeedle, center.y + needleLength * sinNeedle)

            // Needle glow / line
            drawLine(
                color = if (animatedRpm >= redlineRpm) AmgRed else Color(0xFFFF3344),
                start = center,
                end = needleTip,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center Needle Pivot Pin
            drawCircle(
                color = AmgSilver,
                radius = 10.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color(0xFF0F131C),
                radius = 5.dp.toPx(),
                center = center
            )
        }

        // Center Digital Readout (Speed & RPM value)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 70.dp)
        ) {
            Text(
                text = "$speedKmh",
                color = AmgSilver,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "KM / H",
                color = AmgSilverMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${rpm} RPM",
                color = if (rpm >= 6200) AmgRed else AmgCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
