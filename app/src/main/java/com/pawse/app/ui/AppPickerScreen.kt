package com.pawse.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pawse.app.picker.AppIconCache
import com.pawse.app.picker.LaunchableApp
import com.pawse.app.ui.theme.ButtonDark
import com.pawse.app.ui.theme.OnButtonDark
import com.pawse.app.ui.theme.TextSecondary

@Composable
fun AppPickerScreen(
    apps: List<LaunchableApp>,
    alreadyConfiguredPackages: Set<String>,
    onPick: (LaunchableApp) -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Add an app", style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
            ) { Text("Close") }
        }

        LazyColumn {
            items(apps, key = { it.packageName }) { app ->
                val context = LocalContext.current
                val icon = remember(app.packageName) { AppIconCache.get(context, app.packageName) }
                val alreadyConfigured = app.packageName in alreadyConfiguredPackages

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape),
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        if (alreadyConfigured) {
                            Text(
                                "Already added",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                    Button(
                        onClick = { onPick(app) },
                        enabled = !alreadyConfigured,
                        colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
                    ) {
                        Text(if (alreadyConfigured) "Added" else "Add")
                    }
                }
            }
        }
    }
}
