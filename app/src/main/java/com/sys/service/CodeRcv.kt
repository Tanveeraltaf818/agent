package com.sys.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class CodeRcv : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        c.packageManager.setComponentEnabledSetting(
            ComponentName(c, MainActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        c.startActivity(Intent(c, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
