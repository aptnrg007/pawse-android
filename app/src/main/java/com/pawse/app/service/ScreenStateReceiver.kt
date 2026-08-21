package com.pawse.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * SCREEN_ON / SCREEN_OFF are implicit broadcasts and cannot be declared in the
 * manifest (since API 26) — this must be registered at runtime.
 */
class ScreenStateReceiver(
    private val onScreenOn: () -> Unit,
    private val onScreenOff: () -> Unit,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> onScreenOn()
            Intent.ACTION_SCREEN_OFF -> onScreenOff()
        }
    }
}
