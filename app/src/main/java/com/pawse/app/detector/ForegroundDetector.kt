package com.pawse.app.detector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "Pawse"

/**
 * Tracks the current foreground package by polling [UsageStatsManager] incrementally.
 *
 * Events fire on transitions only: a user who opens Instagram and scrolls for five
 * minutes produces no further events after the first second. So [poll] must treat
 * "no new RESUMED event since last poll" as "foreground app unchanged", not as
 * "nothing is foreground" — the latter is the bug that silently stops usage from
 * accruing once a session has been open longer than one poll interval.
 *
 * [lastQueryTimestamp] anchors each query to where the previous one left off, so the
 * query window stays small regardless of how long the service has been running.
 */
class ForegroundDetector(private val usageStatsManager: UsageStatsManager) {

    private var lastQueryTimestamp: Long = startOfTodayMillis()

    var currentForegroundPackage: String? = null
        private set

    /**
     * Queries events since the last poll (or since midnight, on the first call) and
     * updates [currentForegroundPackage] if a newer ACTIVITY_RESUMED event is found.
     *
     * Returns null without changing state if the device is locked — queryEvents
     * returns null in that case (API 30+), which must not be mistaken for "no events".
     */
    fun poll(now: Long = System.currentTimeMillis()): String? {
        val events: UsageEvents = usageStatsManager.queryEvents(lastQueryTimestamp, now)
            ?: return currentForegroundPackage

        var latestResumedPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                latestResumedPackage = event.packageName
            }
        }

        lastQueryTimestamp = now

        if (latestResumedPackage != null && latestResumedPackage != currentForegroundPackage) {
            Log.i(TAG, "foreground -> $latestResumedPackage")
            currentForegroundPackage = latestResumedPackage
        }

        return currentForegroundPackage
    }

    private fun startOfTodayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
