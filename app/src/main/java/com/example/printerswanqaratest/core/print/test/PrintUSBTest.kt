package com.example.printerswanqaratest.core.print.test

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.usb.UsbOutputStream
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.example.printerswanqaratest.core.print.EscposCoffee
import com.github.anastaciocintra.escpos.Style
import java.io.OutputStream

class PrintUSBTest(private val context: Context) {

    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"


    operator fun invoke(): Boolean {
        var usbOutputStream: OutputStream? = null
        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                            try {
                                val usbConnection =
                                    UsbPrintersConnections.selectFirstConnected(context)
                                val usbManager =
                                    context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
                                if (usbConnection != null && usbManager != null) {

                                    usbOutputStream =
                                        UsbOutputStream(usbManager, usbConnection.device)
                                    sendPrint(usbOutputStream)

                                }
                            } catch (e: Exception) {
                                println(e)
                            }
                            return
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(usbReceiver, filter , Context.RECEIVER_EXPORTED)
        try {
            val usbConnection = UsbPrintersConnections.selectFirstConnected(context)
            val usbManager =
                context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager

            if (usbConnection != null && usbManager != null) {
                val permissionIntent: PendingIntent = PendingIntent.getBroadcast(
                    context, 0, Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
                )
                if (!usbManager.hasPermission(usbConnection.device)) {
                    usbManager.requestPermission(usbConnection.device, permissionIntent)
                } else {
                    usbOutputStream = UsbOutputStream(usbManager, usbConnection.device)
                    sendPrint(usbOutputStream)
                }
                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            println(e)
            return false
        }
    }

    private fun sendPrint(usbOutputStream: OutputStream?) {
        if (usbOutputStream != null) {
            try {
                val style = Style()
                val escposCoffee = EscposCoffee(style, usbOutputStream)
                escposCoffee.printUSBTest("B")
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun runTest(): Boolean {
        var usbOutputStream: OutputStream? = null
        val usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (ACTION_USB_PERMISSION == intent.action) {
                    synchronized(this) {
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            try {
                                val usbConnection = UsbPrintersConnections.selectFirstConnected(context)
                                val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
                                if (usbConnection != null && usbManager != null) {
                                    usbOutputStream = UsbOutputStream(usbManager, usbConnection.device)
                                    sendPrint(usbOutputStream)
                                }
                            } catch (e: Exception) {
                                println(e)
                            }
                            return
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(ACTION_USB_PERMISSION)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }

        try {
            val usbConnection = UsbPrintersConnections.selectFirstConnected(context)
            val usbManager = context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager

            if (usbConnection != null && usbManager != null) {
                val permissionIntent: PendingIntent = PendingIntent.getBroadcast(
                    context, 0, Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
                )
                if (!usbManager.hasPermission(usbConnection.device)) {
                    usbManager.requestPermission(usbConnection.device, permissionIntent)
                } else {
                    usbOutputStream = UsbOutputStream(usbManager, usbConnection.device)
                    sendPrint(usbOutputStream)
                }
                return true
            } else {
                return false
            }
        } catch (e: Exception) {
            println(e)
            return false
        }
    }

}