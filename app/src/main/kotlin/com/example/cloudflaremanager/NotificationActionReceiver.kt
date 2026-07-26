package com.example.cloudflaremanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_EXIT -> {
                context.stopService(Intent(context, TunnelService::class.java))
            }
            ACTION_TOGGLE_WAKE -> {
                val serviceIntent = Intent(context, TunnelService::class.java)
                    .setAction(TunnelService.ACTION_TOGGLE_WAKE_LOCK)
                context.startService(serviceIntent)
            }
        }
    }

    companion object {
        const val ACTION_EXIT = "ACTION_EXIT"
        const val ACTION_TOGGLE_WAKE = "ACTION_TOGGLE_WAKE"
    }
}
