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
import androidx.compose.ui.graphics.Path
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

            // Side profile, facing right. Legs and tail first so the shell dome
            // overlaps their tops, leaving just their lower halves peeking out.
            listOf(
                Offset(w * 0.53f, h * 0.60f) to Size(w * 0.14f, h * 0.18f), // front leg
                Offset(w * 0.24f, h * 0.60f) to Size(w * 0.14f, h * 0.18f), // back leg
            ).forEach { (topLeft, legSize) -> drawOval(skinColor, topLeft = topLeft, size = legSize) }
            drawCircle(skinColor, radius = w * 0.035f, center = Offset(w * 0.14f, h * 0.58f)) // tail

            val shellLeft = w * 0.16f
            val shellRight = w * 0.72f
            val shellTop = h * 0.28f
            val shellBottom = h * 0.62f
            val shellMidX = (shellLeft + shellRight) / 2f
            val shellPath = Path().apply {
                moveTo(shellLeft, shellBottom)
                quadraticTo(shellLeft, shellTop, shellMidX, shellTop)
                quadraticTo(shellRight, shellTop, shellRight, shellBottom)
                close()
            }
            drawPath(shellPath, color = shellColor)
            val plateRadius = w * 0.048f
            listOf(
                Offset(w * 0.32f, h * 0.40f),
                Offset(w * 0.44f, h * 0.34f),
                Offset(w * 0.56f, h * 0.40f),
            ).forEach { center -> drawCircle(plateColor, radius = plateRadius, center = center) }

            val headCenter = Offset(w * 0.82f, h * 0.50f)
            drawCircle(skinColor, radius = w * 0.11f, center = headCenter)
            val eyeHeight = h * 0.03f * eyeOpenness
            drawOval(
                eyeColor,
                topLeft = Offset(headCenter.x + w * 0.03f, headCenter.y - h * 0.045f - eyeHeight / 2f),
                size = Size(w * 0.032f, eyeHeight),
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
