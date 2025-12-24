package com.example.printerswanqara.ui.screens.add

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.printerswanqara.core.printType.PrinterType
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dns
import com.example.printerswanqara.ui.theme.Primary
import com.example.printerswanqara.ui.theme.Secondary
import com.example.printerswanqara.core.document.documentType
import androidx.compose.material3.ButtonDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Check

import com.example.printerswanqara.data.database.entities.PrintersEntity
import androidx.compose.ui.platform.LocalContext
import com.example.printerswanqara.data.database.DatabaseProvider
import com.example.printerswanqara.data.database.repositories.PrinterRepository
import com.example.printerswanqara.domain.services.AddPrinters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import coil.ImageLoader

import com.example.printerswanqara.R
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import com.example.printerswanqara.data.bluetooth.AndroidBluetoothController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Print
import androidx.navigation.NavController

import com.example.printerswanqara.core.print.test.PrintUSBTest
import com.example.printerswanqara.core.print.test.PrintWifiTest
import com.example.printerswanqara.core.print.test.PrintBluetoothTest


@OptIn(ExperimentalLayoutApi::class)
//@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AddPrinterScreen(navController: NavController) {

    var selectedMode by remember { mutableStateOf(PrinterType.WIFI) }
    val snackbarHostState = remember { SnackbarHostState() }
    val modes = listOf(
        PrinterType.USB to Icons.Default.Usb,
        PrinterType.BLUETOOTH to Icons.Default.Bluetooth,
        PrinterType.WIFI to Icons.Default.Wifi,
        PrinterType.SERVER to Icons.Default.Dns
    )
    // Step state
    var step by remember { mutableIntStateOf(1) }
    // Data states for each step
    var printerName by remember { mutableStateOf("") }
    var bluetoothDevice by remember { mutableStateOf("") }
    var wifiIp by remember { mutableStateOf("") }
    var wifiPort by remember { mutableIntStateOf(0) }
    var characters by remember { mutableIntStateOf(0) }
    var copyNumber by remember { mutableIntStateOf(0) }
    var showBluetoothPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val gifEnabledLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
            }
            .build()
    }
    val bluetoothController = remember { AndroidBluetoothController(context) }
    val pairedDevices by bluetoothController.pairedDevices.collectAsState()
    val scannedDevices by bluetoothController.scannedDevices.collectAsState()
    val documentType = remember { mutableStateOf("") }
    val selectedDocTypes = remember { mutableStateOf(setOf<String>()) }
    var testFont by remember { mutableStateOf("A") } // Default to "A"


    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {  padding ->
        val animatedProgress by animateFloatAsState(
            targetValue = step / 5f,
            animationSpec = tween(durationMillis = 600)
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Secondary,
            trackColor = Secondary.copy(alpha = 0.2f),
        )
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            // Show impresion.gif from drawable using Accompanist Coil for GIF support
            LaunchedEffect(Unit) { }

            // Step indicator as a progress bar


            // Step 1: Type and value inputs
            if (step == 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    modes.chunked(2).forEach { rowModes ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowModes.forEach { (mode, icon) ->
                                val isSelected = mode == selectedMode
                                val buttonModifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minWidth = 80.dp)
                                    .padding(horizontal = 0.dp)

                                if (isSelected) {
                                    Button(
                                        onClick = {
                                            selectedMode = mode
                                            if (mode == PrinterType.BLUETOOTH) {
                                                showBluetoothPicker = true
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        modifier = buttonModifier,
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                mode.name,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary),
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(icon, contentDescription = mode.type, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            selectedMode = mode
                                            if (mode == PrinterType.BLUETOOTH) {
                                                showBluetoothPicker = true
                                            }
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
                                                mode.name,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(icon, contentDescription = mode.type, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                when (selectedMode) {
                    PrinterType.USB -> {
                        OutlinedTextField(
                            value = printerName,
                            onValueChange = { printerName = it },
                            label = { Text("Nombre de Impresora") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    PrinterType.BLUETOOTH -> {
                        OutlinedTextField(
                            value = printerName,
                            onValueChange = { printerName = it },
                            label = { Text("Nombre de Impresora") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (bluetoothDevice.isNotBlank()) {
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
                                        Text(bluetoothDevice, style = MaterialTheme.typography.bodyLarge, color = Color.White)
                                    }
                                    Text("Cambiar", color = Primary, modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showBluetoothPicker = true },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                Text("Seleccionar dispositivo Bluetooth")
                            }
                        }
                    }

                    PrinterType.WIFI -> {
                        OutlinedTextField(
                            value = printerName,
                            onValueChange = { printerName = it },
                            label = { Text("Nombre de Impresora ") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = wifiIp,
                            onValueChange = { wifiIp = it },
                            label = { Text("Dirección IP") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = if (wifiPort == 0) "" else wifiPort.toString(),
                            onValueChange = { wifiPort = it.toIntOrNull() ?: 0 },
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
                                Text(text = if (wifiPort == 0) "Seleccionar puerto" else "$wifiPort (${portOptions.find { it.first == wifiPort }?.second ?: "Personalizado"})")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                portOptions.forEach { (port, label) ->
                                    DropdownMenuItem(
                                        text = { Text("$port ($label)") },
                                        onClick = {
                                            wifiPort = port
                                            expanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Personalizado") },
                                    onClick = {
                                        wifiPort = 0
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    PrinterType.SERVER -> {
                        OutlinedTextField(
                            value = printerName,
                            onValueChange = { printerName = it },
                            label = { Text("Nombre de Impresora") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = wifiIp,
                            onValueChange = { wifiIp = it },
                            label = { Text("Dirección IP / Host") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = "51512",
                            onValueChange = { },
                            label = { Text("Puerto") },
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            enabled = false,
                            supportingText = { Text("Puerto fijo para Printers Desktop") }
                        )
                    }
                }
                Spacer(Modifier.weight(1f, fill = true))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    Button(
                        onClick = { step = 2 },
                        enabled = when (selectedMode) {
                            PrinterType.USB -> printerName.isNotBlank()
                            PrinterType.BLUETOOTH -> printerName.isNotBlank() && bluetoothDevice.isNotBlank()
                            PrinterType.WIFI -> printerName.isNotBlank() && wifiIp.isNotBlank() && wifiPort > 0
                            PrinterType.SERVER -> printerName.isNotBlank() && wifiIp.isNotBlank()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    ) {
                        Text("Siguiente")
                    }
                }

            }
            //Step 2 : Test Font
            if (step == 2) {

                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Fuente para Test:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { testFont = "A" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (testFont == "A") Primary else Color.LightGray
                            )
                        ) {
                            Text("A (Normal)", color = if (testFont == "A") Color.White else Color.Black)
                        }
                        Button(
                            onClick = { testFont = "B" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (testFont == "B") Primary else Color.LightGray
                            )
                        ) {
                            Text("B (Pequeña)", color = if (testFont == "B") Color.White else Color.Black)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f, fill = true))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = { step = 1 },
                            border = BorderStroke(2.dp, Primary),
                        ) { Text("Atras") }
                        Button(
                            onClick = { step = 3 },
                            enabled = testFont.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("Siguiente") }
                    }
                }
            }
            // Step 2: Character selection
            if (step == 3) {
                var testTriggered by remember { mutableStateOf(false) }
                LaunchedEffect(step, selectedMode) {
                    if (!testTriggered) {
                        when (selectedMode) {
                            PrinterType.BLUETOOTH -> {
                                android.util.Log.d("AddPrinterScreen", "Calling PrintBluetoothTest on step load")
                                PrintBluetoothTest(context).invoke(bluetoothDevice , testFont )
                            }
                            PrinterType.USB -> {
                                android.util.Log.d("AddPrinterScreen", "Calling PrintUSBTest on step load")
                                //could fail on non tiramisu sdk
                                //TODO handle this for legacy devices

                                val result = PrintUSBTest(context).runTest(testFont)
                                if (!result) {
                                    snackbarHostState.showSnackbar("Error al conectar con la impresora USB")
                                }

                            }
                            PrinterType.WIFI -> {
                                android.util.Log.d("AddPrinterScreen", "Calling PrintWifiTest on step load with ip=$wifiIp port=$wifiPort")
                                PrintWifiTest(wifiIp.trim(), wifiPort, "B").invoke()
                            }
                            PrinterType.SERVER -> {
                                android.util.Log.d("AddPrinterScreen", "Server type selected, skipping legacy test")
                            }
                        }
                        testTriggered = true
                    }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Número de caracteres por línea:")
                    // Show animated GIF from assets (must be in app/src/main/assets/impresion.gif)
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(vertical = 16.dp)) {

                        AsyncImage(
                            imageLoader = gifEnabledLoader,

                            model = R.drawable.impresion,
                            contentDescription = "Impresion GIF",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    OutlinedTextField(
                        value = if (characters == 0) "" else characters.toString(),
                        onValueChange = { characters = it.toIntOrNull() ?: 0 },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Caracteres") }
                    )
                    Spacer(modifier = Modifier.weight(1f, fill = true))
                    Button(
                        onClick = {
                            when (selectedMode) {
                                PrinterType.BLUETOOTH -> {
                                    android.util.Log.d("AddPrinterScreen", "Calling PrintBluetoothTest from button")
                                    PrintBluetoothTest(context).invoke(bluetoothDevice, testFont)
                                }
                                PrinterType.USB -> {
                                    android.util.Log.d("AddPrinterScreen", "Calling PrintUSBTest from button")
                                    PrintUSBTest(context).invoke(testFont)
                                }
                                PrinterType.WIFI -> {
                                    android.util.Log.d("AddPrinterScreen", "Calling PrintWifiTest from button with ip=$wifiIp port=$wifiPort")
                                    PrintWifiTest(wifiIp, wifiPort, testFont).invoke()
                                }
                                PrinterType.SERVER -> {
                                    android.util.Log.d("AddPrinterScreen", "Server type selected, skipping legacy test")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Reimprimir", tint = Color.White)
                        Text("Reimprimir" , color = Color.White, modifier = Modifier.padding(start = 8.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(onClick = { step = 2 },   border = BorderStroke(2.dp, Primary)) { Text("Atras") }
                        Button(
                            onClick = { step = 4 },
                            enabled = characters > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("Siguiente") }
                    }
                }
            }
            // Step 4: Copy number
            if (step == 4) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("Numero de copias:")
                    OutlinedTextField(
                        value = if (copyNumber == 0) "" else copyNumber.toString(),
                        onValueChange = { copyNumber = it.toIntOrNull() ?: 0 },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.weight(1f, fill = true))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { step = 2 },
                            border = BorderStroke(2.dp, Primary),
                            ) { Text("Atras") }
                        Button(
                            onClick = { step = 5 },
                            enabled = copyNumber > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary)
                        ) { Text("Siguiente") }
                    }
                }
            }
            // Step 5: Document type selection
            if (step == 5) {
                Text("Que tipo de documento desea imprimir:")
                val docTypeObj = remember { documentType() }
                val docValues = remember { docTypeObj.getDocuments() }

                Box(Modifier.fillMaxSize().padding(vertical = 8.dp),
                    contentAlignment =  Alignment.TopCenter

                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        docValues.chunked(2).forEach { rowTypes ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowTypes.forEach { value ->
                                    val key = docTypeObj.findKeyByDocument(value) ?: value
                                    val isSelected = selectedDocTypes.value.contains(key)
                                    Card(
                                        onClick = {
                                            selectedDocTypes.value = if (isSelected) {
                                                selectedDocTypes.value - key
                                            } else {
                                                selectedDocTypes.value + key
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(3f / 2f)
                                            .padding(4.dp),
                                        border = if (isSelected) BorderStroke(2.dp, Primary) else BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Primary.copy(alpha = 1f) else MaterialTheme.colorScheme.surface
                                        )

                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight()
                                                .padding( 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                value,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .align(Alignment.TopEnd)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }


                    Row(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .fillMaxWidth()
                            .background( MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                            ,
                        horizontalArrangement = Arrangement.SpaceBetween

                    ) {
                        OutlinedButton(onClick = { step = 3 }, border = BorderStroke(2.dp, Primary)) { Text("Atras") }
                        Button(
                            onClick = { step = 6 },
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            enabled = selectedDocTypes.value.isNotEmpty()
                        ) { Text("Siguiente") }
                    }
                }




            }
            // Step 6: Save
            if (step == 6) {
                Text(
                    "Revisar y Guardar",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(16.dp))
                // Show selected printer type icon and name
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                    val icon = when (selectedMode) {
                        PrinterType.USB -> Icons.Default.Usb
                        PrinterType.WIFI -> Icons.Default.Wifi
                        PrinterType.BLUETOOTH -> Icons.Default.Bluetooth
                        PrinterType.SERVER -> Icons.Default.Wifi
                    }
                    Icon(icon, contentDescription = selectedMode.type, tint = Primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = selectedMode.type,
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary
                    )
                }
                Text(
                    "Nombre: $printerName",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (selectedMode == PrinterType.BLUETOOTH) Text(
                    "Dispositivo: $bluetoothDevice",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                if (selectedMode == PrinterType.WIFI) {
                    Text(
                        "IP: $wifiIp",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Puerto: $wifiPort",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (selectedMode == PrinterType.SERVER) {
                    Text(
                        "IP/Host: $wifiIp",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Puerto: 51512 (Fijo)",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "Caracteres por línea: $characters",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Copias: $copyNumber",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                // Show selected document types as small cards
                val docTypeObj = documentType()
                val selectedDocNames = selectedDocTypes.value.mapNotNull { docTypeObj.findDocumentByKey(it) }
                Text("Documentos seleccionados:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp , horizontal = 4.dp),

                ) {
                    selectedDocNames.forEach { docName ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.15f)),
                            modifier = Modifier.height(32.dp).
                                padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(docName, style = MaterialTheme.typography.bodySmall, color = Primary)
                            }
                        }
                    }
                }
                Spacer(Modifier.weight(1f, fill = true))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(onClick = { step = 4 }, border = BorderStroke(2.dp, Primary)) { Text("Regresar") }
                    Button(onClick = {
                        coroutineScope.launch {
                            //val docTypeObj = documentType()
                            //val docKeys = docTypeObj.getDocuments().mapNotNull { docTypeObj.findKeyByDocument(it) }
                            var allSuccess = true
                            println(
                                "Saving printer: $printerName, Mode: ${selectedMode.type}, Characters: $characters, Copies: $copyNumber, Docs: ${selectedDocTypes.value.joinToString(", ")}"
                            )

                            for (docKey in selectedDocTypes.value){
                                println(
                                    "Saving printer for document type: $docKey"
                                )
                                val success = when(selectedMode) {
                                    PrinterType.USB -> {
                                        saveUsbPrinter(context,printerName, characters, copyNumber, docKey)
                                    }
                                    PrinterType.BLUETOOTH -> {
                                        saveBluetoothPrinter(context, printerName, bluetoothDevice, wifiPort,docKey,copyNumber,characters)
                                    }
                                    PrinterType.WIFI -> {
                                        saveWifiPrinter(context, printerName, wifiIp, wifiPort,docKey,copyNumber,characters)
                                    }
                                    PrinterType.SERVER -> {
                                        saveServerPrinter(context, printerName, wifiIp, docKey, copyNumber, characters)
                                    }
                                }
                                if (!success) allSuccess = false
                            }


                            if (allSuccess) {
                                snackbarHostState.showSnackbar("Impresora Guardada Correctamente")
                                // Navigate to home screen after saving
                                step = 1
                                printerName = ""
                                bluetoothDevice = ""
                                wifiIp = ""
                                wifiPort = 0
                                characters = 0
                                copyNumber = 0
                                documentType.value = ""
                                navController.navigate("list_printers") // Navigate to the home screen

                            } else {
                                snackbarHostState.showSnackbar("Error al guardar la impresora")
                            }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = Primary)) { Text("Guardar") }
                }
            }
        }

        if (showBluetoothPicker && selectedMode == PrinterType.BLUETOOTH) {
            BluetoothDevicePickerScreen(
                pairedDevices = pairedDevices,
                scannedDevices = scannedDevices,
                onStartScan = { bluetoothController.startDiscovery() },
                onStopScan = { bluetoothController.stopDiscovery() },
                onDeviceSelected = { device ->
                    bluetoothDevice = device.address
                    showBluetoothPicker = false
                },
                onDismiss = { showBluetoothPicker = false },
                onPairDevice = { device, onResult ->
                    bluetoothController.pairDevice(device) { success ->
                        if (success) {
                            bluetoothDevice = device.address
                        }
                        onResult(success)
                    }
                }
            )

        }

    }

        }


suspend fun saveUsbPrinter(
    context: Context,
    name: String,
    charactersNumber: Int,
    copyNumber: Int,
    documentType: String
): Boolean {
    val entity = PrintersEntity(
        name = name,
        fontSize = "A",
        documentType = documentType,
        copyNumber = copyNumber,
        charactersNumber = charactersNumber,
        type = PrinterType.USB.type,
        address = "",
        port = 0
    )
    println(
        "Saving USB printer: $name, Characters: $charactersNumber, Copies: $copyNumber, Document Type: $documentType"
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


suspend fun saveBluetoothPrinter(
    context: Context,
    name: String,
    ip: String,
    port: Int,
    documentType: String,
    copyNumber: Int ,
    charactersNumber: Int

):Boolean {
    val entity = PrintersEntity(
        name = name,
        fontSize = "A",
        documentType = documentType,
        copyNumber = copyNumber,
        charactersNumber = charactersNumber,
        type = PrinterType.BLUETOOTH.type,
        address = ip,
        port = port
    )
    println(
        "Saving Bluetooth printer: $name, IP: $ip, Port: $port, Characters: $charactersNumber, Copies: $copyNumber, Document Type: $documentType"
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
suspend fun saveWifiPrinter(
    context: Context,
    name: String,
    ip: String,
    port: Int,
    documentType: String,
    copyNumber: Int ,
    charactersNumber: Int
): Boolean {
    val entity = PrintersEntity(
        name = name,
        fontSize = "A",
        documentType = documentType,
        copyNumber = copyNumber,
        charactersNumber = charactersNumber,
        type = PrinterType.WIFI.type,
        address = ip,
        port = port
    )
    println(
        "Saving WiFi printer: $name, IP: $ip, Port: $port, Characters: $charactersNumber, Copies: $copyNumber, Document Type: $documentType"
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

suspend fun saveServerPrinter(
    context: Context,
    name: String,
    address: String,
    documentType: String,
    copyNumber: Int,
    charactersNumber: Int
): Boolean {
    val entity = PrintersEntity(
        name = name,
        fontSize = "A",
        documentType = documentType,
        copyNumber = copyNumber,
        charactersNumber = charactersNumber,
        type = PrinterType.SERVER.type,
        address = address,
        port = 51512 // Fixed port for server printers
    )
    println(
        "Saving SERVER printer: $name, Address: $address, Characters: $charactersNumber, Copies: $copyNumber, Document Type: $documentType"
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
