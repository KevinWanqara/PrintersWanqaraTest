package com.example.printerswanqara.ui.screens.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*


import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.printerswanqara.core.document.documentType
import com.example.printerswanqara.core.print.test.PrintBluetoothTest
import com.example.printerswanqara.core.print.test.PrintUSBTest
import com.example.printerswanqara.core.print.test.PrintWifiTest
import com.example.printerswanqara.core.printType.PrinterType
import com.example.printerswanqara.data.bluetooth.AndroidBluetoothController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.printerswanqara.data.database.DatabaseProvider
import com.example.printerswanqara.data.database.repositories.PrinterRepository
import com.example.printerswanqara.domain.models.Printers
import com.example.printerswanqara.domain.services.GetPrinter
import com.example.printerswanqara.ui.screens.add.BluetoothDevicePickerScreen
import com.example.printerswanqara.ui.screens.add.saveBluetoothPrinter
import com.example.printerswanqara.ui.screens.add.saveUsbPrinter
import com.example.printerswanqara.ui.screens.add.saveWifiPrinter
import com.example.printerswanqara.ui.theme.Primary
import kotlinx.coroutines.launch


@Composable
fun EditPrinterScreen(printerId: String?,navController: NavController) {
    val context = LocalContext.current
    val db = DatabaseProvider.getDatabase(context)
    val repository = PrinterRepository(db.printersDAO())
    val getPrinter = GetPrinter(repository)

    // State for printer data
    var printer by remember { mutableStateOf<Printers?>(null) }
    var selectedType by remember { mutableStateOf("USB") }
    var printerName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var port by remember { mutableStateOf(0) }
    var copyNumber by remember { mutableStateOf(0) }
    var characters by remember { mutableStateOf(0) }
    var documentType by remember { mutableStateOf("") }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

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
    val testFont by remember { mutableStateOf("A") } // Default to "A"
    val coroutineScope = rememberCoroutineScope()
    val docTypeObj = documentType()
    // Load printer data
    LaunchedEffect(printerId) {
        if (printerId != null) {
            val loaded = withContext(Dispatchers.IO) { getPrinter(printerId) }
            printer = loaded
            loaded.let {
                selectedType = it.type
                printerName = it.name
                address = it.address ?: ""
                port = it.port ?: 0
                copyNumber = it.copyNumber
                characters = it.charactersNumber
                documentType = it.documentType ?: ""
            }
        }
    }

   Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()

        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Text(
                    text = docTypeObj.findDocumentByKey(documentType) ?: documentType,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.fillMaxWidth()
                )


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
                    label = { Text("Nombre de Impresora") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // Type-specific fields
                when (selectedType) {
                    "WIFI" -> {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Dirección IP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = if (port == 0) "" else port.toString(),
                            onValueChange = { port = it.toIntOrNull() ?: 0 },
                            label = { Text("Puerto") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Dropdown for port options
                        var expanded by remember { mutableStateOf(false) }
                        val portOptions = listOf(9100 to "LAN", 6001 to "WIFI")
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = if (port == 0) "Seleccionar puerto" else "$port (${portOptions.find { it.first == port }?.second ?: "Personalizado"})")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                portOptions.forEach { (portData, label) ->
                                    DropdownMenuItem(
                                        text = { Text("$portData ($label)") },
                                        onClick = {
                                            port = portData
                                            expanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Personalizado") },
                                    onClick = {
                                        port = 0
                                        expanded = false
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    "BLUETOOTH" -> {

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { showBluetoothPicker = true },
                            colors = CardDefaults.cardColors(containerColor = Primary),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Dirección MAC", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    Text(address, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                }
                                Text("Cambiar", color = Primary, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    // USB has no extra fields
                }
                // Editable copy number and characters
                OutlinedTextField(
                    value = characters.toString(),
                    onValueChange = { newValue ->
                        characters = newValue.toIntOrNull() ?: 0
                    },
                    label = { Text("Caracteres") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = copyNumber.toString(),
                    onValueChange = { newValue ->
                        copyNumber = newValue.toIntOrNull() ?: 0
                    },
                    label = { Text("Numero de copias") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.weight(1f, fill = true))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(onClick = {
                        when (selectedType) {
                            PrinterType.BLUETOOTH.type -> {
                                android.util.Log.d("AddPrinterScreen", "Calling PrintBluetoothTest from button")
                                PrintBluetoothTest(context).invoke(address,testFont)
                            }
                            PrinterType.USB.type -> {
                                android.util.Log.d("AddPrinterScreen", "Calling PrintUSBTest from button")
                                PrintUSBTest(context).invoke(testFont)
                            }
                            PrinterType.WIFI.type -> {
                                android.util.Log.d("AddPrinterScreen", "Calling PrintWifiTest from button with ip=$address port=$port")
                                PrintWifiTest(address, port, testFont).invoke()
                            }
                        }
                    },colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Text("Probar Impresora")
                    }

                    Button(onClick = {

                        coroutineScope.launch {
                            //val docTypeObj = documentType()
                            //val docKeys = docTypeObj.getDocuments().mapNotNull { docTypeObj.findKeyByDocument(it) }
                            var allSuccess = true
                            println(
                                "Saving printer: $printerName, Mode: ${selectedType}, Characters: $characters, Copies: $copyNumber, Docs: ${documentType}"
                            )


                                println(
                                    "Saving printer for document type: $documentType"
                                )
                                val success = when(selectedType) {
                                    PrinterType.USB.type -> {
                                        saveUsbPrinter(context,printerName, characters, copyNumber, documentType)
                                    }
                                    PrinterType.BLUETOOTH.type -> {
                                        saveBluetoothPrinter(context, printerName, bluetoothDevice, port,documentType,copyNumber,characters)
                                    }
                                    PrinterType.WIFI.type -> {
                                        saveWifiPrinter(context, printerName, address, port,documentType,copyNumber,characters)
                                    }
                                    else -> false // Handle unsupported printer types
                                }
                                if (!success) allSuccess = false



                            if (allSuccess) {
                                snackbarHostState.showSnackbar("Impresora acualizada correctamente")


                                navController.navigate("list_printers") // Navigate to the home screen

                            } else {
                                snackbarHostState.showSnackbar("Error saving printer for some document types")
                            }
                        }

                    }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
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
                        address = device.address
                        showBluetoothPicker = false
                    },
                    onDismiss = { showBluetoothPicker = false },
                    onPairDevice = { device, onResult ->
                        bluetoothController.pairDevice(device) { success ->
                            if (success) {
                                address = device.address
                            }
                            onResult(success)
                        }
                    }
                )

            }
        }

    }

}