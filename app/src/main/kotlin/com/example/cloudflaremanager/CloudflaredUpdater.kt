package com.example.cloudflaremanager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object CloudflaredUpdater {

    private const val RELEASES_API =
        "https://api.github.com/repos/cloudflare/cloudflared/releases/latest"

    // Check at most once per 24 hours automatically
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    fun checkIfNeeded(context: Context) {
        val lastChecked = PreferencesManager.getLastUpdateChecked(context)
        if (System.currentTimeMillis() - lastChecked < CHECK_INTERVAL_MS) return
        checkNow(context)
    }

    fun checkNow(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            if (!isInternetAvailable(context)) return@launch

            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder()
                    .url(RELEASES_API)
                    .header("User-Agent", "CloudflareManager-Android")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@launch
                val json = JSONObject(body)

                val latestTag = json.getString("tag_name")
                val currentVersion = VersionStore.get(context)

                PreferencesManager.setLastUpdateChecked(context, System.currentTimeMillis())

                if (latestTag == currentVersion) {
                    broadcastResult(context, updated = false, version = currentVersion)
                    return@launch
                }

                val abi = Build.SUPPORTED_ABIS.firstOrNull {
                    it == "arm64-v8a" || it == "armeabi-v7a"
                } ?: "arm64-v8a"

                val assetName = if (abi == "arm64-v8a")
                    "cloudflared-android-arm64"
                else
                    "cloudflared-android-arm"

                val assets = json.getJSONArray("assets")
                var downloadUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").contains(assetName)) {
                        downloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                if (downloadUrl == null) return@launch

                val tempFile = File(context.filesDir, "cloudflared/cloudflared.tmp")
                downloadFile(client, downloadUrl, tempFile)

                if (tempFile.exists() && tempFile.length() > 1000L) {
                    val finalFile = File(context.filesDir, "cloudflared/cloudflared")
                    tempFile.setExecutable(true, false)
                    if (tempFile.renameTo(finalFile)) {
                        finalFile.setExecutable(true, false)
                        VersionStore.save(context, latestTag)
                        broadcastResult(context, updated = true, version = latestTag)
                    }
                } else {
                    tempFile.delete()
                }
            } catch (e: Exception) {
                // Silent failure — keep existing working binary
            }
        }
    }

    private fun downloadFile(client: OkHttpClient, url: String, dest: File) {
        dest.parentFile?.mkdirs()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            response.body?.byteStream()?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun broadcastResult(context: Context, updated: Boolean, version: String) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent("UPDATE_CHECK_RESULT")
                .putExtra("updated", updated)
                .putExtra("version", version)
        )
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
