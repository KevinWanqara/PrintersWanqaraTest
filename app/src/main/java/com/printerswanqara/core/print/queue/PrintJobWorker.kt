package com.printerswanqara.core.print.queue

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
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

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val uriRaw = PrintJobQueueManager.uriFrom(inputData)
        val workId = id.toString()
        val createdAt = PrintJobQueueManager.createdAtFrom(inputData).takeIf { it > 0L } ?: System.currentTimeMillis()
        val foregroundNotificationId = PrintJobQueueManager.foregroundNotificationIdFrom(workId)
        val notifier = PrintJobNotifier(applicationContext)
        notifier.ensureChannel()

        val descriptor = PrintJobDescriptorResolver.resolve(
            applicationContext,
            Uri.parse(uriRaw ?: "")
        )

        val notification = notifier.showRunning(
            notificationId = foregroundNotificationId,
            workId = workId,
            createdAt = createdAt,
            jobType = descriptor.jobType,
            tenant = descriptor.tenant,
            contentText = "Iniciando trabajo de impresion"
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                foregroundNotificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(
                foregroundNotificationId,
                notification
            )
        }
    }

    override suspend fun doWork(): Result {
        val uriRaw = PrintJobQueueManager.uriFrom(inputData)
        val workId = id.toString()
        val createdAt = PrintJobQueueManager.createdAtFrom(inputData).takeIf { it > 0L } ?: System.currentTimeMillis()
        val foregroundNotificationId = PrintJobQueueManager.foregroundNotificationIdFrom(workId)
        val terminalNotificationId = PrintJobQueueManager.terminalNotificationIdFrom(workId)
        val notifier = PrintJobNotifier(applicationContext)
        notifier.ensureChannel()

        val descriptor = PrintJobDescriptorResolver.resolve(
            applicationContext,
            Uri.parse(uriRaw ?: "")
        )

        if (uriRaw.isNullOrBlank()) {
            Log.e("PrintJobWorker", "Job failed before start: workId=$workId reason=no_uri")
            notifier.showFailure(
                notificationId = terminalNotificationId,
                workId = workId,
                createdAt = createdAt,
                jobType = "Desconocido",
                tenant = descriptor.tenant,
                contentText = "No se pudo iniciar: URI no valida"
            )
            return Result.failure(
                workDataOf(
                    RESULT_SUCCESS to false,
                    RESULT_MESSAGE to "No se recibio URI"
                )
            )
        }

        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.e("PrintJobWorker", "Failed to set foreground info", e)
            // On some Android versions/states, setForeground might still fail even with expedited
            // If it's critical, we might return failure, but usually we try to continue if possible
            // unless the OS strictly requires it.
        }

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
        var resultMessage = "Error desconocido"

        try {
            PrintDiagnosticsBus.appendPersistentLog(applicationContext, "JOB_START id=$workId uri=$uriRaw")

            if (SchemaParser.isSendToPrintSchema(uri)) {
                val configs = SchemaParser.parse(uri)
                if (configs.isEmpty()) {
                    resultMessage = "No hay configuraciones validas en el esquema"
                } else {
                    notifier.updateRunning(
                        notificationId = foregroundNotificationId,
                        workId = workId,
                        createdAt = createdAt,
                        jobType = descriptor.jobType,
                        tenant = descriptor.tenant,
                        contentText = "Procesando 0/${configs.size} elementos"
                    )
                    val handler = NetworkPrintHandler(applicationContext)
                    success = handler.handleConfigs(configs) { current, total, config, ok ->
                        val status = if (ok) "ok" else "error"
                        notifier.updateRunning(
                            notificationId = foregroundNotificationId,
                            workId = workId,
                            createdAt = createdAt,
                            jobType = descriptor.jobType,
                            tenant = descriptor.tenant,
                            contentText = "Elemento $current/$total - ${config.type} - ${config.target} [$status]"
                        )
                    }
                    resultMessage = if (success) {
                        "Impresion por esquema completada"
                    } else {
                        "Una o mas impresiones por esquema fallaron"
                    }
                }
            } else {
                val data = uri.schemeSpecificPart
                if (data.isNullOrBlank()) {
                    resultMessage = "No se encontro contenido de comandos en la URI"
                } else {
                    val commands = data.split(",").map { it.trimStart('/') }.toTypedArray()
                    val totalJobs = commandPairCount(commands)
                    notifier.updateRunning(
                        notificationId = foregroundNotificationId,
                        workId = workId,
                        createdAt = createdAt,
                        jobType = descriptor.jobType,
                        tenant = descriptor.tenant,
                        contentText = "Procesando 0/$totalJobs comandos"
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
                            jobType = descriptor.jobType,
                            tenant = descriptor.tenant,
                            contentText = "Comando $current/$total - $command [$status]"
                        )
                    }
                    resultMessage = if (success) {
                        "Impresion completada"
                    } else {
                        "Uno o mas comandos de impresion fallaron"
                    }
                }
            }
        } catch (e: Exception) {
            resultMessage = e.message ?: "Error no controlado de impresion"
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
                jobType = descriptor.jobType,
                tenant = descriptor.tenant,
                contentText = "Completado en ${durationMs}ms"
            )
        } else {
            Log.e("PrintJobWorker", "Job failed: workId=$workId createdAt=$createdAt uri=$uriRaw msg=$resultMessage")
            notifier.showFailure(
                notificationId = terminalNotificationId,
                workId = workId,
                createdAt = createdAt,
                jobType = descriptor.jobType,
                tenant = descriptor.tenant,
                contentText = "Error: $resultMessage",
                retryUri = uriRaw
            )
        }

        notifier.dismiss(foregroundNotificationId)

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
