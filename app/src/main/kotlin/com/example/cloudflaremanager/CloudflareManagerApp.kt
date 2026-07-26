package com.example.cloudflaremanager

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class CloudflareManagerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TUNNEL_CHANNEL_ID,
                "Tunnel Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the live status of the active Cloudflare tunnel"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TUNNEL_CHANNEL_ID = "tunnel_channel"
        const val TUNNEL_NOTIF_ID = 1001
    }
}
