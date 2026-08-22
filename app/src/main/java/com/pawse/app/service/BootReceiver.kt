package com.pawse.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Restarts monitoring after a reboot. specialUse is not on Android 15's list of
 * foreground-service types blocked from starting during BOOT_COMPLETED (that list is
 * dataSync/camera/mediaPlayback/phoneCall/mediaProjection/microphone), and
 * BOOT_COMPLETED is itself an exemption from the background-activity-start
 * restrictions — so this is safe to call directly, unlike a plain background broadcast.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && MonitoringPreference.isEnabled(context)) {
            ContextCompat.startForegroundService(context, Intent(context, BlockerService::class.java))
        }
    }
}
