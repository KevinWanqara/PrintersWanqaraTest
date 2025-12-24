package com.example.printerswanqara.core.print.utils.schema

import android.content.Context
import android.util.Log
import com.example.printerswanqara.api.ApiClient
import com.example.printerswanqara.core.print.utils.printer1.PrintDiagnosticsBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Handles network-based print requests for the send-to-print schema
 * 
 * This service:
 * 1. Fetches data from API services using the provided ID and type
 * 2. Makes HTTP POST requests to http://{target}/receiptPrinter
 */
class NetworkPrintHandler(private val context: Context) {
    
    companion object {
        private const val TAG = "NetworkPrintHandler"
    }
    
    /**
     * Process a list of printer configurations
     * Returns true if all succeed, false otherwise
     */
    suspend fun handleConfigs(configs: List<PrinterConfig>): Boolean {
        if (configs.isEmpty()) {
            Log.w(TAG, "No configurations to process")
            return false
        }
        
        var successCount = 0
        var errorCount = 0
        
        for ((index, config) in configs.withIndex()) {
            Log.d(TAG, "Processing config ${index + 1}/${configs.size}: $config")
            PrintDiagnosticsBus.appendPersistentLog(
                context,
                "SCHEMA_PRINT config=${index + 1}/${configs.size} type=${config.type} target=${config.target}"
            )
            
            try {
                val success = processConfig(config)
                if (success) {
                    successCount++
                    Log.d(TAG, "Config ${index + 1} succeeded")
                } else {
                    errorCount++
                    Log.e(TAG, "Config ${index + 1} failed")
                }
            } catch (e: Exception) {
                errorCount++
                Log.e(TAG, "Exception processing config ${index + 1}: ${e.message}", e)
                PrintDiagnosticsBus.appendPersistentLog(
                    context,
                    "SCHEMA_ERROR config=${index + 1} error=${e.message}"
                )
            }
        }
        
        val result = errorCount == 0
        PrintDiagnosticsBus.appendPersistentLog(
            context,
            "SCHEMA_RESULT success=$successCount errors=$errorCount total=${configs.size} result=$result"
        )
        
        return result
    }
    
    /**
     * Process a single printer configuration
     */
    private suspend fun processConfig(config: PrinterConfig): Boolean {
        val fetchStart = System.currentTimeMillis()
        
        // 1. Fetch data from API based on type and id
        val data = fetchDataForType(config.type, config.id)
        
        val fetchDur = System.currentTimeMillis() - fetchStart
        PrintDiagnosticsBus.appendPersistentLog(
            context,
            "SCHEMA_FETCH type=${config.type} id=${config.id} dur=${fetchDur}ms success=${data != null}"
        )
        
        if (data == null) {
            Log.e(TAG, "Failed to fetch data for type=${config.type}, id=${config.id}")
            return false
        }
        
        // 2. Send POST request to target IP
        val sendStart = System.currentTimeMillis()
        val result = sendToTarget(config.target, config.type, data)
        val sendDur = System.currentTimeMillis() - sendStart
        
        PrintDiagnosticsBus.appendPersistentLog(
            context,
            "SCHEMA_SEND target=${config.target} dur=${sendDur}ms success=$result"
        )
        
        return result
    }
    
    /**
     * Fetch data from API services based on the type
     * This mirrors the logic in Discrimination.kt
     */
    private suspend fun fetchDataForType(type: String, id: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                when (type.uppercase()) {
                    "PRETICKET", "IMPRESION_PRE_TICKET" -> {
                        val orderService = ApiClient.createOrderService(context)
                        val order = orderService.getOrderById(id).data
                        JSONObject(com.google.gson.Gson().toJson(order))
                    }
                    "COMMAND_A", "COMMAND_B", "COMMAND_C", 
                    "IMPRESION_COMANDA", "IMPRESION_COMANDA:COCINA", 
                    "IMPRESION_COMANDA:BARRA", "IMPRESION_COMANDA:OTROS" -> {
                        val orderPrintService = ApiClient.createOrderPrintService(context)
                        val orderPrint = orderPrintService.getOrderPrintById(id).data
                        JSONObject(com.google.gson.Gson().toJson(orderPrint))
                    }
                    "COTIZACION", "IMPRESION_COTIZACION" -> {
                        val quotesService = ApiClient.createQuotesService(context)
                        val quote = quotesService.getQuoteById(id).data
                        JSONObject(com.google.gson.Gson().toJson(quote))
                    }
                    "CIERRE_CAJA", "IMPRESION_CIERRE_CAJA" -> {
                        val cashRegisterService = ApiClient.createCashRegisterService(context)
                        val cashRegister = cashRegisterService.getCashRegisterById(id).data
                        JSONObject(com.google.gson.Gson().toJson(cashRegister))
                    }
                    else -> {
                        // Default to sales service
                        val salesService = ApiClient.createSalesService(context)
                        val sale = salesService.getSalesById(id).data
                        JSONObject(com.google.gson.Gson().toJson(sale))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data for type=$type, id=$id: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Send POST request to target IP's /receiptPrinter endpoint
     * 
     * Payload format:
     * {
     *   "type": "command",
     *   "data": { ... },
     *   "target": "192.168.100.156"
     * }
     */
    private suspend fun sendToTarget(target: String, type: String, data: JSONObject): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Build base URL for the target IP
                val baseUrl = if (target.startsWith("http://") || target.startsWith("https://")) {
                    target.trimEnd('/')
                } else {
                    "http://$target"
                }
                
                // Create dynamic printer service for this target
                val service = ApiClient.createDynamicPrinterService(context, "$baseUrl/")
                
                // Convert JSONObject to Gson JsonObject
                val gsonData = com.google.gson.JsonParser().parse(data.toString()).asJsonObject
                
                // Create request payload
                val request = com.example.printerswanqara.api.PrintRequest(
                    address = target,
                    data = gsonData,
                    type = type
                )
                
                Log.d(TAG, "Sending to $baseUrl/receiptPrinter - type=$type, target=$target")
                
                // Make the POST request
                val response = service.print(request)
                
                Log.d(TAG, "Response received from $target")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to target=$target: ${e.message}", e)
                false
            }
        }
    }
}
