package com.example.printerswanqara.core.print.utils.printer1

import android.content.Context
import com.example.printerswanqara.core.printType.PrinterType
import com.example.printerswanqara.domain.models.Printers
import com.example.printerswanqara.api.ApiClient
import com.example.printerswanqara.data.AppStorage

import org.json.JSONObject
import org.json.JSONArray
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
        val start = System.currentTimeMillis()
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
                PrinterType.SERVER.type -> {
                    // Server type doesn't use PrinterBuilder for ESC/POS commands
                    // It will be handled directly in invoke() via HTTP requests
                    printerBuilder = null 
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
        val dur = System.currentTimeMillis() - start
        val setupSuccess = printerBuilder != null || (printer != null && printer!!.type == PrinterType.SERVER.type)
        PrintDiagnosticsBus.phaseListener?.invoke(PrintDiagnosticsBus.PhaseEvent(document, "setup", dur, setupSuccess))
    }


    
    suspend operator fun invoke(commands: Array<String>): Boolean {
        var errorCount = 0
        var errorCommand: Array<String> = arrayOf()
        if (commands.isEmpty()) return false
    
        // Parse commands as pairs: (command, id)
        val commandPairs = mutableListOf<Pair<String, String>>()
        var i = 0
        while (i < commands.size) {
            val rawCommand = commands[i].trim().removePrefix("//").removePrefix("wanqaraprintermobile://").trimStart('/')
            val id = commands.getOrNull(i + 1)?.trim()
            if (id.isNullOrEmpty()) {
                android.util.Log.e("Discrimination", "Invalid command format: missing ID for command '$rawCommand'")
                errorCount++
                errorCommand = errorCommand.plus(rawCommand)
                i += 2 // Skip to next pair
                continue
            }
            commandPairs.add(rawCommand to id)
            i += 2
        }
    
        if (commandPairs.isEmpty()) {
            android.util.Log.e("Discrimination", "No valid command pairs found.")
            return false
        }
    
        // Process each command pair
        for ((command, transactionID) in commandPairs) {
            if (command.isEmpty() || transactionID.isEmpty()) {
                android.util.Log.e("Discrimination", "Invalid command pair: command='$command', id='$transactionID'")
                errorCount++
                errorCommand = errorCommand.plus(command)
                continue
            }
    
            setup(command, commands) // Setup printer per command
            android.util.Log.d("Discrimination", "After setup: printerBuilder is ${if (printerBuilder != null) "NOT NULL" else "NULL"}")
    
            // Fetch data per command, based on command type
            val fetchStart = System.currentTimeMillis()
            val jsonObject = withContext(Dispatchers.IO) {
                try {
                    when {
                        command == "IMPRESION_COTIZACION" -> {
                            val quotesService = ApiClient.createQuotesService(context)
                            val quote = quotesService.getQuoteById(transactionID).data
                            println("Discrimination: quote = $quote")
                            JSONObject(com.google.gson.Gson().toJson(quote))
                        }
                        command == "IMPRESION_PRE_TICKET" -> {
                            val orderService = ApiClient.createOrderService(context)
                            val order = orderService.getOrderById(transactionID).data
                            println("Discrimination: order = $order")
                            JSONObject(com.google.gson.Gson().toJson(order))
                        }
                        command.startsWith("IMPRESION_COMANDA") -> { // Always use orderPrintService for comanda commands
                            val orderPrintService = ApiClient.createOrderPrintService(context)
                            val orderPrint = orderPrintService.getOrderPrintById(transactionID).data
                            println("Discrimination: orderPrint/Comanda = $orderPrint")
                            JSONObject(com.google.gson.Gson().toJson(orderPrint))
                        }
                        command == "IMPRESION_CIERRE_CAJA" -> {
                            val cashRegisterService = ApiClient.createCashRegisterService(context)
                            val cashRegister = cashRegisterService.getCashRegisterById(transactionID).data
                            println("Discrimination: cashRegister = $cashRegister")
                            JSONObject(com.google.gson.Gson().toJson(cashRegister))
                        }
                        else -> { // Default to sales for other commands (e.g., IMPRESION_FACTURA_ELECTRONICA, IMPRESION_RECIBO)
                            println("Discrimination: Fetching sale for ID $transactionID")
                            val salesService = ApiClient.createSalesService(context)
                            val sale = salesService.getSalesById(transactionID).data
                            println("Discrimination: sale = $sale")
                            JSONObject(com.google.gson.Gson().toJson(sale))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Discrimination", "Error fetching data for command '$command' with id $transactionID.", e)
                    null
                }
            }
            val fetchDur = System.currentTimeMillis() - fetchStart
            PrintDiagnosticsBus.phaseListener?.invoke(PrintDiagnosticsBus.PhaseEvent(command, "fetch-data", fetchDur, jsonObject != null))
            println("Discrimination: jsonObject for $command = $jsonObject")
    
            // Proceed only if data was fetched
            if (jsonObject == null) {
                android.util.Log.e("Discrimination", "Data not received for command '$command', skipping print.")
                errorCount++
                errorCommand = errorCommand.plus(command)
                continue
            }
    
            if (printerBuilder != null) {
                try {
                    val phaseStart = System.currentTimeMillis()
                    when (command) {
                        "IMPRESION_FACTURA_ELECTRONICA" -> {
                            android.util.Log.d("Discrimination", "Sending imprimirFacturaElectronica command with documentType: $command")
                            printerBuilder!!.imprimirFacturaElectronica(
                                jsonObject,
                                settingJson,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                            )
                        }
                        "IMPRESION_RECIBO" -> {
                            android.util.Log.d("Discrimination", "Sending imprimirRecibo command with documentType: $command")
                            printerBuilder!!.imprimirRecibo(
                                jsonObject,
                                settingJson,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                            )
                        }
                        "IMPRESION_COTIZACION" -> {
                            android.util.Log.d("Discrimination", "Sending imprimirCotizacion command with documentType: $command")
                            printerBuilder!!.imprimirCotizacion(
                                jsonObject,
                                settingJson,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                            )
                        }
                        "IMPRESION_PRE_TICKET" -> {
                            android.util.Log.d("Discrimination", "Sending imprimirPreticket command with documentType: $command")
                            printerBuilder!!.imprimirPreticket(
                                jsonObject,
                                settingJson,
                                printer!!.copyNumber,
                                printer!!.charactersNumber
                            )
                        }
                        "IMPRESION_COMANDA:COCINA", "IMPRESION_COMANDA:BARRA", "IMPRESION_COMANDA:OTROS" -> {
                            android.util.Log.d("Discrimination", "Sending comanda command with documentType: $command")
                            val comandaType = when (command) {
                                "IMPRESION_COMANDA:COCINA" -> "A"
                                "IMPRESION_COMANDA:BARRA" -> "B"
                                "IMPRESION_COMANDA:OTROS" -> "C"
                                else -> ""
                            }
                            printerBuilder!!.imprimirComandas(
                                jsonObject,
                                settingJson,
                                printer!!.copyNumber,
                                printer!!.charactersNumber,
                                comandaType
                            )
                        }
                        "IMPRESION_CIERRE_CAJA" -> {
                            android.util.Log.d("Discrimination", "Sending imprimirCierreCaja command with documentType: $command")
                            printerBuilder!!.imprimirCierreCaja(
                                jsonObject,
                                settingJson,
                                printer!!.copyNumber,
                                printer!!.charactersNumber
                            )
                        }
                        else -> {
                            android.util.Log.e("Discrimination", "Unknown documentType: $command")
                            errorCount++
                            errorCommand = errorCommand.plus(command)
                        }
                    }
                    val phaseDur = System.currentTimeMillis() - phaseStart
                    PrintDiagnosticsBus.phaseListener?.invoke(PrintDiagnosticsBus.PhaseEvent(command, "print-command", phaseDur, true))
                    // Ensure USB jobs are flushed and delayed between jobs
                    if (printerBuilder?.usbOutputStream != null) {
                        printerBuilder?.usbOutputStream?.flush()
                        kotlinx.coroutines.delay(200)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Discrimination", "Error printing job for $command: ${e.message}", e)
                    errorCount++
                    errorCommand = errorCommand.plus(command)
                }
            } else if (printer != null && printer!!.type == PrinterType.SERVER.type) {
                try {
                    val phaseStart = System.currentTimeMillis()
                    val address = printer!!.address ?: ""
                    val baseUrl = "http://$address:51512/"
                    
                    val settingsStr = AppStorage.getSettings(context) ?: ""
                    val settingsJson = if (settingsStr.isNotEmpty()) JSONObject(settingsStr) else JSONObject()
                    
                    // The outer jsonObject is the fetched data (sale, order, etc.)
                    val dataJson = jsonObject 
                    dataJson.put("settings", settingsJson)
                    
                    // Calculate summary for sales
                    if (command == "IMPRESION_FACTURA_ELECTRONICA" || command == "IMPRESION_RECIBO") {
                        val summaryJson = JSONObject()
                        summaryJson.put("discount", String.format(java.util.Locale.US, "%.2f", dataJson.optDouble("discount", 0.0)))
                        summaryJson.put("subtotal", String.format(java.util.Locale.US, "%.2f", dataJson.optDouble("subtotal", 0.0)))
                        summaryJson.put("total", String.format(java.util.Locale.US, "%.2f", dataJson.optDouble("total", 0.0)))
                        
                        val originalSummary = dataJson.optJSONObject("summary")
                        if (originalSummary != null) {
                            val subtotalRates = originalSummary.optJSONArray("subtotal_rate")
                            if (subtotalRates != null) {
                                val newSubtotalRates = JSONArray()
                                for (i in 0 until subtotalRates.length()) {
                                    val rateObj = subtotalRates.getJSONObject(i)
                                    val newRateObj = JSONObject()
                                    newRateObj.put("rate", rateObj.optString("rate"))
                                    newRateObj.put("subtotal", String.format(java.util.Locale.US, "%.2f", rateObj.optDouble("subtotal", 0.0)))
                                    newSubtotalRates.put(newRateObj)
                                }
                                summaryJson.put("subtotal_rate", newSubtotalRates)
                            }
                            
                            val ivaRates = originalSummary.optJSONArray("iva_rate")
                            if (ivaRates != null) {
                                val newIvaRates = JSONArray()
                                for (i in 0 until ivaRates.length()) {
                                    val rateObj = ivaRates.getJSONObject(i)
                                    val newRateObj = JSONObject()
                                    newRateObj.put("rate", rateObj.optString("rate"))
                                    newRateObj.put("iva", String.format(java.util.Locale.US, "%.2f", rateObj.optDouble("iva", 0.0)))
                                    newIvaRates.put(newRateObj)
                                }
                                summaryJson.put("iva_rate", newIvaRates)
                            }
                        }
                        dataJson.put("summary", summaryJson)
                    }

                    val serverService = ApiClient.createServerPrinterService(context, baseUrl)
                    val gsonData = com.google.gson.JsonParser().parse(dataJson.toString()).asJsonObject
                    
                    val request = when (command) {
                        "IMPRESION_FACTURA_ELECTRONICA" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData, printType = "01", openDrawer = true)
                        "IMPRESION_RECIBO" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData, printType = "03", openDrawer = true)
                        "IMPRESION_PRE_TICKET" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData)
                        "IMPRESION_COTIZACION" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData)
                        "IMPRESION_CIERRE_CAJA" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData)
                        "IMPRESION_COMANDA:COCINA" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData)
                        "IMPRESION_COMANDA:BARRA" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData)
                        "IMPRESION_COMANDA:OTROS" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData)
                        "GAVETA_DINERO" -> com.example.printerswanqara.api.ServerPrintRequest(data = gsonData, openDrawer = true)
                        else -> null
                    }

                    if (request != null) {
                        val endpoint = when (command) {
                            "IMPRESION_FACTURA_ELECTRONICA", "IMPRESION_RECIBO", "GAVETA_DINERO" -> "receiptPrinter/invoice-ticket"
                            "IMPRESION_PRE_TICKET" -> "receiptPrinter/preticket"
                            "IMPRESION_COTIZACION" -> "receiptPrinter/quote"
                            "IMPRESION_CIERRE_CAJA" -> "receiptPrinter/cashregister"
                            "IMPRESION_COMANDA:COCINA" -> "receiptPrinter/ticket-a"
                            "IMPRESION_COMANDA:BARRA" -> "receiptPrinter/ticket-b"
                            "IMPRESION_COMANDA:OTROS" -> "receiptPrinter/ticket-c"
                            else -> ""
                        }
                        
                        val fullUrl = "$baseUrl$endpoint"
                        val jsonBody = com.google.gson.Gson().toJson(request)
                        
                        // Log CURL command
//                        val curlCommand = "curl -X POST $fullUrl -H \"Content-Type: application/json\" -d '${jsonBody.replace("'", "'\\''")}'"
//                        android.util.Log.d("Discrimination", "EQUIVALENT CURL: $curlCommand")
//
//                        android.util.Log.d("Discrimination", "Sending SERVER request (Retrofit) to $fullUrl")
//
                        // Run raw test in parallel/sequence for debugging
//                        withContext(Dispatchers.IO) {
//                            sendRawPost(fullUrl, jsonBody)
//                        }

                        val response = when (command) {
                            "IMPRESION_FACTURA_ELECTRONICA", "IMPRESION_RECIBO", "GAVETA_DINERO" -> serverService.printInvoiceTicket(request)
                            "IMPRESION_PRE_TICKET" -> serverService.printPreticket(request)
                            "IMPRESION_COTIZACION" -> serverService.printQuote(request)
                            "IMPRESION_CIERRE_CAJA" -> serverService.printCashRegister(request)
                            "IMPRESION_COMANDA:COCINA" -> serverService.printTicketA(request)
                            "IMPRESION_COMANDA:BARRA" -> serverService.printTicketB(request)
                            "IMPRESION_COMANDA:OTROS" -> serverService.printTicketC(request)
                            else -> null
                        }

                        if (response != null) {
                            val responseBody = response.string()
                            android.util.Log.d("Discrimination", "SERVER response (Retrofit) received for $command. Body: $responseBody")
                            val phaseDur = System.currentTimeMillis() - phaseStart
                            PrintDiagnosticsBus.phaseListener?.invoke(PrintDiagnosticsBus.PhaseEvent(command, "server-print", phaseDur, true))
                        } else {
                            errorCount++
                            errorCommand = errorCommand.plus(command)
                        }
                    } else {
                        android.util.Log.e("Discrimination", "Unknown documentType for SERVER: $command")
                        errorCount++
                        errorCommand = errorCommand.plus(command)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Discrimination", "Error sending SERVER job for $command: ${e.message}", e)
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
        // Handle error storage (unchanged)
        if (errorCommand.isNotEmpty()) {
            var possibleSet = context.getSharedPreferences("asd", 0).getStringSet("Commands", setOf())
            possibleSet
            context.getSharedPreferences("asd", 0).edit().putStringSet("Commands", setOf()).apply()
            possibleSet = context.getSharedPreferences("asd", 0).getStringSet("Commands", setOf())
            possibleSet
            context.getSharedPreferences("asd", 0).edit().putStringSet("Commands", errorCommand.toSet()).apply()
            possibleSet = context.getSharedPreferences("asd", 0).getStringSet("Commands", setOf())
            possibleSet
        } else {
            context.getSharedPreferences("asd", 0).edit().putStringSet("Commands", setOf()).apply()
        }
        PrintDiagnosticsBus.phaseListener?.invoke(PrintDiagnosticsBus.PhaseEvent(commandPairs.firstOrNull()?.first ?: "unknown", "overall", 0L, errorCount == 0, "errors=$errorCount"))
        return errorCount == 0
    }

    private fun sendRawPost(urlString: String, body: String) {
        try {
            android.util.Log.d("Discrimination", "Sending RAW POST to $urlString")
            val url = java.net.URL(urlString)
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            java.io.OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(body)
                writer.flush()
            }

            val code = conn.responseCode
            val responseBuilder = StringBuilder()
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            if (stream != null) {
                java.io.BufferedReader(java.io.InputStreamReader(stream)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        responseBuilder.append(line).append("\n")
                        line = reader.readLine()
                    }
                }
            }
            android.util.Log.d("Discrimination", "RAW POST Response Code: $code")
            android.util.Log.d("Discrimination", "RAW POST Response Body: ${responseBuilder.toString()}")
        } catch (e: Exception) {
            android.util.Log.e("Discrimination", "RAW POST Error: ${e.message}", e)
        }
    }
    
}