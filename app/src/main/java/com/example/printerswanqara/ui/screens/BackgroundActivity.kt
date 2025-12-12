package com.example.printerswanqara.ui.screens

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.printerswanqara.R
import com.example.printerswanqara.core.print.utils.printer1.Discrimination
import com.example.printerswanqara.domain.services.GetAllPrinters
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Added imports for diagnostics
import com.example.printerswanqara.core.print.utils.printer1.PrinterBuilder
import com.example.printerswanqara.core.print.utils.printer1.PrintDiagnosticsBus
// Added imports for schema handling
import com.example.printerswanqara.core.print.utils.schema.SchemaParser
import com.example.printerswanqara.core.print.utils.schema.NetworkPrintHandler

class BackgroundActivity : ComponentActivity() {

    // Hold references to listeners to detach later
    private var transportListener: ((PrinterBuilder.PrinterDiagnosticsEvent) -> Unit)? = null
    private var phaseListener: ((PrintDiagnosticsBus.PhaseEvent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BackgroundActivity", "onCreate called")

        // Subscribe to diagnostics so background prints are persisted to logs
        transportListener = { evt ->
            val ms = evt.endTimestamp - evt.startTimestamp
            val status = if (evt.success) "✅" else "❌"
            val line = "$status JOB transporte=${evt.transportType} destino=${evt.address ?: "-"}:${evt.port ?: "-"} bytes=${evt.bytesLength} tiempo=${ms}ms${evt.errorMessage?.let { " error=$it" } ?: ""}".trim()
            PrintDiagnosticsBus.appendPersistentLog(this, line)
        }
        PrinterBuilder.diagnosticsListener = transportListener

        phaseListener = { evt ->
            val line = "⏱ Fase ${evt.phase} doc=${evt.documentType} dur=${evt.durationMs}ms ok=${evt.success} ${evt.extra ?: ""}".trim()
            PrintDiagnosticsBus.appendPersistentLog(this, line)
        }
        PrintDiagnosticsBus.phaseListener = phaseListener

        setContent {
            // GIF-enabled ImageLoader for Coil
            val gifEnabledLoader = remember(this) {
                ImageLoader.Builder(this)
                    .components {
                        if (android.os.Build.VERSION.SDK_INT >= 28) {
                            add(ImageDecoderDecoder.Factory())
                        } else {
                            add(GifDecoder.Factory())
                        }
                    }
                    .build()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize(), // Fill the whole screen
                contentAlignment = Alignment.Center // Center content in the Box
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        imageLoader = gifEnabledLoader,
                        model = R.drawable.printing,
                        contentDescription = "Impresion GIF",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
        Log.d("BackgroundActivity", "UI loaded, starting print logic")
        // Start print logic in background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            Log.d("BackgroundActivity", "Launching print()")
            print()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detach listeners if still attached
        if (PrinterBuilder.diagnosticsListener === transportListener) {
            PrinterBuilder.diagnosticsListener = null
        }
        if (PrintDiagnosticsBus.phaseListener === phaseListener) {
            PrintDiagnosticsBus.phaseListener = null
        }
    }

    private suspend fun print(): Boolean {
        Log.d("BackgroundActivity", "print() called")
        var result = true
        val jobStart = System.currentTimeMillis()
        try {
            val intent = intent
            val uri = intent.data
            Log.d("BackgroundActivity", "Intent URI: $uri")
            PrintDiagnosticsBus.appendPersistentLog(this, "JOB_START uri=${uri}")
            // Check if this is a send-to-print schema
            if (SchemaParser.isSendToPrintSchema(uri)) {
                Log.d("BackgroundActivity", "Detected send-to-print schema")
                PrintDiagnosticsBus.appendPersistentLog(this, "SCHEMA_TYPE send-to-print")
                
                // Parse the schema
                val configs = SchemaParser.parse(uri!!)
                Log.d("BackgroundActivity", "Parsed ${configs.size} printer configurations")
                PrintDiagnosticsBus.appendPersistentLog(this, "SCHEMA_CONFIGS count=${configs.size}")
                
                if (configs.isEmpty()) {
                    Log.e("BackgroundActivity", "No valid printer configurations found")
                    PrintDiagnosticsBus.appendPersistentLog(this, "JOB_FAIL reason=no_configs")
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(this@BackgroundActivity, "No valid printer configurations", android.widget.Toast.LENGTH_LONG).show()
                    }
                    return false
                }
                
                // Process with NetworkPrintHandler
                val handler = NetworkPrintHandler(this)
                result = handler.handleConfigs(configs)
                
                if (!result) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(this@BackgroundActivity, "Error sending to one or more printers", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                
                return result
            }

            // Existing logic for regular schemas

            // Expecting schema: wanqaraprintermobile://IMPRESION_FACTURA_ELECTRONICA,${'$'}{sale.id}"
            val saleId = uri?.schemeSpecificPart?.split(",")?.getOrNull(1)
            Log.d("BackgroundActivity", "Parsed saleId: $saleId")
            PrintDiagnosticsBus.appendPersistentLog(this, "SCHEMA_PARSE saleId=${saleId}")

            val data = uri?.schemeSpecificPart
            Log.d("BackgroundActivity", "Raw schemeSpecificPart data: $data")
            PrintDiagnosticsBus.appendPersistentLog(this, "SCHEMA_DATA data=${data}")
            if (data.isNullOrBlank()) {
                Log.e("BackgroundActivity", "No data found in intent URI. Aborting print.")
                PrintDiagnosticsBus.appendPersistentLog(this, "JOB_FAIL reason=no_data_in_uri")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(this@BackgroundActivity, "No print data found", android.widget.Toast.LENGTH_LONG).show()
                }
                return false
            }
            val commands = data.split(",")
            Log.d("BackgroundActivity", "Parsed commands: ${commands}")
            PrintDiagnosticsBus.appendPersistentLog(this, "COMMANDS ${commands.joinToString("|")}")

            try {
                Log.d("BackgroundActivity", "Getting database instance...")
                val db = com.example.printerswanqara.data.database.DatabaseProvider.getDatabase(this)
                Log.d("BackgroundActivity", "Database instance obtained: $db")
                val repository = com.example.printerswanqara.data.database.repositories.PrinterRepository(db.printersDAO())
                Log.d("BackgroundActivity", "PrinterRepository created: $repository")
                val getAllPrinters = GetAllPrinters(repository)
                Log.d("BackgroundActivity", "GetAllPrinters created: $getAllPrinters")
                val printers = getAllPrinters.getAll()
                Log.d("BackgroundActivity", "Fetched printers: ${printers.size} found")
                PrintDiagnosticsBus.appendPersistentLog(this, "PRINTERS size=${printers.size}")

                // Prepare commands array for Discrimination
                val commandsArray = commands.map { it.trimStart('/') }.toTypedArray()
                val discStart = System.currentTimeMillis()
                val resultDiscrimination = Discrimination(printers, this).invoke(commandsArray)
                val discMs = System.currentTimeMillis() - discStart
                PrintDiagnosticsBus.appendPersistentLog(this, "DISCRIMINATION done=${resultDiscrimination} dur=${discMs}ms")

                if (!resultDiscrimination) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(this@BackgroundActivity, "Error al imprimir una o mas impresoras ", android.widget.Toast.LENGTH_SHORT).show()
                        kotlin.system.exitProcess(-1)
                    }
                    result = false
                }
            } catch (e: Exception) {
                Log.e("BackgroundActivity", "Exception fetching printers: ${e.message}", e)
                Log.e("BackgroundActivity", "Exception stack trace:", e)
                Log.e("BackgroundActivity", "Commands at error: ${commands}")
                PrintDiagnosticsBus.appendPersistentLog(this, "JOB_EXCEPTION step=db_or_print msg=${e.message}")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(this@BackgroundActivity, "Error fetching printers: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
                result = false
            }
        } catch (e: InterruptedException) {
            Log.e("BackgroundActivity", "InterruptedException: ${'$'}e")
            PrintDiagnosticsBus.appendPersistentLog(this, "JOB_INTERRUPTED msg=${e.message}")
            android.widget.Toast.makeText(this, "Error al imprimir", android.widget.Toast.LENGTH_SHORT).show()
            result = false
            return false
        } finally {
            val jobMs = System.currentTimeMillis() - jobStart
            PrintDiagnosticsBus.appendPersistentLog(this, "JOB_END success=${result} dur=${jobMs}ms")
            val isUsbPermissionSending =
                application.getSharedPreferences("usb", 0).getBoolean("permissions", false)
            if (result && !isUsbPermissionSending) {
                Log.d("BackgroundActivity", "Finishing activity after print")
                moveTaskToBack(true)
                kotlin.system.exitProcess(-1)
            }
        }
        return result
    }

}
