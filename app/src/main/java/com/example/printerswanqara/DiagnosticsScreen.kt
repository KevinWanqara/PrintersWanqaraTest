package com.example.printerswanqara

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.example.printerswanqara.data.database.repositories.PrinterRepository
import com.example.printerswanqara.domain.services.GetAllPrinters
import com.example.printerswanqara.data.database.DatabaseProvider
import com.example.printerswanqara.core.print.utils.printer1.Discrimination
import com.example.printerswanqara.core.print.utils.printer1.PrinterBuilder
import com.example.printerswanqara.core.print.utils.printer1.PrintDiagnosticsBus
import com.example.printerswanqara.core.print.utils.printer1.WifiPrintTester
import com.example.printerswanqara.core.printType.PrinterType
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.printerswanqara.ui.theme.Primary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * DiagnosticsScreen
 * - TCP connect/send test (raw ESC/POS)
 * - Ping using system `ping` command
 * - Traceroute using system `traceroute` or `tracepath`
 *
 * Note: On Android devices `ping`/`traceroute` may not be available on all devices.
 * This implementation falls back gracefully and logs errors.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val hostState = rememberSaveable { mutableStateOf("") }
    val portState = rememberSaveable { mutableStateOf("9100") }
    val timeoutState = rememberSaveable { mutableStateOf("3000") }
    val pingCountState = rememberSaveable { mutableStateOf("4") }
    val packetSizesState = rememberSaveable { mutableStateOf("128,512,1024,2048,4096") }

    // Printer testing states
    val selectedDocumentType = rememberSaveable { mutableStateOf("IMPRESION_RECIBO") }
    val testTransactionId = rememberSaveable { mutableStateOf("") }
    val printerTypeFilter = rememberSaveable { mutableStateOf("ALL") }

    // Advanced diagnostics controls
    val enableRetries = rememberSaveable { mutableStateOf(false) }
    val stressIterations = rememberSaveable { mutableStateOf("0") }
    val includePhaseMetrics = rememberSaveable { mutableStateOf(true) }

    val logs: SnapshotStateList<String> = remember { mutableStateListOf() }
    val scope = rememberCoroutineScope()
    val running = remember { mutableStateOf(false) }
    val statusMessage = remember { mutableStateOf("Listo para diagnosticar") }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .verticalScroll(rememberScrollState())) {

        // Header with back button
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                painter = painterResource(id = R.drawable.ic_wanqara_logo_foreground),
                contentDescription = "Wanqara",
                tint = Primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Diagnóstico de impresora",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Pruebas de conectividad WiFi",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Configuración", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = hostState.value,
                    onValueChange = { hostState.value = it },
                    label = { Text("IP de la impresora") },
                    placeholder = { Text("Ej: 192.168.1.100") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = portState.value,
                        onValueChange = { portState.value = it },
                        label = { Text("Puerto") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = timeoutState.value,
                        onValueChange = { timeoutState.value = it },
                        label = { Text("Timeout (ms)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pingCountState.value,
                        onValueChange = { pingCountState.value = it },
                        label = { Text("Pings") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = packetSizesState.value,
                    onValueChange = { packetSizesState.value = it },
                    label = { Text("Tamaños de paquete (bytes)") },
                    placeholder = { Text("Ej: 128,512,1024,2048,4096") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Separa múltiples tamaños con comas", style = MaterialTheme.typography.bodySmall) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Status indicator
        if (running.value) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = Primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        statusMessage.value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Action Buttons Grid
        Text("Pruebas disponibles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedButton(
                    onClick = {
                        val host = hostState.value.trim()
                        if (host.isBlank()) {
                            logs.add(0, "⚠️ ERROR: Ingresa una IP válida")
                            return@ElevatedButton
                        }
                        val port = portState.value.toIntOrNull() ?: 9100
                        val timeout = timeoutState.value.toIntOrNull() ?: 3000
                        statusMessage.value = "Probando conexión TCP..."
                        logs.add(0, "📡 Iniciando prueba TCP a $host:$port")
                        scope.launch {
                            running.value = true
                            val result = runTcpTest(host, port, timeout)
                            logs.add(0, if (result.startsWith("OK")) "✅ $result" else "❌ $result")
                            statusMessage.value = "TCP completado"
                            running.value = false
                        }
                    },
                    enabled = !running.value,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔌 TCP", fontWeight = FontWeight.Bold)
                        Text("Conectividad", style = MaterialTheme.typography.bodySmall)
                    }
                }

                ElevatedButton(
                    onClick = {
                        val host = hostState.value.trim()
                        if (host.isBlank()) {
                            logs.add(0, "⚠️ ERROR: Ingresa una IP válida")
                            return@ElevatedButton
                        }
                        val count = pingCountState.value.toIntOrNull() ?: 4
                        statusMessage.value = "Ejecutando ping..."
                        logs.add(0, "📶 Ejecutando ping a $host (x$count)")
                        scope.launch {
                            running.value = true
                            val result = runPing(host, count)
                            logs.addAll(0, result.trim().split('\n').map { "  $it" })
                            logs.add(0, "✅ Ping completado")
                            statusMessage.value = "Ping completado"
                            running.value = false
                        }
                    },
                    enabled = !running.value,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📶 Ping", fontWeight = FontWeight.Bold)
                        Text("Latencia", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedButton(
                    onClick = {
                        val host = hostState.value.trim()
                        if (host.isBlank()) {
                            logs.add(0, "⚠️ ERROR: Ingresa una IP válida")
                            return@ElevatedButton
                        }
                        statusMessage.value = "Trazando ruta..."
                        logs.add(0, "🗺️ Iniciando traceroute a $host")
                        scope.launch {
                            running.value = true
                            val result = runTraceroute(host)
                            logs.addAll(0, result.trim().split('\n').map { "  $it" })
                            logs.add(0, "✅ Traceroute completado")
                            statusMessage.value = "Traceroute completado"
                            running.value = false
                        }
                    },
                    enabled = !running.value,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️ Traceroute", fontWeight = FontWeight.Bold)
                        Text("Ruta de red", style = MaterialTheme.typography.bodySmall)
                    }
                }

                ElevatedButton(
                    onClick = {
                        val host = hostState.value.trim()
                        if (host.isBlank()) {
                            logs.add(0, "⚠️ ERROR: Ingresa una IP válida")
                            return@ElevatedButton
                        }
                        val port = portState.value.toIntOrNull() ?: 9100
                        val timeout = timeoutState.value.toIntOrNull() ?: 3000
                        val sizes = packetSizesState.value.split(",").mapNotNull { it.trim().toIntOrNull() }

                        if (sizes.isEmpty()) {
                            logs.add(0, "⚠️ ERROR: Ingresa tamaños de paquete válidos")
                            return@ElevatedButton
                        }

                        statusMessage.value = "Probando tamaños de paquete..."
                        logs.add(0, "📦 Iniciando prueba de tamaños: ${sizes.joinToString(", ")} bytes")

                        scope.launch {
                            running.value = true
                            var successCount = 0
                            var failCount = 0

                            for ((index, size) in sizes.withIndex()) {
                                statusMessage.value = "Probando ${size} bytes (${index + 1}/${sizes.size})..."
                                logs.add(0, "\n📊 Probando paquete de $size bytes...")

                                val result = runTcpTestWithSize(host, port, timeout, size)
                                if (result.startsWith("OK")) {
                                    successCount++
                                    logs.add(0, "✅ $result")
                                } else {
                                    failCount++
                                    logs.add(0, "❌ $result")
                                }
                            }

                            logs.add(0, "\n📊 RESUMEN: ✅ $successCount exitosos, ❌ $failCount fallidos de ${sizes.size} pruebas")
                            logs.add(0, "═══════════════════════════════")
                            statusMessage.value = "Prueba de tamaños completada"
                            running.value = false
                        }
                    },
                    enabled = !running.value,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📦 Tamaños", fontWeight = FontWeight.Bold)
                        Text("Pérdida datos", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val host = hostState.value.trim()
                        if (host.isBlank()) {
                            logs.add(0, "⚠️ ERROR: Ingresa una IP válida")
                            return@Button
                        }
                        val port = portState.value.toIntOrNull() ?: 9100
                        val timeout = timeoutState.value.toIntOrNull() ?: 3000
                        val pingCount = pingCountState.value.toIntOrNull() ?: 4

                        logs.add(0, "═══════════════════════════════")
                        logs.add(0, "🔍 DIAGNÓSTICO COMPLETO - $host")
                        logs.add(0, "═══════════════════════════════")

                        scope.launch {
                            running.value = true

                            // TCP
                            statusMessage.value = "1/3 Probando TCP..."
                            logs.add(0, "\n📡 TEST TCP")
                            val tcpResult = runTcpTest(host, port, timeout)
                            logs.add(0, if (tcpResult.startsWith("OK")) "✅ $tcpResult" else "❌ $tcpResult")

                            // Ping
                            statusMessage.value = "2/3 Ejecutando ping..."
                            logs.add(0, "\n📶 TEST PING")
                            val pingOut = runPing(host, pingCount)
                            logs.addAll(0, pingOut.trim().split('\n').map { "  $it" })

                            // Traceroute
                            statusMessage.value = "3/3 Trazando ruta..."
                            logs.add(0, "\n🗺️ TEST TRACEROUTE")
                            val traceOut = runTraceroute(host)
                            logs.addAll(0, traceOut.trim().split('\n').map { "  $it" })

                            logs.add(0, "\n✅ Diagnóstico completado")
                            logs.add(0, "═══════════════════════════════")
                            statusMessage.value = "Diagnóstico completado"
                            running.value = false
                        }
                    },
                    enabled = !running.value,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍 Completo", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Todo", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    logs.clear()
                    statusMessage.value = "Registros limpiados"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🗑️ Limpiar registros")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Logs Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Registros (${logs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (logs.isNotEmpty()) {
                TextButton(onClick = { logs.clear() }) {
                    Text("Limpiar")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "📝",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No hay registros",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                        Text(
                            "Ejecuta una prueba para ver resultados",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    reverseLayout = true
                ) {
                    items(items = logs) { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // Printer Commands Testing Section
        Text("🖨️ Pruebas de comandos de impresora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("Prueba la lógica de impresión con tipos de impresora reales", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Configuración de prueba", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                // Document type dropdown
                var expandedDocType by remember { mutableStateOf(false) }
                val documentTypes = listOf(
                    "IMPRESION_FACTURA_ELECTRONICA",
                    "IMPRESION_RECIBO",
                    "IMPRESION_COTIZACION",
                    "IMPRESION_PRE_TICKET",
                    "IMPRESION_COMANDA:COCINA",
                    "IMPRESION_COMANDA:BARRA",
                    "IMPRESION_COMANDA:OTROS",
                    "IMPRESION_CIERRE_CAJA"
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDocType,
                    onExpandedChange = { expandedDocType = !expandedDocType }
                ) {
                    OutlinedTextField(
                        value = selectedDocumentType.value,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de documento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDocType) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDocType,
                        onDismissRequest = { expandedDocType = false }
                    ) {
                        documentTypes.forEach { docType ->
                            DropdownMenuItem(
                                text = { Text(docType) },
                                onClick = {
                                    selectedDocumentType.value = docType
                                    expandedDocType = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = testTransactionId.value,
                    onValueChange = { testTransactionId.value = it },
                    label = { Text("ID de transacción (opcional)") },
                    placeholder = { Text("Ej: 12345") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("Deja vacío para prueba sin datos reales", style = MaterialTheme.typography.bodySmall) }
                )

                Spacer(Modifier.height(8.dp))

                // Printer type filter
                var expandedPrinterType by remember { mutableStateOf(false) }
                val printerTypes = listOf("ALL", "WIFI", "BLUETOOTH", "USB")

                ExposedDropdownMenuBox(
                    expanded = expandedPrinterType,
                    onExpandedChange = { expandedPrinterType = !expandedPrinterType }
                ) {
                    OutlinedTextField(
                        value = printerTypeFilter.value,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Filtrar por tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPrinterType) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPrinterType,
                        onDismissRequest = { expandedPrinterType = false }
                    ) {
                        printerTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    printerTypeFilter.value = type
                                    expandedPrinterType = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Printer test buttons
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val docType = selectedDocumentType.value
                    val transId = testTransactionId.value.trim()
                    val filter = printerTypeFilter.value
                    val iterations = stressIterations.value.toIntOrNull() ?: 0

                    statusMessage.value = "Probando impresoras configuradas..."
                    logs.add(0, "═══════════════════════════════")
                    logs.add(0, "🖨️ PRUEBA DE COMANDOS DE IMPRESORA")
                    logs.add(0, "Tipo: $docType | Stress: $iterations | Retries: ${enableRetries.value}")
                    logs.add(0, "ID: ${if (transId.isEmpty()) "Sin ID (prueba básica)" else transId}")
                    logs.add(0, "Filtro: $filter")
                    logs.add(0, "═══════════════════════════════")

                    scope.launch {
                        running.value = true
                        val totalAttempts = if (iterations > 0) iterations else 1
                        var successCount = 0
                        for (attempt in 1..totalAttempts) {
                            statusMessage.value = "Intento $attempt/$totalAttempts"
                            val result = testPrinterCommands(context, docType, transId, filter, logs)
                            if (!result && enableRetries.value && hostState.value.isNotBlank()) {
                                // simple retry once for WiFi printers
                                logs.add(0, "↻ Reintento tras fallo (intento $attempt)")
                                testPrinterCommands(context, docType, transId, filter, logs).also { if (it) successCount++ }
                            } else if (result) successCount++
                        }
                        logs.add(0, "Resumen stress: $successCount/$totalAttempts exitosos")
                        logs.add(0, "═══════════════════════════════")
                        statusMessage.value = "Prueba de impresora completada"
                        running.value = false
                    }
                },
                enabled = !running.value,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("🖨️ Probar impresoras configuradas", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // New: Quick actions for WiFi direct and listing BT/USB
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedButton(
                    onClick = {
                        val ip = hostState.value.trim()
                        if (ip.isBlank()) {
                            logs.add(0, "⚠️ ERROR: Ingresa una IP válida en la configuración")
                            return@ElevatedButton
                        }
                        val port = portState.value.toIntOrNull() ?: 9100

                        statusMessage.value = "Probando impresora WiFi directa..."
                        logs.add(0, "📡 Prueba WiFi directa usando config: $ip:$port")
                        logs.add(0, "📄 Documento: ${selectedDocumentType.value}")

                        scope.launch {
                            running.value = true
                            val result = testDirectWifiPrinter(context, ip, port, selectedDocumentType.value, logs)
                            logs.add(0, if (result) "✅ WiFi OK - Impresión de prueba enviada" else "❌ WiFi falló")
                            statusMessage.value = "Prueba WiFi completada"
                            running.value = false
                        }
                    },
                    enabled = !running.value,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡 WiFi", fontWeight = FontWeight.Bold)
                        Text("Directo", style = MaterialTheme.typography.bodySmall)
                    }
                }

                ElevatedButton(
                    onClick = {
                        statusMessage.value = "Listando impresoras Bluetooth..."
                        logs.add(0, "📲 Escaneando impresoras Bluetooth configuradas...")
                        scope.launch {
                            running.value = true
                            val database = DatabaseProvider.getDatabase(context)
                            val repository = PrinterRepository(database.printersDAO())
                            val getAllPrinters = GetAllPrinters(repository)
                            val printers = getAllPrinters.getAll().filter { it.type == PrinterType.BLUETOOTH.type }
                            if (printers.isEmpty()) {
                                logs.add(0, "⚠️ No hay impresoras Bluetooth configuradas")
                            } else {
                                logs.add(0, "📋 Impresoras Bluetooth encontradas: ${printers.size}")
                                printers.forEach { printer ->
                                    logs.add(0, "  • ${printer.name} - ${printer.address} (${printer.documentType})")
                                }
                            }
                            statusMessage.value = "Escaneo Bluetooth completado"
                            running.value = false
                        }
                    },
                    enabled = !running.value,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📲 BT", fontWeight = FontWeight.Bold)
                        Text("Listar", style = MaterialTheme.typography.bodySmall)
                    }
                }

                ElevatedButton(
                    onClick = {
                        statusMessage.value = "Listando impresoras USB..."
                        logs.add(0, "🔌 Escaneando impresoras USB configuradas...")
                        scope.launch {
                            running.value = true
                            val database = DatabaseProvider.getDatabase(context)
                            val repository = PrinterRepository(database.printersDAO())
                            val getAllPrinters = GetAllPrinters(repository)
                            val printers = getAllPrinters.getAll().filter { it.type == PrinterType.USB.type }
                            if (printers.isEmpty()) {
                                logs.add(0, "⚠️ No hay impresoras USB configuradas")
                            } else {
                                logs.add(0, "📋 Impresoras USB encontradas: ${printers.size}")
                                printers.forEach { printer ->
                                    logs.add(0, "  • ${printer.name} (${printer.documentType})")
                                }
                            }
                            statusMessage.value = "Escaneo USB completado"
                            running.value = false
                        }
                    },
                    enabled = !running.value,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔌 USB", fontWeight = FontWeight.Bold)
                        Text("Listar", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedButton(onClick = {
                    // Export persistent logs
                    val intent = PrintDiagnosticsBus.shareLogsIntent(context)
                    if (intent == null) {
                        logs.add(0, "⚠️ No hay logs para exportar")
                    } else {
                        logs.add(0, "📤 Exportando logs (usa el selector de compartir)")
                        try { context.startActivity(intent) } catch (e: Exception) { logs.add(0, "❌ Error exportando: ${e.message}") }
                    }
                }) { Text("📤 Exportar logs") }
                ElevatedButton(onClick = {
                    PrintDiagnosticsBus.clearPersistentLogs(context)
                    logs.add(0, "🧹 Logs persistentes limpiados")
                }) { Text("🧹 Limpiar persistente") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    PrintDiagnosticsBus.appendPersistentLog(context, "TEST_LOG desde DiagnosticsScreen")
                    logs.add(0, "📝 Línea persistente de prueba escrita")
                }, modifier = Modifier.weight(1f)) { Text("Escribir log prueba") }
                OutlinedButton(onClick = {
                    val persisted = PrintDiagnosticsBus.readPersistentLogs(context, limit = 200)
                    if (persisted.isEmpty()) logs.add(0, "ℹ️ No hay logs persistentes")
                    else {
                        logs.add(0, "📥 Cargando ${persisted.size} líneas de log persistente…")
                        // Show newest first in UI
                        persisted.takeLast(200).reversed().forEach { logs.add(0, it) }
                    }
                }, modifier = Modifier.weight(1f)) { Text("Cargar logs persistentes") }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tips Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "💡 Sugerencias",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text("• Si ping falla pero TCP funciona → bloqueo ICMP en la red", style = MaterialTheme.typography.bodySmall)
                Text("• Si hay pérdida de paquetes → verifica señal WiFi o interferencias", style = MaterialTheme.typography.bodySmall)
                Text("• Puerto típico para impresoras TCP: 9100", style = MaterialTheme.typography.bodySmall)
                Text("• Prueba comandos de impresión sin ID para verificar conectividad básica", style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    DisposableEffect(Unit) {
        val listener: (PrinterBuilder.PrinterDiagnosticsEvent) -> Unit = { evt ->
            val ms = evt.endTimestamp - evt.startTimestamp
            val status = if (evt.success) "✅" else "❌"
            val msg = "$status JOB transporte=${evt.transportType} destino=${evt.address ?: "-"}:${evt.port ?: "-"} bytes=${evt.bytesLength} tiempo=${ms}ms${evt.errorMessage?.let { " error=$it" } ?: ""}".trim()
            logs.add(0, msg)
            // persist
            PrintDiagnosticsBus.appendPersistentLog(
                context = context,
                line = msg
            )
        }
        PrinterBuilder.diagnosticsListener = listener
        onDispose {
            if (PrinterBuilder.diagnosticsListener === listener) {
                PrinterBuilder.diagnosticsListener = null
            }
        }
    }

    if (includePhaseMetrics.value) {
        DisposableEffect(Unit) {
            val phaseListener: (PrintDiagnosticsBus.PhaseEvent) -> Unit = { evt ->
                val line = "⏱ Fase ${evt.phase} doc=${evt.documentType} dur=${evt.durationMs}ms ok=${evt.success} ${evt.extra ?: ""}".trim()
                logs.add(0, line)
                PrintDiagnosticsBus.appendPersistentLog(context, line)
            }
            PrintDiagnosticsBus.phaseListener = phaseListener
            onDispose { if (PrintDiagnosticsBus.phaseListener === phaseListener) PrintDiagnosticsBus.phaseListener = null }
        }
    }
}

suspend fun runTcpTest(host: String, port: Int, timeoutMs: Int): String {
    return try {
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val out = socket.getOutputStream()
                // Minimal ESC/POS init sequence followed by newline
                val payload = byteArrayOf(0x1B, 0x40) + "\n".toByteArray()
                out.write(payload)
                out.flush()
            }
            "OK: TCP conectado y payload enviado a $host:$port"
        }
    } catch (e: Exception) {
        "ERROR TCP: ${e.localizedMessage ?: e.message}"
    }
}

suspend fun runTcpTestWithSize(host: String, port: Int, timeoutMs: Int, payloadSize: Int): String {
    return try {
        withContext(Dispatchers.IO) {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                val out = socket.getOutputStream()

                // Create payload of specified size
                // Start with ESC @ (printer init), then fill with printable data
                val header = byteArrayOf(0x1B, 0x40) // ESC @
                val fillData = ByteArray(maxOf(0, payloadSize - header.size - 1)) { i ->
                    // Fill with repeating pattern of printable ASCII characters
                    ('A'.code + (i % 26)).toByte()
                }
                val footer = byteArrayOf(0x0A) // newline

                val payload = header + fillData + footer

                val startTime = System.currentTimeMillis()
                out.write(payload)
                out.flush()
                val endTime = System.currentTimeMillis()
                val duration = endTime - startTime

                socket.close()

                "OK: $payloadSize bytes enviados en ${duration}ms a $host:$port"
            }
        }
    } catch (e: Exception) {
        "ERROR: ${e.localizedMessage ?: e.message} (tamaño: $payloadSize bytes)"
    }
}

suspend fun runPing(host: String, count: Int): String {
    return withContext(Dispatchers.IO) {
        try {
            // Try common ping variants: Unix '-c' and Windows '-n'
            val commands = listOf(listOf("ping", "-c", count.toString(), host), listOf("ping", "-n", count.toString(), host))
            for (cmd in commands) {
                try {
                    val pb = ProcessBuilder(cmd)
                    pb.redirectErrorStream(true)
                    val proc = pb.start()
                    val out = StringBuilder()
                    BufferedReader(InputStreamReader(proc.inputStream)).use { br ->
                        var line: String? = br.readLine()
                        while (line != null) {
                            out.append(line).append('\n')
                            line = br.readLine()
                        }
                    }
                    val exit = proc.waitFor()
                    if (exit == 0) return@withContext out.toString() else return@withContext "Ping command exited with $exit:\n${out.toString()}"
                } catch (inner: Throwable) {
                    // try next variant
                }
            }
            "ERROR ping: no ping binary available or all variants failed"
        } catch (e: Throwable) {
            "ERROR ping: ${e.localizedMessage ?: e.message}"
        }
    }
}

suspend fun runTraceroute(host: String): String {
    return withContext(Dispatchers.IO) {
        try {
            // Try traceroute (Unix), tracepath (Linux), tracert (Windows)
            val commands = listOf(listOf("traceroute", host), listOf("tracepath", host), listOf("tracert", host))
            for (cmd in commands) {
                try {
                    val pb = ProcessBuilder(cmd)
                    pb.redirectErrorStream(true)
                    val proc = pb.start()
                    val out = StringBuilder()
                    BufferedReader(InputStreamReader(proc.inputStream)).use { br ->
                        var line: String? = br.readLine()
                        while (line != null) {
                            out.append(line).append('\n')
                            line = br.readLine()
                        }
                    }
                    val exit = proc.waitFor()
                    if (exit == 0) return@withContext out.toString()
                } catch (inner: Throwable) {
                    // try next
                }
            }

            // Fallback: emulate traceroute by probing with ping using increasing TTL values
            try {
                return@withContext runTracerouteWithPingProbes(host)
            } catch (e: Throwable) {
                return@withContext "ERROR traceroute: failed traceroute binaries and ping-probe fallback: ${e.localizedMessage ?: e.message}"
            }
        } catch (e: Throwable) {
            "ERROR traceroute: ${e.localizedMessage ?: e.message}"
        }
    }
}

suspend fun runTracerouteWithPingProbes(host: String, maxHops: Int = 30, perProbeTimeoutMs: Int = 2000): String {
    return withContext(Dispatchers.IO) {
        val out = StringBuilder()
        val ipRegex = Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b")
        val destAddr = try {
            InetAddress.getByName(host).hostAddress
        } catch (e: Exception) {
            null
        }

        for (ttl in 1..maxHops) {
            var hopIp: String? = null
            var probeOutput: String? = null
            // Try a few ping command variants that support TTL on different platforms
            val variants = listOf(
                listOf("ping", "-c", "1", "-t", ttl.toString(), host), // common Linux/Android
                listOf("ping", "-c", "1", "-m", ttl.toString(), host), // alternative
                listOf("ping", "-c", "1", "-W", (perProbeTimeoutMs / 1000).toString(), "-t", ttl.toString(), host)
            )
            for (cmd in variants) {
                try {
                    val pb = ProcessBuilder(cmd)
                    pb.redirectErrorStream(true)
                    val proc = pb.start()
                    val sb = StringBuilder()
                    BufferedReader(InputStreamReader(proc.inputStream)).use { br ->
                        var line: String? = br.readLine()
                        while (line != null) {
                            sb.append(line).append('\n')
                            line = br.readLine()
                        }
                    }
                    proc.waitFor()
                    val outputStr = sb.toString()
                    probeOutput = outputStr
                    val m = ipRegex.find(outputStr)
                    if (m != null) {
                        hopIp = m.value
                        break
                    }
                } catch (inner: Throwable) {
                    // try next variant
                }
            }

            if (hopIp == null) {
                out.append("$ttl  *\n")
            } else {
                out.append("$ttl  $hopIp")
                if (probeOutput != null) out.append("  - ${probeOutput.lines().firstOrNull() ?: ""}")
                out.append('\n')
            }

            if (destAddr != null && hopIp == destAddr) {
                out.append("Reached destination at hop $ttl\n")
                break
            }
        }

        out.toString()
    }
}

// Printer command testing functions
suspend fun testPrinterCommands(
    context: Context,
    documentType: String,
    transactionId: String,
    printerTypeFilter: String,
    logs: SnapshotStateList<String>
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            val database = DatabaseProvider.getDatabase(context)
            val repository = PrinterRepository(database.printersDAO())
            val getAllPrinters = GetAllPrinters(repository)

            var printers = getAllPrinters.getAll().filter { it.documentType == documentType }

            // Apply printer type filter
            if (printerTypeFilter != "ALL") {
                val filterType = when (printerTypeFilter) {
                    "WIFI" -> PrinterType.WIFI.type
                    "BLUETOOTH" -> PrinterType.BLUETOOTH.type
                    "USB" -> PrinterType.USB.type
                    else -> null
                }
                if (filterType != null) {
                    printers = printers.filter { it.type == filterType }
                }
            }

            if (printers.isEmpty()) {
                logs.add(0, "⚠️ No hay impresoras configuradas para $documentType (filtro: $printerTypeFilter)")
                return@withContext false
            }

            logs.add(0, "📋 Impresoras encontradas: ${printers.size}")
            printers.forEach { printer ->
                logs.add(0, "  • ${printer.name} - ${printer.type} - ${printer.address ?: "N/A"}")
            }

            // Prepare command array
            val commands = if (transactionId.isNotEmpty()) {
                arrayOf(documentType, transactionId)
            } else {
                arrayOf(documentType, "TEST_ID_DUMMY")
            }

            logs.add(0, "\n🔄 Ejecutando Discrimination...")

            val discrimination = Discrimination(printers, context)
            val result = discrimination.invoke(commands)

            logs.add(0, if (result) "✅ Discrimination ejecutado correctamente" else "❌ Discrimination falló")

            result
        } catch (e: Exception) {
            logs.add(0, "❌ ERROR: ${e.localizedMessage ?: e.message}")
            android.util.Log.e("DiagnosticsScreen", "Error testing printer commands", e)
            false
        }
    }
}

suspend fun testDirectWifiPrinter(
    context: Context,
    ip: String,
    port: Int,
    documentType: String,
    logs: SnapshotStateList<String>
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            logs.add(0, "🔧 Inicializando conexión WiFi...")
            logs.add(0, "📡 Conectando a $ip:$port...")

            // Use TcpIpOutputStream from the library (same as PrinterBuilder does internally)
            com.github.anastaciocintra.output.TcpIpOutputStream(ip, port).use { outputStream ->
                logs.add(0, "✅ Conexión TCP establecida")
                logs.add(0, "📡 Enviando comando de inicialización ESC @...")

                val escposCoffee = com.github.anastaciocintra.escpos.EscPos(outputStream)

                // Send ESC @ (initialize printer)
                val initCommand = byteArrayOf(0x1B, 0x40)
                outputStream.write(initCommand)
                outputStream.flush()

                logs.add(0, "✅ Comando ESC @ enviado correctamente")
                logs.add(0, "📄 Enviando línea de prueba...")

                // Send test line
                escposCoffee.writeLF("=== PRUEBA DE DIAGNOSTICO ===")
                escposCoffee.writeLF("IP: $ip:$port")
                escposCoffee.writeLF("Documento: $documentType")
                escposCoffee.feed(3)
                escposCoffee.cut(com.github.anastaciocintra.escpos.EscPos.CutMode.FULL)

                logs.add(0, "✅ Prueba completada exitosamente")
            }

            logs.add(0, "🔒 Conexión cerrada")
            true
        } catch (e: Exception) {
            logs.add(0, "❌ ERROR WiFi: ${e.localizedMessage ?: e.message}")
            android.util.Log.e("DiagnosticsScreen", "Error testing direct WiFi printer", e)
            false
        }
    }
}

private fun formatWifiResult(label: String, r: WifiPrintTester.WifiSendResult): String {
    val status = if (r.success) "✅" else "❌"
    val err = if (r.errors.isNotEmpty()) r.errors.joinToString(" | ") else "-"
    return "$status WiFi $label bytes=${r.bytesSent}/${r.bytesPlanned} chunks=${r.chunks} attempts=${r.attempts} dur=${r.durationMs}ms errors=$err"
}
