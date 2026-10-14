package com.mercedes.w205.obd.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mercedes.w205.obd.ui.theme.*
import java.util.Locale

@Composable
fun BoostBarGauge(
    boostBar: Float,
    modifier: Modifier = Modifier
) {
    val minBar = -0.8f
    val maxBar = 1.8f
    val totalRange = maxBar - minBar
    val zeroFraction = (-minBar) / totalRange // Where 0.0 bar is positioned (approx 30.7%)

    val animatedBoost by animateFloatAsState(
        targetValue = boostBar.coerceIn(minBar, maxBar),
        animationSpec = tween(durationMillis = 80),
        label = "BoostAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmgCardBg)
            .border(1.dp, AmgBorder, RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TURBO BOOST (BAR)",
                color = AmgSilverMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = String.format(Locale.US, "%+.2f BAR", animatedBoost),
                color = if (animatedBoost > 1.2f) AmgRed else if (animatedBoost > 0f) AmgAmber else AmgCyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Bar Track
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            val w = size.width
            val h = size.height
            val zeroX = w * zeroFraction

            // Background track
            drawRect(
                color = Color(0xFF131722),
                size = size
            )

            // Center Zero Bar Divider
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(zeroX, 0f),
                end = Offset(zeroX, h),
                strokeWidth = 2.dp.toPx()
            )

            // Active Boost/Vacuum Fill
            val currentFraction = (animatedBoost - minBar) / totalRange
            val currentX = w * currentFraction

            if (animatedBoost >= 0f) {
                // Boost zone (from zeroX to currentX, Amber to Red)
                val fillWidth = currentX - zeroX
                val barColor = if (animatedBoost > 1.2f) AmgRed else AmgAmber
                drawRect(
                    color = barColor,
                    topLeft = Offset(zeroX, 0f),
                    size = Size(fillWidth, h)
                )
            } else {
                // Vacuum zone (from currentX to zeroX, Cyan)
                val fillWidth = zeroX - currentX
                drawRect(
                    color = AmgCyan,
                    topLeft = Offset(currentX, 0f),
                    size = Size(fillWidth, h)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Axis Scale Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "-0.8", color = AmgSilverMuted, fontSize = 10.sp)
            Text(text = "0.0", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = "+1.0", color = AmgSilverMuted, fontSize = 10.sp)
            Text(text = "+1.8", color = AmgRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
