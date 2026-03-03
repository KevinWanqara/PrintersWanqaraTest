package com.example.printerswanqara.core.print.utils.printer1

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

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
import java.net.Socket
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID
import androidx.core.content.edit
import com.example.printerswanqara.data.AppStorage

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.compareTo
import kotlin.text.format


class PrinterBuilder(private val tipo: String?,    private val context: Context,) {
    companion object {
        @Volatile
        var diagnosticsListener: ((event: PrinterDiagnosticsEvent) -> Unit)? = null
    }

    data class PrinterDiagnosticsEvent(
        val transportType: String,
        val address: String?,
        val port: Int?,
        val bytesLength: Int,
        val startTimestamp: Long,
        val endTimestamp: Long,
        val success: Boolean,
        val errorMessage: String? = null
    )

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
            val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                return false
            }

            val device = bluetoothAdapter.getRemoteDevice(address)
            val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP UUID
            
            socketBluetooth = device.createRfcommSocketToServiceRecord(uuid)
            socketBluetooth?.connect()
            streamBluetooth = socketBluetooth?.outputStream
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                socketBluetooth?.close()
            } catch (closeException: Exception) {
                closeException.printStackTrace()
            }
            socketBluetooth = null
            streamBluetooth = null
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
                                    if (usbConnection != null) {
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
        caracteres: Int
    ) {
        println("Imprimiendo factura electronica")
        println(js.toString())
        try {
            if (js == null) return


            var detalles: JSONArray
            var paymentMethods : JSONArray

            var jo: JSONObject

            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("invoice")
            println("Printer Config: $printerConfig")

            val configCopies: Int = printerConfig?.let { cfg ->
                when (val v = cfg.opt("copies")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 1
                    else -> 1
                }
            } ?: 1

            println("Copias configuradas $configCopies")

            for (i in 0 until configCopies) {
                println("Imprimiendo copia $i")

                // imprimir
                val prn = PrinterHelpers(caracteres, configCopies)
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

                // Validate if the "image" key exists
                val subsidiary = js.optJSONObject("subsidiary")
                val image = subsidiary?.optJSONObject("image")
                val url = image?.optString("full_path", "")

                if (!url.isNullOrEmpty() && logo == true) {
                    prn.logo(url, this, fontType)
                } else {
                    println("Image URL is missing or invalid.")
                }


                when (fontType.uppercase()) {
                    "A" -> prn.setFontA() //Normal
                    "B" -> prn.setFontB() //Pequeña
                    "AA" -> prn.setFontAA() //Extra Grande
                    "BB" -> prn.setFontBB() //Grande

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


                }else{
                    println("NO SJ VALUE")
                }
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Factura Electrónica Nº:")
                prn.escribirTextoSinSalto(js.optString("number"))
                prn.agregarSalto()
                if (sj != null) {
                    val env = when (sj.optString("environment")) {

                        "1" -> "Pruebas"
                        else -> "Produccion"
                    }
                    prn.escribirTextoSinSalto("Ambiente: " + env)
                    prn.escribirTextoSinSalto(" Emision: " + "Normal")
                }
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Clave de Acceso/Nº de Autorización:")
                prn.escribirTextoSinSalto(js.optString("access_key"))



                prn.agregarSalto()

                prn.alineadoIzquierda()
                prn.escribirTextoSinSalto("Fecha: ")
                prn.escribirTextoSinSalto(js.optString("date","N/A"))
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
                prn.agregarSalto()
                val order = js.optJSONObject("order")
                if (order != null) {
                    prn.escribirTextoSinSalto("Orden: ")

                    prn.escribirTexto(order.optString("sequential", "N/A"))

                    val turn = order.optString("turn", "")
                    if (turn.isNotEmpty()) {
                        prn.escribirTextoSinSalto("Turno: ")

                        prn.escribirTexto(order.optString("turn", "N/A"))

                    }
                    prn.agregarSalto()
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

                //Imprimir Propina
                val additional_tip = js.optString("additional_tip", "")
                if (!additional_tip.isNullOrEmpty()) {
                    prn.escribirTextoSinSalto("Propina:")
                    prn.agregarCaracteresDerecha(10, df.format(js.getDouble("additional_tip")))
                    prn.agregarSalto()


                }
                //agregar total
                prn.escribirTextoSinSalto("Total:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")+js.getDouble("additional_tip")))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Entrega:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")+js.getDouble("additional_tip")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Cambio:")

                prn.agregarCaracteresDerecha(
                    10,
                    df.format(js.optDouble("change_amount" ,0.0 )).replace("-", "")
                )
                prn.agregarSalto()


                paymentMethods = js.getJSONArray("payment_methods")

                prn.LineasGuion()
                prn.alineadoIzquierda()
                prn.escribirTextoSinSalto("Formas de Pago")
                prn.agregarSalto()
                prn.LineasIgual()

                for (j in 0 until paymentMethods.length()) {
                    jo = paymentMethods.getJSONObject(j)

                    val name = jo.optString("name")
                    val amount = df.format(jo.optDouble("amount", 0.0))



                    prn.escribirTextoSinSalto(name )
                    val nameLength = name.length
                    val amountLength = amount.length

                    prn.agregarCaracteres((caracteres - nameLength-amountLength).coerceAtLeast(0), "")
                    prn.escribirTextoSinSalto(amount)

                    prn.agregarSalto()

                }


                prn.LineasGuion()




                val observation = js.optString("observation", "")
                if (!observation.isNullOrEmpty()) {
                    prn.alineadoIzquierdaForce(observation)
                }
                if (printerConfig != null) {
                    if (printerConfig.optBoolean("observation", false) && subsidiary != null) {
                        val subsidiaryObservation = subsidiary.optString("observation", "")
                        if (!subsidiaryObservation.isNullOrEmpty()) {
                            prn.alineadoIzquierdaForce(subsidiaryObservation)
                        }
                    }
                }
                val orderData = js.optJSONObject("order" ) ?: JSONObject()
                val deliveryRecord = orderData.optJSONObject("delivery_record")

                if(deliveryRecord != null) {
                    prn.LineasIgual()
                    val delivery = deliveryRecord.optJSONObject("delivery")
                    val address = deliveryRecord.optJSONObject("address")
                    prn.alineadoIzquierda()


                    prn.agregarTexto( "Dirección: ")
                    if (address != null) {
                        prn.agregarTexto( deliveryRecord.optString("address_string", "N/A")+"/" + address.optString("observation", "N/A"))
                    }


                    if (delivery != null) {
                        prn.agregarTexto("Nombre: " +  delivery.optString("name", "N/A"))
                        prn.agregarTexto("Teléfono: " + delivery.optString("phone" ,"N/A"))
                    } else {
                        prn.agregarTexto("N/A")
                    }




                }
                if(orderData.optString("type") == "Retiro"){
                    prn.LineasIgual()
                    prn.alineadoIzquierda()
                    prn.agregarTexto("Retiro en local")
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
                println(prn.getTrabajo())
                enviarImprimir(prn.getTrabajo())

            }
            closeAll()

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    suspend fun imprimirRecibo(js: JSONObject?, sj : JSONObject?, caracteres: Int) {

        println("Imprimiendo recibo")
        println(js.toString())
        try {
            if (js == null) return
            var detalles: JSONArray
            var paymentMethods : JSONArray
            var jo: JSONObject
            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("receipt")
            println("Printer Config: $printerConfig")

            val configCopies: Int = printerConfig?.let { cfg ->
                when (val v = cfg.opt("copies")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 1
                    else -> 1
                }
            } ?: 1






            for (i in 0 until configCopies) {
                println("Imprimiendo copia $i")
                val prn = PrinterHelpers(caracteres, configCopies)
                // imprimir
                prn.iniciar()
                //prn.dobleAltoOn()


                val lineSpacing = printerConfig?.optBoolean("line_spacing")
                val logo = printerConfig?.optBoolean("logo")
                //Font selection based on printerConfig

                if (lineSpacing == true ) {
                    println("Setting line spacing applied")
                    prn.lineHeight2()
                }else {
                    println("Setting default line spacing")
                    prn.lineHeight()
                }


                prn.alineadoCentro()


                val fontType = printerConfig?.optString("font") ?: "A"
                println("Font Type: $fontType")

                // Validate if the "image" key exists
                val subsidiary = js.optJSONObject("subsidiary")
                val image = subsidiary?.optJSONObject("image")
                val url = image?.optString("full_path", "")

                if (!url.isNullOrEmpty() && logo == true) {
                    prn.logo(url, this, fontType)
                } else {
                    println("Image URL is missing or invalid.")
                }

                when (fontType.uppercase()) {
                    "A" -> prn.setFontA() //Normal
                    "B" -> prn.setFontB() //Pequeña
                    "AA" -> prn.setFontAA() //Extra Grande
                    "BB" -> prn.setFontBB() //Grande

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
                prn.escribirTextoSinSalto(js.optString("date","N/A"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Cliente: ")
                prn.escribirTextoSinSalto(js.getJSONObject("customer").optString("name"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Identificación: ")
                prn.escribirTextoSinSalto(js.getJSONObject("customer").optString("identity"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Teléfono: ")
                val customer = js.optJSONObject("customer")
                val phones = customer?.optJSONArray("phones")
                if (phones != null && phones.length() > 0) {
                    prn.escribirTextoSinSalto(phones.optString(0, ""))
                } else {
                    prn.escribirTextoSinSalto("Sin Teléfono")
                }
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

                    val turn = order.optString("turn", "")
                    if (turn.isNotEmpty()) {
                        prn.escribirTextoSinSalto("Turno: ")

                        prn.escribirTexto(order.optString("turn", "N/A"))

                    }
                    prn.agregarSalto()
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
                //Imprimir Propina
                val additional_tip = js.optString("additional_tip", "")
                if (!additional_tip.isNullOrEmpty()) {
                    prn.escribirTextoSinSalto("Propina:")
                    prn.agregarCaracteresDerecha(10, df.format(js.getDouble("additional_tip")))
                    prn.agregarSalto()

                }


                //agregar total
                prn.escribirTextoSinSalto("Total:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")+js.getDouble("additional_tip")))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Entrega:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")+js.getDouble("additional_tip")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Cambio:")

                prn.agregarCaracteresDerecha(
                    10,
                    df.format(js.optDouble("change_amount" ,0.0)).replace("-", "")
                )
                prn.agregarSalto()
                paymentMethods = js.getJSONArray("payment_methods")

                prn.LineasGuion()
                prn.alineadoIzquierda()
                prn.escribirTextoSinSalto("Formas de Pago")
                prn.agregarSalto()
                prn.LineasIgual()

                for (j in 0 until paymentMethods.length()) {
                    jo = paymentMethods.getJSONObject(j)

                    val name = jo.optString("name")
                    val amount = df.format(jo.optDouble("amount", 0.0))



                    prn.escribirTextoSinSalto(name )
                    val nameLength = name.length
                    val amountLength = amount.length

                    prn.agregarCaracteres((caracteres - nameLength-amountLength).coerceAtLeast(0), "")
                    prn.escribirTextoSinSalto(amount)

                    prn.agregarSalto()

                }


                prn.LineasGuion()
                val observation = js.optString("observation", "")
                if (!observation.isNullOrEmpty()) {
                    prn.alineadoIzquierdaForce(observation)
                }
                if (printerConfig != null) {
                    if (printerConfig.optBoolean("observation", false) && subsidiary != null) {
                        val subsidiaryObservation = subsidiary.optString("observation", "")
                        if (!subsidiaryObservation.isNullOrEmpty()) {
                            prn.alineadoIzquierdaForce(subsidiaryObservation)
                        }
                    }
                }

                val orderData = js.optJSONObject("order" ) ?: JSONObject()
                val deliveryRecord = orderData.optJSONObject("delivery_record")


                if(deliveryRecord != null) {


                    prn.LineasIgual()
                    val delivery = deliveryRecord.optJSONObject("delivery")
                    val address = deliveryRecord.optJSONObject("address")



                    prn.alineadoIzquierda()


                    prn.agregarTexto( "Dirección: ")
                    if (address != null) {
                        prn.agregarTexto( deliveryRecord.optString("address_string", "N/A")+"/" + address.optString("observation", "N/A"))
                    }


                    if (delivery != null) {
                        prn.agregarTexto("Nombre: " +  delivery.optString("name", "N/A"))
                        prn.agregarTexto("Teléfono: " + delivery.optString("phone" ,"N/A"))
                    } else {
                        prn.agregarTexto("N/A")
                    }


                }

                if(orderData.optString("type") == "Retiro"){
                    prn.LineasIgual()
                    prn.alineadoIzquierda()
                    prn.agregarTexto("Retiro en local")
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
                println(prn.getTrabajo())
                enviarImprimir(prn.getTrabajo())


            }


        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    suspend fun imprimirCotizacion(js: JSONObject?, sj : JSONObject?, caracteres: Int) {

        println("Imprimiendo cotizacion")
        println(js.toString())
        try {
            if (js == null) return
            var detalles: JSONArray
            var paymentMethods : JSONArray
            var jo: JSONObject
            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("receipt")
            println("Printer Config: $printerConfig")

            val configCopies: Int = printerConfig?.let { cfg ->
                when (val v = cfg.opt("copies")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 1
                    else -> 1
                }
            } ?: 1

            val lineSpacing = printerConfig?.optBoolean("line_spacing")
            val logo = printerConfig?.optBoolean("logo")
            //Font selection based on printerConfig
            val fontType = printerConfig?.optString("font") ?: "A"
            println("Font Type: $fontType")


            for (i in 0 until configCopies) {
                println("Imprimiendo copia $i")
                val prn = PrinterHelpers(caracteres, configCopies)


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
                //Font selection based on printerConfig
                val fontType = printerConfig?.optString("font") ?: "A"
                println("Font Type: $fontType")

                prn.agregarSalto()
                // Validate if the "image" key exists
                val subsidiary = js.optJSONObject("subsidiary")
                val image = subsidiary?.optJSONObject("image")
                val url = image?.optString("full_path", "")

                if (!url.isNullOrEmpty() && logo == true) {
                    prn.logo(url, this, fontType)
                } else {
                    println("Image URL is missing or invalid.")
                }
                when (fontType.uppercase()) {
                    "A" -> prn.setFontA() //Normal
                    "B" -> prn.setFontB() //Pequeña
                    "AA" -> prn.setFontAA() //Extra Grande
                    "BB" -> prn.setFontBB() //Grande

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

                    val turn = order.optString("turn", "")
                    if (turn.isNotEmpty()) {
                        prn.escribirTextoSinSalto("Turno: ")

                        prn.escribirTexto(order.optString("turn", "N/A"))

                    }
                    prn.agregarSalto()
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
                val taxesData = js.optJSONArray("taxes")
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
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")+js.getDouble("additional_tip")))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Entrega:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("total")+js.getDouble("additional_tip")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Cambio:")

                prn.agregarCaracteresDerecha(
                    10,
                    df.format(js.optDouble("change_amount" ,0.0)).replace("-", "")
                )
                prn.agregarSalto()
                paymentMethods = js.getJSONArray("payment_methods")

                prn.LineasGuion()
                prn.alineadoIzquierda()
                prn.escribirTextoSinSalto("Formas de Pago")
                prn.agregarSalto()
                prn.LineasIgual()

                for (j in 0 until paymentMethods.length()) {
                    jo = paymentMethods.getJSONObject(j)

                    val name = jo.optString("name")
                    val amount = df.format(jo.optDouble("amount", 0.0))



                    prn.escribirTextoSinSalto(name )
                    val nameLength = name.length
                    val amountLength = amount.length

                    prn.agregarCaracteres((caracteres - nameLength-amountLength).coerceAtLeast(0), "")
                    prn.escribirTextoSinSalto(amount)

                    prn.agregarSalto()

                }


                prn.LineasGuion()
                val observation = js.optString("observation", "")
                if (!observation.isNullOrEmpty()) {
                    prn.alineadoIzquierdaForce(observation)
                }
                if (printerConfig != null) {
                    if (printerConfig.optBoolean("observation", false) && subsidiary != null) {
                        val subsidiaryObservation = subsidiary.optString("observation", "")
                        if (!subsidiaryObservation.isNullOrEmpty()) {
                            prn.alineadoIzquierdaForce(subsidiaryObservation)
                        }
                    }
                }

                val orderData = js.optJSONObject("order" ) ?: JSONObject()
                val deliveryRecord = orderData.optJSONObject("delivery_record")


                if(deliveryRecord != null) {


                    prn.LineasIgual()
                    val delivery = deliveryRecord.optJSONObject("delivery")
                    val address = deliveryRecord.optJSONObject("address")



                    prn.alineadoIzquierda()


                    prn.agregarTexto( "Dirección: ")
                    if (address != null) {
                        prn.agregarTexto( deliveryRecord.optString("address_string", "N/A")+"/" + address.optString("observation", "N/A"))
                    }


                    if (delivery != null) {
                        prn.agregarTexto("Nombre: " +  delivery.optString("name", "N/A"))
                        prn.agregarTexto("Teléfono: " + delivery.optString("phone" ,"N/A"))
                    } else {
                        prn.agregarTexto("N/A")
                    }


                }

                if(orderData.optString("type") == "Retiro"){
                    prn.LineasIgual()
                    prn.alineadoIzquierda()
                    prn.agregarTexto("Retiro en local")
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
                println(prn.getTrabajo())
                enviarImprimir(prn.getTrabajo())


            }


        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    suspend fun imprimirPreticket(js: JSONObject?,sj : JSONObject?, caracteres: Int) {
        println("Imprimiendo recibo")
        println(js.toString())

        try {
            if (js == null) return


            var detalles: JSONArray

            var jo: JSONObject

            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("preticket")
            println("Printer Config: $printerConfig")

            val configCopies: Int = printerConfig?.let { cfg ->
                when (val v = cfg.opt("copies")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 1
                    else -> 1
                }
            } ?: 1

            val lineSpacing = printerConfig?.optBoolean("line_spacing")
            val logo = printerConfig?.optBoolean("logo")
            //Font selection based on printerConfig
            val fontType = printerConfig?.optString("font") ?: "A"
            println("Font Type: $fontType")



            for (i in 0 until configCopies) {
                println("Imprimiendo copia $i")
                val prn = PrinterHelpers(caracteres, configCopies)

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
                    prn.logo(url, this, fontType)
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
                val tip = js.optDouble("tip",0.0)
                println("Tip: $tip")
                val tip_field = printerConfig?.optBoolean("tip")
                if(tip_field==true && tip== 0.0  ){

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
                    if (jo.optDouble("pending_amount", 0.0) != 0.0) {
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


                //Add tip value
                if(tip>0){
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Propina:")
                    prn.agregarCaracteresDerecha(12, df.format(tip))
                    prn.agregarSalto()
                }
                //agregar total


                prn.escribirTextoSinSalto("Total A Pagar:")

                prn.agregarCaracteresDerecha(12, df.format(js.getDouble("total")))
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
                println(prn.getTrabajo())
                enviarImprimir(prn.getTrabajo())
            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


     fun imprimirComandas(js: JSONObject?, sj : JSONObject?, caracteres: Int, printCode: String) {
        try {
            if (js == null) return

            // reutilizables
            //val detalles: JSONArray
            //var items: Int
            //var jo: JSONObject
            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("order")
            println("Printer Config: $printerConfig")
            
            val configCopies: Int = printerConfig?.let { cfg ->
                when (val v = cfg.opt("copies")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 1
                    else -> 1
                }
            } ?: 1
            
            // imprimir


            for (i in 0 until configCopies) {
                println("Imprimiendo copia $i")
                val prn = PrinterHelpers(caracteres, configCopies)

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
            val orderData = js
            println("Order Print Data: $orderData")


            //Font selection based on printerConfig
            val fontType = printerConfig?.optString("font") ?: "A"
            println("Font Type: $fontType")
            when (fontType.uppercase()) {
                "A" -> prn.setFontA() //Normal
                "B" -> prn.setFontB() //Pequeña
                "AA" -> prn.setFontAA() //Extra Grande
                "BB" -> prn.setFontBB() //Grande

            }

            val order_prints = orderData.getJSONArray("order_print_details")
            println("Order Prints: $order_prints")
            val commandSequential = orderData.optString("sequential") ?: "N/A"

            prn.agregarTexto("Orden " + (orderData.optJSONObject("order")?.optString("sequential") ?: "N/A"))
            prn.agregarTexto("$printCode - $commandSequential")
            prn.alineadoIzquierda()

            prn.agregarSalto()
            prn.agregarTexto("Mesero: " + (orderData.optString("responsible_name") ?: "N/A"))

            val isoDate = orderData.optString("created_at")
            val formattedDate = try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val date = parser.parse(isoDate)
                val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                if (date != null) formatter.format(date) else "N/A"
            } catch (_: Exception) {
                "N/A"
            }
            val order = orderData.optJSONObject("order")
            if(order?.optJSONObject("table") != null){
                val table = order.optJSONObject("table")
                prn.agregarTexto("Comensales: " + (table?.optString("pax") ?: "N/A"))
                prn.agregarTexto("Area: " + (table?.optJSONObject("area")?.optString("name", "N/A") ?: "N/A"))
                prn.agregarTexto("Mesa: " + (table?.optString("name", "N/A") ?: "N/A") + " - " + order.optString("alias", "N/A"))
            }
            prn.agregarTexto("Fecha: $formattedDate")

            prn.LineasIgual()

                for (j in 0 until order_prints.length()) {
                    val jo = order_prints.getJSONObject(j)
                    val printersArray = jo.optJSONArray("printers")
                    var shouldPrint = false

                    if (printersArray != null) {
                        for (k in 0 until printersArray.length()) {
                            val printerObj = printersArray.getJSONObject(k)
                            println("Debug: Printer Object - $printerObj") // Debug: Log the printer object
                            println("Debug: Current Print Code - $printCode") // Debug: Log the current print code
                            if (printerObj.optString("code") == printCode) {
                                shouldPrint = true
                                println("Debug: Match Found - Printer Code: ${printerObj.optString("code")}") // Debug: Log the match
                                break
                            }
                        }
                    }

                    if (shouldPrint) {
                        println("Debug: Printing Item - ${jo.optString("product_name")} with Amount: ${jo.optInt("amount")}") // Debug: Log the item being printed
                        prn.escribirTextoSinSalto(
                            jo.optString("type") + jo.optInt("amount") + " " + jo.optString("product_name")
                        )
                        prn.agregarSalto()



                        val additionalInformationArr = try { jo.optJSONArray("additional_information") } catch (e: Exception) { null }
                        if (additionalInformationArr != null) {
                            for (i in 0 until additionalInformationArr.length()) {
                                val infoObj = additionalInformationArr.optJSONObject(i)
                                val observationsArr = infoObj?.optJSONArray("observations")
                                if (observationsArr != null) {
                                    for (idx in 0 until observationsArr.length()) {
                                        val obs = observationsArr.optJSONObject(idx)?.optString("item") ?: continue
                                        prn.escribirTextoSinSalto("Observación: "+obs)
                                        prn.agregarSalto()
                                    }
                                }
                                val extrasArr = infoObj?.optJSONArray("extras")
                                if (extrasArr != null) {
                                    for (idx in 0 until extrasArr.length()) {
                                        val extra = extrasArr.optJSONObject(idx)?.optString("item") ?: continue
                                        prn.escribirTextoSinSalto("Extra: "+extra)
                                        prn.agregarSalto()
                                    }
                                }
                            }
                        }
                    }
                }

            prn.LineasIgual()

            prn.negritaOn()

            prn.agregarTexto(orderData.optString("order_type") ?: "N/A" )


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
                println("Comandos")
                println(prn.getTrabajo())
            enviarImprimir(prn.getTrabajo())

            }
            closeAll()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }



    fun imprimirCierreCaja(js: JSONObject?, sj: JSONObject?, caracteres: Int) {
        try {
            if (js == null) return

            val summary = js.getJSONObject("summary")
            val totals = summary.getJSONObject("totals")
            val paymentMethods = summary.optJSONArray("payment_methods")
            val salesByProduct = summary.optJSONArray("sales_by_product")
            val salesByHour = summary.optJSONObject("sales_by_hour")
            val movements = summary.optJSONArray("movements")
            val cashRegisterSettings = sj?.getJSONObject("printers")?.getJSONObject("cash_register")
            val lineSpacing = cashRegisterSettings?.optBoolean("line_spacing",false)

            println("Printer Config: $cashRegisterSettings")

            val configCopies: Int = cashRegisterSettings?.let { cfg ->
                when (val v = cfg.opt("copies")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 1
                    else -> 1
                }
            } ?: 1


            for (i in 0 until configCopies) {
                val prn = PrinterHelpers(caracteres, configCopies)
                prn.iniciar()

                prn.setFontA()
                if (lineSpacing == true ) {
                    println("Setting line spacing applied")
                    prn.lineHeight2()
                }else {
                    println("Setting default line spacing")
                    prn.lineHeight()
                }

                prn.alineadoCentro()
                prn.negritaOn()
                prn.escribirTexto("Cierre de Caja")
                prn.negritaOff()
                prn.agregarSalto()


                prn.escribirTexto("Caja #" + js.optString("sequential"))
                prn.escribirTexto("Usuario: " + js.getJSONObject("user").optString("name"))

                val sub = js.getJSONObject("subsidiary")
                prn.escribirTexto(
                    "Sucursal: " + sub.optString("commercial_name") +
                            " " + sub.optString("code") +
                            " · PV: " + js.optJSONObject("checkout")?.optString("name","-")
                )

                prn.escribirTexto("Apertura: " + js.optString("opened_at"))
                prn.escribirTexto("Cierre: " + js.optString("closed_at", "-"))

                val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                prn.escribirTexto("Impreso: $currentDateTime")
                prn.agregarSalto()
                prn.alineadoIzquierda()
                // ===========================
                // RESUMEN EFECTIVO — PARTE 1
                // ===========================
                prn.LineasGuion()

                prn.escribirTextoSinSalto("Efectivo esperado")
                prn.agregarCaracteresDerecha(caracteres - 17, "$" + df.format(totals.getDouble("expected_cash")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Efectivo contado")
                prn.agregarCaracteresDerecha(caracteres - 16, "$" + df.format(totals.getDouble("received_cash")))
                prn.agregarSalto()

                val difference = totals.getDouble("difference")
                val diffAbs = kotlin.math.abs(difference)
                val diffSuf = if (difference < 0) "S" else "F"

                prn.escribirTextoSinSalto("Diferencia")
                prn.agregarCaracteresDerecha(caracteres - 10, df.format(diffAbs) + " " + diffSuf)
                prn.agregarSalto()

                // ===========================
                // RESUMEN EFECTIVO — PARTE 2
                // ===========================
                prn.LineasGuion()
                prn.negritaOn()
                prn.alineadoCentro()
                prn.escribirTexto("-- Resumen de Efectivo --")
                prn.alineadoIzquierda()
                prn.negritaOff()


                prn.escribirTextoSinSalto("Apertura")
                prn.agregarCaracteresDerecha(caracteres - 8, "$" + df.format(totals.getDouble("opening")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Ventas en efectivo")
                prn.agregarCaracteresDerecha(caracteres - 18, "$" + df.format(totals.getDouble("sales_cash")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("+ Ingresos")
                prn.agregarCaracteresDerecha(caracteres - 10, "+" + df.format(totals.getDouble("revenue")))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("- Egresos")
                prn.agregarCaracteresDerecha(caracteres - 9, "-" + df.format(totals.getDouble("expenses")))
                prn.agregarSalto()

                prn.negritaOn()
                prn.escribirTextoSinSalto("Total esperado")
                prn.agregarCaracteresDerecha(caracteres - 14, "$" + df.format(totals.getDouble("expected_cash")))
                prn.negritaOff()
                prn.agregarSalto()

                // ===========================
                // VENTAS
                // ===========================
                prn.LineasGuion()
                prn.negritaOn()
                prn.alineadoCentro()
                prn.escribirTexto("-- Ventas --")
                prn.alineadoIzquierda()
                prn.negritaOff()

                val totalsHour = salesByHour.optJSONObject("totals")

                prn.escribirTextoSinSalto("# Válidas")
                prn.agregarCaracteresDerecha(caracteres - 9, "" + df.format(totalsHour.optJSONObject("valid")?.optInt("count", 0)))

                prn.escribirTextoSinSalto("# Anuladas")
                prn.agregarCaracteresDerecha(caracteres - 10, "" + df.format(totalsHour.optJSONObject("canceled")?.optInt("count", 0)))

                prn.escribirTextoSinSalto("Total válidas")
                prn.agregarCaracteresDerecha(caracteres - 13, "" + df.format(totals.getDouble("sales_total_valid")))


                prn.agregarSalto()

                // ===========================
                // INGRESOS POR FORMA DE PAGO
                // ===========================
                if (cashRegisterSettings?.optBoolean("payment_methods", false) == true) {
                    prn.LineasGuion()
                    prn.negritaOn()
                    prn.alineadoCentro()
                    prn.escribirTexto("-- Ingresos por forma de pago --")
                    prn.alineadoIzquierda()
                    prn.negritaOff()

                    if (paymentMethods != null) {
                        for (i in 0 until paymentMethods.length()) {
                            val pm = paymentMethods.getJSONObject(i)
                            val name = pm.optString("name")
                            val amt = pm.optJSONObject("valid").optDouble("amount")

                            prn.escribirTextoSinSalto(name)
                            prn.agregarCaracteresDerecha(caracteres - name.length, "$" + df.format(amt))
                            prn.agregarSalto()
                        }
                    }
                }

                // ===========================
                // PROPINAS
                // ===========================
                if (cashRegisterSettings?.optBoolean("tip", false) == true) {
                    prn.LineasGuion()
                    prn.negritaOn()
                    prn.alineadoCentro()
                    prn.escribirTexto("-- Propinas --")
                    prn.alineadoIzquierda()
                    prn.negritaOff()

                    val tip = totals.optDouble("additional_tip_total", 0.0)

                    prn.escribirTextoSinSalto("Propinas adicionales")
                    prn.agregarCaracteresDerecha(caracteres - 20, "" + df.format(tip))
                }


                // ===========================
                // TOP PRODUCTOS
                // ===========================
                if (cashRegisterSettings?.optBoolean("sales_by_product", false) == true) {
                    prn.LineasGuion()
                    prn.negritaOn()
                    prn.alineadoCentro()
                    prn.escribirTexto("-- Top productos --")
                    prn.alineadoIzquierda()
                    prn.negritaOff()

                    if (salesByProduct != null && salesByProduct.length() > 0) {
                        for (i in 0 until salesByProduct.length()) {
                            val p = salesByProduct.getJSONObject(i)

                            prn.escribirTextoSinSalto(p.optString("product_name"))
                            val productLength = p.optString("product_name").length
                            prn.agregarCaracteresDerecha(caracteres - productLength, "" + p.optInt("total_quantity"))

                            prn.negritaOn()
                            prn.lineHeight()
                            prn.LineasGuion()
                            prn.negritaOff()
                            prn.lineHeight2()
                            prn.escribirTextoSinSalto("     Importe")

                            prn.agregarCaracteresDerecha(caracteres - 12, "" + df.format(p.optDouble("total_sold_with_taxes")))
                            prn.agregarSalto()
                        }
                    }else{
                        prn.agregarSalto()
                        prn.escribirTextoSinSalto("No hay productos vendidos")
                        prn.agregarSalto()
                    }
                }

                // ===========================
                // HORA PICO
                // ===========================
                if (cashRegisterSettings?.optBoolean("sales_resume", false) == true) {
                    prn.LineasGuion()
                    prn.negritaOn()
                    prn.alineadoCentro()
                    prn.escribirTexto("-- Hora pico --")
                    prn.alineadoIzquierda()
                    prn.negritaOff()

                    val buckets = salesByHour.optJSONArray("buckets")
                    if (buckets != null && buckets.length() > 0) {
                        for(j in 0 until buckets.length()){
                            val bucket = buckets.getJSONObject(j)
                            val hour = bucket.optString("hour", "N/A")
                            val money = "$" + df.format(bucket.optDouble("total", 0.0))

                            prn.escribirTextoSinSalto(hour)
                            val space = (caracteres - hour.length - money.length).coerceAtLeast(0)
                            prn.agregarCaracteres(space, "")
                            prn.escribirTextoSinSalto(money)
                            prn.agregarSalto()
                        }
                    } else {
                        prn.agregarSalto()
                        prn.escribirTexto("No hay datos disponibles")
                        prn.agregarSalto()
                    }
                }

                // ===========================
                // MOVIMIENTOS DE CAJA
                // ===========================
                if (cashRegisterSettings?.optBoolean("movements", false) == true) {
                    prn.LineasGuion()
                    prn.negritaOn()
                    prn.alineadoCentro()
                    prn.escribirTexto("-- Movimientos de caja --")
                    prn.alineadoIzquierda()
                    prn.negritaOff()

                    if (movements != null && movements.length() > 0) {
                        for(j in 0 until movements.length()){
                            val mov = movements.getJSONObject(j)
                            val type = if(mov.optString("type") == "expense") "(-)" else "(+)"
                            var desc = mov.optString("description", "N/A")
                            val amount = df.format(mov.optDouble("amount", 0.0)) + type

                            // Calculated space
                            val maxDescLen = (caracteres - amount.length - 2).coerceAtLeast(1)
                            if(desc.length > maxDescLen) {
                                desc = desc.substring(0, maxDescLen)
                            }

                            prn.escribirTextoSinSalto(desc)
                            val space = (caracteres - desc.length - amount.length).coerceAtLeast(0)
                            prn.agregarCaracteres(space, "")
                            prn.escribirTextoSinSalto(amount)
                            prn.agregarSalto()
                        }
                    }else {
                        prn.agregarSalto()
                        prn.escribirTexto("No hay movimientos de caja")
                        prn.agregarSalto()
                    }
                }

                // ===========================
                // OBSERVACIÓN (si existe)
                // ===========================
                prn.alineadoCentro()
                if (js.has("observation")) {
                    prn.LineasGuion()

                    prn.escribirTexto("Obs: " + js.optString("observation"))

                }

                // ===========================
                // CIERRE FINAL
                // ===========================


                prn.LineasGuion()
                prn.agregarSalto()
                val cierreTxt = if (difference < 0)
                    "Cierre con sobrante de $" + df.format(diffAbs)
                else
                    "Cierre con faltante de $" + df.format(diffAbs)

                prn.escribirTexto(cierreTxt)
                prn.agregarSalto()
                prn.agregarSalto()
                prn.escribirTexto("------------------------")
                prn.escribirTexto("Firma")
                prn.agregarSalto()
                prn.escribirTexto("¡Gracias!")

                val line_breaks = cashRegisterSettings?.optInt("line_breaks") ?: 0
                for (i in 0 until line_breaks) {
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
                enviarImprimir(prn.getTrabajo())

            } // loop close

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


    /**
     * Imprime comprobante de cobro/abono de cuenta por cobrar
     * @param js JSONObject con los datos del PaymentAccount
     * @param sj JSONObject con la configuración del sistema (settings)
     * @param caracteres Ancho de impresión en caracteres
     */
    suspend fun imprimirAbonoCuenta(js: JSONObject?, sj: JSONObject?, caracteres: Int) {
        println("Imprimiendo comprobante de abono/cobro")
        println(js.toString())
        try {
            if (js == null) return

            val printerConfig = sj?.getJSONObject("printers")?.optJSONObject("paymentAccounts")
            println("Printer Config: $printerConfig")

            val configCopies: Int = printerConfig?.let { cfg ->
                when (val v = cfg.opt("copies")) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: 1
                    else -> 1
                }
            } ?: 1

            println("Copias configuradas $configCopies")

            for (i in 0 until configCopies) {
                println("Imprimiendo copia $i")

                val prn = PrinterHelpers(caracteres, configCopies)
                prn.iniciar()
                prn.dobleAltoOn()

                val lineSpacing = printerConfig?.optBoolean("line_spacing",true)
                val logo = printerConfig?.optBoolean("logo")

                if (lineSpacing == true) {
                    println("Setting line spacing applied")
                    prn.lineHeight2()
                } else {
                    println("Setting default line spacing")
                    prn.lineHeight()
                }

                prn.alineadoCentro()

                // Font selection based on printerConfig
                val fontType = printerConfig?.optString("font") ?: "B"
                println("Font Type: $fontType")

                // Validate if the "image" key exists for logo
                val subsidiary = sj?.optJSONObject("subsidiary")
                val image = subsidiary?.optJSONObject("image")
                val url = image?.optString("full_path", "")

                if (!url.isNullOrEmpty() && logo == true) {
                    prn.logo(url, this, fontType)
                } else {
                    println("Image URL is missing or invalid.")
                }

                when (fontType.uppercase()) {
                    "A" -> prn.setFontA()
                    "B" -> prn.setFontB()
                    "AA" -> prn.setFontAA()
                    "BB" -> prn.setFontBB()
                }

                // ===========================
                // ENCABEZADO EMPRESA
                // ===========================
                if (sj != null) {
                    if (sj.optString("business_name") != "null") {
                        prn.escribirTextoSinSalto(sj.optString("business_name"))
                        prn.agregarSalto()
                    }
                    prn.escribirTextoSinSalto("RUC: " + sj.optString("ruc"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto(sj.optString("address"))
                    prn.agregarSalto()
                    if (sj.optString("phone") != "null") {
                        prn.escribirTextoSinSalto("Tel: " + sj.optString("phone"))
                        prn.agregarSalto()
                    }
                }

                prn.agregarSalto()

                // ===========================
                // TÍTULO DEL COMPROBANTE
                // ===========================
                prn.negritaOn()
                prn.escribirTexto("COMPROBANTE DE COBRO / ABONO")
                prn.negritaOff()
                prn.LineasIgual()

                // ===========================
                // DATOS DEL COMPROBANTE
                // ===========================
                prn.alineadoIzquierda()

                // Número secuencial del abono
                val sequential = js.optString("sequential", "")
                val number = js.optString("number", "")
                //Numero
                prn.escribirTextoSinSalto("Nº: ")
                prn.escribirTextoSinSalto(number)
                prn.agregarSalto()

                // Secuencial
                prn.escribirTextoSinSalto("Sec: ")
                prn.escribirTextoSinSalto(sequential)
                prn.agregarSalto()

                // Referencia
                val reference = js.optString("reference", "")
                prn.escribirTextoSinSalto("Ref: ")
                prn.escribirTextoSinSalto(reference)
                prn.agregarSalto()

                // Fecha del último pago o fecha actual
                val payments = js.optJSONArray("payment_account_details")
//                val lastPaymentDate = if (payments != null && payments.length() > 0) {
//                    payments.getJSONObject(payments.length() - 1).optString("date", "")
//                } else {
//                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
//                }
//                prn.escribirTextoSinSalto("Fecha: ")
//                prn.escribirTextoSinSalto(lastPaymentDate)
//                prn.agregarSalto()

                prn.LineasGuion()

                // ===========================
                // DATOS DEL CLIENTE
                // ===========================
                prn.alineadoCentro()
                prn.negritaOn()
                prn.escribirTexto("CLIENTE")
                prn.negritaOff()
                prn.alineadoIzquierda()

                val person = js.optJSONObject("person")
                val customerName = person?.optString("name", "N/A") ?: "N/A"
                val customerIdentity = person?.optString("identity", "N/A") ?: "N/A"

                prn.escribirTextoSinSalto("Nombre: ")
                prn.escribirTextoSinSalto(customerName)
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Ced/RUC: ")
                prn.escribirTextoSinSalto(customerIdentity)
                prn.agregarSalto()

                prn.LineasGuion()

                // ===========================
                // DOCUMENTO RELACIONADO
                // ===========================
                prn.alineadoCentro()
                prn.negritaOn()
                prn.escribirTexto("DOCUMENTO RELACIONADO")
                prn.negritaOff()
                prn.alineadoIzquierda()

                val paymentAccountable = js.optJSONObject("payment_accountable")
                if (paymentAccountable != null) {
                    val docNumber = paymentAccountable.optString("number", "N/A")
                    val docDate = paymentAccountable.optString("date", "N/A")
                    val docTotal = paymentAccountable.optDouble("total", 0.0)

                    prn.escribirTextoSinSalto("Numero: ")
                    prn.escribirTextoSinSalto(docNumber)
                    prn.agregarSalto()

                    prn.escribirTextoSinSalto("Fecha:  ")
                    prn.escribirTextoSinSalto(docDate)
                    prn.agregarSalto()

                    prn.escribirTextoSinSalto("Total:  ")
                    prn.escribirTextoSinSalto("$" + df.format(docTotal))
                    prn.agregarSalto()
                }



                prn.LineasIgual()

                // ===========================
                // DETALLE DE PAGOS (si hay)
                // ===========================
                if (payments != null && payments.length() > 0) {
                    prn.alineadoCentro()
                    prn.negritaOn()
                    prn.escribirTexto("DETALLE DE PAGOS")
                    prn.negritaOff()
                    prn.alineadoIzquierda()

                    // Find the most recent payment
                    var mostRecentPayment: JSONObject? = null
                    var mostRecentTimestamp = Long.MIN_VALUE
                    
                    for (j in 0 until payments.length()) {
                        val payment = payments.getJSONObject(j)
                        val rawPaymentDate = payment.optString("created_at", "")
                        val timestamp = try {
                            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                            inputFormat.parse(rawPaymentDate)?.time ?: 0L
                        } catch (e: Exception) {
                            0L
                        }
                        
                        if (timestamp > mostRecentTimestamp) {
                            mostRecentTimestamp = timestamp
                            mostRecentPayment = payment
                        }
                    }
                    
                    // Only print payments within 1 millisecond of the most recent
                    if (mostRecentPayment != null) {
                        for (j in 0 until payments.length()) {
                            val payment = payments.getJSONObject(j)
                            val rawPaymentDate = payment.optString("created_at", "")
                            val timestamp = try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                                inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                                inputFormat.parse(rawPaymentDate)?.time ?: 0L
                            } catch (e: Exception) {
                                0L
                            }
                            
                            if (kotlin.math.abs(timestamp - mostRecentTimestamp) <= 1) {
                                val paymentDate = try {
                                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                                    val date = inputFormat.parse(rawPaymentDate)
                                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date!!)
                                } catch (e: Exception) {
                                    rawPaymentDate
                                }
                                val paymentTime = try {
                                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.getDefault())
                                    inputFormat.timeZone = TimeZone.getTimeZone("UTC")
                                    val date = inputFormat.parse(rawPaymentDate)
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(date!!)
                                } catch (e: Exception) {
                                    ""
                                }
                                val paymentAmount = payment.optString("amount", "0")
                                val previousAmount = payment.optString("account_amount", "0")
                                val newAmount = payment.optString("new_amount", "0")
                                val paymentAmountDouble = paymentAmount.toDoubleOrNull() ?: 0.0
                                
                                val paymentable = payment.optJSONObject("paymentable")
                                val paymentMethod = paymentable?.optString("payment_method_name", "N/A") ?: "N/A"
                                val amountStr = "$" + df.format(paymentAmountDouble)

                                prn.escribirTextoSinSalto(paymentDate)
                                if (paymentTime.isNotEmpty()) {
                                    prn.agregarCaracteresDerecha((caracteres - paymentDate.length).coerceAtLeast(0), paymentTime)
                                }
                                prn.agregarSalto()
                                
                                prn.escribirTextoSinSalto(paymentMethod)
                                prn.agregarSalto()
                                
                                prn.escribirTextoSinSalto("Saldo anterior: ")
                                prn.agregarCaracteresDerecha((caracteres - 16).coerceAtLeast(0), "$" + df.format(previousAmount.toDoubleOrNull() ?: 0.0))
                                prn.agregarSalto()
                                
                                prn.escribirTextoSinSalto("Pago: ")
                                prn.agregarCaracteresDerecha((caracteres - 6).coerceAtLeast(0), amountStr)
                                prn.agregarSalto()
                                
                                prn.escribirTextoSinSalto("Nuevo saldo: ")
                                prn.agregarCaracteresDerecha((caracteres - 13).coerceAtLeast(0), "$" + df.format(newAmount.toDoubleOrNull() ?: 0.0))
                                prn.agregarSalto()

                                if (j < payments.length() - 1) {
                                    prn.LineasGuion()
                                }
                            }
                        }
                    }
                    prn.LineasIgual()

                }

    

                // ===========================
                // RESUMEN DE CUENTA
                // ===========================
                prn.alineadoCentro()
                prn.negritaOn()
                prn.escribirTexto("RESUMEN DE CUENTA")
                prn.negritaOff()
                prn.alineadoIzquierda()

                // Monto original (amount)
                val originalAmount = js.optDouble("amount", 0.0)
                val originalAmountValue = if (originalAmount > 1000) originalAmount / 100.0 else originalAmount

                // Monto abonado acumulado (payed_amount)
                val payedAmount = js.optDouble("payed_amount", 0.0)
                val payedAmountValue = if (payedAmount > 1000) payedAmount / 100.0 else payedAmount



                val labelWidth = 18 // "Monto original:" length

                prn.escribirTextoSinSalto("Monto original :")
                prn.agregarCaracteresDerecha(caracteres - 16, "$" + df.format(originalAmountValue))
                prn.agregarSalto()

                prn.escribirTextoSinSalto("Abonado acum.  :")
                prn.agregarCaracteresDerecha(caracteres - 16, "$" + df.format(payedAmountValue))
                prn.agregarSalto()

                                // Saldo pendiente (balance)
                val balance = js.optDouble("balance", 0.0)
                val balanceValue = if (balance > 1000) balance / 100.0 else balance
                prn.negritaOn()
                prn.escribirTextoSinSalto("Saldo pendiente:")
                prn.agregarCaracteresDerecha(caracteres - 16, "$" + df.format(balanceValue))
                prn.negritaOff()
                prn.agregarSalto()
                prn.LineasGuion()
                // ===========================
                // OBSERVACIÓN (si existe)
                // ===========================
                val observation = js.optString("observation", "")
                if (observation.isNotEmpty() && observation != "null") {
                    prn.alineadoIzquierda()
                    prn.escribirTextoSinSalto("Obs: ")
                    prn.escribirTextoSinSalto(observation)
                    prn.agregarSalto()
                    prn.agregarSalto()
                }

                // ===========================
                // PIE DE PÁGINA
                // ===========================


                // Usuario que imprime
                val user = AppStorage.getUserData(context)
                val userName = user?.name
                if (userName?.isNotEmpty() == true) {
                    prn.escribirTextoSinSalto("Usuario: ")
                    prn.escribirTextoSinSalto(userName)
                    prn.agregarSalto()
                }

                // Fecha y hora de impresión
                val currentDateTime = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
                prn.escribirTextoSinSalto("Impreso: ")
                prn.escribirTextoSinSalto(currentDateTime)
                prn.agregarSalto()

                prn.alineadoCentro()
                prn.agregarSalto()
                prn.negritaOn()
                prn.escribirTexto("*** Gracias por su pago ***")
                prn.negritaOff()
                prn.agregarSalto()
                prn.agregarSalto()
                // ===========================
                // SALTOS Y CORTE
                // ===========================
                val lineBreaks = printerConfig?.optInt("line_breaks") ?: 0
                for (j in 0 until lineBreaks) {
                    prn.agregarSalto()
                }

                if (tipo == PrinterType.BLUETOOTH.type) {
                    prn.feed(6)
                } else {
                    if (lineSpacing == true) {
                        println("Setting line spacing applied")
                        prn.feed(7)
                    } else {
                        println("Setting default line spacing")
                        prn.feed(14)
                    }
                }

                prn.cortar()

                println("Comandos")
                println(prn.getTrabajo())
                enviarImprimir(prn.getTrabajo())
            }
            closeAll()

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
        val startTs = System.currentTimeMillis()
        var success = false
        var error: String? = null
        try {
            when (tipo) {
                PrinterType.WIFI.type -> {
                    TcpIpOutputStream(this.address, this.port!!).use { outputStream ->
                        val style = Style()
                        val escposCoffee = EscposCoffee(style, outputStream)
                        escposCoffee.printMessage(trabajo)
                        success = true
                    }
                }
                PrinterType.BLUETOOTH.type -> {
                    if (this.streamBluetooth != null) {
                        val style = Style()
                        val escposCoffee = EscposCoffee(style, this.streamBluetooth!!)
                        escposCoffee.printMessage(trabajo)

                        Thread.sleep(4000)
                        this.streamBluetooth?.flush()
                        success = true
                    } else {
                        error = "Bluetooth stream is null"
                    }
                }
                else -> {
                    if (usbOutputStream != null) {
                        val style = Style()
                        val escposCoffee = EscposCoffee(style, usbOutputStream!!)
                        escposCoffee.printMessage(trabajo)
                        success = true
                    } else {
                        error = "USB output stream is null"
                    }
                }
            }
        } catch (e: Exception) {
            error = e.message
            e.printStackTrace()
        } finally {
            val endTs = System.currentTimeMillis()
            diagnosticsListener?.invoke(
                PrinterDiagnosticsEvent(
                    transportType = tipo ?: "UNKNOWN",
                    address = address,
                    port = port,
                    bytesLength = trabajo.toByteArray(Charsets.ISO_8859_1).size,
                    startTimestamp = startTs,
                    endTimestamp = endTs,
                    success = success,
                    errorMessage = error
                )
            )
        }
    }




    suspend fun printMediaJob(mediaBuilder: MediaBuilder, caracteres: Int, fontName: String = "A") {
            try {
                when (tipo) {
                    PrinterType.WIFI.type -> {
                        TcpIpOutputStream(this.address, this.port!!).use { outputStream ->
                            val style = Style()
                            val escposCoffee = EscposCoffee(style, outputStream, caracteres, fontName)
                            escposCoffee.printMedia(mediaBuilder)
                        }
                    }
                    PrinterType.BLUETOOTH.type -> {
                        if (this.streamBluetooth != null) {
                            val style = Style()
                            val escposCoffee = EscposCoffee(style, this.streamBluetooth!!, caracteres, fontName)
                            escposCoffee.printMedia(mediaBuilder)
                            kotlinx.coroutines.delay(2000)
                            this.streamBluetooth?.flush()
                        } else {
                            println("Bluetooth stream is null")
                        }
                    }
                    else -> {
                        val style = Style()
                        val escposCoffee = EscposCoffee(style, this.usbOutputStream!!, caracteres, fontName)
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

