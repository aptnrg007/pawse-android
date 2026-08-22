package com.pawse.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pawse.app.data.AppLimit
import com.pawse.app.picker.AppIconCache
import com.pawse.app.ui.theme.AccentCards
import com.pawse.app.ui.theme.ButtonDark
import com.pawse.app.ui.theme.OnButtonDark
import com.pawse.app.ui.theme.TextSecondary

private const val LIMIT_STEP_MINUTES = 5
private const val MIN_LIMIT_MINUTES = 5

@Composable
fun AppLimitsScreen(
    appLimits: List<AppLimit>,
    usageMillisByPackage: Map<String, Long>,
    onAddApp: () -> Unit,
    onAdjustLimit: (AppLimit, Int) -> Unit,
    onToggleEnabled: (AppLimit, Boolean) -> Unit,
    onRemove: (AppLimit) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Configured apps", style = MaterialTheme.typography.titleLarge)
            Button(
                onClick = onAddApp,
                colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
            ) { Text("+ Add app") }
        }

        if (appLimits.isEmpty()) {
            EmptyAppLimits(onAddApp)
        }

        appLimits.forEachIndexed { index, appLimit ->
            AppLimitRow(
                appLimit = appLimit,
                usedMillis = usageMillisByPackage[appLimit.packageName] ?: 0L,
                cardColor = AccentCards[index % AccentCards.size],
                onAdjustLimit = { delta -> onAdjustLimit(appLimit, delta) },
                onToggleEnabled = { enabled -> onToggleEnabled(appLimit, enabled) },
                onRemove = { onRemove(appLimit) },
            )
        }
    }
}

@Composable
private fun EmptyAppLimits(onAddApp: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = AccentCards.first()),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Turtle(size = 96.dp)
            Text(
                "No apps yet",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                "Add one to start setting daily limits.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
            Button(
                onClick = onAddApp,
                modifier = Modifier.padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
            ) { Text("+ Add app") }
        }
    }
}

@Composable
private fun AppLimitRow(
    appLimit: AppLimit,
    usedMillis: Long,
    cardColor: androidx.compose.ui.graphics.Color,
    onAdjustLimit: (Int) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val icon = remember(appLimit.packageName) { AppIconCache.get(context, appLimit.packageName) }
    val usedMinutes = usedMillis / 60_000
    val limitMillis = appLimit.dailyLimitMinutes * 60_000L
    val progress = if (limitMillis > 0) (usedMillis.toFloat() / limitMillis).coerceIn(0f, 1f) else 0f

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(appLimit.appName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${usedMinutes}m of ${appLimit.dailyLimitMinutes}m today",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Switch(checked = appLimit.enabled, onCheckedChange = onToggleEnabled)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).clip(CircleShape),
                color = ButtonDark,
                trackColor = OnButtonDark.copy(alpha = 0.5f),
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onAdjustLimit(-LIMIT_STEP_MINUTES) },
                        enabled = appLimit.dailyLimitMinutes > MIN_LIMIT_MINUTES,
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
                    ) { Text("-") }
                    Text(
                        "${appLimit.dailyLimitMinutes} min",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(
                        onClick = { onAdjustLimit(LIMIT_STEP_MINUTES) },
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
                    ) { Text("+") }
                }
                Button(
                    onClick = onRemove,
                    colors = ButtonDefaults.buttonColors(containerColor = OnButtonDark, contentColor = ButtonDark),
                ) { Text("Remove") }
            }
        }
    }
}
