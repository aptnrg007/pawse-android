package com.pawse.app.enforcement

import com.pawse.app.data.AppLimit

/**
 * Holds the latest snapshot of enabled limits, kept current by whoever owns this
 * instance collecting Room's observeAll() Flow and calling [updateLimits] on each
 * emission. Reads are synchronous and lock-free (a plain volatile field) because
 * [Enforcer] calls this from a tight poll loop — no DB access on that path.
 */
class LimitChecker {

    @Volatile
    private var limitsByPackage: Map<String, AppLimit> = emptyMap()

    fun updateLimits(limits: List<AppLimit>) {
        limitsByPackage = limits.filter { it.enabled }.associateBy { it.packageName }
    }

    fun appLimitFor(packageName: String): AppLimit? = limitsByPackage[packageName]

    fun limitMillisFor(packageName: String): Long? =
        appLimitFor(packageName)?.dailyLimitMinutes?.times(60_000L)

    fun isOverLimit(packageName: String, usedMillis: Long): Boolean {
        val limitMillis = limitMillisFor(packageName) ?: return false
        return usedMillis >= limitMillis
    }
}
