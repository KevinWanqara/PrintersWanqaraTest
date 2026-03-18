package com.printerswanqara.core.print.queue

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class PrintJobRetryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_RETRY_PRINT_JOB) return

        val rawUri = intent.getStringExtra(EXTRA_URI)
        if (rawUri.isNullOrBlank()) {
            Log.e(TAG, "Retry requested without URI payload")
            return
        }

        try {
            val uri = Uri.parse(rawUri)
            val workId = PrintJobQueueManager.enqueue(context.applicationContext, uri)
            Log.i(TAG, "Retry enqueued. newWorkId=$workId uri=$rawUri")
        } catch (e: Exception) {
            Log.e(TAG, "Retry enqueue failed: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "PrintJobRetryReceiver"
        const val ACTION_RETRY_PRINT_JOB = "com.printerswanqara.action.RETRY_PRINT_JOB"
        const val EXTRA_URI = "extra_uri"
    }
}

