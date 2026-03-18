package com.printerswanqara.core.print.queue

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.printerswanqara.MainActivity
import com.printerswanqara.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal class PrintJobNotifier(private val context: Context) {

    companion object {
        const val PROGRESS_CHANNEL_ID = "print_jobs_progress"
        const val ALERT_CHANNEL_ID = "print_jobs_alerts"
        private const val PROGRESS_CHANNEL_NAME = "Progreso de impresion"
        private const val ALERT_CHANNEL_NAME = "Alertas de impresion"
        private const val PROGRESS_CHANNEL_DESCRIPTION = "Trabajos en ejecucion"
        private const val ALERT_CHANNEL_DESCRIPTION = "Trabajos en cola, completados y con error"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (manager.getNotificationChannel(PROGRESS_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    PROGRESS_CHANNEL_ID,
                    PROGRESS_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = PROGRESS_CHANNEL_DESCRIPTION
                    setShowBadge(false)
                }
            )
        }

        if (manager.getNotificationChannel(ALERT_CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    ALERT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = ALERT_CHANNEL_DESCRIPTION
                    enableVibration(true)
                }
            )
        }
    }

    fun showQueued(
        notificationId: Int,
        workId: String,
        createdAt: Long,
        jobType: String,
        tenant: String
    ) {
        notify(
            notificationId,
            buildStatusNotification(
                notificationId = notificationId,
                title = "Impresion en cola",
                statusText = "En cola, esperando ejecucion",
                ongoing = false,
                workId = workId,
                createdAt = createdAt,
                jobType = jobType,
                tenant = tenant
            )
        )
    }

    fun showRunning(
        notificationId: Int,
        workId: String,
        createdAt: Long,
        jobType: String,
        tenant: String,
        contentText: String
    ): Notification {
        return buildStatusNotification(
            notificationId = notificationId,
            title = "Imprimiendo",
            statusText = contentText,
            ongoing = true,
            workId = workId,
            createdAt = createdAt,
            jobType = jobType,
            tenant = tenant
        )
    }

    fun updateRunning(
        notificationId: Int,
        workId: String,
        createdAt: Long,
        jobType: String,
        tenant: String,
        contentText: String
    ) {
        notify(
            notificationId,
            buildStatusNotification(
                notificationId = notificationId,
                title = "Imprimiendo",
                statusText = contentText,
                ongoing = true,
                workId = workId,
                createdAt = createdAt,
                jobType = jobType,
                tenant = tenant
            )
        )
    }

    fun showSuccess(
        notificationId: Int,
        workId: String,
        createdAt: Long,
        jobType: String,
        tenant: String,
        contentText: String
    ) {
        notify(
            notificationId,
            buildStatusNotification(
                notificationId = notificationId,
                title = "Impresion completada",
                statusText = contentText,
                ongoing = false,
                workId = workId,
                createdAt = createdAt,
                jobType = jobType,
                tenant = tenant
            )
        )
    }

    fun showFailure(
        notificationId: Int,
        workId: String,
        createdAt: Long,
        jobType: String,
        tenant: String,
        contentText: String,
        retryUri: String? = null
    ) {
        notify(
            notificationId,
            buildStatusNotification(
                notificationId = notificationId,
                title = "Error de impresion",
                statusText = contentText,
                ongoing = false,
                workId = workId,
                createdAt = createdAt,
                jobType = jobType,
                tenant = tenant,
                isError = true,
                retryUri = retryUri
            )
        )
    }

    fun dismiss(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun notify(notificationId: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun buildStatusNotification(
        notificationId: Int,
        title: String,
        statusText: String,
        ongoing: Boolean,
        workId: String,
        createdAt: Long,
        jobType: String,
        tenant: String,
        isError: Boolean = false,
        retryUri: String? = null
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (ongoing) PROGRESS_CHANNEL_ID else ALERT_CHANNEL_ID
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(if (isError) android.R.drawable.stat_notify_error else R.drawable.ic_notification_wanqara)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_wanqara_logo_foreground
                )
            )
            .setContentTitle(title)
            .setContentText("$statusText - $jobType")
            .setSubText("$tenant")
            .setStyle( 
                NotificationCompat.BigTextStyle()
                    .bigText(
                        buildString {
                            append("Estado: ")
                            append(statusText)
                            append("\nTipo: ")
                            append(jobType)
                            append("\nDominio: ")
                            append(tenant)
                            append("\nCreado: ")
                            append(formatDate(createdAt))
                        }
                    )
            )
            .setContentIntent(contentIntent)
            .setCategory(if (isError) NotificationCompat.CATEGORY_ERROR else NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(ongoing)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing && !isError)
            .setPriority(
                when {
                    ongoing -> NotificationCompat.PRIORITY_LOW
                    isError -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )

        if (!ongoing) {
            builder.setDefaults(NotificationCompat.DEFAULT_ALL)
        }

        if (isError && !retryUri.isNullOrBlank()) {
            val retryIntent = Intent(context, PrintJobRetryReceiver::class.java).apply {
                action = PrintJobRetryReceiver.ACTION_RETRY_PRINT_JOB
                putExtra(PrintJobRetryReceiver.EXTRA_URI, retryUri)
            }
            val retryPendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_popup_sync,
                "Reintentar",
                retryPendingIntent
            )
        }

        return builder.build()
    }

    private fun formatDate(timestamp: Long): String {
        if (timestamp <= 0L) return "N/A"
        return DATE_FORMATTER.format(Instant.ofEpochMilli(timestamp))
    }
}

