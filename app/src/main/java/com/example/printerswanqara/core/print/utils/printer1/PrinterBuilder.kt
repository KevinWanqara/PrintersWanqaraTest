package com.example.printerswanqara.core.print.utils.printer1

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.usb.UsbOutputStream
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.example.printerswanqara.core.print.EscposCoffee
import com.example.printerswanqara.core.print.messageBuilder.MediaBuilder
import com.example.printerswanqara.core.printType.PrinterType
import com.github.anastaciocintra.escpos.Style
import com.github.anastaciocintra.output.TcpIpOutputStream
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.OutputStream
import java.math.BigDecimal
import java.net.Socket
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID
import androidx.core.content.edit

import java.text.SimpleDateFormat
import java.util.Date
class PrinterBuilder(private val tipo: String?) {

    private var socketBluetooth: BluetoothSocket? = null
    private var streamBluetooth: OutputStream? = null
    var usbOutputStream: OutputStream? = null
    private var socketRed: Socket? = null
    private var streamRed: OutputStream? = null
    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private val eslocale = Locale("es", "US")
    private val symbols = DecimalFormatSymbols(eslocale)
    private val df = DecimalFormat("0.00", symbols)
    private var port: Int? = null
    private var address: String? = null


    @SuppressLint("MissingPermission")
    fun InicializarImpresoraBluetooth(address: String): Boolean {
        return try {
            val printers = BluetoothPrintersConnections()
            val bluetoothPrinters = printers.list
            if (!bluetoothPrinters.isNullOrEmpty()) {
                for (printer in bluetoothPrinters) {
                    if (printer.device.address == address){
                        try {
                            printer.connect()
                            val btDevice: BluetoothDevice = printer.device
                            val bt =
                                btDevice.createRfcommSocketToServiceRecord(UUID.fromString(btDevice.uuids[0].toString()))
                            printer.disconnect()
                            bt.connect()
                            this.streamBluetooth = bt.outputStream
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun closeBluetooth() {
        try {
            if (socketBluetooth != null) {
                socketBluetooth!!.close()
            }
            if (streamBluetooth != null) {
                streamBluetooth!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun InicializarImpresoraRed(direccion: String?, puerto: Int): Boolean {
        this.port = puerto
        this.address = direccion
        return true
    }

    fun closeRed() {
        try {
            if (socketRed != null) {
                socketRed!!.close()
            }
            if (streamRed != null) {
                streamRed!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    fun InintUsbPrinter(
        context: Context,
    ): Boolean {
        try {
            val usbReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (ACTION_USB_PERMISSION == intent.action) {
                        synchronized(this) {
                            if (intent.getBooleanExtra(
                                    UsbManager.EXTRA_PERMISSION_GRANTED,
                                    false
                                )
                            ) {
                                try {
                                    val usbConnection =
                                        UsbPrintersConnections.selectFirstConnected(context)
                                    val usbManager =
                                        context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager
                                    if (usbConnection != null && usbManager != null) {
                                        usbOutputStream =
                                            UsbOutputStream(usbManager, usbConnection.device)
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

            ContextCompat.registerReceiver(
                context,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            val usbConnection = UsbPrintersConnections.selectFirstConnected(context)
            val usbManager =
                context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager

            if (usbConnection != null) {
                val permissionIntent: PendingIntent = PendingIntent.getBroadcast(
                    context, 0, Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_IMMUTABLE
                )
                if (!usbManager.hasPermission(usbConnection.device)) {
                    usbManager.requestPermission(usbConnection.device, permissionIntent)
                    context.getSharedPreferences("usb", 0).edit {
                        putBoolean("permissions", true)
                    }
                } else {
                    usbOutputStream = UsbOutputStream(usbManager, usbConnection.device)
                    context.getSharedPreferences("usb", 0).edit {
                        putBoolean("permissions", false)
                    }
                }
                return true
            }
        } catch (e: Exception) {
            println(e)
            return false
        }
        return false
    }

    fun closeUsbPrinter() {
        try {
            if (usbOutputStream != null) {
                usbOutputStream!!.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun closeAll() {
        closeBluetooth()
        closeRed()
        closeUsbPrinter()
    }
    //sj settings object
    //js data object

    suspend fun imprimirFacturaElectronica(
        js: JSONObject?,
        sj: JSONObject?,
        copias: Int,
        caracteres: Int
    ) {
        println("Imprimiendo factura electronica")
        println(js.toString())
        try {
            if (js == null) return


            var detalles: JSONArray

            var jo: JSONObject

            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("invoice")
            println("Printer Config: $printerConfig")



            for (i in 0 until copias) {
                println("Imprimiendo copia $i")

                // imprimir
                val prn = PrinterHelpers(caracteres, copias)
                prn.iniciar()
                prn.dobleAltoOn()
                val lineSpacing = printerConfig?.optBoolean("line_spacing")
                val logo = printerConfig?.optBoolean("logo")

                if (lineSpacing == true ) {
                    println("Setting line spacing applied")
                    prn.lineHeight2()
                }else {
                    println("Setting default line spacing")
                    prn.lineHeight()
                }
                prn.alineadoCentro()


                //Font selection based on printerConfig
                val fontType = printerConfig?.optString("font") ?: "A"
                println("Font Type: $fontType")
                when (fontType.uppercase()) {
                    "A" -> prn.setFontA() //Normal
                    "B" -> prn.setFontB() //Pequeña
                    "AA" -> prn.setFontAA() //Extra Grande
                    "BB" -> prn.setFontBB() //Grande

                }


                // Validate if the "image" key exists
                val subsidiary = js.optJSONObject("subsidiary")
                val image = subsidiary?.optJSONObject("image")
                val url = image?.optString("full_path", "")

                if (!url.isNullOrEmpty() && logo == true) {
                    prn.logo(url, this)
                } else {
                    println("Image URL is missing or invalid.")
                }
                if (sj != null) {


                    if (sj.optString("business_name") != "null") {
                        prn.escribirTextoSinSalto(sj.optString("business_name"))
                        prn.agregarSalto()
                    }
                    prn.escribirTextoSinSalto(
                        js.getJSONObject("subsidiary").optString("commercial_name")
                    )
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("RUC: " + sj.optString("ruc"))
                    prn.agregarSalto()

                    prn.escribirTextoSinSalto("Dirección: " + sj.optString("address"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto(
                        "Sucursal: " + js.getJSONObject("subsidiary").optString("address")
                    )
                    prn.agregarSalto()
                    if (sj.optString("phone") != "null") {
                        prn.escribirTextoSinSalto("Teléfono: " + sj.optString("phone"))
                    }
                    prn.agregarSalto()
                    if (sj.optString("email") != "null") prn.escribirTextoSinSalto(
                        "Correo: " + sj.optString(
                            "email"
                        )
                    )


                }
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Factura Electrónica Nº:")
                prn.escribirTextoSinSalto(js.optString("number"))
                prn.agregarSalto()
                if (sj != null) {
                    val env = when (sj.optString("environment")) {

                        "1" -> "Producción"
                        else -> "Pruebas"
                    }
                    prn.escribirTextoSinSalto("Ambiente: " + env)
                    prn.escribirTextoSinSalto("Emision: " + "Normal")
                }
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Clave de Acceso/Nº de Autorización:")
                prn.escribirTextoSinSalto(js.optString("access_key"))



                prn.agregarSalto()

                prn.alineadoIzquierda()
                prn.escribirTextoSinSalto("Fecha: ")
                prn.escribirTextoSinSalto(js.optString("date"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Cliente: ")
                prn.escribirTextoSinSalto(js.getJSONObject("customer").optString("name"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Identificación: ")
                prn.escribirTextoSinSalto(js.getJSONObject("customer").optString("identity"))

                val user = printerConfig?.optBoolean("user")


                if( user == true) {
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Vendedor: ")
                    prn.escribirTextoSinSalto(js.getJSONObject("user").optString("name"))
                }

                val order = js.optJSONObject("order")
                if (order != null) {
                    prn.escribirTextoSinSalto("Orden: ")

                    prn.escribirTexto(order.optString("sequential", "N/A"))
                }
                prn.LineasGuion()
                prn.escribirTextoSinSalto("Cant Descripción")
                prn.agregarCaracteres((caracteres - 26).coerceAtLeast(0), "")
                prn.escribirTextoSinSalto("P.U. Total")
                prn.agregarSalto()
                prn.LineasIgual()
                detalles = js.getJSONArray("details")
                for (j in 0 until detalles.length()) {
                    jo = detalles.getJSONObject(j)

                    val product = jo.getJSONObject("product")
                    val basePrice = product.optDouble("price")

                    val taxesArray = product.optJSONArray("taxes")
                    val taxRate = if (taxesArray != null && taxesArray.length() > 0) {
                        taxesArray.getJSONObject(0).optDouble("rate", 0.0)
                    } else {
                        0.0
                    }
                    val taxedPrice = basePrice * (1 + taxRate / 100)

                    prn.agregarTexto(
                        prn.lineaDetails(

                            jo.optString("amount"),
                            jo.getJSONObject("product").optString("name"),
                            taxedPrice.toString(),
                            jo.optString("total_amount"),
                            caracteres
                        )
                    )
                }
                prn.LineasIgual()


                //Summary
                prn.alineadoDerecha()

                if (js.has("discount")) {

                    prn.escribirTextoSinSalto("Descuentos:")
                    prn.agregarCaracteresDerecha(10, df.format(js.getDouble("discount")))
                    prn.agregarSalto()

                }
                prn.escribirTextoSinSalto("Subtotal:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subtotal")))
                prn.agregarSalto()
                detalles = js.getJSONArray("taxes")
                val detallesList = mutableListOf<JSONObject>()
                for (j in 0 until detalles.length()) {
                    detallesList.add(detalles.getJSONObject(j))
                }
                detallesList.sortBy { it.optDouble("rate", 0.0) }
                // Print Subtotals sorted by rate
                for (dl in detallesList) {
                    prn.escribirTextoSinSalto("Subtotal " + dl.optString("rate").toDoubleOrNull()?.toInt()  + "%:")
                    prn.agregarCaracteresDerecha(10, df.format(dl.getDouble("base")))
                    prn.agregarSalto()
                }

// Print IVA sorted by rate
                for (dl in detallesList) {
                    prn.escribirTextoSinSalto("IVA " + dl.optString("rate").toDoubleOrNull()?.toInt() + "%:")
                    prn.agregarCaracteresDerecha(10, df.format(dl.getDouble("amount")))
                    prn.agregarSalto()
                }
                //agregar total
                prn.escribirTextoSinSalto("Total:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Entrega:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Cambio:")

                prn.agregarCaracteresDerecha(
                    10,
                    df.format(js.getDouble("change_amount")).replace("-", "")
                )

                prn.agregarSalto()
                val observation = js.optString("observation", null)
                if (!observation.isNullOrEmpty()) {
                    prn.alineadoIzquierdaForce("Observacion: ")
                    prn.alineadoIzquierdaForce(observation)
                }
                val orderData = js.optJSONObject("order" ) ?: JSONObject()
                val deliveryRecord = orderData.optJSONObject("delivery_record")

                if(deliveryRecord != null) {
                    prn.alineadoIzquierda()
                    prn.negritaOn()
                    prn.agregarTexto("Datos de Entrega")
                    prn.negritaOff()
                    prn.agregarTexto( "Direccion: " +
                            deliveryRecord.optString("address_string", "N/A"))
                    val delivery = deliveryRecord.optJSONObject("delivery")
                    if (delivery != null) {
                        prn.agregarTexto("Nombre: " +
                                delivery.optString("name", "N/A"))

                        prn.agregarTexto("Telefono: " +
                                delivery.optString("phone" ,"N/A"))
                        prn.agregarTexto("Observacion General: "+
                                deliveryRecord.optString("observation" ,"N/A"))
                        prn.agregarTexto("Observacion de Entrega: " +
                                delivery.optString("observation" ,"N/A"))
                    } else {
                        prn.agregarTexto("N/A")
                    }
                }
                val line_breaks = printerConfig?.optInt("line_breaks") ?: 0
                for (j in 0 until line_breaks) {
                    prn.agregarSalto()
                }
                prn.feedFinal()
                prn.cortar()

                enviarImprimir(prn.getTrabajo())
                closeAll()

            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    suspend fun imprimirRecibo(js: JSONObject?, sj : JSONObject?, copias: Int, caracteres: Int) {

        println("Imprimiendo recibo")
        println(js.toString())
        try {
            if (js == null) return
            var detalles: JSONArray
            var jo: JSONObject
            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("receipt")
            println("Printer Config: $printerConfig")

            val prn = PrinterHelpers(caracteres, copias)
            val lineSpacing = printerConfig?.optBoolean("line_spacing")
            val logo = printerConfig?.optBoolean("logo")
            //Font selection based on printerConfig
            val fontType = printerConfig?.optString("font") ?: "A"
            println("Font Type: $fontType")


            for (i in 0 until copias) {
                println("Imprimiendo copia $i")
                // imprimir
                prn.iniciar()
                //prn.dobleAltoOn()

                when (fontType.uppercase()) {
                    "A" -> prn.setFontA() //Normal
                    "B" -> prn.setFontB() //Pequeña
                    "AA" -> prn.setFontAA() //Extra Grande
                    "BB" -> prn.setFontBB() //Grande

                }



                if (lineSpacing == true ) {
                    println("Setting line spacing applied")
                    prn.lineHeight2()
                }else {
                    println("Setting default line spacing")
                    prn.lineHeight()
                }


                prn.alineadoCentro()

                prn.agregarSalto()
                // Validate if the "image" key exists
                val subsidiary = js.optJSONObject("subsidiary")
                val image = subsidiary?.optJSONObject("image")
                val url = image?.optString("full_path", "")

                if (!url.isNullOrEmpty() && logo == true) {
                    prn.logo(url, this)
                } else {
                    println("Image URL is missing or invalid.")
                }
                if (sj != null) {
                    if (sj.optString("business_name") != "null") {
                        prn.escribirTextoSinSalto(sj.optString("business_name"))
                        prn.agregarSalto()
                    }
                    prn.escribirTextoSinSalto(
                        js.getJSONObject("subsidiary").optString("commercial_name")
                    )
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("RUC: " + sj.optString("ruc"))
                    prn.agregarSalto()

                    prn.escribirTextoSinSalto("Dirección: " + sj.optString("address"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto(
                        "Sucursal: " + js.getJSONObject("subsidiary").optString("address")
                    )
                    prn.agregarSalto()
                    if (sj.optString("phone") != "null") {
                        prn.escribirTextoSinSalto("Teléfono: " + sj.optString("phone"))
                    }
                    prn.agregarSalto()
                    if (sj.optString("email") != "null") prn.escribirTextoSinSalto(
                        "Correo: " + sj.optString(
                            "email"
                        )
                    )
                }
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Recibo ")
                prn.escribirTextoSinSalto(js.optString("number"))
                prn.agregarSalto()
                prn.alineadoIzquierda()
                prn.escribirTextoSinSalto("Fecha: ")
                prn.escribirTextoSinSalto(js.optString("date"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Cliente: ")
                prn.escribirTextoSinSalto(js.getJSONObject("customer").optString("name"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Identificación: ")
                prn.escribirTextoSinSalto(js.getJSONObject("customer").optString("identity"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Teléfono: ")
                prn.escribirTextoSinSalto(js.getJSONObject("customer").getJSONArray("phones").optString(0, ""))
                val user = printerConfig?.optBoolean("user")

                if( user == true) {
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Vendedor: ")
                    prn.escribirTextoSinSalto(js.getJSONObject("user").optString("name"))
                }

                prn.agregarSalto()

                val order = js.optJSONObject("order")
                if (order != null) {
                    prn.escribirTextoSinSalto("Orden: ")

                    prn.escribirTexto(order.optString("sequential", "N/A"))
                }
                prn.LineasGuion()
                prn.escribirTextoSinSalto("Cant Descripción")
                prn.agregarCaracteres((caracteres - 26).coerceAtLeast(0), "")
                prn.escribirTextoSinSalto("P.U. Total")
                prn.agregarSalto()
                prn.LineasIgual()
                detalles = js.getJSONArray("details")
                for (j in 0 until detalles.length()) {
                    jo = detalles.getJSONObject(j)

                    val product = jo.getJSONObject("product")
                    val basePrice = product.optDouble("price")

                    val taxesArray = product.optJSONArray("taxes")
                    val taxRate = if (taxesArray != null && taxesArray.length() > 0) {
                        taxesArray.getJSONObject(0).optDouble("rate", 0.0)
                    } else {
                        0.0
                    }
                    val taxedPrice = basePrice * (1 + taxRate / 100)

                    prn.agregarTexto(
                        prn.lineaDetails(

                            jo.optString("amount"),
                            jo.getJSONObject("product").optString("name"),
                            taxedPrice.toString(),
                            jo.optString("total_amount"),
                            caracteres
                        )
                    )
                }
                prn.LineasIgual()
                //Summary
                prn.alineadoDerecha()
                if (js.has("discount")) {
                    prn.escribirTextoSinSalto("Descuentos:")
                    prn.agregarCaracteresDerecha(10, df.format(js.getDouble("discount")))
                    prn.agregarSalto()
                }
                prn.escribirTextoSinSalto("Subtotal:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subtotal")))
                prn.agregarSalto()
                detalles = js.getJSONArray("taxes")
                val detallesList = mutableListOf<JSONObject>()
                for (j in 0 until detalles.length()) {
                    detallesList.add(detalles.getJSONObject(j))
                }
                detallesList.sortBy { it.optDouble("rate", 0.0) }
                // Print Subtotals sorted by rate
                val taxes = printerConfig?.optBoolean("taxes")
                if(taxes == true){
                    for (dl in detallesList) {
                        prn.escribirTextoSinSalto("Subtotal " + dl.optString("rate").toDoubleOrNull()?.toInt()  + "%:")
                        prn.agregarCaracteresDerecha(10, df.format(dl.getDouble("base")))
                        prn.agregarSalto()
                    }
                    for (dl in detallesList) {
                        prn.escribirTextoSinSalto("IVA " + dl.optString("rate").toDoubleOrNull()?.toInt() + "%:")
                        prn.agregarCaracteresDerecha(10, df.format(dl.getDouble("amount")))
                        prn.agregarSalto()
                    }
                }


// Print IVA sorted by rate

                //agregar total
                prn.escribirTextoSinSalto("Total:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Entrega:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Cambio:")

                prn.agregarCaracteresDerecha(
                    10,
                    df.format(js.getDouble("change_amount")).replace("-", "")
                )
                prn.agregarSalto()
                val observation = js.optString("observation", null)
                if (!observation.isNullOrEmpty()) {
                    prn.alineadoIzquierdaForce("Observacion: ")
                    prn.alineadoIzquierdaForce(observation)
                }


                val orderData = js.optJSONObject("order" ) ?: JSONObject()
                val deliveryRecord = orderData.optJSONObject("delivery_record")

                if(deliveryRecord != null) {
                    prn.agregarTexto( "Direccion de Entrega")
                    prn.agregarTexto( deliveryRecord.optString("address_string", "N/A"))

                    val delivery = deliveryRecord.optJSONObject("delivery")
                    if (delivery != null) {
                        prn.agregarTexto("Nombre")
                        prn.agregarTexto(delivery.optString("name", "N/A"))
                        prn.agregarTexto("Telefono")
                        prn.agregarTexto(delivery.optString("phone" ,"N/A"))
                        prn.agregarTexto("Observacion General")
                        prn.agregarTexto(deliveryRecord.optString("observation" ,"N/A"))
                        prn.agregarTexto("Observacion de Entrega")
                        prn.agregarTexto(delivery.optString("observation" ,"N/A"))
                    } else {
                        prn.agregarTexto("N/A")
                    }
                }
                val line_breaks = printerConfig?.optInt("line_breaks") ?: 0
                for (j in 0 until line_breaks) {
                    prn.agregarSalto()
                }

                if(tipo == PrinterType.BLUETOOTH.type) {
                    prn.feed(6)
                }else {
                    if (lineSpacing == true ) {
                        println("Setting line spacing applied")
                        prn.feed(7)
                    }else {
                        println("Setting default line spacing")

                        prn.feed(14)
                    }
                }



                prn.cortar()

                println("Comandos")
                println(prn.getTrabajo().toString())
                enviarImprimir(prn.getTrabajo())


            }


        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    suspend fun imprimirCotizacion(js: JSONObject?, sj : JSONObject?, copias: Int, caracteres: Int) {

        println("Imprimiendo cotizacion")
        println(js.toString())
        try {
            if (js == null) return
            var detalles: JSONArray
            var jo: JSONObject
            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("receipt")
            println("Printer Config: $printerConfig")

            val prn = PrinterHelpers(caracteres, copias)
            val lineSpacing = printerConfig?.optBoolean("line_spacing")
            val logo = printerConfig?.optBoolean("logo")
            //Font selection based on printerConfig
            val fontType = printerConfig?.optString("font") ?: "A"
            println("Font Type: $fontType")


            for (i in 0 until copias) {
                println("Imprimiendo copia $i")
                // imprimir
                prn.iniciar()
                //prn.dobleAltoOn()

                when (fontType.uppercase()) {
                    "A" -> prn.setFontA() //Normal
                    "B" -> prn.setFontB() //Pequeña
                    "AA" -> prn.setFontAA() //Extra Grande
                    "BB" -> prn.setFontBB() //Grande

                }



                if (lineSpacing == true ) {
                    println("Setting line spacing applied")
                    prn.lineHeight2()
                }else {
                    println("Setting default line spacing")
                    prn.lineHeight()
                }


                prn.alineadoCentro()

                prn.agregarSalto()
                // Validate if the "image" key exists
                val subsidiary = js.optJSONObject("subsidiary")
                val image = subsidiary?.optJSONObject("image")
                val url = image?.optString("full_path", "")

                if (!url.isNullOrEmpty() && logo == true) {
                    prn.logo(url, this)
                } else {
                    println("Image URL is missing or invalid.")
                }
                if (sj != null) {
                    if (sj.optString("business_name") != "null") {
                        prn.escribirTextoSinSalto(sj.optString("business_name"))
                        prn.agregarSalto()
                    }
                    prn.escribirTextoSinSalto(
                        js.getJSONObject("subsidiary").optString("commercial_name")
                    )
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("RUC: " + sj.optString("ruc"))
                    prn.agregarSalto()

                    prn.escribirTextoSinSalto("Dirección: " + sj.optString("address"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto(
                        "Sucursal: " + js.getJSONObject("subsidiary").optString("address")
                    )
                    prn.agregarSalto()
                    if (sj.optString("phone") != "null") {
                        prn.escribirTextoSinSalto("Teléfono: " + sj.optString("phone"))
                    }
                    prn.agregarSalto()
                    if (sj.optString("email") != "null") prn.escribirTextoSinSalto(
                        "Correo: " + sj.optString(
                            "email"
                        )
                    )
                }
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Cotización ")
                prn.escribirTextoSinSalto(js.optString("number"))
                prn.agregarSalto()
                prn.alineadoIzquierda()
                prn.escribirTextoSinSalto("Fecha: ")
                prn.escribirTextoSinSalto(js.optString("date"))
                prn.agregarSalto()
                val person = js.optJSONObject("person")
                if (person != null) {
                    prn.escribirTextoSinSalto("Cliente: ")
                    prn.escribirTextoSinSalto(person.optString("name", "N/A"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Identificación: ")
                    prn.escribirTextoSinSalto(person.optString("identity", "N/A"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Teléfono: ")
                    prn.escribirTextoSinSalto(person.optJSONArray("phones")?.optString(0, "N/A") ?: "N/A")
                }
                val user = printerConfig?.optBoolean("user")


                if( user == true) {
                    val userData = js.optJSONObject("user")
                    if(userData != null) {

                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Vendedor: ")
                    prn.escribirTextoSinSalto(js.getJSONObject("user").optString("name"))
                    }
                }

                prn.agregarSalto()

                val order = js.optJSONObject("order")
                if (order != null) {
                    prn.escribirTextoSinSalto("Orden: ")

                    prn.escribirTexto(order.optString("sequential", "N/A"))
                }
                prn.LineasGuion()
                prn.escribirTextoSinSalto("Cant Descripción")
                prn.agregarCaracteres((caracteres - 26).coerceAtLeast(0), "")
                prn.escribirTextoSinSalto("P.U. Total")
                prn.agregarSalto()
                prn.LineasIgual()
                detalles = js.getJSONArray("details")
                for (j in 0 until detalles.length()) {
                    jo = detalles.getJSONObject(j)

                    val product = jo.getJSONObject("product")
                    val basePrice = product.optDouble("price")

                    val taxesArray = product.optJSONArray("taxes")
                    val taxRate = if (taxesArray != null && taxesArray.length() > 0) {
                        taxesArray.getJSONObject(0).optDouble("rate", 0.0)
                    } else {
                        0.0
                    }
                    val taxedPrice = basePrice * (1 + taxRate / 100)

                    prn.agregarTexto(
                        prn.lineaDetails(

                            jo.optString("amount"),
                            jo.getJSONObject("product").optString("name"),
                            taxedPrice.toString(),
                            jo.optString("total_amount"),
                            caracteres
                        )
                    )
                }
                prn.LineasIgual()
                //Summary
                prn.alineadoDerecha()
                if (js.has("discount")) {
                    prn.escribirTextoSinSalto("Descuentos:")
                    prn.agregarCaracteresDerecha(10, df.format(js.getDouble("discount")))
                    prn.agregarSalto()
                }
                prn.escribirTextoSinSalto("Subtotal:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subtotal")))
                prn.agregarSalto()
                // Print IVA sorted by rate
                var taxesData = js.optJSONArray("taxes")
                if(taxesData != null ){

                detalles = js.getJSONArray("taxes")
                val detallesList = mutableListOf<JSONObject>()
                for (j in 0 until detalles.length()) {
                    detallesList.add(detalles.getJSONObject(j))
                }
                detallesList.sortBy { it.optDouble("rate", 0.0) }
                // Print Subtotals sorted by rate
                val taxes = printerConfig?.optBoolean("taxes")
                if(taxes == true){
                    for (dl in detallesList) {
                        prn.escribirTextoSinSalto("Subtotal " + dl.optString("rate").toDoubleOrNull()?.toInt()  + "%:")
                        prn.agregarCaracteresDerecha(10, df.format(dl.getDouble("base")))
                        prn.agregarSalto()
                    }
                    for (dl in detallesList) {
                        prn.escribirTextoSinSalto("IVA " + dl.optString("rate").toDoubleOrNull()?.toInt() + "%:")
                        prn.agregarCaracteresDerecha(10, df.format(dl.getDouble("amount")))
                        prn.agregarSalto()
                    }
                }
                }




                //Agregar total
                prn.escribirTextoSinSalto("Total:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")))




                prn.agregarSalto()
                val observation = js.optString("observation", null)
                if (!observation.isNullOrEmpty()) {
                    prn.alineadoIzquierdaForce("Observacion: ")
                    prn.alineadoIzquierdaForce(observation)
                }

                val paymentTerms = js.optString("payment_terms", null)
                if (!paymentTerms.isNullOrEmpty()) {
                    prn.alineadoIzquierdaForce("Condiciones de Pago: ")
                    prn.alineadoIzquierdaForce(paymentTerms)
                }
                val line_breaks = printerConfig?.optInt("line_breaks") ?: 0
                for (j in 0 until line_breaks) {
                    prn.agregarSalto()
                }

                if(tipo == PrinterType.BLUETOOTH.type) {
                    prn.feed(6)
                }else {
                    if (lineSpacing == true ) {
                        println("Setting line spacing applied")
                        prn.feed(7)
                    }else {
                        println("Setting default line spacing")

                        prn.feed(14)
                    }
                }



                prn.cortar()

                println("Comandos")
                println(prn.getTrabajo().toString())
                enviarImprimir(prn.getTrabajo())


            }


        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    suspend fun imprimirPreticket(js: JSONObject?,sj : JSONObject?, copias: Int, caracteres: Int) {
        println("Imprimiendo recibo")
        println(js.toString())

        try {
            if (js == null) return


            var detalles: JSONArray

            var jo: JSONObject

            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("preticket")
            println("Printer Config: $printerConfig")

            val prn = PrinterHelpers(caracteres, copias)
            val lineSpacing = printerConfig?.optBoolean("line_spacing")
            val logo = printerConfig?.optBoolean("logo")
            //Font selection based on printerConfig
            val fontType = printerConfig?.optString("font") ?: "A"
            println("Font Type: $fontType")



            for (i in 0 until copias) {
                println("Imprimiendo copia $i")
                // imprimir
                prn.iniciar()
                when (fontType.uppercase()) {
                    "A" -> prn.setFontA() //Normal
                    "B" -> prn.setFontB() //Pequeña
                    "AA" -> prn.setFontAA() //Extra Grande
                    "BB" -> prn.setFontBB() //Grande

                }



                if (lineSpacing == true ) {
                    println("Setting line spacing applied")
                    prn.lineHeight2()
                }else {
                    println("Setting default line spacing")
                    prn.lineHeight()
                }


                prn.alineadoCentro()

                prn.agregarSalto()
                // Validate if the "image" key exists
                val subsidiary = if (js.has("subsidiary")) js.optJSONObject("subsidiary") else null

                val image = subsidiary?.optJSONObject("image")
                val url = image?.optString("full_path", "")

                if (!url.isNullOrEmpty() && logo == true) {
                    prn.logo(url, this)
                } else {
                    println("Image URL is missing or invalid.")
                }
                prn.escribirTextoSinSalto("PRETICKET")
                prn.agregarSalto()
                if (sj != null && subsidiary != null ) {
                    if (sj.optString("business_name") != "null") {
                        prn.escribirTextoSinSalto(sj.optString("business_name"))
                        prn.agregarSalto()
                    }
                    prn.escribirTextoSinSalto(
                        js.getJSONObject("subsidiary").optString("commercial_name")
                    )
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("RUC: " + sj.optString("ruc"))
                    prn.agregarSalto()

                    prn.escribirTextoSinSalto("Dirección: " + sj.optString("address"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto(
                        "Sucursal: " + js.getJSONObject("subsidiary").optString("address")
                    )
                    prn.agregarSalto()
                    if (sj.optString("phone") != "null") {
                        prn.escribirTextoSinSalto("Teléfono: " + sj.optString("phone"))
                    }
                    prn.agregarSalto()
                    if (sj.optString("email") != "null") prn.escribirTexto(
                        "Correo: " + sj.optString(
                            "email"
                        )
                    )
                }

                prn.agregarSalto()
                prn.LineasIgualTexto("Orden " +js.optString("sequential", "N/A"))
                prn.agregarSalto()
                prn.alineadoIzquierda()
                if( js.has("table")){
                    val table = js.optJSONObject("table")

                    val area = table?.optJSONObject("area")
                    prn.escribirTextoSinSalto("Zona: ")
                    prn.escribirTextoSinSalto( area?.optString("name", "N/A") ?: "N/A")
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Mesa: ")
                    prn.escribirTextoSinSalto( table?.optString("name", "N/A") ?: "N/A")
                }



                prn.agregarSalto()
                prn.escribirTextoSinSalto("Fecha : ")
                prn.escribirTextoSinSalto(js.optString("date", "N/A"))

                val user = printerConfig?.optBoolean("user")
                if(user==true){

                prn.agregarSalto()
                prn.escribirTextoSinSalto("Atendido por: ")
                prn.escribirTextoSinSalto(js.optString("waiter", "N/A"))
                    prn.agregarSalto()
                }

                prn.agregarSalto()
                prn.LineasIgualTexto("Datos para Facturación")
                prn.agregarSalto()
                prn.LineaGuionTexto("Nombre: ")
                prn.agregarSalto()
                prn.LineaGuionTexto(" ")
                prn.agregarSalto()
                prn.LineaGuionTexto("Identificación: ")
                prn.agregarSalto()
                prn.LineaGuionTexto("Correo: ")
                prn.agregarSalto()
                prn.LineaGuionTexto("Teléfono: ")
                prn.agregarSalto()
                prn.LineaGuionTexto("Dirección: ")
                val tip = printerConfig?.optBoolean("tip")
                if(tip==true){

                prn.agregarSalto()
                prn.LineaGuionTexto("Propina: ")
                }
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Cant Descripción")
                prn.agregarCaracteres((caracteres - 26).coerceAtLeast(0), "")
                prn.escribirTextoSinSalto("P.U. Total")
                prn.agregarSalto()
                prn.LineasIgual()
                detalles = js.getJSONArray("details")
                for (j in 0 until detalles.length()) {
                    jo = detalles.getJSONObject(j)

                    val product = jo.getJSONObject("product")
                    val basePrice = product.optDouble("price")

                    val taxesArray = product.optJSONArray("taxes")
                    val taxRate = if (taxesArray != null && taxesArray.length() > 0) {
                        taxesArray.getJSONObject(0).optDouble("rate", 0.0)
                    } else {
                        0.0
                    }
                    val taxedPrice = basePrice * (1 + taxRate / 100)

                    prn.agregarTexto(
                        prn.lineaDetails(

                            jo.optString("amount"),
                            jo.getJSONObject("product").optString("name"),
                            taxedPrice.toString(),
                            jo.optString("total_amount"),
                            caracteres
                        )
                    )
                }
                prn.LineasIgual()

                //Summary
                prn.alineadoDerecha()

                /*
                if (js.has("discount")) {
                    prn.escribirTextoSinSalto("Descuentos:")
                    prn.agregarCaracteresDerecha(10, df.format(js.getDouble("discount")))
                    prn.agregarSalto()
                }
                prn.escribirTextoSinSalto("Subtotal:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subtotal")))
                prn.agregarSalto()
                detalles = js.getJSONArray("taxes")
                val detallesList = mutableListOf<JSONObject>()
                for (j in 0 until detalles.length()) {
                    detallesList.add(detalles.getJSONObject(j))
                }
                detallesList.sortBy { it.optDouble("rate", 0.0) }
                // Print Subtotals sorted by rate

                    for (dl in detallesList) {
                        prn.escribirTextoSinSalto("Subtotal " + dl.optString("rate").toDoubleOrNull()?.toInt()  + "%:")
                        prn.agregarCaracteresDerecha(10, df.format(dl.getDouble("base")))
                        prn.agregarSalto()
                    }
                    for (dl in detallesList) {
                        prn.escribirTextoSinSalto("IVA " + dl.optString("rate").toDoubleOrNull()?.toInt() + "%:")
                        prn.agregarCaracteresDerecha(10, df.format(dl.getDouble("amount")))
                        prn.agregarSalto()
                    }
                */


                //agregar total

                prn.escribirTextoSinSalto("Total A Pagar: ")

                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")))
                prn.agregarSalto()
                prn.agregarSalto()
                prn.alineadoIzquierda()
                prn.alineadoIzquierdaForce("ESTE DOCUMENTO ES UN PRETICKET")
                prn.alineadoIzquierdaForce("PARA REVISION DEL CLIENTE")
                prn.alineadoIzquierdaForce("NO CONSTITUYE FACTURA")
                prn.alineadoIzquierdaForce("NO TIENE VALIDEZ TRIBUTARIA")

                val line_breaks = printerConfig?.optInt("line_breaks") ?: 0
                for (j in 0 until line_breaks) {
                    prn.agregarSalto()
                }

                if(tipo == PrinterType.BLUETOOTH.type) {
                    prn.feed(6)
                }else {
                    if (lineSpacing == true ) {
                        println("Setting line spacing applied")
                        prn.feed(7)
                    }else {
                        println("Setting default line spacing")

                        prn.feed(14)
                    }
                }



                prn.cortar()

                println("Comandos")
                println(prn.getTrabajo().toString())
                enviarImprimir(prn.getTrabajo())
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


    suspend fun imprimirComandas(js: JSONObject?, sj : JSONObject?, copias: Int, caracteres: Int, printCode: String) {
        try {
            if (js == null) return

            // reutilizables
            val detalles: JSONArray
            var items: Int
            var jo: JSONObject
            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("order")
            println("Printer Config: $printerConfig")
            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.iniciar()
            val lineSpacing = printerConfig?.optBoolean("line_spacing")


            if (lineSpacing == true ) {
                println("Setting line spacing applied")
                prn.lineHeight2()
            }else {
                println("Setting default line spacing")
                prn.lineHeight()
            }
            prn.alineadoCentro()
            val orderData = if (js != null && js.has("order")) {
                js.getJSONObject("order")
            } else {
                js ?: JSONObject()
            }
            println("Order Data: $orderData")


            //Font selection based on printerConfig
            val fontType = printerConfig?.optString("font") ?: "A"
            println("Font Type: $fontType")
            when (fontType.uppercase()) {
                "A" -> prn.setFontA() //Normal
                "B" -> prn.setFontB() //Pequeña
                "AA" -> prn.setFontAA() //Extra Grande
                "BB" -> prn.setFontBB() //Grande

            }

            val order_prints = orderData.getJSONArray("order_prints")
            println("Order Prints: $order_prints")
            val commandSequential = order_prints.getJSONObject(0).optString("sequential") ?: "N/A"
            val printDetails = order_prints.getJSONObject(0).getJSONArray("order_print_details")

            println("Print Details: $printDetails")
            prn.agregarTexto("Orden " + (orderData.optString("sequential") ?: "N/A"))
            prn.agregarTexto("$printCode-$commandSequential")
            prn.alineadoIzquierda()

            prn.agregarSalto()
            prn.agregarTexto("Mesero: " + (orderData.optString("waiter") ?: "N/A"))

            prn.agregarTexto("Comensales: " + (orderData.optString("pax") ?: "N/A"))

            prn.LineasIgual()


            detalles = printDetails
            for (j in 0 until detalles.length()) {
                val jo = detalles.getJSONObject(j)
                val printersArray = jo.optJSONArray("printers")
                var shouldPrint = false
                if (printersArray != null) {
                    for (k in 0 until printersArray.length()) {
                        val printerObj = printersArray.getJSONObject(k)
                        if (printerObj.optString("code") == printCode) {
                            shouldPrint = true
                            break
                        }
                    }
                }
                if (shouldPrint) {
                    prn.escribirTextoSinSalto(
                        jo.optString("type") +  jo.optInt("amount") + " " +jo.optString("product_name")
                    )
                    prn.agregarSalto()
                }
            }
            prn.LineasIgual()

            prn.negritaOn()
            prn.agregarTexto(orderData.optString("type") ?: "N/A")


            prn.negritaOff()

            val line_breaks = printerConfig?.optInt("line_breaks") ?: 0
            for (i in 0 until line_breaks) {
                prn.agregarSalto()
            }
            prn.beep1()
            prn.beep2()
            if(tipo == PrinterType.BLUETOOTH.type) {
                prn.feed(6)
            }else {
                if (lineSpacing == true ) {
                    println("Setting line spacing applied")
                    prn.feed(7)
                }else {
                    println("Setting default line spacing")

                    prn.feed(14)
                }
            }

            prn.cortar()
            enviarImprimir(prn.getTrabajo())

            closeAll()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }



    fun imprimirCierreCaja(js: JSONObject?,sj : JSONObject?, copias: Int, caracteres: Int) {
        try {
            if (js == null) return

            // reutilizables
            var detalles: JSONArray
            var jo: JSONObject
            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("receipt")
            println("Printer Config [TODO: Change to valid] : $printerConfig")
            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.iniciar()
            prn.setFontA()

            prn.agregarSalto()
            prn.alineadoCentro()
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Caja #" + js.optString("sequential"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Abierta " + js.optString("opened_at"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cerrada " + js.optString("closed_at","N/A" ))
            prn.agregarSalto()
            val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())


            prn.escribirTextoSinSalto("Impreso " + currentDateTime)
            prn.agregarSalto()

            prn.LineasGuion()
            prn.alineadoIzquierda()

            prn.negritaOn()
            prn.escribirTexto("Valores de Caja")
            prn.negritaOff()



            prn.escribirTextoSinSalto("Apertura")

            prn.agregarCaracteresDerecha(caracteres-8,"$"+df.format(js.optDouble("opening")))
            prn.agregarSalto()

            val cashMovements = js.optJSONArray("cash_movements")

            var ins = 0.00
            var outs = 0.00

            if (cashMovements != null) {
                for (i in 0 until cashMovements.length()) {
                    val movement = cashMovements.optJSONObject(i)
                    if (movement != null) {
                        val type = movement.optString("type")
                        val total = movement.optString("total").toDoubleOrNull() ?: 0.00
                        if (type == "in") {
                            ins += total
                        } else if (type == "out") {
                            outs += total
                        }
                    }
                }
            }

            println("Ingresos: $ins")
            println("Outs: $outs")

            prn.escribirTextoSinSalto("+ Ingresos")
            prn.agregarCaracteresDerecha(caracteres-10,"$"+ df.format(ins))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("- Egresos")
            prn.agregarCaracteresDerecha(caracteres-9,"$"+ df.format(outs))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Efectivo Ventas")
            prn.agregarCaracteresDerecha(caracteres-15,"$"+df.format(js.optDouble("sales_cash")))

            prn.negritaOn()
            prn.LineasGuionSinTexto()
            prn.negritaOff()

            prn.escribirTextoSinSalto("Total Caja")
            prn.agregarCaracteresDerecha(caracteres-10,"$"+df.format(js.optDouble("total_cash")))
            prn.LineasGuion()
            prn.negritaOn()
            prn.escribirTexto("Cuadre de Caja")
            prn.negritaOff()
            prn.LineasGuion()

            prn.escribirTextoSinSalto("Total en Efectivo")
            prn.agregarCaracteresDerecha(caracteres-17,"$"+df.format(js.optDouble("total_cash")))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Efectivo Entregado")
            prn.agregarCaracteresDerecha(caracteres-18,"$"+df.format(js.optDouble("received_cash")))
            prn.agregarSalto()
            prn.negritaOn()
            prn.LineasGuionSinTexto()
            prn.negritaOff()
            val totalCash = js.optDouble("total_cash", 0.0)
            val receivedCash = js.optDouble("received_cash", 0.0)
            val difference = receivedCash - totalCash

            val status = if (difference < 0) "F" else if (difference > 0) "S" else null

            prn.escribirTextoSinSalto("Diferencia")
            if(status != null) {
                prn.agregarCaracteresDerecha(caracteres-10,"$"+df.format(js.optDouble("difference"))+" "+status)
            } else {
                prn.agregarCaracteresDerecha(caracteres-10,"$"+df.format(js.optDouble("difference")))
            }
            prn.agregarSalto()
            prn.LineasGuion()
            prn.negritaOn()
            prn.escribirTexto("Resumen de Pagos")
            prn.negritaOff()
            prn.LineasGuion()


            val summary = js.optJSONObject("summary")
            val paymentMethods = summary?.optJSONArray("payment_methods")

            if (paymentMethods != null && paymentMethods.length() > 0) {
                for (i in 0 until paymentMethods.length()) {
                    val paymentMethod = paymentMethods.getJSONObject(i)
                    val paymentMethodName = paymentMethod.optString("name", "N/A")
                    val remainingSpace = caracteres - paymentMethodName.length
                    prn.escribirTextoSinSalto(paymentMethod.optString("name", "N/A"))
                    prn.agregarCaracteresDerecha(remainingSpace,"$"+df.format(paymentMethod.optDouble("amount", 0.0)))
                    prn.agregarSalto()
                }
            } else {
                prn.escribirTextoSinSalto("No hay métodos de pago")
                prn.agregarSalto()
            }
            prn.negritaOn()
            prn.LineasGuionSinTexto()
            prn.negritaOff()
            prn.escribirTextoSinSalto("Total Ventas")
            prn.agregarCaracteresDerecha(caracteres-12,"$"+df.format(js.optDouble("sales_total")))

            prn.agregarSalto()

            prn.LineasGuion()
            prn.negritaOn()
            prn.escribirTexto("Resumen de Productos")
            prn.negritaOff()
            prn.LineasGuion()
            val salesByProduct = summary?.optJSONArray("sales_by_product")

            if (salesByProduct != null && salesByProduct.length() > 0) {
                for (i in 0 until salesByProduct.length()) {
                    val saleProduct = salesByProduct.getJSONObject(i)
                    val productName = saleProduct.optString("product_name", "N/A")
                    val quantity = saleProduct.optInt("total_quantity").toString()
                    val price = "$" + df.format(saleProduct.optDouble("total_sold_with_taxes", 0.0))

                    val productNameWidth = (caracteres * 0.6).toInt()
                    val quantityWidth = (caracteres * 0.2).toInt()
                    val priceWidth = (caracteres * 0.2).toInt()


                    val truncatedProductName = if (productName.length > productNameWidth) {
                        productName.take(productNameWidth - 3) + "..."
                    } else {
                        productName.padEnd(productNameWidth)
                    }


                    val paddedQuantity = quantity.padStart(quantityWidth)
                    val paddedPrice = price.padStart(priceWidth)


                    prn.escribirTextoSinSalto(truncatedProductName)
                    prn.escribirTextoSinSalto(paddedQuantity)
                    prn.escribirTextoSinSalto(paddedPrice)
                    prn.agregarSalto()
                }
            } else {
                prn.escribirTextoSinSalto("No hay productos vendidos")
                prn.agregarSalto()
            }
            prn.LineasGuion()
            prn.agregarSalto()
            prn.alineadoCentro()
            prn.escribirTexto("¡Gracias!")

            prn.feedFinal()
            prn.alineadoIzquierda()
            prn.cortar()
            enviarImprimir(prn.getTrabajo())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun abrirGaveta() {
        val prn = PrinterHelpers(50, 1)
        prn.abrirGaveta1()
        prn.abrirGaveta2()
        enviarImprimir(prn.getTrabajo())
    }


    fun enviarImprimir(trabajo: String) {
        println("Enviando trabajo de impresión...")
        try {
            when (tipo) {
                PrinterType.WIFI.type -> {
                    TcpIpOutputStream(this.address, this.port!!).use { outputStream ->
                        val style = Style()
                        val escposCoffee = EscposCoffee(style, outputStream)
                        escposCoffee.printMessage(trabajo)

                    }
                }
                PrinterType.BLUETOOTH.type -> {
                    val style = Style()
                    val escposCoffee = EscposCoffee(style, this.streamBluetooth!!)
                    escposCoffee.printMessage(trabajo)
                    //streamBluetooth!!.write(trabajo.toByteArray(charset("ISO-8859-1")))
                    Thread.sleep(10)

                }
                else -> {
                    val style = Style()
                    val escposCoffee = EscposCoffee(style, this.usbOutputStream!!)
                    escposCoffee.printMessage(trabajo)

                }
            }
        } catch (e: Exception) {
            println("")
        }
    }


    suspend fun printMediaJob(mediaBuilder: MediaBuilder) {
            try {
                when (tipo) {
                    PrinterType.WIFI.type -> {
                        TcpIpOutputStream(this.address, this.port!!).use { outputStream ->
                            val style = Style()
                            val escposCoffee = EscposCoffee(style, outputStream)
                            escposCoffee.printMedia(mediaBuilder)
                        }
                    }
                    PrinterType.BLUETOOTH.type -> {
                        val style = Style()
                        val escposCoffee = EscposCoffee(style, this.streamBluetooth!!)
                        escposCoffee.printMedia(mediaBuilder)
                        //streamBluetooth!!.write(trabajo.toByteArray(charset("ISO-8859-1")))
                        //Thread.sleep(10)
                    }
                    else -> {
                        val style = Style()
                        val escposCoffee = EscposCoffee(style, this.usbOutputStream!!)
                        escposCoffee.printMedia(mediaBuilder)
                    }
                }
            } catch (e: Exception) {
                println("")
            }

    }

    fun cerrarConexionBluetooth() {
        try {
            Thread.sleep(1500)
            if (streamBluetooth != null) {
                streamBluetooth!!.flush()
                streamBluetooth!!.close()
            }
            streamBluetooth = null
            socketBluetooth?.close()
            socketBluetooth = null
        } catch (e: Exception) {
            println(e)
        }
    }

    fun cerrarConexionRed() {
        try {
            streamRed?.close()
            streamRed = null
            socketRed?.close()
            socketRed = null
        } catch (e: IOException) {
            println(e)
        }
    }

    fun cerrarConexion() {
        if (tipo == PrinterType.WIFI.type) {
            cerrarConexionRed()
        } else {
            cerrarConexionBluetooth()
        }
    }
}