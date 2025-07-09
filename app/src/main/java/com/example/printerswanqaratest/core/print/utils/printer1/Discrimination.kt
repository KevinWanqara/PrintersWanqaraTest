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
        for (i in commands.indices) {
            val command = commands[i]
            val cleanCommand = command.trim().removePrefix("//").removePrefix("wanqaraprintermobile://").trim()
            // Defensive: Only split if there's a comma, else treat as a single part
            val parts = if (cleanCommand.contains(",")) {
                cleanCommand.split(",").map { it.trim() }
            } else {
                listOf(cleanCommand)
            }
            // Fix: If only one command and two are expected, try to join with the next
            val documentType: String?
            val saleId: String?
            if (parts.size == 2) {
                documentType = parts[0]
                saleId = parts[1]
            } else if (parts.size == 1 && i + 1 < commands.size) {
                // Try to join with the next command
                val nextCommand = commands[i + 1].trim().removePrefix("//").removePrefix("wanqaraprintermobile://").trim()
                documentType = parts[0]
                saleId = nextCommand
                android.util.Log.d("Discrimination", "Detected split command, using documentType: $documentType, saleId: $saleId")
            } else {
                documentType = parts.getOrNull(0)
                saleId = parts.getOrNull(1)
            }
            android.util.Log.d("Discrimination", "Parsed documentType: $documentType, saleId: $saleId")
            if (documentType.isNullOrEmpty() || saleId.isNullOrEmpty()) {
                android.util.Log.e("Discrimination", "Invalid command format: $command (parsed as: $cleanCommand)")
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
                        android.util.Log.d("Discrimination", "Sending imprimirFacturaElectronica command with cleanCommand: $cleanCommand")
                        android.util.Log.d("Discrimination", "Printer: ${printer?.copyNumber}, Characters: ${printer?.charactersNumber}")
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
                        android.util.Log.d("Discrimination", "Sending imprimirRecibo command with cleanCommand: $cleanCommand")
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
                        android.util.Log.d("Discrimination", "Sending imprimirPreticket command with cleanCommand: $cleanCommand")
                        printerBuilder!!.imprimirPreticket(
                            jsonObject,
                            printer!!.copyNumber,
                            printer!!.charactersNumber
                        )
                    }
                    // Add more document types as needed
                    else -> {
                        android.util.Log.e("Discrimination", "Unknown documentType: $documentType in cleanCommand: $cleanCommand")
                        errorCount++
                        errorCommand = errorCommand.plus(command)
                    }
                }
            } else {
                android.util.Log.e("Discrimination", "printerBuilder or jsonObject is NULL for cleanCommand: $cleanCommand")
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