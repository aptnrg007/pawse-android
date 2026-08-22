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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.pawse.app.data.AppLimit
import com.pawse.app.data.PawseDatabase
import com.pawse.app.detector.UsageStatsRepository
import com.pawse.app.permissions.PermissionState
import com.pawse.app.picker.AppIconCache
import com.pawse.app.picker.InstalledAppsRepository
import com.pawse.app.picker.LaunchableApp
import com.pawse.app.service.BlockerService
import com.pawse.app.ui.AppLimitsScreen
import com.pawse.app.ui.AppPickerScreen
import com.pawse.app.ui.Turtle
import com.pawse.app.ui.theme.ButtonDark
import com.pawse.app.ui.theme.Danger
import com.pawse.app.ui.theme.Lavender
import com.pawse.app.ui.theme.OnButtonDark
import com.pawse.app.ui.theme.PawseTheme
import com.pawse.app.ui.theme.Success
import com.pawse.app.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

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
            PawseTheme {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
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
        HeroCard()
        Spacer(Modifier.height(16.dp))

        if (!allGranted) {
            PermissionsCard(
                usageAccessGranted = usageAccessGranted,
                notificationsGranted = notificationsGranted,
                overlayGranted = overlayGranted,
                batteryIgnored = batteryIgnored,
                onOpenAppInfo = onOpenAppInfo,
                onGrantUsageAccess = onGrantUsageAccess,
                onGrantNotifications = onGrantNotifications,
                onGrantOverlay = onGrantOverlay,
                onGrantBattery = onGrantBattery,
            )
        } else {
            Text(
                "✓ All permissions granted",
                color = Success,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(16.dp))

        MonitoringCard(allGranted = allGranted, onStartService = onStartService, onStopService = onStopService)
        Spacer(Modifier.height(24.dp))

        AppMonitoringRow(appLimits = appLimits, onAddApp = onAddApp)
        Spacer(Modifier.height(8.dp))

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
private fun HeroCard() {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Lavender),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(greeting, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Keep your scrolling in check.",
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            Turtle(size = 96.dp)
        }
    }
}

@Composable
private fun PermissionsCard(
    usageAccessGranted: Boolean,
    notificationsGranted: Boolean,
    overlayGranted: Boolean,
    batteryIgnored: Boolean,
    onOpenAppInfo: () -> Unit,
    onGrantUsageAccess: () -> Unit,
    onGrantNotifications: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantBattery: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "If a Settings toggle below is greyed out, Android has restricted it " +
                    "because this app was sideloaded. Open app info first and allow " +
                    "restricted settings from the overflow menu.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onOpenAppInfo,
                colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
            ) { Text("Open app info") }

            Spacer(Modifier.height(12.dp))
            PermissionRow("Usage access", usageAccessGranted, onGrantUsageAccess)
            PermissionRow("Notifications", notificationsGranted, onGrantNotifications)
            PermissionRow("Display over other apps", overlayGranted, onGrantOverlay)
            PermissionRow("Ignore battery optimisation", batteryIgnored, onGrantBattery)
        }
    }
}

@Composable
private fun MonitoringCard(allGranted: Boolean, onStartService: () -> Unit, onStopService: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Monitoring", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onStartService,
                    enabled = allGranted,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
                ) { Text("Start") }
                Button(
                    onClick = onStopService,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = OnButtonDark, contentColor = ButtonDark),
                ) { Text("Stop") }
            }
        }
    }
}

@Composable
private fun AppMonitoringRow(appLimits: List<AppLimit>, onAddApp: () -> Unit) {
    val context = LocalContext.current
    Column {
        Text("App monitoring", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color = ButtonDark),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = OnButtonDark, style = MaterialTheme.typography.titleLarge)
            }
            appLimits.forEach { appLimit ->
                Spacer(Modifier.width(10.dp))
                val icon = remember(appLimit.packageName) { AppIconCache.get(context, appLimit.packageName) }
                Image(
                    icon,
                    contentDescription = appLimit.appName,
                    modifier = Modifier.size(44.dp).clip(CircleShape),
                )
            }
        }
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
                color = if (granted) Success else Danger,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (!granted) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = ButtonDark, contentColor = OnButtonDark),
            ) { Text("Grant") }
        }
    }
}
