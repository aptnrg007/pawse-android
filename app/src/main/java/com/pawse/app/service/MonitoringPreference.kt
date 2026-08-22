package com.pawse.app.service

import android.content.Context

/**
 * Whether the user wants monitoring on, independent of whether BlockerService happens
 * to be alive right now. Read on every app open and boot so an explicit Stop stays
 * stopped, and an explicit Start survives a reinstall, a reboot, or the OS killing the
 * service without asking — none of which should require a manual tap to notice and fix.
 */
object MonitoringPreference {
    private const val PREFS_NAME = "monitoring"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }
}
