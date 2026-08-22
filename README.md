# pawse-android

<img src="docs/icon.png" width="96" alt="Pawse app icon" />

A playful Android app that helps reduce screen time by setting daily limits for apps and showing a friendly animal reminder when you've reached your limit.

Android-only, Kotlin + Jetpack Compose, sideload-first (no Play Store constraints assumed). minSdk 29, targetSdk 36, compileSdk 37.

## Download

[**Download the APK (v0.1)**](https://github.com/aptnrg007/pawse-android/releases/download/v0.1/pawse-v0.1-debug.apk) — open this link on your phone, then open the downloaded file to install. Android will ask to allow installs from whatever app you downloaded it with (browser, Files) — that's expected for a sideloaded app. Needs Android 10+ (minSdk 29). See [Permissions](#permissions) below for the one-time setup after installing.

## Status

Built in phases per the dev plan, technical risk first:

- **Phase 0 — Detector**: foreground-app polling via `UsageStatsManager`. Done, device-verified.
- **Phase 1 — Usage calculator**: recomputes today's per-app usage from midnight on every check, no accumulated state. Done, device-verified against the real event log and Digital Wellbeing.
- **Phase 2 — Enforcement**: hard blocklist, cooldown, full-screen block on limit breach. Done, device-verified.
- **Phase 3 — Configuration**: Room-backed per-app limits, app picker, limit editor, boot receiver. Done, device-verified — independent multi-app blocking and config surviving a reboot both confirmed.
- **Phase 4 — The turtle**: an animated Compose Canvas turtle on the block screen, naming the blocked app and its limit. Done, device-verified.

All five phases are complete.

The core design rule: Android's usage events are the only source of truth. The app never keeps a running total — every check recomputes from `queryEvents(midnight, now)`, so service kills, reboots, crashes, and midnight rollover all cost nothing.

## Architecture

```
ForegroundDetector  ->  current foreground package
UsageCalculator     ->  today's usage per package
LimitChecker        ->  over/under limit
Enforcer            ->  launch BlockActivity
```

Each piece is independently swappable — `ForegroundDetector` polls today; it could move to an `AccessibilityService` later without touching anything downstream.

## Permissions

Sideloaded apps hit Android 15's Restricted Settings — after installing, open **App info → ⋮ → Allow restricted settings** before granting Usage access or Display-over-other-apps, or the toggles stay greyed out. The in-app permission screen walks through all of these:

- Usage access (`PACKAGE_USAGE_STATS`) — required for everything
- Notifications — for the foreground-service notification
- Display over other apps (`SYSTEM_ALERT_WINDOW`) — exempts the block screen from Android's background-activity-launch restrictions
- Ignore battery optimisation — keeps the monitoring service alive

## Known limitations

A session that ends right at/near a reboot can lose its last few minutes from Android's
on-disk usage log — enforcement still fires correctly in the moment (it reads from
Android's live event buffer), but that last session may read back as unused afterward
because Android hadn't flushed it to disk yet. This is a platform characteristic of
`UsageStatsManager`, not something Pawse can fix.

Bypass resistance is intentionally out of scope: home button, force-stop, uninstall, and
permission revocation all defeat this app. It's a speed bump, not a lock — see the plan
doc for why that's a deliberate boundary, not an oversight.

## Building

Requires JDK 17 and the Android SDK (platforms 35–37, build-tools 36+).

```
./gradlew assembleDebug   # build
./gradlew testDebugUnitTest   # unit tests (computeUsage's edge cases run on the JVM, no device needed)
```

Real device only for anything usage-stats related — emulator usage stats are unreliable.
