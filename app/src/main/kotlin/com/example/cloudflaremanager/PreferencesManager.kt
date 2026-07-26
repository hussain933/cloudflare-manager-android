package com.example.cloudflaremanager

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

object PreferencesManager {

    private const val PREFS_NAME = "cloudflare_manager_prefs"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ─── Terms ───────────────────────────────────────────────────────────────
    fun isTermsAccepted(context: Context): Boolean =
        prefs(context).getBoolean("terms_accepted", false)

    fun setTermsAccepted(context: Context, accepted: Boolean) =
        prefs(context).edit().putBoolean("terms_accepted", accepted).apply()

    // ─── Local server port ────────────────────────────────────────────────────
    fun getLocalServerPort(context: Context): Int =
        prefs(context).getInt("local_server_port", 60000)

    fun setLocalServerPort(context: Context, port: Int) =
        prefs(context).edit().putInt("local_server_port", port).apply()

    // ─── Tunnel settings ──────────────────────────────────────────────────────
    fun getTunnelName(context: Context): String =
        prefs(context).getString("tunnel_name", "MyTunnel") ?: "MyTunnel"

    fun setTunnelName(context: Context, name: String) =
        prefs(context).edit().putString("tunnel_name", name).apply()

    fun getTunnelLocalPort(context: Context): Int =
        prefs(context).getInt("tunnel_local_port", 3000)

    fun setTunnelLocalPort(context: Context, port: Int) =
        prefs(context).edit().putInt("tunnel_local_port", port).apply()

    // ─── Theme ────────────────────────────────────────────────────────────────
    fun getTheme(context: Context): String =
        prefs(context).getString("theme", "dark") ?: "dark"

    fun setTheme(context: Context, theme: String) =
        prefs(context).edit().putString("theme", theme).apply()

    // ─── Toggles ──────────────────────────────────────────────────────────────
    fun isAutoCopyUrl(context: Context): Boolean =
        prefs(context).getBoolean("auto_copy_url", true)

    fun isAutoRestartTunnel(context: Context): Boolean =
        prefs(context).getBoolean("auto_restart_tunnel", true)

    fun isNotificationEnabled(context: Context): Boolean =
        prefs(context).getBoolean("notification_enabled", true)

    fun isWakeLockActive(context: Context): Boolean =
        prefs(context).getBoolean("wake_lock_active", false)

    fun setWakeLockActive(context: Context, active: Boolean) =
        prefs(context).edit().putBoolean("wake_lock_active", active).apply()

    // ─── Last known status ────────────────────────────────────────────────────
    fun getLastKnownStatus(context: Context): String =
        prefs(context).getString("last_known_status", "offline") ?: "offline"

    fun setLastKnownStatus(context: Context, status: String) =
        prefs(context).edit().putString("last_known_status", status).apply()

    // ─── Cloudflared version & update check time ──────────────────────────────
    fun getCloudflaredVersion(context: Context): String =
        prefs(context).getString("cloudflared_version", BuildConfig.CLOUDFLARED_BUNDLED_VERSION)
            ?: BuildConfig.CLOUDFLARED_BUNDLED_VERSION

    fun setCloudflaredVersion(context: Context, version: String) =
        prefs(context).edit().putString("cloudflared_version", version).apply()

    fun getLastUpdateChecked(context: Context): Long =
        prefs(context).getLong("cloudflared_last_checked", 0L)

    fun setLastUpdateChecked(context: Context, timestamp: Long) =
        prefs(context).edit().putLong("cloudflared_last_checked", timestamp).apply()

    // ─── Generic set (called from JS bridge) ─────────────────────────────────
    fun set(context: Context, key: String, value: String) {
        val editor = prefs(context).edit()
        when (key) {
            "theme"                -> editor.putString("theme", value)
            "auto_copy_url"        -> editor.putBoolean("auto_copy_url", value == "true")
            "auto_restart_tunnel"  -> editor.putBoolean("auto_restart_tunnel", value == "true")
            "notification_enabled" -> editor.putBoolean("notification_enabled", value == "true")
            "tunnel_name"          -> editor.putString("tunnel_name", value)
            "tunnel_local_port"    -> value.toIntOrNull()?.let { editor.putInt("tunnel_local_port", it) }
            else                   -> editor.putString(key, value)
        }
        editor.apply()
    }

    // ─── JSON snapshot for JS ─────────────────────────────────────────────────
    fun getAllSettingsJson(context: Context): String {
        return JSONObject().apply {
            put("theme", getTheme(context))
            put("autoCopyUrl", isAutoCopyUrl(context))
            put("autoRestartTunnel", isAutoRestartTunnel(context))
            put("notificationEnabled", isNotificationEnabled(context))
            put("localServerPort", getLocalServerPort(context))
            put("tunnelName", getTunnelName(context))
            put("tunnelLocalPort", getTunnelLocalPort(context))
        }.toString()
    }
}
