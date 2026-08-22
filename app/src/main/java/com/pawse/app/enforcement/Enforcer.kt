package com.pawse.app.enforcement

import android.content.Context
import android.content.Intent
import com.pawse.app.BlockActivity
import com.pawse.app.detector.UsageStatsRepository

/**
 * Recomputes usage from scratch and launches [BlockActivity] when the currently
 * foreground package is over its limit.
 *
 * Two guards against a block/dismiss/reblock flicker loop:
 *  - [Blocklist] already covers "BlockActivity is the foreground package" — while it's
 *    showing, the foreground package is this app's own package, which is always protected.
 *  - A per-package cooldown after a block, independent of the above, in case the
 *    foreground package briefly reads as the blocked app again during the dismiss
 *    transition before the next poll settles.
 */
class Enforcer(
    private val context: Context,
    private val usageStatsRepository: UsageStatsRepository,
    private val limitChecker: LimitChecker,
) {
    companion object {
        private const val COOLDOWN_MILLIS = 5_000L
    }

    private val lastBlockedAtMillis = mutableMapOf<String, Long>()

    fun maybeEnforce(foregroundPackage: String?, now: Long = System.currentTimeMillis()) {
        if (foregroundPackage == null) return
        if (Blocklist.isProtected(context, foregroundPackage)) return

        val appLimit = limitChecker.appLimitFor(foregroundPackage) ?: return
        val lastBlocked = lastBlockedAtMillis[foregroundPackage] ?: 0L
        if (now - lastBlocked < COOLDOWN_MILLIS) return

        val usedMillis = usageStatsRepository.todayUsageMillis(foregroundPackage, now)
        if (!limitChecker.isOverLimit(foregroundPackage, usedMillis)) return

        lastBlockedAtMillis[foregroundPackage] = now
        launchBlockActivity(appLimit.appName, appLimit.dailyLimitMinutes, appLimit.avatar)
    }

    private fun launchBlockActivity(appName: String, dailyLimitMinutes: Int, avatar: String) {
        val intent = Intent(context, BlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .putExtra(BlockActivity.EXTRA_APP_NAME, appName)
            .putExtra(BlockActivity.EXTRA_LIMIT_MINUTES, dailyLimitMinutes)
            .putExtra(BlockActivity.EXTRA_AVATAR, avatar)
        context.startActivity(intent)
    }
}
