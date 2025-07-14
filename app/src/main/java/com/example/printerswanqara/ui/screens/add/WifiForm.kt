package com.example.printerswanqara.ui.screens.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.printerswanqara.core.printType.PrinterType
import com.example.printerswanqara.data.database.entities.PrintersEntity
import com.example.printerswanqara.data.database.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WifiForm(snackbarHostState: SnackbarHostState) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Printer Name") })
        OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP Address") })
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text("Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(
            onClick = {
                scope.launch {
                    val entity = PrintersEntity(
                        name = name,
                        fontSize = "A",
                        documentType = "IMPRESION_RECIBO",
                        copyNumber = 1,
                        charactersNumber = 32,
                        type = PrinterType.WIFI.type,
                        address = ip,
                        port = port.toIntOrNull() ?: 0
                    )
                    val success = savePrinterToDbWIFI(context, entity)
                    val message = if (success) {
                        "Printer saved successfully"
                    } else {
                        "Failed to save printer"
                    }
                    snackbarHostState.showSnackbar(message)
                }
            },
            shape = RoundedCornerShape(24.dp)
        ) { Text("Save WiFi Printer") }
    }
}

suspend fun savePrinterToDbWIFI(context: android.content.Context, printer: PrintersEntity): Boolean {
    return try {
        withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)
            db.printersDAO().insertAll(printer)
        }
        true
    } catch (_: Exception) {
        false
    }
}

