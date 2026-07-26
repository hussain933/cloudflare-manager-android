package com.example.cloudflaremanager

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream

object CloudflaredInstaller {

    /**
     * Ensures the cloudflared binary exists in the app's private filesDir.
     * Copies from bundled assets on first run. Returns the binary File.
     */
    fun ensureInstalled(context: Context): File {
        val destDir = File(context.filesDir, "cloudflared")
        val destFile = File(destDir, "cloudflared")

        if (destFile.exists() && destFile.length() > 1000L) return destFile

        destDir.mkdirs()

        val abi = Build.SUPPORTED_ABIS.firstOrNull {
            it == "arm64-v8a" || it == "armeabi-v7a"
        } ?: "arm64-v8a"

        val assetPath = "cloudflared/$abi/cloudflared"

        context.assets.open(assetPath).use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.setExecutable(true, false)

        // Record the bundled version
        VersionStore.save(context, BuildConfig.CLOUDFLARED_BUNDLED_VERSION)

        return destFile
    }

    fun isInstalled(context: Context): Boolean {
        val destFile = File(context.filesDir, "cloudflared/cloudflared")
        return destFile.exists() && destFile.length() > 1000L
    }

    /**
     * Re-extracts the binary from bundled assets, overwriting whatever is there.
     * Used when the binary is detected as corrupt or missing.
     */
    fun reinstall(context: Context): File {
        val destFile = File(context.filesDir, "cloudflared/cloudflared")
        if (destFile.exists()) destFile.delete()
        return ensureInstalled(context)
    }
}
