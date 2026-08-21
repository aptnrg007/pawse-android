package com.pawse.app.detector

import android.app.usage.UsageStatsManager
import java.time.LocalDate
import java.time.ZoneId

/**
 * Recomputes today's usage from scratch on every call — never accumulates a running
 * total. Service kills, crashes, reboots, and midnight rollover all cost nothing
 * because there's nothing stateful to get out of sync.
 */
class UsageStatsRepository(private val usageStatsManager: UsageStatsManager) {

    fun todayUsageMillis(packageName: String, now: Long = System.currentTimeMillis()): Long {
        val dayStart = startOfTodayMillis()
        // Null while the device is locked (API 30+) — not "no usage today".
        val events = usageStatsManager.queryEvents(dayStart, now) ?: return 0L
        val totals = computeUsage(events.toUsageEventList(), dayStart, now)
        return totals[packageName] ?: 0L
    }

    private fun startOfTodayMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
