package com.pawse.app.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Read-only checks for every permission this app needs. None of these can be
 * requested through the normal runtime-permission flow except notifications —
 * usage access, overlay, and battery-optimisation exemption all require sending
 * the user to a Settings screen and re-checking on resume.
 */
object PermissionState {

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * True once every permission above is granted. Does not check Android 15+
     * Restricted Settings directly (there is no public API for that) — if it is
     * still blocking usage access or overlay, [hasUsageAccess] / [hasOverlayPermission]
     * will simply keep reading false even after the user visits the Settings screen,
     * which is the signal the UI shows the "clear restricted settings" hint for.
     */
    fun allGranted(context: Context): Boolean =
        hasUsageAccess(context) &&
            hasNotificationPermission(context) &&
            hasOverlayPermission(context) &&
            isIgnoringBatteryOptimizations(context)
}
