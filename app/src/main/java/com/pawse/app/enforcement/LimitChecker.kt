package com.pawse.app.enforcement

/**
 * Hardcoded for Phase 2's proof-of-concept: one package, a short limit so it can
 * actually be tested repeatedly. Real per-app config (Room-backed) is Phase 3.
 */
object LimitChecker {

    private val HARDCODED_LIMITS_MILLIS = mapOf(
        "com.instagram.android" to 2 * 60 * 1000L,
    )

    fun limitMillisFor(packageName: String): Long? = HARDCODED_LIMITS_MILLIS[packageName]

    fun isOverLimit(packageName: String, usedMillis: Long): Boolean {
        val limit = limitMillisFor(packageName) ?: return false
        return usedMillis >= limit
    }
}
