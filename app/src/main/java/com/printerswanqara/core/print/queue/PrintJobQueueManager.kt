package com.printerswanqara.core.print.queue

import android.content.Context
import android.net.Uri
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID

object PrintJobQueueManager {

    private const val UNIQUE_QUEUE_WORK_NAME = "wanqara_print_job_queue"
    private const val KEY_URI = "key_uri"
    private const val KEY_CREATED_AT = "key_created_at"

    fun enqueue(context: Context, uri: Uri): UUID {
        val appContext = context.applicationContext
        val notifier = PrintJobNotifier(appContext)
        notifier.ensureChannel()

        val createdAt = System.currentTimeMillis()

        val input = Data.Builder()
            .putString(KEY_URI, uri.toString())
            .putLong(KEY_CREATED_AT, createdAt)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<PrintJobWorker>()
            .setInputData(input)
            .build()

        val workId = workRequest.id.toString()
        val descriptor = PrintJobDescriptorResolver.resolve(appContext, uri)
        val notificationId = foregroundNotificationIdFrom(workId)
        notifier.showQueued(
            notificationId = notificationId,
            workId = workId,
            createdAt = createdAt,
            jobType = descriptor.jobType,
            tenant = descriptor.tenant
        )

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_QUEUE_WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest
        )

        return workRequest.id
    }

    internal fun uriFrom(data: Data): String? = data.getString(KEY_URI)

    internal fun createdAtFrom(data: Data): Long = data.getLong(KEY_CREATED_AT, 0L)

    internal fun foregroundNotificationIdFrom(workId: String): Int {
        return stableNotificationId("fg:$workId")
    }

    internal fun terminalNotificationIdFrom(workId: String): Int {
        return stableNotificationId("term:$workId")
    }

    private fun stableNotificationId(key: String): Int {
        val value = key.hashCode()
        return if (value == Int.MIN_VALUE) 1 else kotlin.math.abs(value)
    }
}
