package com.pawse.app.detector

/**
 * Plain, Android-free mirror of the subset of [android.app.usage.UsageEvents.Event]
 * this app cares about. Kept separate from the Android type so the usage-pairing
 * logic (Phase 1) can be unit tested on the JVM without a device.
 */
data class UsageEvent(
    val packageName: String,
    val type: Type,
    val timestamp: Long,
) {
    enum class Type {
        ACTIVITY_RESUMED,

        // Session terminators. A screen/keyguard/shutdown event ends whatever session
        // is open regardless of which package (if any) it's reported against; a pause
        // or stop only ends the session if it names the package that's actually open.
        ACTIVITY_PAUSED,
        ACTIVITY_STOPPED,
        SCREEN_NON_INTERACTIVE,
        KEYGUARD_SHOWN,
        DEVICE_SHUTDOWN,

        OTHER,
    }
}
