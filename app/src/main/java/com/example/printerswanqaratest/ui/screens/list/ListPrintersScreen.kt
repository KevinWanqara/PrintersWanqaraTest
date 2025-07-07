package com.example.printerswanqaratest.ui.screens.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.printerswanqaratest.data.database.DatabaseProvider
import com.example.printerswanqaratest.data.database.repositories.PrinterRepository
import com.example.printerswanqaratest.domain.models.Printers
import com.example.printerswanqaratest.domain.services.GetAllPrinters
import com.example.printerswanqaratest.domain.services.DeletePrinter
import com.example.printerswanqaratest.ui.theme.Primary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import kotlinx.coroutines.launch

@Composable
fun ListPrintersScreen() {
    val context = LocalContext.current
    var printers by remember { mutableStateOf<List<Printers>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    suspend fun refreshPrinters() {
        val db = DatabaseProvider.getDatabase(context)
        val repository = PrinterRepository(db.printersDAO())
        val getAll = GetAllPrinters(repository)
        printers = getAll.getAll()
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            refreshPrinters()
        }
    }

    if (printers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "No tiene impresoras agregadas")
                Spacer(modifier = Modifier.height(16.dp))
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "No printers icon",
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            items(printers) { printer ->
                PrinterCard(
                    printer = printer,
                    onDelete = {
                        // Delete and refresh on confirmation
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                val db = DatabaseProvider.getDatabase(context)
                                val repository = PrinterRepository(db.printersDAO())
                                val deletePrinter = DeletePrinter(repository)
                                printer.id?.let { deletePrinter(it.toInt() ) }
                                refreshPrinters()
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun PrinterCard(
    printer: Printers,
    onDelete: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val icon = when (printer.type) {
        "USB" -> Icons.Default.Usb
        "WIFI" -> Icons.Default.Wifi
        "BLUETOOTH" -> Icons.Default.Bluetooth
        else -> Icons.Default.Usb
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = printer.type,
                    tint = Primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = printer.documentType, style = MaterialTheme.typography.titleMedium, color =
                    Primary)
                    Text(text = printer.name, style = MaterialTheme.typography.titleMedium)
                    if (!printer.address.isNullOrBlank()) {
                        Text(text = "IP: ${printer.address}", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = printer.type,
                            color = Primary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Copies: ${printer.copyNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Chars: ${printer.charactersNumber}",
                            style = MaterialTheme.typography.bodySmall
                        )

                    }

                }
                IconButton(
                    onClick = { showDialog = true },



                    ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                tint = Color.Red,
                        contentDescription = "Delete printer"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))


        }
    }

    // Confirmation Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete Printer") },
            text = {
                Text("Are you sure you want to delete the printer:\n\n" +
                        "**${printer.name}**\n" +
                        "Type: ${printer.type}\n" +
                        "IP: ${printer.address}")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDelete()
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}