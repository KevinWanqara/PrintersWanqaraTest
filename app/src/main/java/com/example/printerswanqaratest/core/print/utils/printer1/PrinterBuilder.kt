package com.example.printerswanqaratest.core.print.utils.printer1

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
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.connection.usb.UsbOutputStream
import com.dantsu.escposprinter.connection.usb.UsbPrintersConnections
import com.example.printerswanqaratest.core.print.EscposCoffee
import com.example.printerswanqaratest.core.printType.PrinterType
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

class PrinterBuilder(private val tipo: String?) {

    private var socketBluetooth: BluetoothSocket? = null
    private var streamBluetooth: OutputStream? = null
    private var usbOutputStream: OutputStream? = null
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

    fun InicializarImpresoraRed(direccion: String?, puerto: Int): Boolean {
        this.port = puerto
        this.address = direccion
        return true
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
            context.registerReceiver(usbReceiver, filter)
            val usbConnection = UsbPrintersConnections.selectFirstConnected(context)
            val usbManager =
                context.getSystemService(AppCompatActivity.USB_SERVICE) as UsbManager

            if (usbConnection != null && usbManager != null) {
                val permissionIntent: PendingIntent = PendingIntent.getBroadcast(
                    context, 0, Intent(ACTION_USB_PERMISSION),
                    PendingIntent.FLAG_MUTABLE
                )
                if (!usbManager.hasPermission(usbConnection.device)) {
                    usbManager.requestPermission(usbConnection.device, permissionIntent)
                    context.getSharedPreferences("usb", 0).edit().putBoolean("permissions", true)
                        .apply()
                } else {
                    usbOutputStream = UsbOutputStream(usbManager, usbConnection.device)
                    context.getSharedPreferences("usb", 0).edit().putBoolean("permissions", false)
                        .apply()
                }
                return true
            }
        } catch (e: Exception) {
            println(e)
            return false
        }
        return false
    }

    fun imprimirPreticket(js: JSONObject?, copias: Int, caracteres: Int) {
        try {
            if (js == null) return

            // reutilizables
            var detalles: JSONArray
            var items: Int
            var jo: JSONObject

            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.iniciar()
            prn.setFontB()
            prn.agregarSalto()
            //Linea 1 numero de Factura y Fecha
            prn.negritaOff()
            prn.alineadoCentro()
            prn.lineHeight2()
            prn.dobleAltoOn()
            prn.dobleAnchoOn()
            prn.escribirTextoSinSalto("Preticket")
            prn.dobleAnchoOff()
            prn.dobleAltoOff()
            prn.agregarSalto()
            prn.agregarSalto()
            prn.escribirTextoSinSalto(js.optString("razonSociallEm"))
            prn.agregarSalto()
            if (js.optString("nombreComercialEm") != "null") {
                prn.escribirTextoSinSalto(js.optString("nombreComercialEm"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("RUC: " + js.optString("rucEm"))
            prn.agregarSalto()
            if (js.optBoolean("obligadoLlevarContabilidadEm")) prn.agregarTexto("Obligado a llevar Contabilidad")
            if (js.optBoolean("regimenMicroEmpresa0v")) prn.agregarTexto("Contribuyente Régimen RIMPE")
            if (js.optBoolean("agenteRetencionOv")) prn.agregarTexto(
                "Agente de Retención " + "Nº de Resolución: " + js.optString(
                    "numeroResolucionOv"
                )
            )
            prn.setFontB()
            prn.agregarTexto("Dirección: " + js.optString("direccionEm"))
            if (js.optString("direccionSucursalEm") != "S/N") prn.agregarTexto(
                "Sucursal: " + js.optString(
                    "direccionSucursalEm"
                )
            )
            if (js.optString("telefonoEm") != "null") {
                prn.agregarTexto("Teléfono: " + js.optString("telefonoEm"))
            }
            if (js.optString("correoEm") != "null") prn.agregarTexto("Correo: " + js.optString("correoEm"))
            prn.agregarSalto()
            prn.setFontB()
            prn.alineadoIzquierda()
            prn.LineasIgualTexto(js.optString("tipoVentaRest"))
            prn.agregarSalto()
            if (js.has("salonRest")) {
                if (js.optString("salonRest") != "null") prn.agregarTexto("Zona: " + js.optString("salonRest"))
            }
            if (js.has("numMesaRest")) {
                if (js.optString("numMesaRest") != "null") prn.agregarTexto(
                    "Mesa: " + js.optString(
                        "numMesaRest"
                    )
                )
            }
            prn.agregarTexto("Pedido Nº: " + js.optString("numPedidoRest"))
            prn.agregarTexto("Fecha: " + js.optString("fechaEmisionOv"))
            prn.agregarTexto("Atendido por: " + js.optString("atendidoPorRest"))
            prn.agregarSalto()
            prn.LineasIgualTexto("Datos del Cliente")
            prn.agregarSalto()
            prn.LineaGuionTexto("Nombre: ")
            prn.agregarSalto()
            prn.LineaGuionTexto("Identificación: ")
            prn.agregarSalto()
            prn.LineaGuionTexto("Correo: ")
            prn.agregarSalto()
            prn.LineaGuionTexto("Teléfono: ")
            prn.agregarSalto()
            prn.LineaGuionTexto("Dirección: ")
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cant Descripción")
            prn.agregarCaracteres(caracteres - 26, "")
            prn.escribirTextoSinSalto("P.U. Total")
            prn.agregarSalto()
            prn.LineasIgual()
            val x: String? = "cantidadDet"
            detalles = js.getJSONArray("listaDetalleOrdenVenta")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.agregarTextoDosLineas(
                    prn.lineaVentaDoble(
                        jo.optString("cantidadDet"),
                        jo.optString("descripcionDet"),
                        jo.optString("precioUnitarioDet"),
                        jo.optString("precioTotalDet"),
                        jo.getJSONArray("detalleImpuesto"),
                        caracteres
                    )
                )
            }
            prn.LineasIgual()
            prn.alineadoDerecha()
            prn.setFontA()
            if (js.getDouble("descuentoOv") > 0) {
                prn.escribirTextoSinSalto("Descuentos:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("descuentoOv")))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Subtotal:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subTotalOv")))
            prn.agregarSalto()
            detalles = js.getJSONArray("listaImpuesto")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.escribirTextoSinSalto("Subtotal " + jo.optString("porcentajeIm") + "%:")
                prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("subTotalIm")))
                prn.agregarSalto()
            }
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                if (jo.getDouble("porcentajeIm") > 0) {
                    prn.escribirTextoSinSalto(jo.optString("nombreIm") + " " + jo.optString("porcentajeIm") + "%:")
                    prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("valorIm")))
                    prn.agregarSalto()
                }
            }
            //agregar total
            prn.escribirTextoSinSalto("Total:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalPagarOv")))
            prn.agregarSalto()
            prn.setFontB()
            prn.agregarSalto()
            prn.LineasGuion()
            if (js.has("observacionOv")) {
                if (js["observacionOv"].toString() !== "null") {
                    prn.agregarTexto("Observaciones:")
                    prn.agregarTexto(js.optString("observacionOv"))
                }
            }
            prn.agregarSalto()
            prn.feedFinal()
            prn.alineadoIzquierda()
            prn.cortar()
            enviarImprimir(prn.getTrabajo())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun imprimirRecibo(js: JSONObject?, copias: Int, caracteres: Int, programa: String) {
        try {
            if (js == null) return

            // reutilizables
            var detalles: JSONArray
            var items: Int
            var jo: JSONObject

            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.iniciar()
            prn.setFontB()
            prn.lineHeight2()
            prn.alineadoCentro()
            //prn.lineHeight();
            prn.agregarTexto("**** Documento sin validez tributaria ****")
            prn.agregarSalto()
            prn.setFontA()
            prn.negritaOff()
            prn.escribirTextoSinSalto(js.optString("razonSociallEm"))
            prn.agregarSalto()
            if (js.optString("nombreComercialEm") != "null") {
                prn.escribirTextoSinSalto(js.optString("nombreComercialEm"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("RUC: " + js.optString("rucEm"))
            prn.agregarSalto()
            if (js.optBoolean("obligadoLlevarContabilidadEm")) prn.agregarTexto("Obligado a llevar Contabilidad")
            prn.setFontB()
            prn.agregarTexto("Dirección: " + js.optString("direccionEm"))
            if (js.optString("direccionSucursalEm") != "S/N") prn.agregarTexto(
                "Sucursal: " + js.optString(
                    "direccionSucursalEm"
                )
            )
            if (js.optString("telefonoEm") != "null") prn.agregarTexto("Teléfono: " + js.optString("telefonoEm"))
            if (js.optString("correoEm") != "null") prn.agregarTexto("Correo: " + js.optString("correoEm"))
            prn.agregarSalto()
            prn.alineadoIzquierda()
            if (programa == "RESTAURANTES") prn.LineasIgualTexto(" PEDIDO " + js.optString("numPedidoRest") + " ")
            prn.escribirTextoSinSalto("Trans Nº: ")
            prn.escribirTextoSinSalto(js.optString("numeroDocumentoOv"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Fecha: ")
            prn.escribirTextoSinSalto(js.optString("fechaEmisionOv"))
            prn.agregarSalto()
            if (js.has("salonRest")) {
                if (js.optString("salonRest") != "null") prn.agregarTexto("Zona: " + js.optString("salonRest"))
            }
            if (js.has("numMesaRest")) {
                if (js.optString("numMesaRest") != "null") prn.agregarTexto(
                    "Mesa: " + js.optString(
                        "numMesaRest"
                    )
                )
            }
            prn.escribirTextoSinSalto("Cliente: ")
            if (js.optString("tipoIdentificacionCl") == "RUC") {
                var nombreComercial = ""
                if (js.optString("apellidosCl") != "null") {
                    nombreComercial = "(" + js.optString("apellidosCl") + ")"
                }
                prn.agregarCaracteres(caracteres - 9, js.optString("nombresCl") + nombreComercial)
            } else {
                prn.agregarCaracteres(
                    caracteres - 9,
                    js.optString("apellidosCl") + " " + js.optString("nombresCl")
                )
            }
            prn.agregarSalto()
            prn.escribirTextoSinSalto(js.optString("tipoIdentificacionCl") + ": ")
            prn.escribirTextoSinSalto(js.optString("identificacionCl"))
            prn.agregarSalto()
            if (js.optString("telefonoCl") != "null") prn.agregarTexto("Teléfono: " + js.optString("telefonoCl"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Dirección: ")
            prn.agregarCaracteres(caracteres * 2 - 11, js.optString("direccionFacturacionCl"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cant Descripción")
            prn.agregarCaracteres(caracteres - 26, "")
            prn.escribirTextoSinSalto("P.U. Total")
            prn.agregarSalto()
            prn.LineasIgual()
            detalles = js.getJSONArray("listaDetalleOrdenVenta")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                val pu = jo.getDouble("precioTotalImpuestoDet") / jo.getDouble("cantidadDet")
                prn.agregarTexto(
                    prn.lineaVentaDoble(
                        jo.optString("cantidadDet"),
                        jo.optString("descripcionDet"),
                        df.format(pu),
                        jo.optString("precioTotalImpuestoDet"),
                        jo.getJSONArray("detalleImpuesto"),
                        caracteres
                    )
                )
            }
            prn.LineasIgual()
            prn.alineadoDerecha()
            prn.setFontA()
            if (js.getDouble("descuentoOv") > 0) {
                prn.escribirTextoSinSalto("Descuentos:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("descuentoOv")))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Total:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalPagarOv")))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Entrega:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalEntregaOv")))
            prn.agregarSalto()
            if (js.getDouble("totalDiferenciaOv") < 0) {
                prn.escribirTextoSinSalto("Deuda:")
            } else {
                prn.escribirTextoSinSalto("Cambio:")
            }
            prn.agregarCaracteresDerecha(
                10,
                df.format(js.getDouble("totalDiferenciaOv")).replace("-", "")
            )
            prn.agregarSalto()
            prn.agregarSalto()
            prn.setFontB()
            if (js.has("listaFormaPago")) {
                if (js["listaFormaPago"].toString() !== "null") {
                    detalles = js.getJSONArray("listaFormaPago")
                    for (j in 0 until detalles.length()) {
                        jo = detalles.getJSONObject(j)
                        prn.alineadoCentro()
                        prn.escribirTextoSinSalto(
                            "Pago en " + jo.optString("nombreFp") + "= " + df.format(
                                jo.getDouble("valorFp")
                            )
                        )
                        prn.agregarSalto()
                        prn.alineadoIzquierda()
                    }
                }
            }
            if (js.has("observacionOv")) {
                if (js["observacionOv"].toString() !== "null") {
                    prn.agregarTexto("Observaciones:")
                    prn.agregarTexto(js.optString("observacionOv"))
                }
            }
            if (js.has("observacionRest")) {
                if (js["observacionRest"].toString() !== "null") {
                    prn.agregarTexto("Observaciones:")
                    prn.agregarTexto(js.optString("observacionRest"))
                }
            }
            prn.feedFinal()
            prn.cortar()
            enviarImprimir(prn.getTrabajo())
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }


    fun imprimirComandas(js: JSONObject?, copias: Int, caracteres: Int, lugar: String?) {
        try {
            if (js == null) return

            // reutilizables
            val detalles: JSONArray
            var items: Int
            var jo: JSONObject

            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.iniciar()
            prn.setFontB()
            prn.lineHeight2()
            prn.alineadoCentro()
            prn.dobleAnchoOn()
            prn.agregarTexto("PEDIDO " + js.optString("numeroPed"))
            prn.agregarTexto(js.optString("areaImpresionPed") + " (" + js.optString("numeroCom") + ")")
            prn.agregarSalto()
            prn.dobleAnchoOff()
            prn.setFontA()
            prn.alineadoIzquierda()
            prn.escribirTextoSinSalto("Atendido por: ")
            prn.escribirTextoSinSalto(js.optString("usuarioAtencionPed"))
            prn.agregarSalto()
            if (js.optString("zonaPed") != "null") prn.agregarTexto("Zona: " + js.optString("zonaPed"))
            if (js.optString("numeroMesaPed") != "null") prn.agregarTexto("Mesa: " + js.optString("numeroMesaPed"))
            prn.escribirTextoSinSalto("Fecha y hora: ")
            prn.escribirTextoSinSalto(js.optString("fechaPed"))
            prn.agregarSalto()
            prn.setFontA()
            prn.escribirTextoSinSalto("Cant   Descripción")
            prn.agregarSalto()
            prn.LineasIgual()
            detalles = js.getJSONArray("detallePed")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.escribirTextoSinSalto(
                    jo.optString("signoPro") + jo.optString("cantidadPro") + " " + jo.optString(
                        "descripcionPro"
                    )
                )
                if (jo.optString("observacionPro") !== "null") {
                    prn.escribirTextoSinSalto(" ( " + jo.optString("observacionPro") + " )")
                }
                prn.agregarSalto()
            }
            prn.LineasIgual()
            prn.alineadoIzquierda()
            if (js.optString("observacionPed") != "null") {
                prn.agregarTexto("OBSERVACIONES:")
                prn.agregarTexto(js.optString("observacionPed"))
                prn.agregarSalto()
            }
            prn.beep1()
            prn.beep2()
            prn.negritaOn()
            prn.agregarTexto(js.optString("tipoPed"))
            prn.negritaOff()
            prn.feedFinal()
            prn.cortar()
            enviarImprimir(prn.getTrabajo())
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    fun imprimirFacturaElectronica(
        js: JSONObject?,
        copias: Int,
        caracteres: Int,
        programa: String
    ) {
        try {
            if (js == null) return

            // reutilizables
            var detalles: JSONArray
            var items: Int
            var jo: JSONObject

            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.iniciar()
            prn.lineHeight2()
            prn.alineadoCentro()
            prn.dobleAltoOn()
            //prn.lineHeight();
            prn.agregarTexto("Factura Electrónica Nº:")
            prn.escribirTextoSinSalto(js.optString("numeroDocumentoOv"))
            prn.agregarSalto()
            prn.dobleAltoOff()
            prn.setFontA()
            prn.negritaOff()
            prn.agregarTexto("Ambiente: " + js.optString("tipoAmbienteOv"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto(js.optString("razonSociallEm"))
            prn.agregarSalto()
            if (js.optString("nombreComercialEm") != "null") {
                prn.escribirTextoSinSalto(js.optString("nombreComercialEm"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("RUC: " + js.optString("rucEm"))
            prn.agregarSalto()
            if (js.optBoolean("obligadoLlevarContabilidadEm")) prn.agregarTexto("Obligado a llevar Contabilidad")
            if (js.optBoolean("regimenMicroEmpresa0v")) prn.agregarTexto("Contribuyente Régimen RIMPE")
            if (js.optBoolean("agenteRetencionOv")) prn.agregarTexto(
                "Agente de Retención " + "Nº de Resolución: " + js.optString(
                    "numeroResolucionOv"
                )
            )
            prn.setFontB()
            prn.agregarTexto("Dirección: " + js.optString("direccionEm"))
            if (js.optString("direccionSucursalEm") != "S/N") prn.agregarTexto(
                "Sucursal: " + js.optString(
                    "direccionSucursalEm"
                )
            )
            if (js.optString("telefonoEm") != "null") {
                prn.agregarTexto("Teléfono: " + js.optString("telefonoEm"))
            }
            if (js.optString("correoEm") != "null") prn.agregarTexto("Correo: " + js.optString("correoEm"))
            prn.agregarSalto()
            prn.setFontB()
            prn.alineadoIzquierda()
            if (programa == "RESTAURANTES") prn.LineasIgualTexto(" PEDIDO " + js.optString("numPedidoRest") + " ")
            prn.escribirTextoSinSalto("Fecha: ")
            prn.escribirTextoSinSalto(js.optString("fechaEmisionOv"))
            prn.agregarSalto()
            if (js.has("salonRest")) {
                if (js.optString("salonRest") != "null") prn.agregarTexto("Zona: " + js.optString("salonRest"))
            }
            if (js.has("numMesaRest")) {
                if (js.optString("numMesaRest") != "null") prn.agregarTexto(
                    "Mesa: " + js.optString(
                        "numMesaRest"
                    )
                )
            }
            prn.escribirTextoSinSalto("Cliente: ")
            if (js.optString("tipoIdentificacionCl") == "RUC") {
                var nombreComercial = ""
                if (js.optString("apellidosCl") != "null") {
                    nombreComercial = "(" + js.optString("apellidosCl") + ")"
                }
                prn.agregarCaracteres(caracteres - 9, js.optString("nombresCl") + nombreComercial)
            } else {
                prn.agregarCaracteres(
                    caracteres - 9,
                    js.optString("apellidosCl") + " " + js.optString("nombresCl")
                )
            }
            prn.agregarSalto()
            prn.escribirTextoSinSalto(js.optString("tipoIdentificacionCl") + ": ")
            prn.escribirTextoSinSalto(js.optString("identificacionCl"))
            prn.agregarSalto()
            if (js.optString("telefonoCl") != "null") {
                prn.escribirTextoSinSalto("Teléfono: ")
                prn.escribirTextoSinSalto(js.optString("telefonoCl"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Dirección: ")
            prn.escribirTextoSinSalto(js.optString("direccionFacturacionCl"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cant Descripción")
            prn.agregarCaracteres(caracteres - 26, "")
            prn.escribirTextoSinSalto("P.U. Total")
            prn.agregarSalto()
            prn.LineasIgual()
            detalles = js.getJSONArray("listaDetalleOrdenVenta")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.agregarTexto(
                    prn.lineaVentaDoble(
                        jo.optString("cantidadDet"),
                        jo.optString("descripcionDet"),
                        jo.optString("precioUnitarioDet"),
                        jo.optString("precioTotalImpuestoDet"),
                        jo.getJSONArray("detalleImpuesto"),
                        caracteres
                    )
                )
            }
            prn.LineasIgual()
            prn.alineadoDerecha()
            prn.setFontA()
            if (js.getDouble("descuentoOv") > 0) {
                prn.escribirTextoSinSalto("Descuentos:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("descuentoOv")))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Subtotal:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subTotalOv")))
            prn.agregarSalto()
            detalles = js.getJSONArray("listaImpuesto")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.escribirTextoSinSalto("Subtotal " + jo.optString("porcentajeIm") + "%:")
                prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("subTotalIm")))
                prn.agregarSalto()
            }
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                if (jo.getDouble("porcentajeIm") > 0) {
                    prn.escribirTextoSinSalto(jo.optString("nombreIm") + " " + jo.optString("porcentajeIm") + "%:")
                    prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("valorIm")))
                    prn.agregarSalto()
                }
            }
            //agregar total
            prn.escribirTextoSinSalto("Total:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalPagarOv")))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Entrega:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalEntregaOv")))
            prn.agregarSalto()
            if (js.getDouble("totalDiferenciaOv") < 0) {
                prn.escribirTextoSinSalto("Deuda:")
            } else {
                prn.escribirTextoSinSalto("Cambio:")
            }
            prn.agregarCaracteresDerecha(
                10,
                df.format(js.getDouble("totalDiferenciaOv")).replace("-", "")
            )
            prn.agregarSalto()
            prn.agregarSalto()
            prn.setFontB()
            if (js.has("listaFormaPago")) {
                if (js["listaFormaPago"].toString() !== "null") {
                    detalles = js.getJSONArray("listaFormaPago")
                    for (j in 0 until detalles.length()) {
                        jo = detalles.getJSONObject(j)
                        prn.alineadoCentro()
                        prn.escribirTextoSinSalto(
                            "Pago en " + jo.optString("nombreFp") + "= " + df.format(
                                jo.getDouble("valorFp")
                            )
                        )
                        prn.agregarSalto()
                        prn.alineadoIzquierda()
                    }
                }
            }
            if (js.has("observacionOv")) {
                if (js["observacionOv"].toString() !== "null") {
                    prn.agregarTexto("Observaciones:")
                    prn.agregarTexto(js.optString("observacionOv"))
                }
            }
            if (js.has("observacionRest")) {
                if (js["observacionRest"].toString() !== "null") {
                    prn.agregarTexto("Observaciones:")
                    prn.agregarTexto(js.optString("observacionRest"))
                }
            }
            prn.escribirTextoSinSalto("Su comprobante electrónico ha sido generado correctamente")
            if (js.has("correoCl")) {
                if (js.optString("correoCl") != "null") {
                    prn.escribirTextoSinSalto(" y ha sido enviado al correo: " + js.optString("correoCl"))
                }
            }
            prn.escribirTextoSinSalto(". Recuerde que también puede consultar su comprobante en el portal del SRI https://srienlinea.sri.gob.ec/ dentro de las próximas 24 horas con la siguiente clave de acceso:")
            prn.agregarSalto()
            prn.agregarSalto()
            prn.alineadoCentro()
            prn.setFontA()
            prn.escribirTextoSinSalto(js.optString("claveAccesoOv"))
            prn.agregarSalto()
            prn.feedFinal()
            prn.cortar()
            if (js.has("tipoVentaRest")) {
                if (js.optString("tipoVentaRest") == "PEDIDO TELEFÓNICO") {
                    prn.lineHeight2()
                    prn.setFontA()
                    prn.alineadoCentro()
                    prn.dobleAltoOn()
                    //prn.lineHeight();
                    prn.agregarTexto("Factura Electrónica Nº:")
                    prn.escribirTextoSinSalto(js.optString("numeroDocumentoOv"))
                    prn.agregarSalto()
                    prn.dobleAltoOff()
                    prn.negritaOff()
                    prn.alineadoIzquierda()
                    prn.setFontB()
                    if (programa == "RESTAURANTES") prn.LineasIgualTexto(" PEDIDO " + js.optString("numPedidoRest") + " ")
                    prn.setFontA()
                    prn.escribirTextoSinSalto("Fecha: ")
                    prn.escribirTextoSinSalto(js.optString("fechaEmisionOv"))
                    prn.agregarSalto()
                    if (js.has("salonRest")) {
                        if (js.optString("salonRest") != "null") prn.agregarTexto(
                            "Zona: " + js.optString(
                                "salonRest"
                            )
                        )
                    }
                    if (js.has("numMesaRest")) {
                        if (js.optString("numMesaRest") != "null") prn.agregarTexto(
                            "Mesa: " + js.optString(
                                "numMesaRest"
                            )
                        )
                    }
                    prn.escribirTextoSinSalto("FACTURADO A: ")
                    if (js.optString("tipoIdentificacionCl") == "RUC") {
                        var nombreComercial = ""
                        if (js.optString("apellidosCl") != "null") {
                            nombreComercial = "(" + js.optString("apellidosCl") + ")"
                        }
                        prn.agregarCaracteres(
                            caracteres - 9,
                            js.optString("nombresCl") + nombreComercial
                        )
                    } else {
                        prn.agregarCaracteres(
                            caracteres - 9,
                            js.optString("apellidosCl") + " " + js.optString("nombresCl")
                        )
                    }
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto(js.optString("tipoIdentificacionCl") + ": ")
                    prn.escribirTextoSinSalto(js.optString("identificacionCl"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Teléfono: ")
                    prn.escribirTextoSinSalto(js.optString("telefonoCl"))
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto("Dirección: ")
                    prn.escribirTextoSinSalto(js.optString("direccionFacturacionCl"))
                    prn.agregarSalto()
                    prn.alineadoCentro()
                    prn.agregarSalto()
                    prn.agregarTexto("=====================")
                    prn.dobleAltoOn()
                    prn.agregarTexto("OBSERVACIONES DE ENTREGA")
                    prn.dobleAltoOff()
                    prn.alineadoIzquierda()
                    prn.agregarSalto()
                    prn.escribirTextoSinSalto(js.optString("observacionRest"))
                    prn.agregarSalto()
                    prn.negritaOn()
                    prn.agregarTexto(js.optString("tipoEntregaRest"))
                    prn.negritaOff()
                    prn.feedFinal()
                    prn.cortar()
                }
            }
            enviarImprimir(prn.getTrabajo())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun imprimirFacturPreimpresa(
        js: JSONObject?,
        copias: Int,
        caracteres: Int,
        programa: String
    ) {
        try {
            if (js == null) return

            // reutilizables
            var detalles: JSONArray
            var items: Int
            var jo: JSONObject

            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.iniciar()
            prn.setFontB()
            prn.alineadoIzquierda()
            if (programa == "RESTAURANTES") prn.LineasIgualTexto(" PEDIDO " + js.optString("numPedidoRest") + " ")
            prn.escribirTextoSinSalto("Transacción Nº: ")
            prn.escribirTextoSinSalto(js.optString("numeroDocumentoOv"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Fecha: ")
            prn.escribirTextoSinSalto(js.optString("fechaEmisionOv"))
            prn.agregarSalto()
            if (js.has("salonRest")) {
                if (js.optString("salonRest") != "null") prn.agregarTexto("Zona: " + js.optString("salonRest"))
            }
            if (js.has("numMesaRest")) {
                if (js.optString("numMesaRest") != "null") prn.agregarTexto(
                    "Mesa: " + js.optString(
                        "numMesaRest"
                    )
                )
            }
            prn.escribirTextoSinSalto("Cliente: ")
            if (js.optString("tipoIdentificacionCl") == "RUC") {
                var nombreComercial = ""
                if (js.optString("apellidosCl") != "null") {
                    nombreComercial = "(" + js.optString("apellidosCl") + ")"
                }
                prn.agregarCaracteres(caracteres - 9, js.optString("nombresCl") + nombreComercial)
            } else {
                prn.agregarCaracteres(
                    caracteres - 9,
                    js.optString("apellidosCl") + " " + js.optString("nombresCl")
                )
            }
            prn.agregarSalto()
            prn.escribirTextoSinSalto(js.optString("tipoIdentificacionCl") + ": ")
            prn.escribirTextoSinSalto(js.optString("identificacionCl"))
            prn.agregarSalto()
            if (js.optString("telefonoCl") != "null") {
                prn.escribirTextoSinSalto("Teléfono: ")
                prn.escribirTextoSinSalto(js.optString("telefonoCl"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Dirección: ")
            prn.escribirTextoSinSalto(js.optString("direccionFacturacionCl"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cant Descripción")
            prn.agregarCaracteres(caracteres - 26, "")
            prn.escribirTextoSinSalto("P.U. Total")
            prn.agregarSalto()
            prn.LineasIgual()
            detalles = js.getJSONArray("listaDetalleOrdenVenta")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.agregarTexto(
                    prn.lineaVenta(
                        jo.optString("cantidadDet"),
                        jo.optString("descripcionDet"),
                        jo.optString("precioUnitarioDet"),
                        jo.optString("precioTotalImpuestoDet"),
                        jo.getJSONArray("detalleImpuesto"),
                        caracteres
                    )
                )
            }
            prn.LineasIgual()
            prn.alineadoDerecha()
            prn.setFontA()
            if (js.has("descuentoOv")) {
                if (js.getDouble("descuentoOv") > 0) {
                    prn.escribirTextoSinSalto("Descuentos:")
                    prn.agregarCaracteresDerecha(10, df.format(js.getDouble("descuentoOv")))
                    prn.agregarSalto()
                }
            }
            prn.escribirTextoSinSalto("Subtotal:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subTotalOv")))
            prn.agregarSalto()
            detalles = js.getJSONArray("listaImpuesto")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.escribirTextoSinSalto("Subtotal " + jo.optString("porcentajeIm") + "%:")
                prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("subTotalIm")))
                prn.agregarSalto()
            }
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                if (jo.getDouble("porcentajeIm") > 0) {
                    prn.escribirTextoSinSalto(jo.optString("nombreIm") + " " + jo.optString("porcentajeIm") + "%:")
                    prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("valorIm")))
                    prn.agregarSalto()
                }
            }
            //agregar total
            prn.escribirTextoSinSalto("Total:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalPagarOv")))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Entrega:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalEntregaOv")))
            prn.agregarSalto()
            if (js.getDouble("totalDiferenciaOv") < 0) {
                prn.escribirTextoSinSalto("Deuda:")
            } else {
                prn.escribirTextoSinSalto("Cambio:")
            }
            prn.agregarCaracteresDerecha(
                10,
                df.format(js.getDouble("totalDiferenciaOv")).replace("-", "")
            )
            prn.agregarSalto()
            prn.agregarSalto()
            prn.setFontB()
            detalles = js.getJSONArray("listaFormaPago")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.alineadoCentro()
                prn.escribirTextoSinSalto(
                    "Pago en " + jo.optString("nombreFp") + "= " + df.format(
                        jo.getDouble("valorFp")
                    )
                )
                prn.agregarSalto()
                prn.alineadoIzquierda()
            }
            if (js.has("observacionOv")) {
                if (js.optString("observacionOv") != "null") {
                    prn.agregarTexto("Observaciones:")
                    prn.agregarTexto(js.optString("observacionOv"))
                }
            }
            prn.escribirTextoSinSalto("Su comprobante electrónico ha sido generado correctamente")
            if (js.optString("correoCl") != "null") {
                prn.escribirTextoSinSalto(" y ha sido enviado al correo: " + js.optString("correoCl"))
            }
            prn.escribirTextoSinSalto(". Recuerde que también puede consultar su comprobante en el portal del SRI https://srienlinea.sri.gob.ec/ dentro de las próximas 24 horas con la siguiente clave de acceso:")
            prn.agregarSalto()
            prn.agregarSalto()
            prn.alineadoCentro()
            prn.setFontA()
            prn.escribirTextoSinSalto(js.optString("claveAccesoOv"))
            prn.agregarSalto()
            prn.feedFinal()
            prn.cortar()
            if (js.optString("tipoVentaRest") == "PEDIDO TELEFÓNICO") {
                prn.lineHeight2()
                prn.setFontA()
                prn.alineadoCentro()
                prn.dobleAltoOn()
                //prn.lineHeight();
                prn.agregarTexto("Factura Electrónica Nº:")
                prn.escribirTextoSinSalto(js.optString("numeroDocumentoOv"))
                prn.agregarSalto()
                prn.dobleAltoOff()
                prn.negritaOff()
                prn.alineadoIzquierda()
                prn.setFontB()
                if (programa == "RESTAURANTES") prn.LineasIgualTexto(" PEDIDO " + js.optString("numPedidoRest") + " ")
                prn.setFontA()
                prn.escribirTextoSinSalto("Fecha: ")
                prn.escribirTextoSinSalto(js.optString("fechaEmisionOv"))
                prn.agregarSalto()
                if (js.has("salonRest")) {
                    if (js.optString("salonRest") != "null") prn.agregarTexto(
                        "Zona: " + js.optString(
                            "salonRest"
                        )
                    )
                }
                if (js.has("numMesaRest")) {
                    if (js.optString("numMesaRest") != "null") prn.agregarTexto(
                        "Mesa: " + js.optString(
                            "numMesaRest"
                        )
                    )
                }
                prn.escribirTextoSinSalto("FACTURADO A: ")
                if (js.optString("tipoIdentificacionCl") == "RUC") {
                    var nombreComercial = ""
                    if (js.optString("apellidosCl") != "null") {
                        nombreComercial = "(" + js.optString("apellidosCl") + ")"
                    }
                    prn.agregarCaracteres(
                        caracteres - 9,
                        js.optString("nombresCl") + nombreComercial
                    )
                } else {
                    prn.agregarCaracteres(
                        caracteres - 9,
                        js.optString("apellidosCl") + " " + js.optString("nombresCl")
                    )
                }
                prn.agregarSalto()
                prn.escribirTextoSinSalto(js.optString("tipoIdentificacionCl") + ": ")
                prn.escribirTextoSinSalto(js.optString("identificacionCl"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Teléfono: ")
                prn.escribirTextoSinSalto(js.optString("telefonoCl"))
                prn.agregarSalto()
                prn.escribirTextoSinSalto("Dirección: ")
                prn.escribirTextoSinSalto(js.optString("direccionFacturacionCl"))
                prn.agregarSalto()
                prn.alineadoCentro()
                prn.agregarSalto()
                prn.agregarTexto("=====================")
                prn.dobleAltoOn()
                prn.agregarTexto("OBSERVACIONES DE ENTREGA")
                prn.dobleAltoOff()
                prn.alineadoIzquierda()
                prn.agregarSalto()
                prn.escribirTextoSinSalto(js.optString("observacionRest"))
                prn.agregarSalto()
                prn.negritaOn()
                prn.agregarTexto(js.optString("tipoEntregaRest"))
                prn.negritaOff()
                prn.feedFinal()
                prn.cortar()
            }
            enviarImprimir(prn.getTrabajo())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun imprimirCotizacionResumida(js: JSONObject?, copias: Int, caracteres: Int) {
        try {
            if (js == null) return

            // reutilizables
            var detalles: JSONArray
            var items: Int
            var jo: JSONObject

            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.lineHeight2()
            prn.alineadoCentro()
            prn.dobleAltoOn()
            //prn.lineHeight();
            prn.agregarTexto("Cotización")
            prn.agregarSalto()
            prn.dobleAltoOff()
            prn.setFontA()
            prn.negritaOff()
            prn.escribirTextoSinSalto(js.optString("razonSociallEm"))
            prn.agregarSalto()
            if (js.optString("nombreComercialEm") != "null") {
                prn.escribirTextoSinSalto(js.optString("nombreComercialEm"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("RUC: " + js.optString("rucEm"))
            prn.agregarSalto()
            if (js.getBoolean("obligadoLlevarContabilidadEm")) prn.agregarTexto("Obligado a llevar Contabilidad")
            prn.setFontB()
            prn.agregarTexto("Dirección: " + js.optString("direccionEm"))
            if (js.optString("direccionSucursalEm") != "S/N") prn.agregarTexto(
                "Sucursal: " + js.optString(
                    "direccionSucursalEm"
                )
            )
            if (js.optString("telefonoEm") != "null") prn.agregarTexto("Teléfono: " + js.optString("telefonoEm"))
            if (js.optString("correoEm") != "null") prn.agregarTexto("Correo: " + js.optString("correoEm"))
            prn.agregarSalto()
            prn.alineadoIzquierda()
            prn.escribirTextoSinSalto("Trans Nº: ")
            prn.escribirTextoSinSalto(js.optString("numeroDocumentoOv"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Fecha: ")
            prn.escribirTextoSinSalto(js.optString("fechaEmisionOv"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cliente: ")
            if (js.optString("tipoIdentificacionCl") == "RUC") {
                var nombreComercial = ""
                if (js.optString("apellidosCl") != "null") {
                    nombreComercial = "(" + js.optString("apellidosCl") + ")"
                }
                prn.agregarCaracteres(caracteres - 9, js.optString("nombresCl") + nombreComercial)
            } else {
                prn.agregarCaracteres(
                    caracteres - 9,
                    js.optString("apellidosCl") + " " + js.optString("nombresCl")
                )
            }
            prn.agregarSalto()
            prn.escribirTextoSinSalto(js.optString("tipoIdentificacionCl") + ": ")
            prn.escribirTextoSinSalto(js.optString("identificacionCl"))
            prn.agregarSalto()
            if (js.optString("telefonoCl") != "null") {
                prn.escribirTextoSinSalto("Teléfono: ")
                prn.escribirTextoSinSalto(js.optString("telefonoCl"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Dirección: ")
            prn.escribirTextoSinSalto(js.optString("direccionFacturacionCl"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cant Descripción")
            prn.agregarCaracteres(caracteres - 26, "")
            prn.escribirTextoSinSalto("P.U. Total")
            prn.agregarSalto()
            prn.LineasIgual()
            detalles = js.getJSONArray("listaDetalleOrdenVenta")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.agregarTexto(
                    prn.lineaVentaDobleResumida(
                        jo.optString("cantidadDet"),
                        jo.optString("descripcionDet"),
                        jo.optString("precioUnitarioDet"),
                        jo.optString("precioTotalDet"),
                        jo.getJSONArray("detalleImpuesto"),
                        caracteres
                    )
                )
            }
            prn.LineasIgual()
            prn.alineadoDerecha()
            prn.setFontA()
            if (js.getDouble("descuentoOv") > 0) {
                prn.escribirTextoSinSalto("Descuentos:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("descuentoOv")))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Subtotal:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subTotalOv")))
            prn.agregarSalto()
            detalles = js.getJSONArray("listaImpuesto")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.escribirTextoSinSalto("Subtotal " + jo.optString("porcentajeIm") + "%:")
                prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("subTotalIm")))
                prn.agregarSalto()
            }
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                if (jo.getDouble("porcentajeIm") > 0) {
                    prn.escribirTextoSinSalto(jo.optString("nombreIm") + " " + jo.optString("porcentajeIm") + "%:")
                    prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("valorIm")))
                    prn.agregarSalto()
                }
            }
            //agregar total
            prn.escribirTextoSinSalto("Total:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalPagarOv")))
            prn.agregarSalto()
            prn.setFontB()
            prn.alineadoIzquierda()
            if (js.has("observacionOv")) {
                if (js["observacionOv"].toString() !== "null") {
                    prn.agregarTexto("Observaciones:")
                    prn.agregarTexto(js.optString("observacionOv"))
                }
            }
            prn.agregarSalto()
            prn.feedFinal()
            prn.cortar()
            enviarImprimir(prn.getTrabajo())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun imprimirCotizacionDetallada(js: JSONObject?, copias: Int, caracteres: Int) {
        try {
            if (js == null) return

            // reutilizables
            var detalles: JSONArray
            var items: Int
            var jo: JSONObject

            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.lineHeight2()
            prn.alineadoCentro()
            prn.dobleAltoOn()
            //prn.lineHeight();
            prn.agregarTexto("Cotización")
            prn.agregarSalto()
            prn.dobleAltoOff()
            prn.setFontA()
            prn.negritaOff()
            prn.escribirTextoSinSalto(js.optString("razonSociallEm"))
            prn.agregarSalto()
            if (js.optString("nombreComercialEm") != "null") {
                prn.escribirTextoSinSalto(js.optString("nombreComercialEm"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("RUC: " + js.optString("rucEm"))
            prn.agregarSalto()
            if (js.getBoolean("obligadoLlevarContabilidadEm")) prn.agregarTexto("Obligado a llevar Contabilidad")
            prn.setFontB()
            prn.agregarTexto("Dirección: " + js.optString("direccionEm"))
            if (js.optString("direccionSucursalEm") != "S/N") prn.agregarTexto(
                "Sucursal: " + js.optString(
                    "direccionSucursalEm"
                )
            )
            if (js.optString("telefonoEm") != "null") prn.agregarTexto("Teléfono: " + js.optString("telefonoEm"))
            if (js.optString("correoEm") != "null") prn.agregarTexto("Correo: " + js.optString("correoEm"))
            prn.agregarSalto()
            prn.alineadoIzquierda()
            prn.escribirTextoSinSalto("Trans Nº: ")
            prn.escribirTextoSinSalto(js.optString("numeroDocumentoOv"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Fecha: ")
            prn.escribirTextoSinSalto(js.optString("fechaEmisionOv"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cliente: ")
            if (js.optString("tipoIdentificacionCl") == "RUC") {
                var nombreComercial = ""
                if (js.optString("apellidosCl") != "null") {
                    nombreComercial = "(" + js.optString("apellidosCl") + ")"
                }
                prn.agregarCaracteres(caracteres - 9, js.optString("nombresCl") + nombreComercial)
            } else {
                prn.agregarCaracteres(
                    caracteres - 9,
                    js.optString("apellidosCl") + " " + js.optString("nombresCl")
                )
            }
            prn.agregarSalto()
            prn.escribirTextoSinSalto(js.optString("tipoIdentificacionCl") + ": ")
            prn.escribirTextoSinSalto(js.optString("identificacionCl"))
            prn.agregarSalto()
            if (js.optString("telefonoCl") != "null") {
                prn.escribirTextoSinSalto("Teléfono: ")
                prn.escribirTextoSinSalto(js.optString("telefonoCl"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Dirección: ")
            prn.escribirTextoSinSalto(js.optString("direccionFacturacionCl"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Cant Descripción")
            prn.agregarCaracteres(caracteres - 26, "")
            prn.escribirTextoSinSalto("P.U. Total")
            prn.agregarSalto()
            prn.LineasIgual()
            detalles = js.getJSONArray("listaDetalleOrdenVenta")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.agregarTexto(
                    prn.lineaVentaDoble(
                        jo.optString("cantidadDet"),
                        jo.optString("descripcionDet"),
                        jo.optString("precioUnitarioDet"),
                        jo.optString("precioTotalDet"),
                        jo.getJSONArray("detalleImpuesto"),
                        caracteres
                    )
                )
            }
            prn.LineasIgual()
            prn.alineadoDerecha()
            prn.setFontA()
            if (js.getDouble("descuentoOv") > 0) {
                prn.escribirTextoSinSalto("Descuentos:")
                prn.agregarCaracteresDerecha(10, df.format(js.getDouble("descuentoOv")))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("Subtotal:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("subTotalOv")))
            prn.agregarSalto()
            detalles = js.getJSONArray("listaImpuesto")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.escribirTextoSinSalto("Subtotal " + jo.optString("porcentajeIm") + "%:")
                prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("subTotalIm")))
                prn.agregarSalto()
            }
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                if (jo.getDouble("porcentajeIm") > 0) {
                    prn.escribirTextoSinSalto(jo.optString("nombreIm") + " " + jo.optString("porcentajeIm") + "%:")
                    prn.agregarCaracteresDerecha(10, df.format(jo.getDouble("valorIm")))
                    prn.agregarSalto()
                }
            }
            //agregar total
            prn.escribirTextoSinSalto("Total:")
            prn.agregarCaracteresDerecha(10, df.format(js.getDouble("totalPagarOv")))
            prn.agregarSalto()
            prn.setFontB()
            prn.alineadoIzquierda()
            if (js.has("observacionOv")) {
                if (js["observacionOv"].toString() !== "null") {
                    prn.agregarTexto("Observaciones:")
                    prn.agregarTexto(js.optString("observacionOv"))
                }
            }
            prn.agregarSalto()
            prn.feedFinal()
            prn.cortar()
            enviarImprimir(prn.getTrabajo())
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }


    // FUNCIONES
    fun imprimirCierreCaja(js: JSONObject?, copias: Int, caracteres: Int) {
        try {
            if (js == null) return

            // reutilizables
            var detalles: JSONArray
            var items: Int
            var jo: JSONObject

            // imprimir
            val prn = PrinterHelpers(caracteres, copias)
            prn.iniciar()
            prn.setFontB()
            prn.agregarSalto()
            //Linea 1 numero de Factura y Fecha
            prn.negritaOff()
            prn.alineadoCentro()
            prn.lineHeight2()
            prn.dobleAltoOn()
            prn.dobleAnchoOn()
            prn.escribirTextoSinSalto("Cierre de Caja")
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Nº: " + js.optString("numeroSec"))
            prn.agregarSalto()
            prn.dobleAnchoOff()
            prn.dobleAltoOff()
            prn.agregarSalto()
            prn.setFontA()
            prn.negritaOff()
            prn.escribirTextoSinSalto(js.optString("razonSocialEmp"))
            prn.agregarSalto()
            if (js.optString("nombreComercialEmp") != "null") {
                prn.escribirTextoSinSalto(js.optString("nombreComercialEmp"))
                prn.agregarSalto()
            }
            prn.escribirTextoSinSalto("RUC: " + js.optString("rucEmp"))
            prn.agregarSalto()
            prn.agregarTexto("Dirección: " + js.optString("direccionSuc"))
            prn.agregarSalto()
            prn.setFontB()
            prn.alineadoIzquierda()
            prn.escribirTextoSinSalto("CAJA: " + js.optString("nombreCaj"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Responsable: ")
            prn.escribirTextoSinSalto(js.optString("responsableCaj"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Fecha Apertura: ")
            prn.escribirTextoSinSalto(js.optString("fechaApertura"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Fecha Cierre: ")
            prn.escribirTextoSinSalto(js.optString("fechaCierre"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Nº Ventas Realizadas: ")
            prn.escribirTextoSinSalto(js.optString("totalVentasRealizadas"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Nº Ventas Anuladas: ")
            prn.escribirTextoSinSalto(js.optString("totalVentasAnuladas"))
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Valor de Apertura: ")
            prn.escribirTextoSinSalto(js.optString("totalApertura"))
            prn.agregarSalto()
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Detalle de Ingresos:")
            prn.agregarSalto()
            prn.LineasIgual()
            prn.escribirTextoSinSalto("Cant  F. Pago")
            prn.agregarCaracteres(caracteres - 18, "")
            prn.escribirTextoSinSalto("Total")
            prn.agregarSalto()
            prn.LineasIgual()
            detalles = js.getJSONArray("listaIngresos")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.agregarTexto(
                    prn.lineaCierre(
                        jo.optString("cantidad"),
                        jo.optString("formaPago"),
                        jo.optString("total"),
                        caracteres
                    )
                )
            }
            prn.LineasIgual()
            prn.agregarSalto()
            prn.escribirTextoSinSalto("Detalle de Egresos:")
            prn.agregarSalto()
            prn.LineasIgual()
            prn.escribirTextoSinSalto("Cant  F. Pago")
            prn.agregarCaracteres(caracteres - 18, "")
            prn.escribirTextoSinSalto("Total")
            prn.agregarSalto()
            prn.LineasIgual()
            detalles = js.getJSONArray("listaEgresos")
            for (j in 0 until detalles.length()) {
                jo = detalles.getJSONObject(j)
                prn.agregarTexto(
                    prn.lineaCierre(
                        jo.optString("cantidad"),
                        jo.optString("formaPago"),
                        jo.optString("total"),
                        caracteres
                    )
                )
            }
            prn.LineasIgual()
            prn.alineadoDerecha()
            var resultado = BigDecimal(js.optString("totalSobranteFaltante"))
            val totalEfectivoSistema =
                resultado.multiply(BigDecimal("-1")).add(BigDecimal(js.optString("totalContado")))
                    .setScale(2, BigDecimal.ROUND_HALF_UP)
            //agregar total
            prn.escribirTextoSinSalto("(+)Total Ingresos:")
            val ingresos =
                BigDecimal(js.optString("totalIgresos")).subtract(BigDecimal(js.optString("totalApertura")))
                    .setScale(2, BigDecimal.ROUND_HALF_UP)
            prn.agregarCaracteresDerecha(10, ingresos.toString())
            prn.agregarSalto()
            //agregar total
            prn.escribirTextoSinSalto("(+)Total Apertura:")
            val totalApertura = BigDecimal(js.optString("totalApertura"))
            prn.agregarCaracteresDerecha(10, totalApertura.toString())
            prn.agregarSalto()
            //agregar total
            prn.escribirTextoSinSalto("(-)Total Egresos:")
            val totalEgresos = BigDecimal(js.optString("totalEgresos"))
            prn.agregarCaracteresDerecha(10, totalEgresos.toString())
            prn.agregarSalto()
            //agregar total
            prn.escribirTextoSinSalto("(a)Total Efectivo Sistema:")
            prn.agregarCaracteresDerecha(10, totalEfectivoSistema.toString())
            prn.agregarSalto()
            //agregar total
            prn.escribirTextoSinSalto("(b)Total Efectivo Contado:")
            val totalContado = BigDecimal(js.optString("totalContado"))
            prn.agregarCaracteresDerecha(10, totalContado.toString())
            prn.agregarSalto()
            prn.alineadoIzquierda()
            if (resultado.compareTo(BigDecimal.ZERO) == 0) {
                prn.escribirTextoSinSalto("El valor del Dinero en EFECTIVO DEL SISTEMA (a) es igual al valor del Dinero en EFECTIVO CONTADO (b)")
                prn.agregarSalto()
            } else {
                prn.escribirTextoSinSalto("El valor del Dinero en EFECTIVO DEL SISTEMA (a) NO es igual al valor del Dinero en EFECTIVO CONTADO (b)")
                prn.agregarSalto()
                prn.agregarSalto()
                prn.negritaOn()
                prn.alineadoCentro()
                if (resultado.compareTo(BigDecimal.ZERO) == 1) {
                    prn.escribirTextoSinSalto("SOBRANTE: ")
                } else {
                    prn.escribirTextoSinSalto("FALTANTE: ")
                    resultado = resultado.multiply(BigDecimal("-1"))
                }
                prn.escribirTextoSinSalto(resultado.toString() + "")
                prn.negritaOff()
            }
            prn.agregarSalto()
            prn.agregarSalto()
            prn.agregarSalto()
            prn.agregarSalto()
            prn.escribirTextoSinSalto("_______________")
            prn.agregarSalto()
            prn.escribirTextoSinSalto(js.optString("responsableCaj"))
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

    fun imprimirTest(tipoTransaccion: String) {
        val prn = PrinterHelpers(100, 1)
        prn.iniciar()
        prn.setFontA()
        prn.lineHeight2()
        prn.negritaOff()
        prn.alineadoIzquierda()
        prn.agregarTexto("font A")
        prn.agregarSalto()
        prn.agregarTexto("000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-")
        prn.agregarSalto()
        prn.setFontB()
        prn.agregarTexto("font B")
        prn.agregarSalto()
        prn.agregarTexto("000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-000000000-")
        prn.agregarSalto()
        prn.alineadoCentro()
        prn.setFontA()
        prn.agregarTexto("************************************")
        prn.agregarTexto("Cuente los caracteres que entran en la primera línea de texto y coloque el valor en la columa correspondiente")
        prn.agregarTexto("************************************")
        prn.agregarSalto()
        prn.agregarTexto(tipoTransaccion)
        prn.feedFinal()
        prn.cortar()
        enviarImprimir(prn.getTrabajo())
    }

    fun enviarImprimir(trabajo: String) {
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
        //                streamBluetooth!!.write(trabajo.toByteArray(charset("ISO-8859-1")))
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