package com.example.cloudflaremanager

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.ContextCompat

class JsBridge(private val activity: Activity) {

    @JavascriptInterface
    fun startTunnel(tunnelName: String, localPort: Int) {
        val intent = Intent(activity, TunnelService::class.java).apply {
            action = TunnelService.ACTION_START_TUNNEL
            putExtra("tunnelName", tunnelName)
            putExtra("localPort", localPort)
        }
        ContextCompat.startForegroundService(activity, intent)
    }

    @JavascriptInterface
    fun stopTunnel() {
        activity.startService(
            Intent(activity, TunnelService::class.java)
                .setAction(TunnelService.ACTION_STOP_TUNNEL)
        )
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Public URL", text))
        activity.runOnUiThread {
            Toast.makeText(activity, "✓ Link Copied", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun getSettings(): String = PreferencesManager.getAllSettingsJson(activity)

    @JavascriptInterface
    fun saveSetting(key: String, value: String) {
        PreferencesManager.set(activity, key, value)
    }

    @JavascriptInterface
    fun getStatus(): String = TunnelStateHolder.currentStatusJson()

    @JavascriptInterface
    fun getAppInfo(): String = AppInfoProvider.buildJson(activity)

    @JavascriptInterface
    fun checkCloudflaredUpdate() {
        CloudflaredUpdater.checkNow(activity)
    }

    @JavascriptInterface
    fun openExternalLink(url: String) {
        activity.runOnUiThread {
            activity.startActivity(
                Intent(activity, SimpleWebActivity::class.java)
                    .putExtra("url", url)
            )
        }
    }

    @JavascriptInterface
    fun openChangePort() {
        activity.runOnUiThread {
            activity.startActivity(Intent(activity, PortSetupActivity::class.java))
        }
    }

    @JavascriptInterface
    fun vibrateShort() {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = activity.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(30)
            }
        } catch (e: Exception) {
            // Ignore — vibration is optional
        }
    }
}
