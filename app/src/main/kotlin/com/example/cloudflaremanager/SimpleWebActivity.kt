package com.example.cloudflaremanager

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple full-screen WebView for displaying bundled offline HTML pages
 * (Terms of Service, Privacy Policy) or local asset URLs.
 */
class SimpleWebActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple_web)

        val url = intent.getStringExtra("url") ?: return
        val title = intent.getStringExtra("title")
        if (title != null) supportActionBar?.title = title

        val webView = findViewById<WebView>(R.id.simple_webview)
        webView.settings.javaScriptEnabled = false // Not needed for static docs
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finish()
    }
}
