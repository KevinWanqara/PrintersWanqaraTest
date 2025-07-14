package com.example.printerswanqara.ui.screens.add


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.printerswanqara.domain.models.BluetoothDomain
import com.example.printerswanqara.ui.theme.Primary
import kotlinx.coroutines.delay
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun BluetoothDevicePickerScreen(
    pairedDevices: List<BluetoothDomain>,
    scannedDevices: List<BluetoothDomain>,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeviceSelected: (BluetoothDomain) -> Unit,
    onPairDevice: (BluetoothDomain, (Boolean) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    var pairingAddress by remember { mutableStateOf<String?>(null) }
    var loadingPair by remember { mutableStateOf(false) }
    val allDevices = remember(scannedDevices, pairedDevices) {
        val pairedAddresses = pairedDevices.map { it.address }
        val uniqueScanned = scannedDevices.filter { it.address !in pairedAddresses }
        pairedDevices + uniqueScanned
    }


    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            scanning = true
            onStartScan()
        }
    }

    // Check and request permissions
    fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            scanning = true
            onStartScan()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }


    LaunchedEffect(Unit) {

        checkAndRequestPermissions()


        scanning = true
        onStartScan()
        delay(3000) // 2 seconds, adjust as needed
        scanning = false
        onStopScan()
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.BluetoothSearching, contentDescription = null, tint = Color.Blue)
                Spacer(Modifier.width(8.dp))
                Text("Dispositivos", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                if (scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                Button(onClick = {
                    scanning = !scanning
                    if (scanning) onStartScan() else onStopScan()
                }, modifier = Modifier.padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)

                    ) {
                    Text(if (scanning) "Parar" else "Buscar")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(allDevices) { device ->
                    val isPaired = pairedDevices.any { it.address == device.address }
                    BluetoothDeviceRow(
                        device = device,
                        isPaired = isPaired,
                        loadingPair = loadingPair && pairingAddress == device.address,
                        onPair = {
                            loadingPair = true
                            pairingAddress = device.address
                            onPairDevice(device) { success ->
                                loadingPair = false
                                pairingAddress = null
                            }
                        },
                        onClick = {
                            if (isPaired) onDeviceSelected(device)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ){
                OutlinedButton(onClick = onDismiss, border = BorderStroke(2.dp, Primary)) {
                    Text( "Cancelar" , color = Primary)
                }
            }

        }
    }
}

@Composable
fun BluetoothDeviceRow(
    device: BluetoothDomain,
    isPaired: Boolean,
    loadingPair: Boolean,
    onPair: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = isPaired && !loadingPair) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaired) Primary.copy(alpha = 0.9f) else Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = if (isPaired) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (isPaired) Color.White else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name ?: "Desconocido", style = MaterialTheme.typography.bodyLarge ,color = if (isPaired) Color.White else Color.Black)
                Text(device.address, style = MaterialTheme.typography.bodySmall, color = if (isPaired) Color.White else Color.Gray)
            }
            if (!isPaired) {
                if (loadingPair) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Button(onClick = onPair, modifier = Modifier.height(32.dp),     colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                        Icon(Icons.Default.AddLink, contentDescription = "Vincular")
                        Spacer(Modifier.width(4.dp))
                        Text("Vincular", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                }
            } else {
                Icon(Icons.Default.Link, contentDescription = "Paired", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}
