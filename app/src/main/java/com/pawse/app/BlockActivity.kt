package com.pawse.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The block screen. Phase 4: a turtle, a calm ground, one message — the plan's scope
 * fence is deliberate, nothing else lives here.
 *
 * Back press must not fall through to the default behavior (return to the blocked
 * app underneath) — it's rerouted to the home screen, same as the button.
 */
class BlockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_NAME = "com.pawse.app.extra.APP_NAME"
        const val EXTRA_LIMIT_MINUTES = "com.pawse.app.extra.LIMIT_MINUTES"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) { goHome() }

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val limitMinutes = intent.getIntExtra(EXTRA_LIMIT_MINUTES, -1)

        setContent {
            MaterialTheme {
                Surface(color = GroundColor, modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Turtle()
                        Spacer(Modifier.height(24.dp))
                        Text(
                            if (limitMinutes >= 0) {
                                "You've used $appName for $limitMinutes minutes today 🐢"
                            } else {
                                "You've reached today's limit 🐢"
                            },
                            color = TextColor,
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Come back tomorrow.", color = TextColor, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(32.dp))
                        Button(onClick = ::goHome) { Text("Okay") }
                    }
                }
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

private val GroundColor = Color(0xFF1B3A3E)
private val TextColor = Color(0xFFF3ECDD)
private val ShellColor = Color(0xFF4C7A67)
private val PlateColor = Color(0xFF35594C)
private val SkinColor = Color(0xFF8FBF9F)
private val EyeColor = Color(0xFF1B3A3E)

@Composable
private fun Turtle(modifier: Modifier = Modifier) {
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

    Canvas(modifier = modifier.size(200.dp)) {
        translate(top = bob) {
            val w = size.width
            val h = size.height

            // Flippers first so the shell overlaps their base.
            listOf(
                Offset(w * 0.02f, h * 0.52f) to Size(w * 0.22f, h * 0.26f),
                Offset(w * 0.76f, h * 0.52f) to Size(w * 0.22f, h * 0.26f),
                Offset(w * 0.10f, h * 0.76f) to Size(w * 0.20f, h * 0.20f),
                Offset(w * 0.70f, h * 0.76f) to Size(w * 0.20f, h * 0.20f),
            ).forEach { (topLeft, flipperSize) -> drawOval(SkinColor, topLeft = topLeft, size = flipperSize) }

            drawOval(ShellColor, topLeft = Offset(w * 0.15f, h * 0.28f), size = Size(w * 0.70f, h * 0.55f))
            val plateRadius = w * 0.055f
            listOf(
                Offset(w * 0.50f, h * 0.42f),
                Offset(w * 0.35f, h * 0.55f),
                Offset(w * 0.65f, h * 0.55f),
                Offset(w * 0.50f, h * 0.66f),
            ).forEach { center -> drawCircle(PlateColor, radius = plateRadius, center = center) }

            val headCenter = Offset(w * 0.5f, h * 0.18f)
            drawCircle(SkinColor, radius = w * 0.13f, center = headCenter)
            val eyeHeight = h * 0.035f * eyeOpenness
            drawOval(
                EyeColor,
                topLeft = Offset(headCenter.x - w * 0.02f, headCenter.y - eyeHeight / 2f),
                size = Size(w * 0.04f, eyeHeight),
            )
        }
    }
}
