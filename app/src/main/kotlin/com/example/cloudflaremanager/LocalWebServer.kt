package com.example.cloudflaremanager

import android.content.Context
import fi.iki.elonen.NanoHTTPD
import java.io.IOException

class LocalWebServer(private val context: Context, port: Int) : NanoHTTPD("127.0.0.1", port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = if (session.uri == "/" || session.uri.isEmpty()) "/index.html" else session.uri

        // Health check endpoint for LoaderActivity to verify the server is up
        if (uri == "/ping") {
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "ok")
        }

        return try {
            // All website files live under assets/www/
            val assetPath = "www$uri"
            val stream = context.assets.open(assetPath)
            val mime = mimeFor(uri)
            newChunkedResponse(Response.Status.OK, mime, stream)
        } catch (e: IOException) {
            newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found: $uri")
        }
    }

    private fun mimeFor(path: String): String = when {
        path.endsWith(".html") -> "text/html; charset=utf-8"
        path.endsWith(".css")  -> "text/css"
        path.endsWith(".js")   -> "application/javascript"
        path.endsWith(".png")  -> "image/png"
        path.endsWith(".svg")  -> "image/svg+xml"
        path.endsWith(".ico")  -> "image/x-icon"
        path.endsWith(".json") -> "application/json"
        else -> "application/octet-stream"
    }
}
