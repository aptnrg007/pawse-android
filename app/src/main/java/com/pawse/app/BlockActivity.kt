package com.pawse.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pawse.app.data.Avatar
import com.pawse.app.ui.AnimalAvatar
import com.pawse.app.ui.theme.PawseTheme

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
        const val EXTRA_AVATAR = "com.pawse.app.extra.AVATAR"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this) { goHome() }

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val limitMinutes = intent.getIntExtra(EXTRA_LIMIT_MINUTES, -1)
        val avatar = Avatar.fromName(intent.getStringExtra(EXTRA_AVATAR) ?: Avatar.TURTLE.name)
        val emoji = when (avatar) {
            Avatar.TURTLE -> "🐢"
            Avatar.CAT -> "🐱"
            Avatar.OWL -> "🦉"
            Avatar.FOX -> "🦊"
        }

        setContent {
            PawseTheme {
                Surface(color = ScrimColor, modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AnimalAvatar(avatar)
                        Spacer(Modifier.height(24.dp))
                        Text(
                            if (limitMinutes >= 0) {
                                "You've used $appName for $limitMinutes minutes today $emoji"
                            } else {
                                "You've reached today's limit $emoji"
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

// Translucent (BlockActivity's manifest theme is Theme.Pawse.Translucent) — this tints
// and dims whatever's behind rather than hiding it, while still catching every touch.
private val ScrimColor = Color(0xFF1B3A3E).copy(alpha = 0.85f)
private val TextColor = Color(0xFFF3ECDD)
