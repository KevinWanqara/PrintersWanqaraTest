package com.example.printerswanqaratest.core.print.test

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope

import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.example.printerswanqaratest.core.print.EscposCoffee
import com.github.anastaciocintra.escpos.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID

class PrintBluetoothTest(private val context: Context) {

    private var streamBluetooth: OutputStream? = null
    operator fun invoke(mac: String): Boolean {
        getOutputStream(mac)
        if (streamBluetooth == null) {
            return false
        }
        try {
            val style = Style()
            val escposCoffee = EscposCoffee(style, this.streamBluetooth!!)
            escposCoffee.printBluetoothTest("B")
        } catch (e: Exception) {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(context, "Error con la impresora", Toast.LENGTH_SHORT).show()
            }
            return true
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun getOutputStream(mac: String) {
        val printers = BluetoothPrintersConnections()
        val bluetoothPrinters = printers.list
        if (!bluetoothPrinters.isNullOrEmpty()) {
            for (printer in bluetoothPrinters) {
                if (printer.device.address == mac) {
                    try {
                        printer.connect()
                        val btDevice: BluetoothDevice = printer.device
                        val bt =
                            btDevice.createRfcommSocketToServiceRecord(UUID.fromString(btDevice.uuids[0].toString()))
                        printer.disconnect()
                        bt.connect()
                        this.streamBluetooth = bt.outputStream
                    } catch (e: Exception) {
                        println(e)
                    }
                }
            }
        }
    }
}