package com.printerswanqara.core.print.queue

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.graphics.BitmapFactory
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        const val CHANNEL_ID = "print_jobs"
        private const val CHANNEL_NAME = "Print Jobs"
        private const val CHANNEL_DESCRIPTION = "Queue and status for print jobs"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    fun showQueued(notificationId: Int, workId: String, createdAt: Long) {
        notify(
            notificationId,
            buildStatusNotification(
                notificationId = notificationId,
                title = "Print queued",
                statusText = "Added to queue",
                ongoing = false,
                workId = workId,
                createdAt = createdAt
            )
        )
    }

    fun showRunning(notificationId: Int, workId: String, createdAt: Long, contentText: String): Notification {
        return buildStatusNotification(
            notificationId = notificationId,
            title = "Printing in progress",
            statusText = contentText,
            ongoing = true,
            workId = workId,
            createdAt = createdAt
        )
    }

    fun updateRunning(notificationId: Int, workId: String, createdAt: Long, contentText: String) {
        notify(
            notificationId,
            buildStatusNotification(
                notificationId = notificationId,
                title = "Printing in progress",
                statusText = contentText,
                ongoing = true,
                workId = workId,
                createdAt = createdAt
            )
        )
    }

    fun showSuccess(notificationId: Int, workId: String, createdAt: Long, contentText: String) {
        notify(
            notificationId,
            buildStatusNotification(
                notificationId = notificationId,
                title = "Print completed",
                statusText = contentText,
                ongoing = false,
                workId = workId,
                createdAt = createdAt
            )
        )
    }

    fun showFailure(
        notificationId: Int,
        workId: String,
        createdAt: Long,
        contentText: String,
        retryUri: String? = null
    ) {
        notify(
            notificationId,
            buildStatusNotification(
                notificationId = notificationId,
                title = "Print failed",
                statusText = contentText,
                ongoing = false,
                workId = workId,
                createdAt = createdAt,
                isError = true,
                retryUri = retryUri
            )
        )
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

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (isError) android.R.drawable.stat_notify_error else R.drawable.ic_notification_wanqara)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_wanqara_logo_foreground
                )
            )
            .setContentTitle(title)
            .setContentText(statusText)
            .setSubText("Work ${workId.take(8)}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        buildString {
                            append(statusText)
                            append("\nWork ID: ")
                            append(workId)
                            append("\nCreated: ")
                            append(formatDate(createdAt))
                        }
                    )
            )
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing && !isError)
            .setPriority(if (isError) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)

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
                "Retry",
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
