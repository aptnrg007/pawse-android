package com.pawse.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Config only — no `usedTodayMillis`, no `isBlockedToday`. Usage is always recomputed
 * from UsageStatsManager; "blocked for the rest of the day" is emergent from
 * usage >= dailyLimitMinutes, not a stored flag. If a fifth column starts looking
 * tempting, it should probably be derived instead.
 */
@Entity
data class AppLimit(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int,
    val enabled: Boolean,
    val avatar: String = Avatar.TURTLE.name,
)
