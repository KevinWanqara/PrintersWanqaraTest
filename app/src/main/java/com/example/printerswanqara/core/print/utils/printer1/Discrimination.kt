package com.example.printerswanqara.core.print.utils.printer1

import android.content.Context
import com.example.printerswanqara.core.printType.PrinterType
import com.example.printerswanqara.domain.models.Printers
import com.example.printerswanqara.api.ApiClient
import com.example.printerswanqara.data.AppStorage

import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Discrimination(
    private val allPrinters: List<Printers>,
    private val context: Context,

) {
    private val settingJson: JSONObject? = AppStorage.getSettings(context)?.let { JSONObject(it) }

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
        val transactionID = commands.getOrNull(1)?.trim()
        println("Main command: $mainCommand, Transaction ID: $transactionID")
        val additionalCommands = if (commands.size > 2) commands.sliceArray(2 until commands.size).map { it.trimStart('/') } else emptyList()
        val allCommands = mutableListOf<String>()
        allCommands.add(mainCommand)
        allCommands.addAll(additionalCommands)

        // Fetch data from API ONCE
        val isCotizacion = mainCommand == "IMPRESION_COTIZACION"
        println("Discrimination: isCotizacion = $isCotizacion")
        val transactionObject = if (!transactionID.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    if (isCotizacion) {
                        val quotesService = ApiClient.createQuotesService(context)
                        val quote = quotesService.getQuoteById(transactionID).data
                        org.json.JSONObject(com.google.gson.Gson().toJson(quote))
                    } else {
                        val salesService = ApiClient.createSalesService(context)
                        val sale = salesService.getSalesById(transactionID).data
                        org.json.JSONObject(com.google.gson.Gson().toJson(sale))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Discrimination", "Error fetching data for all jobs with id $transactionID.", e)
                    null
                }
            }
        } else null
        println("Discrimination: transactionObject = $transactionObject")

        // Only proceed with print jobs if saleJsonObject is received
        if (!transactionID.isNullOrEmpty() && transactionObject == null) {
            android.util.Log.e("Discrimination", "Data not received, aborting print jobs.")
            return false
        }

        for (command in allCommands) {
            if (command.isEmpty() || transactionID.isNullOrEmpty()) {
                android.util.Log.e("Discrimination", "Invalid command format: $command (documentType or saleId is empty)")
                errorCount++
                errorCommand = errorCommand.plus(command)
                continue
            }
            setup(command, commands)
            android.util.Log.d("Discrimination", "After setup: printerBuilder is ${if (printerBuilder != null) "NOT NULL" else "NULL"}")
            // Use saleJsonObject for all jobs
            jsonObject = transactionObject
            android.util.Log.d("Discrimination", "After API call: jsonObject is ${if (jsonObject != null) "NOT NULL" else "NULL"}")
            if (printerBuilder != null && jsonObject != null) {
                try {
                    when (command) {
                        "IMPRESION_FACTURA_ELECTRONICA" -> {
                            android.util.Log.d(
                                "Discrimination",
                                "Sending imprimirFacturaElectronica command with documentType: $command"
                            )
                            if (printerBuilder != null) {
                                printerBuilder!!.imprimirFacturaElectronica(
                                    jsonObject,
                                    settingJson,
                                    printer!!.copyNumber,
                                    printer!!.charactersNumber,
                                )
                            }

                        }

                        "IMPRESION_RECIBO" -> {
                            android.util.Log.d(
                                "Discrimination",
                                "Sending imprimirRecibo command with documentType: $command"
                            )
                            if (printerBuilder != null) {
                                printerBuilder!!.imprimirRecibo(
                                    jsonObject,
                                    settingJson,
                                    printer!!.copyNumber,
                                    printer!!.charactersNumber,
                                )
                            }

                        }

                        "IMPRESION_COTIZACION" -> {
                            android.util.Log.d(
                                "Discrimination",
                                "Sending imprimirRecibo command with documentType: $command"
                            )
                            if (printerBuilder != null) {
                                printerBuilder!!.imprimirCotizacion(
                                    jsonObject,
                                    settingJson,
                                    printer!!.copyNumber,
                                    printer!!.charactersNumber,
                                )
                            }

                        }
                        "IMPRESION_PRE_TICKET" -> {
                            android.util.Log.d(
                                "Discrimination",
                                "Sending imprimirPreticket command with documentType: $command"
                            )
                            printerBuilder!!.imprimirPreticket(
                                jsonObject,
                                printer!!.copyNumber,
                                printer!!.charactersNumber
                            )
                        }

                        "IMPRESION_COMANDA:COCINA", "IMPRESION_COMANDA:BARRA", "IMPRESION_COMANDA:OTROS" -> {
                            android.util.Log.d(
                                "Discrimination",
                                "Sending comanda command with documentType: $command"
                            )
                            val comandaType = when (command) {
                                "IMPRESION_COMANDA:COCINA" -> "A"
                                "IMPRESION_COMANDA:BARRA" -> "B"
                                "IMPRESION_COMANDA:OTROS" -> "C"
                                else -> ""
                            }
                            if (printerBuilder != null) {
                                printerBuilder!!.imprimirComandas(
                                    jsonObject,
                                    settingJson,
                                    printer!!.copyNumber,
                                    printer!!.charactersNumber,
                                    comandaType
                                )
                            }

                        }
                        // Add more document types as needed
                        else -> {
                            android.util.Log.e("Discrimination", "Unknown documentType: $command")
                            errorCount++
                            errorCommand = errorCommand.plus(command)
                        }
                    }
                    // Ensure USB jobs are flushed and delayed between jobs
                    if (printerBuilder?.usbOutputStream != null) {
                        printerBuilder?.usbOutputStream?.flush()
                        kotlinx.coroutines.delay(200)
                    }
                } catch (e: Exception) {
                    android.util.Log.e(
                        "Discrimination",
                        "Error printing job for $command: ${e.message}",
                        e
                    )
                    errorCount++
                    errorCommand = errorCommand.plus(command)
                }
            } else {
                errorCount++
                if (!errorCommand.contains(command)) errorCommand = errorCommand.plus(command)
            }
        }
        // Close connection only after all jobs
        printerBuilder?.closeAll()
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