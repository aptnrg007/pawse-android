package com.pawse.app.detector

import com.pawse.app.detector.UsageEvent.Type

/**
 * Sums today's foreground time per package from a chronological event list.
 *
 * Pure and Android-free by design (see UsageEventMapper for the [android.app.usage.UsageEvents]
 * boundary) so every edge case here is a JVM unit test, not a stopwatch-and-Instagram session.
 *
 * Two edge cases the naive "pair RESUMED with the next PAUSED" approach silently gets wrong:
 *  - A session already open at [dayStart] has no RESUMED in the window — it's represented here
 *    by starting the open session at [dayStart] itself, not by seeing a RESUMED event.
 *  - A session still open at [now] (the current foreground app) has no terminator in the
 *    window — closeSession(now) at the end handles this.
 * Getting either wrong doesn't crash anything; the total just quietly reads low. Screen-off
 * without a paired PAUSED (not guaranteed by the platform) would read *high* if screen/keyguard
 * events weren't also treated as terminators — which is why they're included below alongside
 * ACTIVITY_PAUSED/ACTIVITY_STOPPED, not as a special case.
 */
fun computeUsage(events: List<UsageEvent>, dayStart: Long, now: Long): Map<String, Long> {
    val totals = mutableMapOf<String, Long>()
    var openPackage: String? = null
    var openStart = dayStart

    // The midnight back-date only applies to the one session that could genuinely have
    // been running when the window opened. Real event streams contain terminators
    // (ACTIVITY_STOPPED especially) that fire for activities other than the true
    // foreground app — e.g. a background activity finishing hours later. Without this
    // guard, every such stray terminator arriving while nothing is open would get
    // mistaken for "this package has been running since midnight," which silently
    // inflates totals by hours per occurrence.
    var beforeFirstTransition = true

    fun closeSession(endTime: Long) {
        val pkg = openPackage ?: return
        if (endTime > openStart) {
            totals[pkg] = (totals[pkg] ?: 0L) + (endTime - openStart)
        }
        openPackage = null
    }

    for (event in events) {
        if (event.timestamp < dayStart || event.timestamp > now) continue

        when (event.type) {
            Type.ACTIVITY_RESUMED -> {
                // A second RESUMED for the package already open is a no-op, not a reset.
                if (event.packageName != openPackage) {
                    closeSession(event.timestamp)
                    openPackage = event.packageName
                    openStart = event.timestamp
                }
                beforeFirstTransition = false
            }

            Type.ACTIVITY_PAUSED, Type.ACTIVITY_STOPPED -> {
                if (openPackage == null) {
                    if (beforeFirstTransition) {
                        // No RESUMED seen yet in this window — the session was already
                        // running at dayStart. Back-date it rather than dropping the event.
                        openPackage = event.packageName
                        openStart = dayStart
                        closeSession(event.timestamp)
                    }
                    // else: a stray terminator with nothing open — not the midnight
                    // session, just an unrelated lifecycle event. Ignore it.
                } else if (event.packageName == openPackage) {
                    closeSession(event.timestamp)
                }
                // else: a terminator for a package that isn't the open one — stale/unrelated, ignore.
                beforeFirstTransition = false
            }

            Type.SCREEN_NON_INTERACTIVE, Type.KEYGUARD_SHOWN, Type.DEVICE_SHUTDOWN -> {
                closeSession(event.timestamp)
                beforeFirstTransition = false
            }

            Type.OTHER -> Unit
        }
    }

    closeSession(now)

    return totals
}
