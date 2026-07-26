package com.example.cloudflaremanager

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import java.io.File

class TunnelService : Service() {

    private val runner = lazy { CloudflaredRunner(this) }
    private val wakeLockManager = lazy { WakeLockManager(this) }
    private var logParser: LogParser? = null
    private var monitorJob: Job? = null
    private var restartAttempts = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // Restore wake lock state from prefs
        if (PreferencesManager.isWakeLockActive(this)) {
            wakeLockManager.value.acquire()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TUNNEL -> {
                val tunnelName = intent.getStringExtra("tunnelName") ?: "MyTunnel"
                val localPort = intent.getIntExtra("localPort", 3000)
                startTunnel(tunnelName, localPort)
            }
            ACTION_STOP_TUNNEL -> stopTunnel()
            ACTION_TOGGLE_WAKE_LOCK -> toggleWakeLock()
        }
        return START_STICKY
    }

    private fun startTunnel(tunnelName: String, localPort: Int) {
        val port = PreferencesManager.getTunnelLocalPort(this).let {
            if (localPort > 0) localPort else it
        }
        PreferencesManager.setTunnelName(this, tunnelName)
        PreferencesManager.setTunnelLocalPort(this, port)
        TunnelStateHolder.setStarting(port)
        PreferencesManager.setLastKnownStatus(this, "starting")

        // Start foreground immediately (Android requires this within 5 seconds)
        val notif = NotificationHelper.buildTunnelNotification(
            this, port, wakeLockManager.value.isActive(),
            PreferencesManager.isNotificationEnabled(this)
        )
        startForeground(CloudflareManagerApp.TUNNEL_NOTIF_ID, notif)
        broadcastStatus("starting")

        val logFile = File(filesDir, "logs/cloudflare.log")
        logFile.parentFile?.mkdirs()

        val started = runner.value.start(port, logFile)
        if (!started) {
            // Binary may be corrupt — attempt reinstall
            try {
                CloudflaredInstaller.reinstall(this)
                runner.value.start(port, logFile)
            } catch (e: Exception) {
                setError("Could not start tunnel engine, please reinstall the app.")
                return
            }
        }

        restartAttempts = 0
        startLogParser(logFile)
        startMonitor(port, logFile)
    }

    private fun startLogParser(logFile: File) {
        logParser?.stop()
        logParser = LogParser(
            logFile = logFile,
            onUrlFound = { url ->
                TunnelStateHolder.setOnline(url)
                PreferencesManager.setLastKnownStatus(this, "online")
                broadcastUrl(url)
                broadcastStatus("online")
                updateNotification()

                if (PreferencesManager.isAutoCopyUrl(this)) {
                    broadcastAutoCopy(url)
                }
            },
            onErrorLine = { line ->
                if (TunnelStateHolder.status == "starting") {
                    // Only surface errors while starting; mid-session errors are logged
                }
            }
        ).also { it.start() }
    }

    private fun startMonitor(port: Int, logFile: File) {
        monitorJob?.cancel()
        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(2000)
                if (!runner.value.isRunning() && TunnelStateHolder.status != "offline") {
                    if (PreferencesManager.isAutoRestartTunnel(this@TunnelService)
                        && TunnelStateHolder.wasOnline()
                        && restartAttempts < 3
                    ) {
                        restartAttempts++
                        delay(5000)
                        runner.value.start(port, logFile)
                        broadcastStatus("starting")
                    } else if (TunnelStateHolder.status != "offline") {
                        setError("Tunnel crashed repeatedly, please restart manually.")
                    }
                }
            }
        }
    }

    private fun stopTunnel() {
        logParser?.stop()
        logParser = null
        monitorJob?.cancel()
        monitorJob = null
        runner.value.stop()
        wakeLockManager.value.release()
        PreferencesManager.setWakeLockActive(this, false)
        TunnelStateHolder.setOffline()
        PreferencesManager.setLastKnownStatus(this, "offline")
        broadcastStatus("offline")
        stopForeground(true)
        stopSelf()
    }

    private fun setError(message: String) {
        TunnelStateHolder.setError(message)
        PreferencesManager.setLastKnownStatus(this, "error")
        broadcastError(message)
        broadcastStatus("error")
    }

    private fun toggleWakeLock() {
        val nowActive = wakeLockManager.value.toggle()
        PreferencesManager.setWakeLockActive(this, nowActive)
        updateNotification()
        broadcastWakeLockChange(nowActive)
    }

    private fun updateNotification() {
        val port = PreferencesManager.getTunnelLocalPort(this)
        val notif = NotificationHelper.buildTunnelNotification(
            this, port, wakeLockManager.value.isActive(),
            PreferencesManager.isNotificationEnabled(this)
        )
        NotificationManagerCompat.from(this)
            .notify(CloudflareManagerApp.TUNNEL_NOTIF_ID, notif)
    }

    // ─── Local broadcast helpers ──────────────────────────────────────────────

    private fun broadcastStatus(status: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent("STATUS_CHANGED").putExtra("status", status)
        )
    }

    private fun broadcastUrl(url: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent("URL_READY").putExtra("url", url)
        )
    }

    private fun broadcastError(message: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent("TUNNEL_ERROR").putExtra("message", message)
        )
    }

    private fun broadcastAutoCopy(url: String) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent("AUTO_COPY_URL").putExtra("url", url)
        )
    }

    private fun broadcastWakeLockChange(active: Boolean) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent("WAKE_LOCK_CHANGED").putExtra("active", active)
        )
    }

    override fun onDestroy() {
        logParser?.stop()
        monitorJob?.cancel()
        runner.value.stop()
        wakeLockManager.value.release() // Always release on destroy
        TunnelStateHolder.setOffline()
        PreferencesManager.setLastKnownStatus(this, "offline")
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_TUNNEL = "START_TUNNEL"
        const val ACTION_STOP_TUNNEL = "STOP_TUNNEL"
        const val ACTION_TOGGLE_WAKE_LOCK = "TOGGLE_WAKE_LOCK"
    }
}
