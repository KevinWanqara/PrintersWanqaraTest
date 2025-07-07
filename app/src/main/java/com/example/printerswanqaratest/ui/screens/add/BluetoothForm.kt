package com.example.printerswanqaratest.ui.screens.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.printerswanqaratest.core.printType.PrinterType
import com.example.printerswanqaratest.data.database.entities.PrintersEntity
import com.example.printerswanqaratest.data.database.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BluetoothForm(snackbarHostState: SnackbarHostState) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Printer Name") })
        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Bluetooth Address") })
        Button(
            onClick = {
                scope.launch {
                    val entity = PrintersEntity(
                        name = name,
                        fontSize = "A",
                        documentType = "IMPRESION_RECIBO",
                        copyNumber = 1,
                        charactersNumber = 32,
                        type = PrinterType.BLUETOOTH.type,
                        address = address,
                        port = 0
                    )
                    val success = savePrinterToDbBT(context, entity)
                    val message = if (success) {
                        "Bluetooth printer saved successfully"
                    } else {
                        "Failed to save Bluetooth printer"
                    }
                    snackbarHostState.showSnackbar(message)
                }
            },
            shape = RoundedCornerShape(24.dp)
        ) { Text("Save Bluetooth Printer") }
    }
}

suspend fun savePrinterToDbBT(context: android.content.Context, printer: PrintersEntity): Boolean {
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
