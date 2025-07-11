package com.example.printerswanqaratest.ui.screens.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.printerswanqaratest.domain.models.BluetoothDomain
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
    var scanning by remember { mutableStateOf(false) }
    var pairingAddress by remember { mutableStateOf<String?>(null) }
    var loadingPair by remember { mutableStateOf(false) }
    val allDevices = remember(scannedDevices, pairedDevices) {
        val pairedAddresses = pairedDevices.map { it.address }
        val uniqueScanned = scannedDevices.filter { it.address !in pairedAddresses }
        pairedDevices + uniqueScanned
    }

    LaunchedEffect(Unit) {
        scanning = true
        onStartScan()
        delay(3000) // 2 seconds, adjust as needed
        scanning = false
        onStopScan()
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = Color.Blue)
                Spacer(Modifier.width(8.dp))
                Text("Dispositivos", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.weight(1f))
                if (scanning) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                Button(onClick = {
                    scanning = !scanning
                    if (scanning) onStartScan() else onStopScan()
                }, modifier = Modifier.padding(start = 8.dp)) {
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
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancelar")
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
            containerColor = if (isPaired) Color(0xFFE3F2FD) else Color.White
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = if (isPaired) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (isPaired) Color(0xFF1976D2) else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(device.name ?: "Desconocido", style = MaterialTheme.typography.bodyLarge ,color = Color.Black)
                Text(device.address, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            if (!isPaired) {
                if (loadingPair) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Button(onClick = onPair, modifier = Modifier.height(32.dp)) {
                        Icon(Icons.Default.AddLink, contentDescription = "Vincular")
                        Spacer(Modifier.width(4.dp))
                        Text("Vincular", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                    }
                }
            } else {
                Icon(Icons.Default.Link, contentDescription = "Paired", tint = Color(0xFF388E3C), modifier = Modifier.size(20.dp))
            }
        }
    }
}
