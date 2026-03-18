package com.printerswanqara.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
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
        showLogoToast("Enviando trabajo de impresión...")
    }

    private fun showLogoToast(message: String) {
        val systemContext = createSystemThemedContext()
        val backgroundColor = resolveThemeColor(systemContext, android.R.attr.colorBackground, Color.BLACK)
        val textColor = resolveThemeColor(systemContext, android.R.attr.textColorPrimary, Color.WHITE)
        val borderColor = resolveThemeColor(systemContext, android.R.attr.textColorSecondary, 0xFF7A7A7A.toInt())

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24f
                setColor(backgroundColor)
                setStroke(2, borderColor)
            }
        }

        val logoView = ImageView(this).apply {
            setImageResource(R.drawable.ic_wanqara_logo_foreground)
            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                marginEnd = 16
            }
        }

//        val gifView = ImageView(this).apply {
//            layoutParams = LinearLayout.LayoutParams(48, 48).apply {
//                marginEnd = 16
//            }
//        }
//        loadPrintingGif(gifView)

        val textView = TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(textColor)
        }

        container.addView(logoView)
//        container.addView(gifView)
        container.addView(textView)

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            view = container
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, 160)
        }.show()
    }

    private fun loadPrintingGif(target: ImageView) {
        val source = ImageDecoder.createSource(resources, R.drawable.printing)
        val drawable = ImageDecoder.decodeDrawable(source)
        target.setImageDrawable(drawable)
        (drawable as? AnimatedImageDrawable)?.apply {
            repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            start()
        }
    }

    private fun isSystemDarkMode(): Boolean {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun createSystemThemedContext(): ContextThemeWrapper {
        val systemTheme = if (isSystemDarkMode()) {
            android.R.style.Theme_DeviceDefault
        } else {
            android.R.style.Theme_DeviceDefault_Light
        }
        return ContextThemeWrapper(this, systemTheme)
    }

    private fun resolveThemeColor(themedContext: ContextThemeWrapper, attr: Int, fallback: Int): Int {
        val typedValue = TypedValue()
        val wasResolved = themedContext.theme.resolveAttribute(attr, typedValue, true)
        if (!wasResolved) return fallback
        return if (typedValue.resourceId != 0) {
            themedContext.getColor(typedValue.resourceId)
        } else {
            typedValue.data
        }
    }
}
