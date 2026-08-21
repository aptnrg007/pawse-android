package com.pawse.app.detector

import android.app.usage.UsageEvents

/**
 * The only place `android.app.usage.UsageEvents` is touched — keeps [computeUsage]
 * unit-testable on the JVM without a device.
 */
fun UsageEvents.toUsageEventList(): List<UsageEvent> {
    val result = mutableListOf<UsageEvent>()
    val event = UsageEvents.Event()
    while (hasNextEvent()) {
        getNextEvent(event)
        val type = when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> UsageEvent.Type.ACTIVITY_RESUMED
            UsageEvents.Event.ACTIVITY_PAUSED -> UsageEvent.Type.ACTIVITY_PAUSED
            UsageEvents.Event.ACTIVITY_STOPPED -> UsageEvent.Type.ACTIVITY_STOPPED
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEvent.Type.SCREEN_NON_INTERACTIVE
            UsageEvents.Event.KEYGUARD_SHOWN -> UsageEvent.Type.KEYGUARD_SHOWN
            UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEvent.Type.DEVICE_SHUTDOWN
            else -> UsageEvent.Type.OTHER
        }
        result.add(UsageEvent(event.packageName, type, event.timeStamp))
    }
    return result
}
