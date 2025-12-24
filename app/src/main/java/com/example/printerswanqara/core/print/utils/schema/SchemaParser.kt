package com.example.printerswanqara.core.print.utils.schema

import android.net.Uri
import android.util.Log

/**
 * Data class representing a single printer configuration from schema
 */
data class PrinterConfig(
    val printer: String,      // e.g., "ip_config1"
    val target: String,       // e.g., "192.168.100.156"
    val type: String,         // e.g., "command_a", "preticket"
    val id: String            // e.g., "abcd-1234-abcd-1234-abcd-1234"
)

/**
 * Parser for the advanced send-to-print schema format
 * 
 * Example schema:
 * schema://send-to-print?printer=ip_config1&target=192.168.100.156&type=command_a?id=abcd-1234...
 * 
 * Note: This uses a non-standard format where '?' separates multiple printer configs
 */
object SchemaParser {
    private const val TAG = "SchemaParser"
    
    /**
     * Check if a URI is a send-to-print schema
     */
    fun isSendToPrintSchema(uri: Uri?): Boolean {
        if (uri == null) return false
        val scheme = uri.scheme ?: return false
        val host = uri.host ?: uri.schemeSpecificPart?.substringBefore("?") ?: ""
        
        // Check for both "schema://send-to-print" and "send-to-print://"
        return (scheme.equals("schema", ignoreCase = true) && host.equals("send-to-print", ignoreCase = true)) ||
               scheme.equals("send-to-print", ignoreCase = true)
    }
    
    /**
     * Parse the send-to-print schema into a list of PrinterConfig objects
     * 
     * The schema uses '?' to separate multiple printer configurations
     * Each configuration has parameters like printer=X&target=Y&type=Z followed by ?id=ABC
     */
    fun parse(uri: Uri): List<PrinterConfig> {
        if (!isSendToPrintSchema(uri)) {
            Log.w(TAG, "URI is not a send-to-print schema: $uri")
            return emptyList()
        }
        
        val configs = mutableListOf<PrinterConfig>()
        
        try {
            // Get the part after the host (send-to-print)
            val schemeSpecific = uri.schemeSpecificPart ?: ""
            Log.d(TAG, "Parsing schema: $schemeSpecific")
            
            // Remove leading "//" and host if present
            val query = schemeSpecific
                .removePrefix("//send-to-print")
                .removePrefix("send-to-print")
                .trimStart('?')
            
            // Split by '?' to get individual printer configurations
            // Format: printer=X&target=Y&type=Z?id=ABC?printer=...
            val segments = query.split('?').filter { it.isNotBlank() }
            Log.d(TAG, "Found ${segments.size} segments")
            
            var currentPrinter: String? = null
            var currentTarget: String? = null
            var currentType: String? = null
            
            for (segment in segments) {
                val params = parseParams(segment)
                Log.d(TAG, "Segment params: $params")
                
                // Check if this segment has printer, target, or type (start of new config)
                if (params.containsKey("printer") || params.containsKey("target") || params.containsKey("type")) {
                    // Update current config parameters
                    params["printer"]?.let { currentPrinter = it }
                    params["target"]?.let { currentTarget = it }
                    params["type"]?.let { currentType = it }
                }
                
                // If this segment has id, complete the current config
                if (params.containsKey("id")) {
                    val id = params["id"]!!
                    
                    if (currentPrinter != null && currentTarget != null && currentType != null) {
                        val config = PrinterConfig(
                            printer = currentPrinter!!,
                            target = currentTarget!!,
                            type = currentType!!,
                            id = id
                        )
                        configs.add(config)
                        Log.d(TAG, "Added config: $config")
                        
                        // Reset for next config
                        currentPrinter = null
                        currentTarget = null
                        currentType = null
                    } else {
                        Log.w(TAG, "Incomplete config when id found: printer=$currentPrinter, target=$currentTarget, type=$currentType")
                    }
                }
            }
            
            Log.d(TAG, "Parsed ${configs.size} printer configurations")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing schema: ${e.message}", e)
        }
        
        return configs
    }
    
    /**
     * Parse a parameter string like "printer=ip_config1&target=192.168.100.156&type=command_a"
     * into a map
     */
    private fun parseParams(segment: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        // Split by '&' to get individual key=value pairs
        val pairs = segment.split('&')
        for (pair in pairs) {
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2) {
                val key = parts[0].trim()
                val value = parts[1].trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    params[key] = value
                }
            }
        }
        
        return params
    }
}
