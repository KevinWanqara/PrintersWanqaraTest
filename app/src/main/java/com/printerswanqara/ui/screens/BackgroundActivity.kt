package com.printerswanqara.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.printerswanqara.R
import com.printerswanqara.core.print.queue.PrintJobQueueManager

class BackgroundActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
        finish()
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri == null) {
            Log.w("BackgroundActivity", "No URI received. Ignoring request.")
            return
        }

        val workId = PrintJobQueueManager.enqueue(applicationContext, uri)
        Log.d("BackgroundActivity", "Print job queued. workId=$workId uri=$uri")
        showLogoToast("Enviando trabajo de impresion...")
    }

    private fun showLogoToast(message: String) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(0xFF0F172A.toInt())
            }
        }

        val logoView = ImageView(this).apply {
            setImageResource(R.drawable.ic_wanqara_logo_foreground)
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                marginEnd = 16
            }
        }

        val textView = TextView(this).apply {
            text = message
            setTextColor(ContextCompat.getColor(this@BackgroundActivity, android.R.color.white))
            textSize = 14f
        }

        container.addView(logoView)
        container.addView(textView)

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = container
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 160)
        }.show()
    }
}
