package com.pawse.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pawse's one mascot. Started as the block-screen-only illustration in Phase 4; now
 * reused on the home screen too, so it lives here instead of inside BlockActivity.
 *
 * Deliberately just this one animal, drawn with Canvas shapes rather than an asset —
 * see the plan's scope fence.
 */
@Composable
fun Turtle(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    shellColor: Color = TurtleColors.Shell,
    plateColor: Color = TurtleColors.Plate,
    skinColor: Color = TurtleColors.Skin,
    eyeColor: Color = TurtleColors.Eye,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "turtle")
    val bob by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val eyeOpenness by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 4000
                1f at 0
                1f at 3600
                0.1f at 3800
                1f at 4000
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "blink",
    )

    Canvas(modifier = modifier.size(size)) {
        translate(top = bob) {
            val w = this.size.width
            val h = this.size.height

            // Flippers first so the shell overlaps their base.
            listOf(
                Offset(w * 0.02f, h * 0.52f) to Size(w * 0.22f, h * 0.26f),
                Offset(w * 0.76f, h * 0.52f) to Size(w * 0.22f, h * 0.26f),
                Offset(w * 0.10f, h * 0.76f) to Size(w * 0.20f, h * 0.20f),
                Offset(w * 0.70f, h * 0.76f) to Size(w * 0.20f, h * 0.20f),
            ).forEach { (topLeft, flipperSize) -> drawOval(skinColor, topLeft = topLeft, size = flipperSize) }

            drawOval(shellColor, topLeft = Offset(w * 0.15f, h * 0.28f), size = Size(w * 0.70f, h * 0.55f))
            val plateRadius = w * 0.055f
            listOf(
                Offset(w * 0.50f, h * 0.42f),
                Offset(w * 0.35f, h * 0.55f),
                Offset(w * 0.65f, h * 0.55f),
                Offset(w * 0.50f, h * 0.66f),
            ).forEach { center -> drawCircle(plateColor, radius = plateRadius, center = center) }

            val headCenter = Offset(w * 0.5f, h * 0.18f)
            drawCircle(skinColor, radius = w * 0.13f, center = headCenter)
            val eyeHeight = h * 0.035f * eyeOpenness
            drawOval(
                eyeColor,
                topLeft = Offset(headCenter.x - w * 0.02f, headCenter.y - eyeHeight / 2f),
                size = Size(w * 0.04f, eyeHeight),
            )
        }
    }
}

/** Default palette — a calm green-shelled turtle. Callers can override per-scene. */
object TurtleColors {
    val Shell = Color(0xFF4C7A67)
    val Plate = Color(0xFF35594C)
    val Skin = Color(0xFF8FBF9F)
    val Eye = Color(0xFF1B3A3E)
}
