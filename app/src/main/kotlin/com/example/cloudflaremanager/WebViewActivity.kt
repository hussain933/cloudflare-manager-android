package com.example.cloudflaremanager

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val broadcastReceiver = createBroadcastReceiver()

    // Notification permission request (Android 13+)
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Show explanation — tunnel can still run but without a notification it may be killed
            android.widget.Toast.makeText(
                this,
                "Notification permission denied. The tunnel may be stopped by Android in the background.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        requestNotificationPermissionIfNeeded()

        webView = findViewById(R.id.webview)
        configureWebView()

        val port = PreferencesManager.getLocalServerPort(this)
        webView.loadUrl("http://127.0.0.1:$port/index.html")

        registerBroadcastReceiver()

        // Detect mismatched state (e.g. service killed unexpectedly)
        val lastStatus = PreferencesManager.getLastKnownStatus(this)
        if (lastStatus == "online" || lastStatus == "starting") {
            // Service was probably killed; reset to offline
            PreferencesManager.setLastKnownStatus(this, "offline")
        }
    }

    private fun configureWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(false)
            builtInZoomControls = false
            textZoom = 100
        }
        webView.isLongClickable = false
        webView.setOnLongClickListener { true }
        webView.overScrollMode = WebView.OVER_SCROLL_NEVER
        webView.isHorizontalScrollBarEnabled = false
        webView.isVerticalScrollBarEnabled = false
        webView.addJavascriptInterface(JsBridge(this), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                // Restrict navigation to loopback only; block everything else
                val host = request.url.host ?: return true
                return host != "127.0.0.1"
            }
        }
    }

    private fun registerBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction("STATUS_CHANGED")
            addAction("URL_READY")
            addAction("TUNNEL_ERROR")
            addAction("WAKE_LOCK_CHANGED")
            addAction("UPDATE_CHECK_RESULT")
            addAction("AUTO_COPY_URL")
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter)
    }

    private fun createBroadcastReceiver() = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "STATUS_CHANGED" -> dispatchJsEvent(
                    "onStatusChanged",
                    """{ "status": "${intent.getStringExtra("status")}" }"""
                )
                "URL_READY" -> dispatchJsEvent(
                    "onUrlReady",
                    """{ "url": "${intent.getStringExtra("url")}" }"""
                )
                "TUNNEL_ERROR" -> dispatchJsEvent(
                    "onError",
                    """{ "message": "${intent.getStringExtra("message")?.replace("\"", "'")}" }"""
                )
                "WAKE_LOCK_CHANGED" -> dispatchJsEvent(
                    "onWakeLockChanged",
                    """{ "active": ${intent.getBooleanExtra("active", false)} }"""
                )
                "UPDATE_CHECK_RESULT" -> dispatchJsEvent(
                    "onUpdateCheckResult",
                    """{ "updated": ${intent.getBooleanExtra("updated", false)}, "newVersion": "${intent.getStringExtra("version")}" }"""
                )
                "AUTO_COPY_URL" -> {
                    val url = intent.getStringExtra("url") ?: return
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("Public URL", url)
                    )
                    android.widget.Toast.makeText(
                        this@WebViewActivity, "✓ Link auto-copied", android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun dispatchJsEvent(name: String, detailJson: String) {
        webView.post {
            webView.evaluateJavascript(
                "window.dispatchEvent(new CustomEvent('$name', { detail: $detailJson }));",
                null
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
        // Else do nothing — don't close the app on back press from WebView
    }
}
