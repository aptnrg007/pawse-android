package com.pawse.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pawse.app.data.AppLimit
import com.pawse.app.picker.AppIconCache

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
            Button(onClick = onAddApp) { Text("+ Add app") }
        }

        if (appLimits.isEmpty()) {
            Text(
                "No apps configured yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        appLimits.forEach { appLimit ->
            AppLimitRow(
                appLimit = appLimit,
                usedMillis = usageMillisByPackage[appLimit.packageName] ?: 0L,
                onAdjustLimit = { delta -> onAdjustLimit(appLimit, delta) },
                onToggleEnabled = { enabled -> onToggleEnabled(appLimit, enabled) },
                onRemove = { onRemove(appLimit) },
            )
        }
    }
}

@Composable
private fun AppLimitRow(
    appLimit: AppLimit,
    usedMillis: Long,
    onAdjustLimit: (Int) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val icon = remember(appLimit.packageName) { AppIconCache.get(context, appLimit.packageName) }
    val usedMinutes = usedMillis / 60_000

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(appLimit.appName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${usedMinutes}m / ${appLimit.dailyLimitMinutes}m today",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(checked = appLimit.enabled, onCheckedChange = onToggleEnabled)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { onAdjustLimit(-LIMIT_STEP_MINUTES) },
                        enabled = appLimit.dailyLimitMinutes > MIN_LIMIT_MINUTES,
                    ) { Text("-") }
                    Text(
                        "${appLimit.dailyLimitMinutes} min",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Button(onClick = { onAdjustLimit(LIMIT_STEP_MINUTES) }) { Text("+") }
                }
                Button(onClick = onRemove) { Text("Remove") }
            }
        }
    }
}
