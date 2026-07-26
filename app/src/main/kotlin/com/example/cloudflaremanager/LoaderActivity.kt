package com.example.cloudflaremanager

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

class LoaderActivity : AppCompatActivity() {

    private val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧")
    private var spinnerIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var tvSpinner: TextView
    private lateinit var tvStatus: TextView
    private var server: LocalWebServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loader)

        tvSpinner = findViewById(R.id.tv_spinner)
        tvStatus = findViewById(R.id.tv_status)

        startSpinner()
        initializeInBackground()
    }

    private fun startSpinner() {
        val spinnerRunnable = object : Runnable {
            override fun run() {
                tvSpinner.text = spinnerFrames[spinnerIndex % spinnerFrames.size]
                spinnerIndex++
                handler.postDelayed(this, 80)
            }
        }
        handler.post(spinnerRunnable)
    }

    private fun initializeInBackground() {
        CoroutineScope(Dispatchers.IO).launch {
            // Step 1: Extract / verify cloudflared binary
            updateStatus("Preparing engine...")
            try {
                CloudflaredInstaller.ensureInstalled(this@LoaderActivity)
            } catch (e: Exception) {
                showError("Failed to extract engine: ${e.message}")
                return@launch
            }

            // Step 2: Kick off background update check (non-blocking)
            CloudflaredUpdater.checkIfNeeded(this@LoaderActivity)

            // Step 3: Start local web server
            updateStatus("Preparing Local Server...")
            val port = PreferencesManager.getLocalServerPort(this@LoaderActivity)
            val webServer = LocalWebServer(this@LoaderActivity, port)
            try {
                webServer.start(fi.iki.elonen.NanoHTTPD.SOCKET_READY_TIMEOUT, false)
                server = webServer
            } catch (e: java.net.BindException) {
                showError("Port $port already in use — please choose another port in Settings.")
                return@launch
            } catch (e: Exception) {
                showError("Server failed to start: ${e.message}")
                return@launch
            }

            // Step 4: Ping server to confirm it's ready
            updateStatus("Verifying server...")
            val ready = waitForServer(port)
            if (!ready) {
                showError("Server did not respond in time. Tap to retry.")
                return@launch
            }

            // Step 5: Launch WebView
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@LoaderActivity, WebViewActivity::class.java))
                finish()
            }
        }
    }

    private suspend fun waitForServer(port: Int, maxAttempts: Int = 20): Boolean {
        repeat(maxAttempts) {
            delay(250)
            try {
                val conn = URL("http://127.0.0.1:$port/ping").openConnection() as HttpURLConnection
                conn.connectTimeout = 500
                conn.readTimeout = 500
                val code = conn.responseCode
                conn.disconnect()
                if (code == 200) return true
            } catch (e: Exception) {
                // Not ready yet
            }
        }
        return false
    }

    private suspend fun updateStatus(message: String) {
        withContext(Dispatchers.Main) { tvStatus.text = message }
    }

    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            tvStatus.text = "❌ $message"
            tvSpinner.text = "✕"
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
