package com.example.cloudflaremanager

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

object NotificationHelper {

    fun buildTunnelNotification(
        context: Context,
        port: Int,
        wakeLockActive: Boolean,
        richNotification: Boolean = true
    ): Notification {
        val openAppIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, WebViewActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CloudflareManagerApp.TUNNEL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with R.drawable.ic_cloud
            .setContentTitle("Tunnel Running")
            .setContentText("🟢 Port : $port")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)

        if (richNotification) {
            val exitIntent = PendingIntent.getBroadcast(
                context, 0,
                Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_EXIT),
                PendingIntent.FLAG_IMMUTABLE
            )
            val wakeIntent = PendingIntent.getBroadcast(
                context, 1,
                Intent(context, NotificationActionReceiver::class.java)
                    .setAction(NotificationActionReceiver.ACTION_TOGGLE_WAKE),
                PendingIntent.FLAG_IMMUTABLE
            )
            builder
                .addAction(0, "Exit", exitIntent)
                .addAction(0, if (wakeLockActive) "Lock: On" else "Awake to Lock", wakeIntent)
        } else {
            // Minimal notification (user disabled rich notifications)
            // Android still requires a notification for foreground services
            builder.setContentText("Tunnel active")
                .setSubText("Some notification required by Android while tunnel is active")
        }

        return builder.build()
    }
}
