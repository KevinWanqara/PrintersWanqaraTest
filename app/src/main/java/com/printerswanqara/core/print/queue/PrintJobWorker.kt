package com.printerswanqara.core.print.queue

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.printerswanqara.core.print.utils.printer1.Discrimination
import com.printerswanqara.core.print.utils.printer1.PrintDiagnosticsBus
import com.printerswanqara.core.print.utils.printer1.PrinterBuilder
import com.printerswanqara.core.print.utils.schema.NetworkPrintHandler
import com.printerswanqara.core.print.utils.schema.SchemaParser
import com.printerswanqara.data.database.DatabaseProvider
import com.printerswanqara.data.database.repositories.PrinterRepository
import com.printerswanqara.domain.services.GetAllPrinters

class PrintJobWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val RESULT_SUCCESS = "result_success"
        private const val RESULT_MESSAGE = "result_message"
    }

    override suspend fun doWork(): Result {
        val uriRaw = PrintJobQueueManager.uriFrom(inputData)
        val workId = id.toString()
        val createdAt = PrintJobQueueManager.createdAtFrom(inputData).takeIf { it > 0L } ?: System.currentTimeMillis()
        val shortJobId = workId.take(8)
        val foregroundNotificationId = PrintJobQueueManager.foregroundNotificationIdFrom(workId)
        val terminalNotificationId = PrintJobQueueManager.terminalNotificationIdFrom(workId)
        val notifier = PrintJobNotifier(applicationContext)
        notifier.ensureChannel()

        if (uriRaw.isNullOrBlank()) {
            Log.e("PrintJobWorker", "Job failed before start: workId=$workId reason=no_uri")
            notifier.showFailure(
                notificationId = terminalNotificationId,
                workId = workId,
                createdAt = createdAt,
                contentText = "Job $shortJobId has no valid URI"
            )
            return Result.failure(
                workDataOf(
                    RESULT_SUCCESS to false,
                    RESULT_MESSAGE to "No URI provided"
                )
            )
        }

        setForeground(
            ForegroundInfo(
                foregroundNotificationId,
                notifier.showRunning(
                    notificationId = foregroundNotificationId,
                    workId = workId,
                    createdAt = createdAt,
                    contentText = "Job $shortJobId started"
                )
            )
        )

        val transportListener: (PrinterBuilder.PrinterDiagnosticsEvent) -> Unit = { evt ->
            val ms = evt.endTimestamp - evt.startTimestamp
            val line =
                "JOB_TRANSPORT ok=${evt.success} type=${evt.transportType} target=${evt.address ?: "-"}:${evt.port ?: "-"} bytes=${evt.bytesLength} dur=${ms}ms${evt.errorMessage?.let { " error=$it" } ?: ""}"
            PrintDiagnosticsBus.appendPersistentLog(applicationContext, line)
        }
        val phaseListener: (PrintDiagnosticsBus.PhaseEvent) -> Unit = { evt ->
            val line =
                "JOB_PHASE doc=${evt.documentType} phase=${evt.phase} dur=${evt.durationMs}ms ok=${evt.success}${evt.extra?.let { " extra=$it" } ?: ""}"
            PrintDiagnosticsBus.appendPersistentLog(applicationContext, line)
        }

        PrinterBuilder.diagnosticsListener = transportListener
        PrintDiagnosticsBus.phaseListener = phaseListener

        val uri = Uri.parse(uriRaw)
        val startedAt = System.currentTimeMillis()
        var success = false
        var resultMessage = "Unknown error"

        try {
            PrintDiagnosticsBus.appendPersistentLog(applicationContext, "JOB_START id=$workId uri=$uriRaw")

            if (SchemaParser.isSendToPrintSchema(uri)) {
                val configs = SchemaParser.parse(uri)
                if (configs.isEmpty()) {
                    resultMessage = "No valid configurations in schema"
                } else {
                    notifier.updateRunning(
                        notificationId = foregroundNotificationId,
                        workId = workId,
                        createdAt = createdAt,
                        contentText = "Job $shortJobId processing 0/${configs.size}"
                    )
                    val handler = NetworkPrintHandler(applicationContext)
                    success = handler.handleConfigs(configs) { current, total, config, ok ->
                        val status = if (ok) "ok" else "error"
                        notifier.updateRunning(
                            notificationId = foregroundNotificationId,
                            workId = workId,
                            createdAt = createdAt,
                            contentText = "Job $shortJobId $current/$total ${config.type} -> ${config.target} [$status]"
                        )
                    }
                    resultMessage = if (success) "Network print schema completed" else "One or more network prints failed"
                }
            } else {
                val data = uri.schemeSpecificPart
                if (data.isNullOrBlank()) {
                    resultMessage = "No command payload in URI"
                } else {
                    val commands = data.split(",").map { it.trimStart('/') }.toTypedArray()
                    val totalJobs = commandPairCount(commands)
                    notifier.updateRunning(
                        notificationId = foregroundNotificationId,
                        workId = workId,
                        createdAt = createdAt,
                        contentText = "Job $shortJobId processing 0/$totalJobs"
                    )

                    val db = DatabaseProvider.getDatabase(applicationContext)
                    val repository = PrinterRepository(db.printersDAO())
                    val printers = GetAllPrinters(repository).getAll()

                    success = Discrimination(printers, applicationContext).invoke(commands) { current, total, command, ok ->
                        val status = if (ok) "ok" else "error"
                        notifier.updateRunning(
                            notificationId = foregroundNotificationId,
                            workId = workId,
                            createdAt = createdAt,
                            contentText = "Job $shortJobId $current/$total $command [$status]"
                        )
                    }
                    resultMessage = if (success) "Print schema completed" else "One or more print commands failed"
                }
            }
        } catch (e: Exception) {
            resultMessage = e.message ?: "Unhandled print error"
            PrintDiagnosticsBus.appendPersistentLog(applicationContext, "JOB_ERROR id=$workId msg=$resultMessage")
            Log.e("PrintJobWorker", "Unhandled error workId=$workId uri=$uriRaw msg=$resultMessage", e)
            success = false
        } finally {
            if (PrinterBuilder.diagnosticsListener === transportListener) {
                PrinterBuilder.diagnosticsListener = null
            }
            if (PrintDiagnosticsBus.phaseListener === phaseListener) {
                PrintDiagnosticsBus.phaseListener = null
            }
        }

        val durationMs = System.currentTimeMillis() - startedAt
        PrintDiagnosticsBus.appendPersistentLog(
            applicationContext,
            "JOB_END id=$workId success=$success dur=${durationMs}ms msg=$resultMessage"
        )

        if (success) {
            notifier.showSuccess(
                notificationId = terminalNotificationId,
                workId = workId,
                createdAt = createdAt,
                contentText = "Job $shortJobId completed in ${durationMs}ms"
            )
        } else {
            Log.e("PrintJobWorker", "Job failed: workId=$workId createdAt=$createdAt uri=$uriRaw msg=$resultMessage")
            notifier.showFailure(
                notificationId = terminalNotificationId,
                workId = workId,
                createdAt = createdAt,
                contentText = "Job $shortJobId failed: $resultMessage",
                retryUri = uriRaw
            )
        }

        return Result.success(
            workDataOf(
                RESULT_SUCCESS to success,
                RESULT_MESSAGE to resultMessage
            )
        )
    }

    private fun commandPairCount(commands: Array<String>): Int {
        if (commands.isEmpty()) return 0
        return commands.size / 2
    }
}
