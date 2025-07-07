package com.example.printerswanqaratest.ui.screens.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.printerswanqaratest.core.printType.PrinterType
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import com.example.printerswanqaratest.ui.theme.Primary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.example.printerswanqaratest.data.database.entities.PrintersEntity
import androidx.compose.ui.platform.LocalContext
import com.example.printerswanqaratest.data.database.DatabaseProvider
import com.example.printerswanqaratest.data.database.repositories.PrinterRepository
import com.example.printerswanqaratest.domain.services.AddPrinters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
@Composable
fun AddPrinterScreen() {
    var selectedMode by remember { mutableStateOf(PrinterType.USB) }
    val snackbarHostState = remember { SnackbarHostState() }
    val modes = listOf(
        PrinterType.USB to Icons.Default.Usb,
        PrinterType.BLUETOOTH to Icons.Default.Bluetooth,
        PrinterType.WIFI to Icons.Default.Wifi
    )
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        // mode switches
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            modes.forEach { (mode, icon) ->
                if (mode == selectedMode) {
                    Button(
                        onClick = { selectedMode = mode },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(icon, contentDescription = mode.type)
                        Spacer(Modifier.width(4.dp))
                        Text(mode.type)
                    }
                } else {
                    OutlinedButton(
                        onClick = { selectedMode = mode },
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Primary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) {
                        Icon(icon, contentDescription = mode.type)
                        Spacer(Modifier.width(4.dp))
                        Text(mode.type)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedContent(
            targetState = selectedMode,
            transitionSpec = {
                // slide forward or backward based on enum order
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth * direction },
                    animationSpec = tween(300)
                ) togetherWith slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth * direction },
                    animationSpec = tween(300)
                )
            }
        ) { mode ->
            when (mode) {
                PrinterType.USB -> UsbForm()
                PrinterType.BLUETOOTH -> BluetoothForm(snackbarHostState)
                PrinterType.WIFI -> WifiForm(snackbarHostState)
            }
        }
    }
}

@Composable
fun UsbForm() {
    // Mockup only
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("USB printer form (mockup)")
    }
}

@Composable
fun BluetoothForm() {
    // Mockup only
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Bluetooth printer form (mockup)")
    }
}


// Mock save functions
fun saveUsbPrinter(name: String, chars: Int, copies: Int, docType: String) {
    // TODO: persist USB settings
}
fun saveBluetoothPrinter(device: String) {
    // TODO: persist Bluetooth settings
}
suspend fun saveWifiPrinter(
    context: android.content.Context,
    name: String,
    ip: String,
    port: Int
): Boolean {
    val entity = PrintersEntity(
        name = name,
        fontSize = "A",
        documentType = "IMPRESION_RECIBO",
        copyNumber = 1,
        charactersNumber = 32,
        type = PrinterType.WIFI.type,
        address = ip,
        port = port
    )
    return try {
        withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)
            val repository = PrinterRepository(db.printersDAO())
            val addPrinter = AddPrinters(repository)
            addPrinter(entity)
        }
        true
    } catch (_: Exception) {
        false
    }
}
suspend fun savePrinterToDb(context: android.content.Context, printer: PrintersEntity): Boolean {
    return try {
        withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)
            db.printersDAO().insertAll(printer)
        }
        true
    } catch (_: Exception) {
        false
    }
} }
