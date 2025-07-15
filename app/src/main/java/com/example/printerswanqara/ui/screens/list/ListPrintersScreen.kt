package com.example.printerswanqara.ui.screens.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.printerswanqara.data.database.DatabaseProvider
import com.example.printerswanqara.data.database.repositories.PrinterRepository
import com.example.printerswanqara.domain.models.Printers
import com.example.printerswanqara.domain.services.GetAllPrinters
import com.example.printerswanqara.domain.services.DeletePrinter
import com.example.printerswanqara.ui.theme.Primary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person

import androidx.compose.material.icons.filled.TextFields
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import kotlinx.coroutines.launch
import com.example.printerswanqara.core.document.documentType
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.navigation.NavController
import com.example.printerswanqara.api.ApiClient
import com.example.printerswanqara.data.AppStorage
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ListPrintersScreen(navController: NavController) {
    val context = LocalContext.current
    var printers by remember { mutableStateOf<List<Printers>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var groupBy by remember { mutableStateOf("Todos") }
    val searchName by remember { mutableStateOf("") }
    val groupOptions = listOf("Todos", "Nombre", "Tipo")
    val groupIcons = mapOf(
        "Todos" to Icons.Default.FilterNone, // None
        "Nombre" to Icons.Default.TextFields,      // Name
        "Tipo" to Icons.Default.Category       // Type
    )
    val baseInfoService = ApiClient.createBaseInfoService(context)
    var isLoading by remember { mutableStateOf(false) } // Loading state

    suspend fun refreshPrinters() {
        val db = DatabaseProvider.getDatabase(context)
        val repository = PrinterRepository(db.printersDAO())
        val getAllP = GetAllPrinters(repository)
        printers = getAllP.getAll()
    }

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            refreshPrinters()
            try {
                val response = baseInfoService.getBaseInfo().data // Replace with the actual method
                println("Saving Base Info from list: $response")
                AppStorage.saveSettings(context, response) // Save the data locally
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (isLoading) {
            androidx.compose.material3.LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        // Agrupar por label on top row
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Agrupar por:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        // Buttons in a separate, compact row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            groupOptions.forEach { option ->
                val selected = groupBy == option
                Button(
                    onClick = { groupBy = option },
                    colors = if (selected) ButtonDefaults.buttonColors(containerColor = Primary) else ButtonDefaults.buttonColors(containerColor = Color.Transparent),

                    border =  if (selected) null else BorderStroke(2.dp, Primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = groupIcons[option] ?: Icons.Default.Print,
                        contentDescription = option,

                        modifier = Modifier.size(16.dp),
                        tint =   if (selected) Color.White else Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(option, style = MaterialTheme.typography.labelMedium,  color = if (selected) Color.White else Primary )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Filter and group printers
        val filteredPrinters = printers.filter {
            searchName.isBlank() || it.name.contains(searchName, ignoreCase = true)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                            text = "Copias: ${printer.copyNumber}",
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
                        contentDescription = "Eliminar Impresora",
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
            title = { Text("Eliminar Impresora") },
            text = {
                Column {
                    Text("Está seguro de que desea eliminar la impresora:\n\n")
                    Text(printer.name, style = MaterialTheme.typography.bodyLarge , fontWeight =  FontWeight.Bold )
                    Text("Tipo: ${printer.type}", style = MaterialTheme.typography.bodySmall)
                    if (printer.type == "WIFI" && !printer.address.isNullOrBlank()) {
                        Text("IP: ${printer.address}", style = MaterialTheme.typography.bodySmall)
                    } else if (printer.type != "USB" && !printer.address.isNullOrBlank()) {
                        Text("MAC: ${printer.address}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDelete()
                } , colors =  ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("Eliminar Impresora", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}