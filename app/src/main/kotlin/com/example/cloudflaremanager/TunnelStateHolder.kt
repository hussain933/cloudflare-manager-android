package com.example.cloudflaremanager

import org.json.JSONObject

/**
 * In-memory singleton that holds the current tunnel state.
 * Lets WebViewActivity + JsBridge read state synchronously
 * without needing a service binding.
 */
object TunnelStateHolder {

    // "offline" | "starting" | "online" | "error"
    @Volatile var status: String = "offline"
        private set

    @Volatile var publicUrl: String = ""
        private set

    @Volatile var errorMessage: String = ""
        private set

    @Volatile var activeLocalPort: Int = 0
        private set

    fun setStarting(localPort: Int) {
        status = "starting"
        publicUrl = ""
        errorMessage = ""
        activeLocalPort = localPort
    }

    fun setOnline(url: String) {
        status = "online"
        publicUrl = url
        errorMessage = ""
    }

    fun setOffline() {
        status = "offline"
        publicUrl = ""
        errorMessage = ""
        activeLocalPort = 0
    }

    fun setError(message: String) {
        status = "error"
        errorMessage = message
    }

    fun wasOnline(): Boolean = status == "online"

    fun currentStatusJson(): String {
        return JSONObject().apply {
            put("status", status)
            put("url", publicUrl)
            put("message", errorMessage)
            put("activeLocalPort", activeLocalPort)
        }.toString()
    }
}
