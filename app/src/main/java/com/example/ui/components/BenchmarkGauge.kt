package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BenchmarkGauge(
    score: Int,
    maxScore: Int = 5000,
    label: String = "БАЛЛЫ",
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(score) {
        val target = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            val startAngle = 135f
            val sweepAngle = 270f

            // Track background arc
            drawArc(
                color = primaryColor.copy(alpha = 0.15f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Dynamic progress arc with gradient
            val currentSweep = sweepAngle * animatedProgress.value
            if (currentSweep > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to primaryColor.copy(alpha = 0.6f),
                        0.5f to primaryColor,
                        1.0f to primaryColor,
                        center = center
                    ),
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Tick marks
            val ticks = 18
            val tickRadius = (diameter / 2f) - 16.dp.toPx()
            for (i in 0..ticks) {
                val tickAngle = startAngle + (sweepAngle / ticks) * i
                val rad = Math.toRadians(tickAngle.toDouble())
                val start = Offset(
                    (center.x + (tickRadius - 6.dp.toPx()) * cos(rad)).toFloat(),
                    (center.y + (tickRadius - 6.dp.toPx()) * sin(rad)).toFloat()
                )
                val end = Offset(
                    (center.x + tickRadius * cos(rad)).toFloat(),
                    (center.y + tickRadius * sin(rad)).toFloat()
                )
                val isActive = (i.toFloat() / ticks) <= animatedProgress.value
                drawLine(
                    color = if (isActive) primaryColor else primaryColor.copy(alpha = 0.2f),
                    start = start,
                    end = end,
                    strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.5.dp.toPx()
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = (score * animatedProgress.value / ((score.toFloat() / maxScore.toFloat()).coerceAtLeast(0.01f))).toInt().toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 38.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
