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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
@Composable
fun AddPrinterScreen() {
    var selectedMode by remember { mutableStateOf(PrinterType.USB) }
    val modes = listOf(
        PrinterType.USB to Icons.Default.Usb,
        PrinterType.BLUETOOTH to Icons.Default.Bluetooth,
        PrinterType.WIFI to Icons.Default.Wifi
    )
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                PrinterType.BLUETOOTH -> BluetoothForm()
                PrinterType.WIFI -> WifiForm()
            }
        }
    }
}

@Composable
private fun UsbForm() {
    var name by remember { mutableStateOf("") }
    var charNumber by remember { mutableStateOf("") }
    var copyNumber by remember { mutableStateOf("") }
    var docTypeExpanded by remember { mutableStateOf(false) }
    val docTypes = listOf("DNI", "RUC", "Passport")
    var selectedDoc by remember { mutableStateOf(docTypes.first()) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("USB Printer Name") })
        OutlinedTextField(
            value = charNumber,
            onValueChange = { charNumber = it.takeWhile { c -> c.isDigit() } },
            label = { Text("Characters Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        OutlinedTextField(
            value = copyNumber,
            onValueChange = { copyNumber = it.takeWhile { c -> c.isDigit() } },
            label = { Text("Copy Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Box {
            OutlinedButton(onClick = { docTypeExpanded = true }) {
                Text("Tipo Documento: $selectedDoc")
            }
            DropdownMenu(expanded = docTypeExpanded, onDismissRequest = { docTypeExpanded = false }) {
                docTypes.forEach { type ->
                    DropdownMenuItem(text = { Text(type) }, onClick = {
                        selectedDoc = type; docTypeExpanded = false
                    })
                }
            }
        }
        Button(
            onClick = { saveUsbPrinter(name, charNumber.toIntOrNull() ?: 0, copyNumber.toIntOrNull() ?: 0, selectedDoc) },
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Save USB Printer")
        }
    }
}

@Composable
private fun BluetoothForm() {
    // mock paired devices
    val devices = listOf("BT_Printer_01", "BT_Printer_02")
    var selected by remember { mutableStateOf(devices.first()) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Paired Devices", style = MaterialTheme.typography.titleMedium)
        devices.forEach { dev ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected==dev, onClick = { selected = dev })
                Text(dev)
            }
        }
        Button(
            onClick = { saveBluetoothPrinter(selected) },
            shape = RoundedCornerShape(24.dp)
        ) { Text("Save Bluetooth Printer") }
    }
}

@Composable
private fun WifiForm() {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Printer Name") })
        OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP Address") })
        OutlinedTextField(
            value = port,
            onValueChange = { port = it.takeWhile { c -> c.isDigit() } },
            label = { Text("Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        val context = LocalContext.current
        Button(
            onClick = { saveWifiPrinter(context, name, ip, port.toIntOrNull() ?: 0) },
            shape = RoundedCornerShape(24.dp)
        ) { Text("Save WiFi Printer") }
    }
}

// Mock save functions
fun saveUsbPrinter(name: String, chars: Int, copies: Int, docType: String) {
    // TODO: persist USB settings
}
private fun saveBluetoothPrinter(device: String) {
    // TODO: persist Bluetooth settings
}
private fun saveWifiPrinter(context: android.content.Context, name: String, ip: String, port: Int) {
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
    CoroutineScope(Dispatchers.IO).launch {
        val db = DatabaseProvider.getDatabase(context)
        val repository = PrinterRepository(db.printersDAO())
        val addPrinter = AddPrinters(repository)
        addPrinter(entity)
    }
}
