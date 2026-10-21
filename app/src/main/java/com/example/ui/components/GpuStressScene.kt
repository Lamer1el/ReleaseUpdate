package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.cos
import kotlin.math.sin

data class Particle3D(var x: Float, var y: Float, var z: Float, val vx: Float, val vy: Float, val vz: Float)

@Composable
fun GpuStressScene(
    isRunning: Boolean,
    onFrameSample: (fps: Double, frameTimeMs: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val particles = remember {
        List(250) {
            Particle3D(
                x = (Math.random().toFloat() - 0.5f) * 600f,
                y = (Math.random().toFloat() - 0.5f) * 600f,
                z = Math.random().toFloat() * 400f + 100f,
                vx = (Math.random().toFloat() - 0.5f) * 4f,
                vy = (Math.random().toFloat() - 0.5f) * 4f,
                vz = (Math.random().toFloat() - 0.5f) * 2f
            )
        }
    }

    var rotationAngle by remember { mutableStateOf(0f) }
    var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }
    var frameCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (isRunning) {
            withFrameNanos { now ->
                if (lastFrameTimeNanos != 0L) {
                    val frameTimeNs = now - lastFrameTimeNanos
                    val frameTimeMs = frameTimeNs / 1_000_000.0
                    val fps = if (frameTimeMs > 0) 1000.0 / frameTimeMs else 60.0
                    onFrameSample(fps, frameTimeMs)
                }
                lastFrameTimeNanos = now
                rotationAngle += 0.035f
                frameCount++
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val fov = 350f

        val points = mutableListOf<Offset>()

        particles.forEach { p ->
            p.x += p.vx
            p.y += p.vy
            p.z += p.vz
            if (p.x < -300f || p.x > 300f) p.x = -p.x
            if (p.y < -300f || p.y > 300f) p.y = -p.y
            if (p.z < 50f || p.z > 500f) p.z = 250f

            // Rotate around Y axis
            val rx = p.x * cos(rotationAngle) - p.z * sin(rotationAngle)
            val rz = p.x * sin(rotationAngle) + p.z * cos(rotationAngle) + 400f

            val scale = fov / rz
            val projX = cx + rx * scale
            val projY = cy + p.y * scale

            points.add(Offset(projX, projY))
        }

        drawPoints(
            points = points,
            pointMode = PointMode.Points,
            color = primaryColor,
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        // Draw geometric grid lines connecting nearby nodes
        for (i in 0 until points.size step 4) {
            val p1 = points[i]
            val nextIdx = (i + 1) % points.size
            val p2 = points[nextIdx]
            val dist = (p1 - p2).getDistance()
            if (dist < 120f) {
                drawLine(
                    color = secondaryColor.copy(alpha = (1f - dist / 120f).coerceIn(0.1f, 0.7f)),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.5f
                )
            }
        }
    }
}
