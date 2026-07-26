package com.example.cloudflaremanager

import android.content.Context
import android.content.pm.PackageManager
import org.json.JSONObject

object AppInfoProvider {

    fun buildJson(context: Context): String {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
        val cloudflaredVersion = VersionStore.get(context)
        return JSONObject().apply {
            put("versionName", versionName)
            put("cloudflaredVersion", cloudflaredVersion)
            put("packageName", context.packageName)
        }.toString()
    }
}
