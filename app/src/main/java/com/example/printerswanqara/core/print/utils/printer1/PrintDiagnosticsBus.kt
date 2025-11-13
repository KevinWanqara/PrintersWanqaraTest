package com.example.printerswanqara.core.print.utils.printer1

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Central lightweight bus for publishing print diagnostics events (phase metrics + transport send events persistence).
 */
object PrintDiagnosticsBus {
    data class PhaseEvent(
        val documentType: String,
        val phase: String,
        val durationMs: Long,
        val success: Boolean,
        val extra: String? = null
    )

    @Volatile
    var phaseListener: ((PhaseEvent) -> Unit)? = null

    private const val LOG_FILE_NAME = "printer_diagnostics.log"

    private fun logFile(context: Context): File = File(context.filesDir, LOG_FILE_NAME)

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun appendPersistentLog(context: Context, line: String) {
        try {
            val ts = timeFormatter.format(Date())
            FileWriter(logFile(context), true).use { fw ->
                fw.append("[$ts] ").append(line).append('\n')
            }
        } catch (_: Exception) {}
    }

    fun readPersistentLogs(context: Context, limit: Int = 5000): List<String> = try {
        logFile(context).takeIf { it.exists() }?.readLines()?.takeLast(limit) ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun clearPersistentLogs(context: Context) {
        try { logFile(context).delete() } catch (_: Exception) {}
    }

    fun shareLogsIntent(context: Context): Intent? {
        return try {
            val file = logFile(context)
            if (!file.exists()) {
                null
            } else {
                val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
