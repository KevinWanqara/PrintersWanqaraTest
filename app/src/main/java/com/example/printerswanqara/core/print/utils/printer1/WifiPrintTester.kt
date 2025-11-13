package com.example.printerswanqara.core.print.utils.printer1

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Utility for diagnostic WiFi printing: chunked sending, retries, synthetic payload generation.
 */
object WifiPrintTester {
    data class WifiSendResult(
        val success: Boolean,
        val bytesPlanned: Int,
        val bytesSent: Int,
        val attempts: Int,
        val chunks: Int,
        val errors: List<String>,
        val durationMs: Long
    )

    /** Build a synthetic ESC/POS text payload of approximately targetBytes (not strict). */
    fun buildSyntheticEscPosPayload(lines: Int, includeCut: Boolean, lineWidth: Int = 42): ByteArray {
        val sb = StringBuilder()
        // ESC @ init
        sb.append("\u001B@")
        for (i in 1..lines) {
            val content = "LINE "+i+" "+".".repeat((lineWidth - 12).coerceAtLeast(0))
            sb.append(content).append('\n')
        }
        if (includeCut) {
            // GS V full cut
            sb.append('\u001D').append('V').append(1.toChar())
        }
        return sb.toString().toByteArray(Charsets.ISO_8859_1)
    }

    /** Chunked WiFi send with reconnect retries. */
    suspend fun wifiChunkedSend(
        host: String,
        port: Int,
        data: ByteArray,
        chunkSize: Int = 512,
        delayMs: Long = 8,
        retries: Int = 2,
        connectTimeoutMs: Int = 3000,
        soTimeoutMs: Int = 4000
    ): WifiSendResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var sent = 0
        var attempts = 0
        val errors = mutableListOf<String>()
        var socket: Socket? = null
        var out = tryConnect(host, port, connectTimeoutMs, soTimeoutMs, errors)
        while (sent < data.size) {
            if (out == null) {
                if (attempts >= retries) break
                attempts++
                out = tryConnect(host, port, connectTimeoutMs, soTimeoutMs, errors)
                if (out == null) break
            }
            val remaining = data.size - sent
            val size = minOf(chunkSize, remaining)
            try {
                out.write(data, sent, size)
                out.flush()
                sent += size
                if (delayMs > 0) delay(delayMs)
            } catch (e: Exception) {
                errors.add("chunkFail offset=$sent size=$size msg=${e.message}")
                try { out.close() } catch (_: Exception) {}
                out = null
            }
        }
        try { out?.close() } catch (_: Exception) {}
        val end = System.currentTimeMillis()
        WifiSendResult(
            success = sent == data.size,
            bytesPlanned = data.size,
            bytesSent = sent,
            attempts = attempts,
            chunks = (sent + chunkSize - 1) / chunkSize,
            errors = errors,
            durationMs = end - start
        )
    }

    private fun tryConnect(host: String, port: Int, connectTimeoutMs: Int, soTimeoutMs: Int, errors: MutableList<String>): java.io.OutputStream? {
        return try {
            val s = Socket()
            s.connect(InetSocketAddress(host, port), connectTimeoutMs)
            s.soTimeout = soTimeoutMs
            s.tcpNoDelay = true
            s.keepAlive = true
            s.getOutputStream()
        } catch (e: Exception) {
            errors.add("connectFail ${e.message}")
            null
        }
    }
}

