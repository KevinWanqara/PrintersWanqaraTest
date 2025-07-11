package com.example.printerswanqaratest.ui.screens

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
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
import com.example.printerswanqaratest.R
import com.example.printerswanqaratest.core.print.utils.printer1.Discrimination
import com.example.printerswanqaratest.domain.services.GetAllPrinters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

class BackgroundActivity : ComponentActivity() {


    private var m_ambiente: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BackgroundActivity", "onCreate called")
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

    private suspend fun print(): Boolean {
        Log.d("BackgroundActivity", "print() called")
        var result = true
        try {
            val intent = intent
            val uri = intent.data
            Log.d("BackgroundActivity", "Intent URI: $uri")
            // Expecting schema: wanqaraprintermobile://IMPRESION_FACTURA_ELECTRONICA,${'$'}{sale.id}"
            val saleId = uri?.schemeSpecificPart?.split(",")?.getOrNull(1)
            Log.d("BackgroundActivity", "Parsed saleId: $saleId")

            val data = uri?.schemeSpecificPart
            Log.d("BackgroundActivity", "Raw schemeSpecificPart data: $data")
            if (data.isNullOrBlank()) {
                Log.e("BackgroundActivity", "No data found in intent URI. Aborting print.")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(this@BackgroundActivity, "No print data found", android.widget.Toast.LENGTH_LONG).show()
                }
                return false
            }
            val commands = data.split(",")
            Log.d("BackgroundActivity", "Parsed commands: ${commands}")

            try {
                Log.d("BackgroundActivity", "Getting database instance...")
                val db = com.example.printerswanqaratest.data.database.DatabaseProvider.getDatabase(this)
                Log.d("BackgroundActivity", "Database instance obtained: $db")
                val repository = com.example.printerswanqaratest.data.database.repositories.PrinterRepository(db.printersDAO())
                Log.d("BackgroundActivity", "PrinterRepository created: $repository")
                val getAllPrinters = GetAllPrinters(repository)
                Log.d("BackgroundActivity", "GetAllPrinters created: $getAllPrinters")
                val printers = getAllPrinters.getAll()
                Log.d("BackgroundActivity", "Fetched printers: ${printers.size} found")
                m_ambiente = "wanqara.app"
                val commandList = mutableListOf<String>()
                if (commands.isNotEmpty()) {
                    // Remove all leading slashes from main command if present
                    val mainCommand = commands[0].trimStart('/')
                    commandList.add(mainCommand)
                    if (commands.size > 2) {
                        // Add all additional commands, trimming any leading slashes
                        commandList.addAll(commands.subList(2, commands.size).map { it.trimStart('/') })
                    }
                }
                // Instead of splitting and looping, just pass the commands array to Discrimination
                val commandsArray = commands.map { it.trimStart('/') }.toTypedArray()
                val resultDiscrimination = Discrimination(printers, this).invoke(commandsArray)
                if (!resultDiscrimination) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(this@BackgroundActivity, "Error printing one or more jobs", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                result = resultDiscrimination
            } catch (e: Exception) {
                Log.e("BackgroundActivity", "Exception fetching printers: ${e.message}", e)
                Log.e("BackgroundActivity", "Exception stack trace:", e)
                Log.e("BackgroundActivity", "Commands at error: ${commands}")
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(this@BackgroundActivity, "Error fetching printers: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
                result = false
            }
        } catch (e: InterruptedException) {
            Log.e("BackgroundActivity", "InterruptedException: ${'$'}e")
            android.widget.Toast.makeText(this, "Error al imprimir", android.widget.Toast.LENGTH_SHORT).show()
            result = false
            return false
        } finally {
            val isUsbPermissionSending =
                application.getSharedPreferences("usb", 0).getBoolean("permissions", false)
            if (result && !isUsbPermissionSending) {
                Log.d("BackgroundActivity", "Finishing activity after print")
                moveTaskToBack(true)
                kotlin.system.exitProcess(-1)
            }
        }
        return true
    }

}