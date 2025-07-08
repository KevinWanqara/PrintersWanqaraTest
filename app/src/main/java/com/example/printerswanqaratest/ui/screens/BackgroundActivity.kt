package com.example.printerswanqaratest.ui.screens

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.ComponentActivity

import androidx.appcompat.app.AppCompatActivity
import com.example.printerswanqaratest.R
import com.example.printerswanqaratest.core.print.utils.printer1.Discrimination


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess
import com.example.printerswanqaratest.domain.services.GetAllPrinters

class BackgroundActivity : ComponentActivity() {


    private var m_ambiente: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BackgroundActivity", "onCreate called")
        // Set a white background to avoid black square
        val root = android.widget.FrameLayout(this)
        root.setBackgroundColor(android.graphics.Color.WHITE)
        setContentView(root)
        // Optionally, show a logo or loading indicator
        val imageView = android.widget.ImageView(this)
        imageView.setImageResource(R.drawable.ic_wanqara_logo_foreground)
        imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
        imageView.adjustViewBounds = true
        val params = android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        )
        root.addView(imageView, params)
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
            val commands = arrayOf(data)
            Log.d("BackgroundActivity", "Parsed commands: ${commands.contentToString()}")

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
                Log.d("BackgroundActivity", "Calling Discrimination with sale.id: $saleId")
                Log.d("BackgroundActivity", "Commands: ${commands.contentToString()}")
                if (!commands.let {
                        (saleId)?.let { it1 -> Discrimination(printers, this , it1) }?.let { it2 ->
                            it2(
                                commands
                            )
                        }
                    }!!) {
                    result = false
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(this@BackgroundActivity, "Error printing sale", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BackgroundActivity", "Exception fetching printers: ${e.message}", e)
                Log.e("BackgroundActivity", "Exception stack trace:", e)
                Log.e("BackgroundActivity", "Commands at error: ${commands.contentToString()}")
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