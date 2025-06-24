package com.example.printerswanqaratest.core.print.utils.printer1

import android.content.Context
import com.example.printerswanqaratest.core.printType.PrinterType
import com.example.printerswanqaratest.domain.models.Printers
import com.example.printerswanqaratest.api.sales.SalesService
import com.example.printerswanqaratest.api.sales.SalesApiResponse
import com.example.printerswanqaratest.api.ApiClient
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

class Discrimination(
    private val allPrinters: List<Printers>,
    private val systemType: String,
    private val context: Context
) {
    private val urlVentasComercios = "/appWs/servicio/ordenVenta?idOrdenVenta="
    private val urlVentasRestaurantes = "/appWs/servicio/ventaPedido?idPedido="
    private val urlComandas = "/appWs/servicio/comanda?idComanda="
    private val urlPretickets = "/appWs/servicio/preTicketPedido?idPedido="
    private val urlCotizacionesComercios = "/appWs/servicio/cotizacion?idCotizacion="
    private val urlCierreCajaComercios = "/appWs/servicio/cierreCaja?idCierreCaja="

    private var printer: Printers? = null
    private var printerBuilder: PrinterBuilder? = null

    private val salesService: SalesService = ApiClient.createSalesService(context)

    private fun setup(document: String , commands: Array<String>) {
        printer = allPrinters.find {
            it.documentType == document
        }
        if (printer != null) {
            when (printer!!.type) {
                PrinterType.WIFI.type -> {
                    printerBuilder = PrinterBuilder(PrinterType.WIFI.type)
                    printerBuilder!!.InicializarImpresoraRed(
                        printer!!.address,
                        printer!!.port!!
                    )
                }
                PrinterType.BLUETOOTH.type -> {
                    printerBuilder = PrinterBuilder(PrinterType.BLUETOOTH.type)
                    printerBuilder!!.InicializarImpresoraBluetooth(printer!!.address!!)
                }
                else -> {
                    printerBuilder = PrinterBuilder(PrinterType.USB.type)
                    printerBuilder!!.InintUsbPrinter(context)
                }
            }
        }else{
            printerBuilder = null
        }
    }

    operator fun invoke(commands: Array<String>): Boolean {
        var jsonObject: JSONObject?
        var errorCount = 0
        var errorCommand: Array<String> = arrayOf()
        for (command in commands) {
            val parts = command.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            setup(parts[0],commands)
            when (parts[0]) {
                "IMPRESION_RECIBO" -> when (parts[2]) {
                    "COMERCIOS" -> {
                        val jsonObject = runBlocking {
                            val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                            org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                        }
                        if (printerBuilder != null) {
                            printerBuilder!!.imprimirRecibo(
                                jsonObject,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                                parts[2]
                            )
                        } else {
                            errorCount++
                            if (!errorCommand.contains(command)) errorCommand =
                                errorCommand.plus(command)
                        }
                    }

                    "RESTAURANTES" -> {
                        jsonObject = runBlocking {
                            val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                            org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                        }
                        if (printerBuilder != null) {
                            printerBuilder!!.imprimirRecibo(
                                jsonObject,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                                parts[2]
                            )
                        } else {
                            errorCount++
                            if (!errorCommand.contains(command)) errorCommand =
                                errorCommand.plus(command)
                        }
                    }
                }

                "IMPRESION_PRE_TICKET" -> {
                    jsonObject = runBlocking {
                        val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                        org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                    }
                    if (printerBuilder != null) {
                        printerBuilder!!.imprimirPreticket(
                            jsonObject,
                            printer!!.copyNumber,
                            printer!!.charactersNumber
                        )
                    } else {
                        errorCount++
                        if (!errorCommand.contains(command)) errorCommand =
                            errorCommand.plus(command)
                    }
                }

                "IMPRESION_COMANDA" -> {
                    jsonObject = runBlocking {
                        val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                        org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                    }
                    val area = jsonObject?.optString("areaImpresionPed")
                    setup("IMPRESION_COMANDA:$area",commands)
                    if (printerBuilder != null) {
                        printerBuilder!!.imprimirComandas(
                            jsonObject,
                            printer!!.copyNumber,
                            printer!!.charactersNumber,
                            area
                        )
                    } else {
                        errorCount++
                        if (!errorCommand.contains(command)) errorCommand =
                            errorCommand.plus(command)
                    }
                }

                "IMPRESION_FACTURA_PRE_IMPRESA" -> when (parts[2]) {
                    "COMERCIOS" -> {
                        jsonObject = runBlocking {
                            val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                            org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                        }
                        if (printerBuilder != null) {
                            printerBuilder!!.imprimirFacturPreimpresa(
                                jsonObject,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                                parts[2]
                            )
                        } else {
                            errorCount++
                            if (!errorCommand.contains(command)) errorCommand =
                                errorCommand.plus(command)
                        }
                    }

                    "RESTAURANTES" -> {
                        jsonObject = runBlocking {
                            val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                            org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                        }
                        if (printerBuilder != null) {
                            printerBuilder!!.imprimirFacturPreimpresa(
                                jsonObject,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                                parts[2]
                            )
                        } else {
                            errorCount++
                            if (!errorCommand.contains(command)) errorCommand =
                                errorCommand.plus(command)
                        }
                    }
                }

                "IMPRESION_FACTURA_ELECTRONICA" -> when (parts[2]) {
                    "COMERCIOS" -> {
                        jsonObject = runBlocking {
                            val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                            org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                        }
                        if (printerBuilder != null) {
                            printerBuilder!!.imprimirFacturaElectronica(
                                jsonObject,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                                "COMERCIOS"
                            )
                        } else {
                            errorCount++
                            if (!errorCommand.contains(command)) errorCommand =
                                errorCommand.plus(command)
                        }
                    }

                    "RESTAURANTES" -> {
                        jsonObject = runBlocking {
                            val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                            org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                        }
                        if (printerBuilder != null) {
                            printerBuilder!!.imprimirFacturaElectronica(
                                jsonObject,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                                parts[2]
                            )
                            printerBuilder!!.cerrarConexion()
                        } else {
                            errorCount++
                            if (!errorCommand.contains(command)) errorCommand =
                                errorCommand.plus(command)
                        }
                    }
                }

                "IMPRESION_CIERRE_CAJA" -> {

                    val jsonObject = runBlocking {
                        val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                        org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                    }
                    if (printerBuilder != null) {
                        printerBuilder!!.imprimirCierreCaja(
                            jsonObject,
                            printer!!.copyNumber,
                            printer!!.charactersNumber
                        )
                        printerBuilder!!.cerrarConexion()
                    } else {
                        errorCount++
                        if (!errorCommand.contains(command)) errorCommand =
                            errorCommand.plus(command)
                    }
                }

                "IMPRESION_COTIZACION_DETALLADA" -> {

                    val jsonObject = runBlocking {
                        val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                        org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                    }
                    if (printerBuilder != null) {
                        printerBuilder!!.imprimirCotizacionDetallada(
                            jsonObject,
                            printer!!.copyNumber,
                            printer!!.charactersNumber
                        )
                        printerBuilder!!.cerrarConexion()
                    } else {
                        errorCount++
                        if (!errorCommand.contains(command)) errorCommand =
                            errorCommand.plus(command)
                    }
                }

                "IMPRESION_COTIZACION_RESUMIDA" -> {

                    val jsonObject = runBlocking {
                        val response: SalesApiResponse = salesService.getSalesById(parts[1], include = "user")
                        org.json.JSONObject(com.google.gson.Gson().toJson(response.data))
                    }
                    if (printerBuilder != null) {
                        printerBuilder!!.imprimirCotizacionResumida(
                            jsonObject,
                            printer!!.copyNumber,
                            printer!!.charactersNumber
                        )
                        printerBuilder!!.cerrarConexion()
                    } else {
                        errorCount++
                        if (!errorCommand.contains(command)) errorCommand =
                            errorCommand.plus(command)
                    }
                }

                "APERTURA_GAVETA" -> {
                    if (printerBuilder != null) {
                        printerBuilder!!.abrirGaveta()
                        printerBuilder!!.cerrarConexion()
                    } else {
                        errorCount++
                        if (!errorCommand.contains(command)) errorCommand =
                            errorCommand.plus(command)
                    }
                }
            }
        }
        if (errorCommand.size > 0) {
            var possibleSet =
                context.getSharedPreferences("asd", 0).getStringSet("Commands", setOf())
            possibleSet
            context.getSharedPreferences("asd", 0).edit().putStringSet("Commands", setOf()).apply()
            possibleSet =
                context.getSharedPreferences("asd", 0).getStringSet("Commands", setOf())
            possibleSet
            context.getSharedPreferences("asd", 0).edit()
                .putStringSet("Commands", errorCommand.toSet()).apply()
            possibleSet =
                context.getSharedPreferences("asd", 0).getStringSet("Commands", setOf())
            possibleSet
        } else {
            context.getSharedPreferences("asd", 0).edit().putStringSet("Commands", setOf()).apply()
        }
        return errorCount == 0
    }
}