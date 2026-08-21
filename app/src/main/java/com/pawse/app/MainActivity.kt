package com.pawse.app

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pawse.app.data.AppLimit
import com.pawse.app.data.PawseDatabase
import com.pawse.app.detector.UsageStatsRepository
import com.pawse.app.permissions.PermissionState
import com.pawse.app.picker.InstalledAppsRepository
import com.pawse.app.picker.LaunchableApp
import com.pawse.app.service.BlockerService
import com.pawse.app.ui.AppLimitsScreen
import com.pawse.app.ui.AppPickerScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEFAULT_LIMIT_MINUTES = 30
private const val USAGE_REFRESH_INTERVAL_MS = 5_000L

private sealed interface Screen {
    data object Home : Screen
    data object Picker : Screen
}

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
        val dao = PawseDatabase.getInstance(applicationContext).appLimitDao()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                    val coroutineScope = rememberCoroutineScope()

                    var appLimits by remember { mutableStateOf<List<AppLimit>>(emptyList()) }
                    LaunchedEffect(Unit) {
                        dao.observeAll().collect { appLimits = it }
                    }

                    var usageMillisByPackage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
                    LaunchedEffect(appLimits) {
                        val usageStatsManager =
                            getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                        val repository = UsageStatsRepository(usageStatsManager)
                        while (true) {
                            usageMillisByPackage = appLimits.associate {
                                it.packageName to repository.todayUsageMillis(it.packageName)
                            }
                            delay(USAGE_REFRESH_INTERVAL_MS)
                        }
                    }

                    when (val current = screen) {
                        Screen.Home -> HomeScreen(
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
                            appLimits = appLimits,
                            usageMillisByPackage = usageMillisByPackage,
                            onAddApp = { screen = Screen.Picker },
                            onAdjustLimit = { appLimit, deltaMinutes ->
                                coroutineScope.launch {
                                    dao.upsert(
                                        appLimit.copy(
                                            dailyLimitMinutes = (appLimit.dailyLimitMinutes + deltaMinutes)
                                                .coerceAtLeast(5),
                                        ),
                                    )
                                }
                            },
                            onToggleEnabled = { appLimit, enabled ->
                                coroutineScope.launch { dao.upsert(appLimit.copy(enabled = enabled)) }
                            },
                            onRemove = { appLimit ->
                                coroutineScope.launch { dao.delete(appLimit) }
                            },
                        )

                        Screen.Picker -> {
                            val launchableApps = remember {
                                InstalledAppsRepository.getLaunchableApps(this@MainActivity)
                            }
                            AppPickerScreen(
                                apps = launchableApps,
                                alreadyConfiguredPackages = appLimits.map { it.packageName }.toSet(),
                                onPick = { app: LaunchableApp ->
                                    coroutineScope.launch {
                                        dao.upsert(
                                            AppLimit(
                                                packageName = app.packageName,
                                                appName = app.label,
                                                dailyLimitMinutes = DEFAULT_LIMIT_MINUTES,
                                                enabled = true,
                                            ),
                                        )
                                    }
                                    screen = Screen.Home
                                },
                                onClose = { screen = Screen.Home },
                            )
                        }
                    }
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
private fun HomeScreen(
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
    appLimits: List<AppLimit>,
    usageMillisByPackage: Map<String, Long>,
    onAddApp: () -> Unit,
    onAdjustLimit: (AppLimit, Int) -> Unit,
    onToggleEnabled: (AppLimit, Boolean) -> Unit,
    onRemove: (AppLimit) -> Unit,
) {
    val allGranted = usageAccessGranted && notificationsGranted && overlayGranted && batteryIgnored

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Pawse", style = MaterialTheme.typography.headlineMedium)
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

        Spacer(Modifier.height(24.dp))
        AppLimitsScreen(
            appLimits = appLimits,
            usageMillisByPackage = usageMillisByPackage,
            onAddApp = onAddApp,
            onAdjustLimit = onAdjustLimit,
            onToggleEnabled = onToggleEnabled,
            onRemove = onRemove,
        )
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
