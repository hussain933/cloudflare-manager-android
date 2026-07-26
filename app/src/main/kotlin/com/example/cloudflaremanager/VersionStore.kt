package com.example.cloudflaremanager

import android.content.Context
import org.json.JSONObject
import java.io.File

object VersionStore {

    private fun versionFile(context: Context): File =
        File(context.filesDir, "cloudflared/version.json")

    fun get(context: Context): String {
        return try {
            val json = JSONObject(versionFile(context).readText())
            json.getString("version")
        } catch (e: Exception) {
            PreferencesManager.getCloudflaredVersion(context)
        }
    }

    fun save(context: Context, version: String) {
        try {
            versionFile(context).parentFile?.mkdirs()
            val json = JSONObject().apply {
                put("version", version)
                put("lastChecked", System.currentTimeMillis())
            }
            versionFile(context).writeText(json.toString())
            PreferencesManager.setCloudflaredVersion(context, version)
        } catch (e: Exception) {
            // Silently ignore — prefs already updated above
        }
    }
}
