package com.example.cloudflaremanager

import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile

class LogParser(
    private val logFile: File,
    private val onUrlFound: (String) -> Unit,
    private val onErrorLine: (String) -> Unit
) {

    private var job: Job? = null

    fun start() {
        job = CoroutineScope(Dispatchers.IO).launch {
            var lastPosition = 0L
            var urlAlreadyFound = false
            while (isActive) {
                if (logFile.exists() && logFile.length() > lastPosition) {
                    try {
                        RandomAccessFile(logFile, "r").use { raf ->
                            raf.seek(lastPosition)
                            var line: String?
                            while (raf.readLine().also { line = it } != null) {
                                val l = line ?: continue
                                if (!urlAlreadyFound) {
                                    URL_REGEX.find(l)?.let { match ->
                                        urlAlreadyFound = true
                                        onUrlFound(match.value)
                                    }
                                }
                                if (l.contains("ERR", ignoreCase = true) ||
                                    l.contains("connection refused", ignoreCase = true) ||
                                    l.contains("failed", ignoreCase = true)
                                ) {
                                    onErrorLine(l)
                                }
                            }
                            lastPosition = raf.filePointer
                        }
                    } catch (e: Exception) {
                        // File may be temporarily unavailable; retry next iteration
                    }
                }
                delay(300)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    companion object {
        val URL_REGEX = Regex("""https://[a-zA-Z0-9\-]+\.trycloudflare\.com""")
    }
}
