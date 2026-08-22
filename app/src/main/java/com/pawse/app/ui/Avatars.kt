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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pawse.app.data.Avatar

/** Dispatches to the composable for [avatar], so callers don't need a `when` of their own. */
@Composable
fun AnimalAvatar(
    avatar: Avatar,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {
    when (avatar) {
        Avatar.TURTLE -> Turtle(modifier, size)
        Avatar.CAT -> Cat(modifier, size)
        Avatar.OWL -> Owl(modifier, size)
        Avatar.FOX -> Fox(modifier, size)
    }
}

/** A shared bob (vertical drift) + blink animation so every avatar feels alive the same way. */
@Composable
private fun rememberBobAndBlink(): Pair<Float, Float> {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
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
    return bob to eyeOpenness
}

private fun trianglePath(a: Offset, b: Offset, c: Offset): Path = Path().apply {
    moveTo(a.x, a.y)
    lineTo(b.x, b.y)
    lineTo(c.x, c.y)
    close()
}

@Composable
fun Cat(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    bodyColor: Color = CatColors.Body,
    eyeColor: Color = CatColors.Eye,
) {
    val (bob, eyeOpenness) = rememberBobAndBlink()
    Canvas(modifier = modifier.size(size)) {
        translate(top = bob) {
            val w = this.size.width
            val h = this.size.height

            rotate(degrees = -25f, pivot = Offset(w * 0.18f, h * 0.55f)) {
                drawOval(bodyColor, topLeft = Offset(w * 0.13f, h * 0.40f), size = Size(w * 0.10f, h * 0.32f))
            }
            drawOval(bodyColor, topLeft = Offset(w * 0.20f, h * 0.55f), size = Size(w * 0.42f, h * 0.30f))
            drawOval(bodyColor, topLeft = Offset(w * 0.30f, h * 0.78f), size = Size(w * 0.10f, h * 0.12f))

            val headCenter = Offset(w * 0.68f, h * 0.45f)
            drawPath(
                trianglePath(
                    Offset(w * 0.58f, h * 0.34f),
                    Offset(w * 0.63f, h * 0.20f),
                    Offset(w * 0.70f, h * 0.34f),
                ),
                color = bodyColor,
            )
            drawPath(
                trianglePath(
                    Offset(w * 0.66f, h * 0.34f),
                    Offset(w * 0.74f, h * 0.18f),
                    Offset(w * 0.80f, h * 0.34f),
                ),
                color = bodyColor,
            )
            drawCircle(bodyColor, radius = w * 0.16f, center = headCenter)

            val eyeHeight = h * 0.028f * eyeOpenness
            drawOval(
                eyeColor,
                topLeft = Offset(headCenter.x + w * 0.05f, headCenter.y - eyeHeight / 2f),
                size = Size(w * 0.03f, eyeHeight),
            )
        }
    }
}

@Composable
fun Owl(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    bodyColor: Color = OwlColors.Body,
    faceColor: Color = OwlColors.Face,
    beakColor: Color = OwlColors.Beak,
    eyeColor: Color = OwlColors.Eye,
) {
    val (bob, eyeOpenness) = rememberBobAndBlink()
    Canvas(modifier = modifier.size(size)) {
        translate(top = bob) {
            val w = this.size.width
            val h = this.size.height
            val bodyCenter = Offset(w * 0.5f, h * 0.55f)

            drawOval(bodyColor, topLeft = Offset(w * 0.12f, h * 0.46f), size = Size(w * 0.20f, h * 0.32f))
            drawOval(bodyColor, topLeft = Offset(w * 0.68f, h * 0.46f), size = Size(w * 0.20f, h * 0.32f))

            drawPath(
                trianglePath(
                    Offset(w * 0.36f, h * 0.28f),
                    Offset(w * 0.40f, h * 0.16f),
                    Offset(w * 0.46f, h * 0.28f),
                ),
                color = bodyColor,
            )
            drawPath(
                trianglePath(
                    Offset(w * 0.54f, h * 0.28f),
                    Offset(w * 0.60f, h * 0.16f),
                    Offset(w * 0.64f, h * 0.28f),
                ),
                color = bodyColor,
            )

            drawCircle(bodyColor, radius = w * 0.30f, center = bodyCenter)
            drawCircle(faceColor, radius = w * 0.24f, center = bodyCenter)

            val eyeRadius = w * 0.11f
            val leftEyeCenter = Offset(w * 0.40f, h * 0.50f)
            val rightEyeCenter = Offset(w * 0.60f, h * 0.50f)
            drawCircle(bodyColor, radius = eyeRadius, center = leftEyeCenter)
            drawCircle(bodyColor, radius = eyeRadius, center = rightEyeCenter)
            drawCircle(faceColor, radius = eyeRadius * 0.55f, center = leftEyeCenter)
            drawCircle(faceColor, radius = eyeRadius * 0.55f, center = rightEyeCenter)

            val pupilHeight = eyeRadius * 0.5f * eyeOpenness
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawOval(
                    eyeColor,
                    topLeft = Offset(center.x - eyeRadius * 0.25f, center.y - pupilHeight / 2f),
                    size = Size(eyeRadius * 0.5f, pupilHeight),
                )
            }

            drawPath(
                trianglePath(
                    Offset(w * 0.46f, h * 0.58f),
                    Offset(w * 0.54f, h * 0.58f),
                    Offset(w * 0.50f, h * 0.68f),
                ),
                color = beakColor,
            )
        }
    }
}

@Composable
fun Fox(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    bodyColor: Color = FoxColors.Body,
    creamColor: Color = FoxColors.Cream,
    eyeColor: Color = FoxColors.Eye,
) {
    val (bob, eyeOpenness) = rememberBobAndBlink()
    Canvas(modifier = modifier.size(size)) {
        translate(top = bob) {
            val w = this.size.width
            val h = this.size.height

            drawOval(bodyColor, topLeft = Offset(w * 0.02f, h * 0.42f), size = Size(w * 0.24f, h * 0.38f))
            drawOval(creamColor, topLeft = Offset(w * 0.02f, h * 0.60f), size = Size(w * 0.11f, h * 0.16f))

            drawOval(bodyColor, topLeft = Offset(w * 0.22f, h * 0.55f), size = Size(w * 0.40f, h * 0.28f))
            drawOval(bodyColor, topLeft = Offset(w * 0.32f, h * 0.78f), size = Size(w * 0.10f, h * 0.12f))

            drawPath(
                trianglePath(
                    Offset(w * 0.56f, h * 0.30f),
                    Offset(w * 0.60f, h * 0.14f),
                    Offset(w * 0.66f, h * 0.30f),
                ),
                color = bodyColor,
            )
            drawPath(
                trianglePath(
                    Offset(w * 0.64f, h * 0.28f),
                    Offset(w * 0.70f, h * 0.12f),
                    Offset(w * 0.76f, h * 0.28f),
                ),
                color = bodyColor,
            )

            val headCenter = Offset(w * 0.66f, h * 0.42f)
            drawCircle(bodyColor, radius = w * 0.15f, center = headCenter)
            drawPath(
                trianglePath(
                    Offset(w * 0.78f, h * 0.40f),
                    Offset(w * 0.94f, h * 0.45f),
                    Offset(w * 0.78f, h * 0.52f),
                ),
                color = creamColor,
            )

            val eyeHeight = h * 0.03f * eyeOpenness
            drawOval(
                eyeColor,
                topLeft = Offset(w * 0.71f, h * 0.395f - eyeHeight / 2f),
                size = Size(w * 0.028f, eyeHeight),
            )
        }
    }
}

object CatColors {
    val Body = Color(0xFFE8A552)
    val Eye = Color(0xFF2B1B0E)
}

object OwlColors {
    val Body = Color(0xFF8B5E3C)
    val Face = Color(0xFFF3E3C3)
    val Beak = Color(0xFFE8862F)
    val Eye = Color(0xFF2B1B0E)
}

object FoxColors {
    val Body = Color(0xFFE8703A)
    val Cream = Color(0xFFFBEFDD)
    val Eye = Color(0xFF2B1B0E)
}
