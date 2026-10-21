package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.benchmark.BenchmarkCategory

enum class IconAnimationStyle {
    NONE,
    PULSE,      // Breathing scale & glowing halo
    ROTATE,     // Continuous cyber sensor rotation
    BOUNCE,     // Floating vertical bounce
    GLITCH_GLOW // Neon scanline alpha pulsing
}

@Composable
fun SkitefiIcon(
    @DrawableRes drawableId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    animationStyle: IconAnimationStyle = IconAnimationStyle.NONE,
    showCyberHalo: Boolean = false,
    haloColor: Color = MaterialTheme.colorScheme.primary,
    containerSize: Dp = 44.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_anim")

    val scale by infiniteTransition.animateFloat(
        initialValue = if (animationStyle == IconAnimationStyle.PULSE) 0.90f else 1.0f,
        targetValue = if (animationStyle == IconAnimationStyle.PULSE) 1.10f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animationStyle == IconAnimationStyle.ROTATE) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = if (animationStyle == IconAnimationStyle.BOUNCE) -3f else 0f,
        targetValue = if (animationStyle == IconAnimationStyle.BOUNCE) 3f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val iconModifier = modifier
        .then(if (animationStyle == IconAnimationStyle.PULSE) Modifier.scale(scale) else Modifier)
        .then(if (animationStyle == IconAnimationStyle.ROTATE) Modifier.rotate(rotation) else Modifier)
        .then(if (animationStyle == IconAnimationStyle.BOUNCE) Modifier.offset(y = bounceOffset.dp) else Modifier)

    if (showCyberHalo) {
        Box(
            modifier = Modifier
                .size(containerSize)
                .clip(RoundedCornerShape(12.dp))
                .background(haloColor.copy(alpha = 0.12f * glowAlpha))
                .border(1.dp, haloColor.copy(alpha = 0.35f * glowAlpha), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = drawableId),
                contentDescription = contentDescription,
                modifier = iconModifier.size(containerSize * 0.6f),
                tint = tint
            )
        }
    } else {
        Icon(
            painter = painterResource(id = drawableId),
            contentDescription = contentDescription,
            modifier = iconModifier,
            tint = tint
        )
    }
}

fun getBenchmarkCategoryIcon(category: BenchmarkCategory): Int {
    return when (category) {
        BenchmarkCategory.OVERALL -> R.drawable.ic_benchmark_all
        BenchmarkCategory.CPU -> R.drawable.ic_benchmark_cpu
        BenchmarkCategory.GPU -> R.drawable.ic_benchmark_gpu
        BenchmarkCategory.RAM -> R.drawable.ic_benchmark_ram
        BenchmarkCategory.STORAGE -> R.drawable.ic_benchmark_storage
        BenchmarkCategory.AI_NEURAL -> R.drawable.ic_benchmark_cpu // High-performance compute
        BenchmarkCategory.CRYPTO -> R.drawable.ic_benchmark_ram
        BenchmarkCategory.RAYTRACING -> R.drawable.ic_benchmark_gpu
        BenchmarkCategory.STABILITY -> R.drawable.ic_benchmark_stability
        BenchmarkCategory.BATTERY -> R.drawable.ic_benchmark_battery
        BenchmarkCategory.DISPLAY -> R.drawable.ic_benchmark_display
    }
}
