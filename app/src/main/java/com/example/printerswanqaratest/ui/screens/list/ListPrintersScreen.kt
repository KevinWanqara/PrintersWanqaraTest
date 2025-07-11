package com.example.printerswanqaratest.ui.screens.list

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import com.example.printerswanqaratest.core.document.documentType
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.navigation.NavController

@Composable
fun ListPrintersScreen(navController: NavController) {
    val context = LocalContext.current
    var printers by remember { mutableStateOf<List<Printers>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var groupBy by remember { mutableStateOf("Ninguno") }
    val searchName by remember { mutableStateOf("") }
    val groupOptions = listOf("Ninguno", "Nombre", "Tipo")

    suspend fun refreshPrinters() {
        val db = DatabaseProvider.getDatabase(context)
        val repository = PrinterRepository(db.printersDAO())
        val getAllP = GetAllPrinters(repository)
        printers = getAllP.getAll()
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            refreshPrinters()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // Group by dropdown
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Agrupar por:", modifier = Modifier.padding(end = 8.dp))
            var expanded by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expanded = true }) {
                    Text(groupBy)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    groupOptions.forEach { option ->
                        DropdownMenuItem(onClick = {
                            groupBy = option
                            expanded = false
                        }, text = { Text(option) })
                    }
                }
            }


        }


        Spacer(Modifier.height(16.dp))

        // Filter and group printers
        val filteredPrinters = printers.filter {
            searchName.isBlank() || (it.name.contains(searchName, ignoreCase = true) == true)
        }
        val grouped: Map<String, List<Printers>> = when (groupBy) {
            "Nombre" -> filteredPrinters.groupBy { it.name }
            "Tipo" -> filteredPrinters.groupBy { it.type }
            else -> mapOf("Todos" to filteredPrinters)
        }

        if (filteredPrinters.isEmpty()) {
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
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                grouped.forEach { (groupKey, groupList) ->
                    item {
                        var expanded by remember { mutableStateOf(true) }
                        var showGroupDeleteDialog by remember { mutableStateOf(false) }
                        Column(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = if (expanded) "Colapsar" else "Expandir"
                                    )
                                }
                                Text(
                                    text = when (groupBy) {
                                        "Nombre" -> "Nombre: $groupKey"
                                        "Tipo" -> "Tipo: $groupKey"
                                        else -> "Todas las impresoras"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (groupList.isNotEmpty()) {
                                    TextButton(onClick = { showGroupDeleteDialog = true }) {
                                        Text("Eliminar grupo", color = Color.Red)
                                    }
                                }
                            }
                            if (showGroupDeleteDialog) {
                                AlertDialog(
                                    onDismissRequest = { showGroupDeleteDialog = false },
                                    title = { Text("Eliminar grupo") },
                                    text = { Text("¿Está seguro que desea eliminar todas las impresoras de este grupo?") },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            showGroupDeleteDialog = false
                                            coroutineScope.launch {
                                                withContext(Dispatchers.IO) {
                                                    val db = DatabaseProvider.getDatabase(context)
                                                    val repository = PrinterRepository(db.printersDAO())
                                                    groupList.forEach { printer ->
                                                        val deletePrinter = DeletePrinter(repository)
                                                        printer.id?.let { deletePrinter(it.toInt()) }
                                                    }
                                                    refreshPrinters()
                                                }
                                            }
                                        }) { Text("Sí") }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showGroupDeleteDialog = false }) { Text("Cancelar") }
                                    }
                                )
                            }
                        }
                        if (expanded) {
                            groupList.forEach { printer ->
                                PrinterCard(
                                    printer = printer,
                                    onDelete = {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.IO) {
                                                val db = DatabaseProvider.getDatabase(context)
                                                val repository = PrinterRepository(db.printersDAO())
                                                val deletePrinter = DeletePrinter(repository)
                                                printer.id?.let { deletePrinter(it.toInt() ) }
                                                refreshPrinters()
                                            }
                                        }
                                    },
                                    onEdit = {
                                        printer.id?.let { navController.navigate("edit_printer/$it") }
                                    }
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrinterCard(
    printer: Printers,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    val icon = when (printer.type) {
        "USB" -> Icons.Default.Usb
        "WIFI" -> Icons.Default.Wifi
        "BLUETOOTH" -> Icons.Default.Bluetooth
        else -> Icons.Default.Usb
    }

    val docTypeObj = documentType()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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
                    Text(
                        text = docTypeObj.findDocumentByKey(printer.documentType) ?: printer.documentType,
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary
                    )
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
                        "Tipo: ${printer.type}\n" +
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