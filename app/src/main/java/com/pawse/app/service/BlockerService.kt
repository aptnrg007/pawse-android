package com.pawse.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.pawse.app.MainActivity
import com.pawse.app.R
import com.pawse.app.detector.ForegroundDetector
import com.pawse.app.detector.UsageStatsRepository
import com.pawse.app.enforcement.Enforcer

/**
 * Foreground service (type specialUse — see manifest) that keeps [ForegroundDetector]
 * polling while the screen is on, and — from Phase 2 — runs [Enforcer] against the
 * currently foreground package on every tick.
 *
 * Only ever started from an already-foreground context (MainActivity) or, from
 * Phase 3 onward, BOOT_COMPLETED — both are exempt from Android 15's restriction on
 * starting a foreground service from the background. Never start this from a plain
 * background broadcast or alarm.
 */
class BlockerService : Service() {

    companion object {
        const val ACTION_STOP = "com.pawse.app.action.STOP_MONITORING"
        private const val CHANNEL_ID = "pawse_monitoring"
        private const val NOTIFICATION_ID = 1
        private const val POLL_INTERVAL_MS = 1_500L
    }

    private lateinit var foregroundDetector: ForegroundDetector
    private lateinit var enforcer: Enforcer
    private lateinit var screenStateReceiver: ScreenStateReceiver
    private val handler = Handler(Looper.getMainLooper())
    private var isScreenOn = true

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (isScreenOn) {
                val foregroundPackage = foregroundDetector.poll()
                enforcer.maybeEnforce(foregroundPackage)
            }
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        foregroundDetector = ForegroundDetector(usageStatsManager)
        enforcer = Enforcer(applicationContext, UsageStatsRepository(usageStatsManager))

        screenStateReceiver = ScreenStateReceiver(
            onScreenOn = { isScreenOn = true },
            onScreenOff = { isScreenOn = false },
        )
        registerReceiver(
            screenStateReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(pollRunnable)
        unregisterReceiver(screenStateReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): android.app.Notification {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, BlockerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_pawse)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setContentIntent(contentIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
