package com.pawse.app.detector

import com.pawse.app.detector.UsageEvent.Type
import org.junit.Assert.assertEquals
import org.junit.Test

private const val INSTAGRAM = "com.instagram.android"
private const val YOUTUBE = "com.google.android.youtube"

private const val DAY_START = 1_000_000L

class UsageCalculatorTest {

    @Test
    fun `session open at midnight with no RESUMED is clamped to dayStart`() {
        val now = DAY_START + 60_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_PAUSED, DAY_START + 20_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        assertEquals(20_000L, totals[INSTAGRAM])
    }

    @Test
    fun `session still open at now is clamped to now`() {
        val now = DAY_START + 90_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 10_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        assertEquals(80_000L, totals[INSTAGRAM])
    }

    @Test
    fun `screen off without a paired PAUSED still terminates the session`() {
        val now = DAY_START + 10_000_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 10_000),
            UsageEvent(INSTAGRAM, Type.SCREEN_NON_INTERACTIVE, DAY_START + 40_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        assertEquals(30_000L, totals[INSTAGRAM])
    }

    @Test
    fun `keyguard shown terminates an open session`() {
        val now = DAY_START + 10_000_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 10_000),
            UsageEvent(INSTAGRAM, Type.KEYGUARD_SHOWN, DAY_START + 25_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        assertEquals(15_000L, totals[INSTAGRAM])
    }

    @Test
    fun `device shutdown terminates an open session`() {
        val now = DAY_START + 10_000_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 10_000),
            UsageEvent(INSTAGRAM, Type.DEVICE_SHUTDOWN, DAY_START + 50_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        assertEquals(40_000L, totals[INSTAGRAM])
    }

    @Test
    fun `a second RESUMED for the same package is a no-op, not a reset`() {
        val now = DAY_START + 100_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 10_000),
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 50_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        // If the second RESUMED reset the start time, this would read 50_000 instead.
        assertEquals(90_000L, totals[INSTAGRAM])
    }

    @Test
    fun `RESUMED for a different package closes the previous session`() {
        val now = DAY_START + 100_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 10_000),
            UsageEvent(YOUTUBE, Type.ACTIVITY_RESUMED, DAY_START + 40_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        assertEquals(30_000L, totals[INSTAGRAM])
        assertEquals(60_000L, totals[YOUTUBE])
    }

    @Test
    fun `a PAUSED for a package that is not currently open is ignored`() {
        val now = DAY_START + 100_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 10_000),
            // Stray/late PAUSED for a package that was never the open session here.
            UsageEvent(YOUTUBE, Type.ACTIVITY_PAUSED, DAY_START + 30_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        // Instagram's session must still be open and run to `now`, undisturbed.
        assertEquals(90_000L, totals[INSTAGRAM])
        assertEquals(null, totals[YOUTUBE])
    }

    @Test
    fun `multiple sessions for the same package accumulate`() {
        val now = DAY_START + 200_000
        val events = listOf(
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 10_000),
            UsageEvent(INSTAGRAM, Type.ACTIVITY_PAUSED, DAY_START + 40_000),
            UsageEvent(YOUTUBE, Type.ACTIVITY_RESUMED, DAY_START + 40_000),
            UsageEvent(YOUTUBE, Type.ACTIVITY_PAUSED, DAY_START + 70_000),
            UsageEvent(INSTAGRAM, Type.ACTIVITY_RESUMED, DAY_START + 70_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        // 30_000 (first session) + 130_000 (second session, open through `now`)
        assertEquals(160_000L, totals[INSTAGRAM])
        assertEquals(30_000L, totals[YOUTUBE])
    }

    @Test
    fun `a stray STOPPED later in the day is not mistaken for a midnight session`() {
        val now = DAY_START + 10_000_000
        val events = listOf(
            // The real midnight session: no RESUMED, correctly back-dated.
            UsageEvent(INSTAGRAM, Type.ACTIVITY_PAUSED, DAY_START + 20_000),
            // A real session in between.
            UsageEvent(YOUTUBE, Type.ACTIVITY_RESUMED, DAY_START + 20_000),
            UsageEvent(YOUTUBE, Type.ACTIVITY_PAUSED, DAY_START + 50_000),
            // Hours later, Instagram emits a STOPPED for a background activity that was
            // never actually foreground again. With nothing open, this must NOT be
            // treated as a second midnight-open session for Instagram.
            UsageEvent(INSTAGRAM, Type.ACTIVITY_STOPPED, DAY_START + 5_000_000),
        )
        val totals = computeUsage(events, DAY_START, now)
        assertEquals(20_000L, totals[INSTAGRAM])
        assertEquals(30_000L, totals[YOUTUBE])
    }

    @Test
    fun `no events yields an empty map`() {
        val totals = computeUsage(emptyList(), DAY_START, DAY_START + 100_000)
        assertEquals(emptyMap<String, Long>(), totals)
    }
}
