package com.pawse.app.enforcement

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telecom.TelecomManager

/**
 * Packages the enforcer must never block, written before any enforcement logic exists.
 * A bug that fires the blocker on Settings or the launcher means the device can't be
 * used to fix itself.
 */
object Blocklist {

    private val HARD_BLOCKED_PACKAGES = setOf(
        "com.android.settings",
        "com.android.systemui",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.phone",
    )

    fun isProtected(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return true
        if (packageName in HARD_BLOCKED_PACKAGES) return true
        if (packageName == currentLauncherPackage(context)) return true
        if (packageName == defaultDialerPackage(context)) return true
        if (packageName == defaultInputMethodPackage(context)) return true
        return false
    }

    private fun currentLauncherPackage(context: Context): String? {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager
            .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
    }

    private fun defaultDialerPackage(context: Context): String? {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        return telecomManager?.defaultDialerPackage
    }

    private fun defaultInputMethodPackage(context: Context): String? {
        val value = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        return value?.substringBefore('/')
    }
}
