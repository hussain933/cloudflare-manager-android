package com.example.cloudflaremanager

import android.content.Context
import android.os.PowerManager

class WakeLockManager(private val context: Context) {

    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire() {
        if (wakeLock?.isHeld == true) return
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CloudflareManager::TunnelWakeLock"
        ).apply {
            setReferenceCounted(false)
            // Safety timeout: auto-release after 6 hours even if never explicitly released
            acquire(6 * 60 * 60 * 1000L)
        }
    }

    fun release() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    fun isActive(): Boolean = wakeLock?.isHeld == true

    fun toggle(): Boolean {
        return if (isActive()) {
            release()
            false
        } else {
            acquire()
            true
        }
    }
}
