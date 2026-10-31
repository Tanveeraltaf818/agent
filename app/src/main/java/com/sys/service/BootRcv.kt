package com.sys.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootRcv : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        runCatching { c.startForegroundService(Intent(c, AgentSvc::class.java)) }
        runCatching { c.startForegroundService(Intent(c, AgentSvc::class.java)) }
    }
}
