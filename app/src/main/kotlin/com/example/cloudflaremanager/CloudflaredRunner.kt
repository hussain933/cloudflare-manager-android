package com.example.cloudflaremanager

import android.content.Context
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class CloudflaredRunner(private val context: Context) {

    private var process: Process? = null

    fun start(localPort: Int, logFile: File): Boolean {
        val binaryPath = File(context.filesDir, "cloudflared/cloudflared").absolutePath
        val command = listOf(
            binaryPath,
            "tunnel",
            "--url", "http://127.0.0.1:$localPort",
            "--no-autoupdate"   // We manage updates ourselves via CloudflaredUpdater
        )
        return try {
            // Overwrite (truncate) the log file for this new session
            logFile.parentFile?.mkdirs()
            if (logFile.exists()) {
                // Archive previous log (one level of history)
                val previous = File(logFile.parent, "cloudflare_previous.log")
                logFile.renameTo(previous)
            }
            logFile.createNewFile()

            val builder = ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(logFile)
            process = builder.start()
            true
        } catch (e: IOException) {
            false
        }
    }

    fun stop() {
        process?.let { p ->
            p.destroy() // SIGTERM
            Thread {
                if (!p.waitFor(3, TimeUnit.SECONDS)) {
                    p.destroyForcibly() // escalate to SIGKILL
                }
            }.start()
        }
        process = null
    }

    fun isRunning(): Boolean = process?.isAlive == true

    fun exitValue(): Int? = try { process?.exitValue() } catch (e: IllegalThreadStateException) { null }
}
