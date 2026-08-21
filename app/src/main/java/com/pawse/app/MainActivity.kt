package com.pawse.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pawse.app.permissions.PermissionState
import com.pawse.app.service.BlockerService

class MainActivity : ComponentActivity() {

    private val usageAccessGranted = mutableStateOf(false)
    private val notificationsGranted = mutableStateOf(false)
    private val overlayGranted = mutableStateOf(false)
    private val batteryIgnored = mutableStateOf(false)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshPermissionState()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionScreen(
                        usageAccessGranted = usageAccessGranted.value,
                        notificationsGranted = notificationsGranted.value,
                        overlayGranted = overlayGranted.value,
                        batteryIgnored = batteryIgnored.value,
                        onOpenAppInfo = ::openAppInfo,
                        onGrantUsageAccess = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        onGrantNotifications = ::requestNotificationPermission,
                        onGrantOverlay = ::openOverlaySettings,
                        onGrantBattery = ::openBatterySettings,
                        onStartService = ::startBlockerService,
                        onStopService = ::stopBlockerService,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionState()
    }

    private fun refreshPermissionState() {
        usageAccessGranted.value = PermissionState.hasUsageAccess(this)
        notificationsGranted.value = PermissionState.hasNotificationPermission(this)
        overlayGranted.value = PermissionState.hasOverlayPermission(this)
        batteryIgnored.value = PermissionState.isIgnoringBatteryOptimizations(this)
    }

    private fun openAppInfo() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")),
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
        )
    }

    private fun openBatterySettings() {
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")),
        )
    }

    private fun startBlockerService() {
        ContextCompat.startForegroundService(this, Intent(this, BlockerService::class.java))
    }

    private fun stopBlockerService() {
        startService(Intent(this, BlockerService::class.java).setAction(BlockerService.ACTION_STOP))
    }
}

@Composable
private fun PermissionScreen(
    usageAccessGranted: Boolean,
    notificationsGranted: Boolean,
    overlayGranted: Boolean,
    batteryIgnored: Boolean,
    onOpenAppInfo: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onGrantNotifications: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantBattery: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
) {
    val allGranted = usageAccessGranted && notificationsGranted && overlayGranted && batteryIgnored

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Pawse", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Phase 0: foreground-app detection only. No limits enforced yet.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    "If a Settings toggle below is greyed out, Android has restricted it " +
                        "because this app was sideloaded. Open app info first and allow " +
                        "restricted settings from the overflow menu.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenAppInfo) { Text("Open app info") }
            }
        }

        PermissionRow("Usage access", usageAccessGranted, onGrantUsageAccess)
        PermissionRow("Notifications", notificationsGranted, onGrantNotifications)
        PermissionRow("Display over other apps", overlayGranted, onGrantOverlay)
        PermissionRow("Ignore battery optimisation", batteryIgnored, onGrantBattery)

        Spacer(Modifier.height(24.dp))
        Button(onClick = onStartService, enabled = allGranted) { Text("Start service") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onStopService) { Text("Stop service") }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (granted) "Granted" else "Not granted",
                color = if (granted) Color(0xFF2E7D32) else Color(0xFFC62828),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (!granted) {
            Button(onClick = onClick) { Text("Grant") }
        }
    }
}
