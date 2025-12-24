package com.example.printerswanqara.ui.screens.onboarding


import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import com.example.printerswanqara.core.printType.PrinterType
import com.example.printerswanqara.data.bluetooth.AndroidBluetoothController
import com.example.printerswanqara.ui.screens.add.saveBluetoothPrinter
import com.example.printerswanqara.ui.screens.add.saveServerPrinter
import com.example.printerswanqara.ui.screens.add.saveUsbPrinter
import com.example.printerswanqara.ui.screens.add.saveWifiPrinter
import com.example.printerswanqara.ui.theme.Primary
import com.example.printerswanqara.core.document.documentType
import com.example.printerswanqara.utils.NetworkScanner
import com.example.printerswanqara.core.print.test.PrintUSBTest
import com.example.printerswanqara.core.print.test.PrintWifiTest
import com.example.printerswanqara.core.print.test.PrintBluetoothTest
import kotlinx.coroutines.launch

@Composable
fun SimplifiedPrinterSetup(
    onNext: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // State
    var currentSubStep by remember { mutableIntStateOf(1) }
    var selectedMode by remember { mutableStateOf(PrinterType.WIFI) }
    var printerName by remember { mutableStateOf("Impresora Wanqara") }
    var bluetoothDevice by remember { mutableStateOf("") }
    var wifiIp by remember { mutableStateOf("") }
    var wifiPort by remember { mutableIntStateOf(9100) }
    var characters by remember { mutableIntStateOf(42) }
    
    // Scanning/Testing state
    var isScanning by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var foundIps by remember { mutableStateOf<List<String>>(emptyList()) }

    val bluetoothController = remember { AndroidBluetoothController(context) }
    val pairedDevices by bluetoothController.pairedDevices.collectAsState()
    
    val modes = listOf(
        PrinterType.WIFI to Icons.Default.Wifi,
        PrinterType.BLUETOOTH to Icons.Default.Bluetooth,
        PrinterType.USB to Icons.Default.Usb,
        PrinterType.SERVER to Icons.Default.Dns
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            // Header with Back Button and Skip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (currentSubStep > 1) {
                        IconButton(onClick = { currentSubStep-- }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Primary)
                        }
                    }
                    Text(
                        text = "Paso $currentSubStep de 3",
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TextButton(onClick = onNext) {
                    Text("Omitir", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { currentSubStep / 3f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Primary,
                trackColor = Primary.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentSubStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                        } else {
                            slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                        }
                    }
                ) { step ->
                    when (step) {
                        1 -> StepSelectType(
                            selectedMode = selectedMode,
                            onModeSelected = { 
                                selectedMode = it
                                // Set defaults based on mode
                                if (it == PrinterType.SERVER) wifiPort = 51512 else if (it == PrinterType.WIFI) wifiPort = 9100
                            },
                            modes = modes
                        )
                        2 -> StepConfigureDetails(
                            selectedMode = selectedMode,
                            wifiIp = wifiIp,
                            onWifiIpChange = { wifiIp = it },
                            wifiPort = wifiPort,
                            onWifiPortChange = { wifiPort = it },
                            bluetoothDevice = bluetoothDevice,
                            onBluetoothDeviceChange = { bluetoothDevice = it },
                            pairedDevices = pairedDevices,
                            isScanning = isScanning,
                            onScanRequested = {
                                coroutineScope.launch {
                                    isScanning = true
                                    foundIps = NetworkScanner.scanLocalSubnet(51512)
                                    isScanning = false
                                }
                            },
                            foundIps = foundIps,
                            isTesting = isTesting,
                            onTestRequested = {
                                coroutineScope.launch {
                                    isTesting = true
                                    try {
                                        when (selectedMode) {
                                            PrinterType.USB -> PrintUSBTest(context).runTest("A")
                                            PrinterType.BLUETOOTH -> PrintBluetoothTest(context).invoke(bluetoothDevice, "A")
                                            PrinterType.WIFI -> PrintWifiTest(wifiIp.trim(), wifiPort, "A").invoke()
                                            PrinterType.SERVER -> snackbarHostState.showSnackbar("Test no disponible para servidor")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Error en el test: ${e.message}")
                                    }
                                    isTesting = false
                                }
                            }
                        )
                        3 -> StepFinalize(
                            printerName = printerName,
                            onPrinterNameChange = { printerName = it },
                            characters = characters,
                            onCharactersChange = { characters = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (currentSubStep < 3) {
                        currentSubStep++
                    } else {
                        coroutineScope.launch {
                            val docTypeObj = documentType()
                            val allDocKeys = docTypeObj.getDocuments().map { docTypeObj.findKeyByDocument(it) }
                            
                            var allSuccess = true
                            for (docKey in allDocKeys) {
                                val success = when (selectedMode) {
                                    PrinterType.USB -> saveUsbPrinter(context, printerName, characters, 1, docKey)
                                    PrinterType.BLUETOOTH -> saveBluetoothPrinter(context, printerName, bluetoothDevice, 0, docKey, 1, characters)
                                    PrinterType.WIFI -> saveWifiPrinter(context, printerName, wifiIp, wifiPort, docKey, 1, characters)
                                    PrinterType.SERVER -> saveServerPrinter(context, printerName, wifiIp, docKey, 1, characters)
                                }
                                if (!success) allSuccess = false
                            }
                            
                            if (allSuccess) {
                                onNext()
                            } else {
                                snackbarHostState.showSnackbar("Error al guardar algunas configuraciones")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = when (currentSubStep) {
                    1 -> true
                    2 -> when (selectedMode) {
                        PrinterType.USB -> true
                        PrinterType.BLUETOOTH -> bluetoothDevice.isNotBlank()
                        PrinterType.WIFI, PrinterType.SERVER -> wifiIp.isNotBlank()
                    }
                    3 -> printerName.isNotBlank()
                    else -> false
                }
            ) {
                Text(if (currentSubStep < 3) "Siguiente" else "Guardar y Continuar", fontWeight = FontWeight.Bold)
                if (currentSubStep < 3) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun StepSelectType(
    selectedMode: PrinterType,
    onModeSelected: (PrinterType) -> Unit,
    modes: List<Pair<PrinterType, ImageVector>>
) {
    Column {
        Text("¿Cómo se conecta tu impresora?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Selecciona el tipo de conexión que utiliza tu equipo.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        
        // Visual Connection Diagram
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            ConnectionAnimation(selectedMode)
        }

        modes.forEach { (mode, icon) ->
            PrinterModeItem(
                mode = mode,
                icon = icon,
                isSelected = selectedMode == mode,
                onClick = { onModeSelected(mode) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun ConnectionAnimation(mode: PrinterType) {
    val infiniteTransition = rememberInfiniteTransition(label = "connection")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Phone Icon
        Icon(
            Icons.Default.Smartphone,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Connection Path
        Box(modifier = Modifier.width(80.dp).height(40.dp), contentAlignment = Alignment.Center) {
            when(mode) {
                PrinterType.WIFI, PrinterType.SERVER -> {
                    Icon(Icons.Default.Wifi, contentDescription = null, tint = Primary.copy(alpha = alpha))
                }
                PrinterType.BLUETOOTH -> {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Primary.copy(alpha = alpha))
                }
                PrinterType.USB -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Primary,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // Moving particles for all modes to show data flow
            Canvas(modifier = Modifier.fillMaxSize()) {
                val particleCount = 3
                for (i in 0 until particleCount) {
                    val progress = (offset + i * (20f / particleCount)) % 20f
                    val xPos = (progress / 20f) * size.width
                    drawCircle(
                        color = Primary,
                        radius = 4f,
                        center = Offset(xPos, size.height / 2)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Printer Icon
        Icon(
            Icons.Default.Print,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PrinterSetupVisuals(mode: PrinterType) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Primary.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when(mode) {
            PrinterType.WIFI -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Imprime un 'Self Test'", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Busca la línea 'IP Address'", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Ej: 192.168.1.87", style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = Primary)
                    }
                }
            }
            PrinterType.BLUETOOTH -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Ajustes de Android", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Vincula primero la impresora", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Clave usual: 0000 o 1234", style = MaterialTheme.typography.bodySmall, color = Primary)
                    }
                }
            }
            PrinterType.USB -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Usb, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Conexión Directa", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Usa un cable OTG si es necesario", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Acepta el permiso USB", style = MaterialTheme.typography.bodySmall, color = Primary)
                    }
                }
            }
            PrinterType.SERVER -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Printers Desktop", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text("Instala el agente en tu PC", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("La PC debe estar encendida", style = MaterialTheme.typography.bodySmall, color = Primary)
                    }
                }
            }
        }
    }
}

@Composable
fun StepConfigureDetails(
    selectedMode: PrinterType,
    wifiIp: String,
    onWifiIpChange: (String) -> Unit,
    wifiPort: Int,
    onWifiPortChange: (Int) -> Unit,
    bluetoothDevice: String,
    onBluetoothDeviceChange: (String) -> Unit,
    pairedDevices: List<com.example.printerswanqara.domain.models.BluetoothDomain>,
    isScanning: Boolean,
    onScanRequested: () -> Unit,
    foundIps: List<String>,
    isTesting: Boolean,
    onTestRequested: () -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text("Detalles de Conexión", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Visual Helper
        PrinterSetupVisuals(selectedMode)
        Spacer(modifier = Modifier.height(24.dp))

        when (selectedMode) {
            PrinterType.USB -> {
                InfoCard(icon = Icons.Default.Usb, title = "Conexión USB", description = "Ideal para puntos de venta fijos. Conecta el cable y otorga los permisos necesarios.")
            }
            PrinterType.BLUETOOTH -> {
                Text("Selecciona tu impresora Bluetooth", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                if (pairedDevices.isEmpty()) {
                    InfoCard(icon = Icons.Default.BluetoothDisabled, title = "No hay dispositivos", description = "Vincula tu impresora en los ajustes de Bluetooth de tu teléfono.")
                } else {
                    pairedDevices.forEach { device ->
                        DeviceItem(
                            name = device.name ?: "Desconocido",
                            address = device.address,
                            isSelected = bluetoothDevice == device.address,
                            onClick = { onBluetoothDeviceChange(device.address) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            PrinterType.WIFI, PrinterType.SERVER -> {
                Text(
                    text = if (selectedMode == PrinterType.WIFI) "Conexión WiFi: Ideal para impresoras de cocina o compartidas." 
                           else "Servidor: Permite imprimir desde cualquier lugar a través de internet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = wifiIp,
                    onValueChange = onWifiIpChange,
                    label = { Text("Dirección IP / Host") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null) }
                )
                
                if (selectedMode == PrinterType.SERVER) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onScanRequested,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.1f), contentColor = Primary)
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Escaneando red...")
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Buscar impresoras en la red")
                        }
                    }
                    
                    if (foundIps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Estas son las computadoras que tienen instalada la aplicación Printers Desktop en tu red local:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        foundIps.chunked(2).forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunk.forEach { ip ->
                                    val isSelected = wifiIp == ip
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onWifiIpChange(ip) },
                                        label = { Text(ip) },
                                        leadingIcon = { Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Primary,
                                            selectedLabelColor = Color.White,
                                            selectedLeadingIconColor = Color.White
                                        )
                                    )
                                }
                                if (chunk.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
                
                if (selectedMode == PrinterType.WIFI) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Opciones de Puerto", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(9100 to "LAN", 6001 to "WIFI").forEach { (port, label) ->
                            FilterChip(
                                selected = wifiPort == port,
                                onClick = { onWifiPortChange(port) },
                                label = { Text("$port ($label)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = wifiPort.toString(),
                    onValueChange = { onWifiPortChange(it.toIntOrNull() ?: 9100) },
                    label = { Text("Puerto") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                    readOnly = selectedMode == PrinterType.SERVER,
                    enabled = selectedMode != PrinterType.SERVER,
                    supportingText = if (selectedMode == PrinterType.SERVER) {
                        { Text("Puerto fijo para Printers Desktop") }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test Button
        if (selectedMode != PrinterType.SERVER) {
            OutlinedButton(
                onClick = onTestRequested,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isTesting && when(selectedMode) {
                    PrinterType.USB -> true
                    PrinterType.BLUETOOTH -> bluetoothDevice.isNotBlank()
                    PrinterType.WIFI -> wifiIp.isNotBlank()
                    else -> false
                },
                border = BorderStroke(1.dp, Primary)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Probando...", color = Primary)
                } else {
                    Icon(Icons.Default.Print, contentDescription = null, tint = Primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Probar Impresora", color = Primary)
                }
            }
        }
    }
}

@Composable
fun StepFinalize(
    printerName: String,
    onPrinterNameChange: (String) -> Unit,
    characters: Int,
    onCharactersChange: (Int) -> Unit
) {
    Column {
        Text("Personalización", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = printerName,
            onValueChange = onPrinterNameChange,
            label = { Text("Nombre de la Impresora") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Ej: Impresora Caja") }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Tamaño de papel (Caracteres)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("La mayoría de impresoras de 58mm usan 32 chars, y las de 80mm usan 42 o 48.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(32, 42, 48).forEach { valOption ->
                val isSelected = characters == valOption
                FilterChip(
                    selected = isSelected,
                    onClick = { onCharactersChange(valOption) },
                    label = { Text("$valOption chars") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = if (characters == 0) "" else characters.toString(),
            onValueChange = { onCharactersChange(it.toIntOrNull() ?: 0) },
            label = { Text("Caracteres personalizados") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
    }
}

@Composable
fun PrinterModeItem(
    mode: PrinterType,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, if (isSelected) Primary else Color.LightGray.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(mode.name, fontWeight = FontWeight.Bold, color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface)
                Text(
                    text = when(mode) {
                        PrinterType.WIFI -> "Red Local / IP"
                        PrinterType.BLUETOOTH -> "Inalámbrica"
                        PrinterType.USB -> "Cable Físico"
                        PrinterType.SERVER -> "Printers Desktop"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Primary)
            }
        }
    }
}

@Composable
fun DeviceItem(
    name: String,
    address: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isSelected) BorderStroke(1.dp, Primary) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = if (isSelected) Primary else Color.Gray)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(address, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = Primary)
            } else {
                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun InfoCard(icon: ImageVector, title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
