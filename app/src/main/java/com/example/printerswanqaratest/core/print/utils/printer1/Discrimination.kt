package com.example.printerswanqaratest.core.print.utils.printer1

import android.content.Context
import com.example.printerswanqaratest.core.printType.PrinterType
import com.example.printerswanqaratest.domain.models.Printers
import com.example.printerswanqaratest.api.sales.SalesApiResponse
import com.example.printerswanqaratest.api.ApiClient
import com.example.printerswanqaratest.api.Setting
import com.example.printerswanqaratest.data.AppStorage

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import com.google.gson.Gson
class Discrimination(
    private val allPrinters: List<Printers>,
    private val context: Context,
    private val id : String,
    private val settingJson: JSONObject? = AppStorage.getSettings(context)?.let { JSONObject(it) }
) {
    private var printer: Printers? = null
    private var printerBuilder: PrinterBuilder? = null

    private fun setup(document: String , commands: Array<String>) {
        android.util.Log.d("Discrimination", "setup() called with document: $document")
        printer = allPrinters.find {
            it.documentType == document
        }
        if (printer != null) {
            android.util.Log.d("Discrimination", "Printer found: ${printer!!.charactersNumber} characters, ${printer!!.copyNumber} copies, type: ${printer!!.type}")
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
        } else {
            android.util.Log.e("Discrimination", "No printer found for document: $document")
            printerBuilder = null
        }
    }

    suspend operator fun invoke(commands: Array<String>): Boolean {
        var jsonObject: JSONObject?
        var errorCount = 0
        var errorCommand: Array<String> = arrayOf()
        if (commands.isEmpty()) return false
        // Parse main command, saleId, and additional commands
        val mainCommand = commands[0].trim().removePrefix("//").removePrefix("wanqaraprintermobile://").trimStart('/')
        val saleId = commands.getOrNull(1)?.trim()
        val additionalCommands = if (commands.size > 2) commands.sliceArray(2 until commands.size).map { it.trimStart('/') } else emptyList()
        val allCommands = mutableListOf<String>()
        allCommands.add(mainCommand)
        allCommands.addAll(additionalCommands)
        for (command in allCommands) {
            val documentType = command
            if (documentType.isEmpty() || saleId.isNullOrEmpty()) {
                android.util.Log.e("Discrimination", "Invalid command format: $command (documentType or saleId is empty)")
                errorCount++
                errorCommand = errorCommand.plus(command)
                continue
            }
            setup(documentType, commands)
            android.util.Log.d("Discrimination", "After setup: printerBuilder is ${if (printerBuilder != null) "NOT NULL" else "NULL"}")
            // Fetch sale data from API
            jsonObject = runBlocking {
                try {
                    val salesService = ApiClient.createSalesService(context)
                    val url = "billing/sales/$saleId"
                    android.util.Log.d("Discrimination", "Request URL: $url")
                    val sale = salesService.getSalesById(saleId).data
                    android.util.Log.d("Discrimination", "Fetched sale for $documentType: $sale")
                    android.util.Log.d("Discrimination", "Request Data: saleId=$saleId")
                    org.json.JSONObject(com.google.gson.Gson().toJson(sale))
                } catch (e: Exception) {
                    android.util.Log.e("Discrimination", "Error fetching sale for $documentType with id $saleId. Request URL: billing/sales/$saleId", e)
                    null
                }
            }
            android.util.Log.d("Discrimination", "After API call: jsonObject is ${if (jsonObject != null) "NOT NULL" else "NULL"}")
            if (printerBuilder != null && jsonObject != null) {
                when (documentType) {
                    "IMPRESION_FACTURA_ELECTRONICA" -> {
                        android.util.Log.d("Discrimination", "Sending imprimirFacturaElectronica command with documentType: $documentType")
                        printer?.let {
                            printerBuilder!!.imprimirFacturaElectronica(
                                jsonObject,
                                settingJson,
                                it.copyNumber,
                                it.charactersNumber,
                            )
                        }
                    }
                    "IMPRESION_RECIBO" -> {
                        android.util.Log.d("Discrimination", "Sending imprimirRecibo command with documentType: $documentType")
                        printer?.let {
                            printerBuilder!!.imprimirRecibo(
                                jsonObject,
                                settingJson,
                                it.copyNumber,
                                it.charactersNumber,
                            )
                        }
                    }
                    "IMPRESION_PRE_TICKET" -> {
                        android.util.Log.d("Discrimination", "Sending imprimirPreticket command with documentType: $documentType")
                        printerBuilder!!.imprimirPreticket(
                            jsonObject,
                            printer!!.copyNumber,
                            printer!!.charactersNumber
                        )
                    }
                    "IMPRESION_COMANDA:COCINA", "IMPRESION_COMANDA:BARRA", "IMPRESION_COMANDA:OTROS" -> {
                        android.util.Log.d("Discrimination", "Sending comanda command with documentType: $documentType")
                        val comandaType = when (documentType) {
                            "IMPRESION_COMANDA:COCINA" -> "A"
                            "IMPRESION_COMANDA:BARRA" -> "B"
                            "IMPRESION_COMANDA:OTROS" -> "C"
                            else -> ""
                        }
                        printer?.let {
                            printerBuilder!!.imprimirComandas(
                                jsonObject,
                                settingJson,
                                it.copyNumber,
                                it.charactersNumber,
                                comandaType
                            )
                        }
                    }
                    // Add more document types as needed
                    else -> {
                        android.util.Log.e("Discrimination", "Unknown documentType: $documentType")
                        errorCount++
                        errorCommand = errorCommand.plus(command)
                    }
                }
            } else {
                errorCount++
                if (!errorCommand.contains(command)) errorCommand = errorCommand.plus(command)
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