package com.pawse.app.picker

import android.content.Context
import android.content.pm.PackageManager

data class LaunchableApp(val packageName: String, val label: String)

object InstalledAppsRepository {

    /** Launchable apps only, sorted by label — matches what a user would recognize. */
    fun getLaunchableApps(context: Context): List<LaunchableApp> {
        val packageManager = context.packageManager
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { LaunchableApp(it.packageName, it.loadLabel(packageManager).toString()) }
            .sortedBy { it.label.lowercase() }
    }
}
