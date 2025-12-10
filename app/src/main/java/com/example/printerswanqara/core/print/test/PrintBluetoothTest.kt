package com.example.printerswanqara.core.print.test

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast

import com.example.printerswanqara.core.print.EscposCoffee
import com.github.anastaciocintra.escpos.Style
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.util.UUID

class PrintBluetoothTest(private val context: Context) {

    private var streamBluetooth: OutputStream? = null
    operator fun invoke(mac: String , testFont : String): Boolean {
        getOutputStream(mac)
        if (streamBluetooth == null) {
            return false
        }
        try {
            val style = Style()
            val escposCoffee = EscposCoffee(style, this.streamBluetooth!!)
            escposCoffee.printBluetoothTest(testFont)
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
        try {
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                return
            }

            val device = bluetoothAdapter.getRemoteDevice(mac)
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
            
            val socket = device.createRfcommSocketToServiceRecord(uuid)
            socket.connect()
            this.streamBluetooth = socket.outputStream
        } catch (e: Exception) {
            println(e)
            this.streamBluetooth = null
        }
    }
}