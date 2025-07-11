package com.example.printerswanqaratest.ui.screens.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printerswanqaratest.core.print.test.PrintBluetoothTest
import com.example.printerswanqaratest.core.print.test.PrintUSBTest
import com.example.printerswanqaratest.core.print.test.PrintWifiTest
import com.example.printerswanqaratest.core.printType.PrinterType
import com.example.printerswanqaratest.data.bluetooth.AndroidBluetoothController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.printerswanqaratest.data.database.DatabaseProvider
import com.example.printerswanqaratest.data.database.repositories.PrinterRepository
import com.example.printerswanqaratest.domain.models.Printers
import com.example.printerswanqaratest.domain.services.GetPrinter
import com.example.printerswanqaratest.ui.screens.add.BluetoothDevicePickerScreen
import com.example.printerswanqaratest.ui.theme.Primary


@Composable
fun EditPrinterScreen(printerId: String?) {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val repository = PrinterRepository(db.printersDAO())
    val getPrinter = GetPrinter(repository)

    // State for printer data
    var printer by remember { mutableStateOf<Printers?>(null) }
    var selectedType by remember { mutableStateOf("USB") }
    var printerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var copyNumber by remember { mutableStateOf("") }
    var characters by remember { mutableStateOf("") }
    val typeOptions = listOf(
        PrinterType.USB to Icons.Default.Usb,
        PrinterType.BLUETOOTH to Icons.Default.Bluetooth,
        PrinterType.WIFI to Icons.Default.Wifi
    )

    val buttonMinWidth = 80.dp
    var showBluetoothPicker by remember { mutableStateOf(false) }
    val bluetoothController = remember { AndroidBluetoothController(context) }
    val pairedDevices by bluetoothController.pairedDevices.collectAsState()
    val scannedDevices by bluetoothController.scannedDevices.collectAsState()
    var bluetoothDevice by remember { mutableStateOf("") }
    // Load printer data
    LaunchedEffect(printerId) {
        if (printerId != null) {
            val loaded = withContext(Dispatchers.IO) { getPrinter(printerId) }
            printer = loaded
            loaded?.let {
                selectedType = it.type
                printerName = it.name
                address = it.address ?: ""
                port = it.port?.toString() ?: ""
                copyNumber = it.copyNumber?.toString() ?: ""
                characters = it.charactersNumber?.toString() ?: ""
            }
        }
    }

    val types = listOf("USB", "BLUETOOTH", "WIFI")

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center

    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Type selection
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                typeOptions.forEach { (type, icon) ->
                    val buttonModifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minWidth = buttonMinWidth)
                        .padding(
                            horizontal = 0.dp,
                        )
                    val isSelected = selectedType == type.type
                    if (isSelected) {
                        Button(
                            onClick = {
                                selectedType = type.type
                                if (type == PrinterType.BLUETOOTH) showBluetoothPicker = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            modifier = buttonModifier
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    type.name,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary),
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(icon, contentDescription = type.type, modifier = Modifier.size(15.dp))
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                selectedType = type.type
                                if (type == PrinterType.BLUETOOTH) showBluetoothPicker = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Primary),
                            modifier = buttonModifier
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    type.name,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(icon, contentDescription = type.type, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // Common fields
            OutlinedTextField(
                value = printerName,
                onValueChange = { printerName = it },
                label = { Text("Printer Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            // Type-specific fields
            when (selectedType) {
                "WIFI" -> {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("IP Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                "BLUETOOTH" -> {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Bluetooth Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }
                // USB has no extra fields
            }
            // Editable copy number and characters
            OutlinedTextField(
                value = characters,
                onValueChange = { characters = it },
                label = { Text("Characters per line") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = copyNumber,
                onValueChange = { copyNumber = it },
                label = { Text("Copy Number") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = {
                    when (selectedType) {
                        PrinterType.BLUETOOTH.type -> {
                            android.util.Log.d("AddPrinterScreen", "Calling PrintBluetoothTest from button")
                            PrintBluetoothTest(context).invoke(bluetoothDevice)
                        }
                        PrinterType.USB.type -> {
                            android.util.Log.d("AddPrinterScreen", "Calling PrintUSBTest from button")
                            PrintUSBTest(context).invoke()
                        }
                        PrinterType.WIFI.type -> {
                            android.util.Log.d("AddPrinterScreen", "Calling PrintWifiTest from button with ip=$address port=$port")
                            PrintWifiTest(address, port.toInt(), "B").invoke()
                        }
                    }
                },colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Probar Impresora")
                }

                Button(onClick = { /* Save changes */ }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Guardar")
                }
            }

        }

        if (showBluetoothPicker && selectedType == PrinterType.BLUETOOTH.type) {
            BluetoothDevicePickerScreen(
                pairedDevices = pairedDevices,
                scannedDevices = scannedDevices,
                onStartScan = { bluetoothController.startDiscovery() },
                onStopScan = { bluetoothController.stopDiscovery() },
                onDeviceSelected = { device ->
                    bluetoothDevice = device.address ?: ""
                    showBluetoothPicker = false
                },
                onDismiss = { showBluetoothPicker = false },
                onPairDevice = { device, onResult ->
                    bluetoothController.pairDevice(device) { success ->
                        if (success) {
                            bluetoothDevice = device.address ?: ""
                        }
                        onResult(success)
                    }
                }
            )

        }
    }

}