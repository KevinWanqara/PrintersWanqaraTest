package com.example.printerswanqara.ui.screens.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import kotlinx.coroutines.launch
import com.example.printerswanqara.core.document.documentType
import androidx.navigation.NavController
import com.example.printerswanqara.api.ApiClient
import com.example.printerswanqara.data.AppStorage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListPrintersScreen(navController: NavController) {
    val context = LocalContext.current
    var printers by remember { mutableStateOf<List<Printers>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var groupBy by remember { mutableStateOf("Todos") }
    val searchName by remember { mutableStateOf("") }
    val groupOptions = listOf("Todos", "Nombre", "Tipo")
    val groupIcons = mapOf(
        "Todos" to Icons.Default.FilterNone,
        "Nombre" to Icons.Default.TextFields,
        "Tipo" to Icons.Default.Category
    )
    val baseInfoService = ApiClient.createBaseInfoService(context)
    var isLoading by remember { mutableStateOf(false) }

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
                val response = baseInfoService.getBaseInfo().data
                AppStorage.saveSettings(context, response)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Primary
            )
        }
        
        // Fixed Header Section
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Agrupar por:",
                    style = MaterialTheme.typography.titleMedium
                )
                
                TextButton(
                    onClick = {
                        AppStorage.setOnboardingCompleted(context, false)
                        (context as? android.app.Activity)?.recreate()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reiniciar Onboarding", style = MaterialTheme.typography.labelMedium)
                }
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupOptions.forEach { option ->
                    val selected = groupBy == option
                    FilterChip(
                        selected = selected,
                        onClick = { groupBy = option },
                        label = { Text(option) },
                        leadingIcon = {
                            Icon(
                                imageVector = groupIcons[option] ?: Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        val filteredPrinters = printers.filter {
            searchName.isBlank() || it.name.contains(searchName, ignoreCase = true)
        }
        val grouped: Map<String, List<Printers>> = when (groupBy) {
            "Nombre" -> filteredPrinters.groupBy { it.name }
            "Tipo" -> filteredPrinters.groupBy { it.type }
            else -> mapOf("Todos" to filteredPrinters)
        }

        if (filteredPrinters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "No tiene impresoras agregadas", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "No printers icon",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
        "SERVER" -> Icons.Default.Dns
        else -> Icons.Default.Usb
    }

    val docTypeObj = documentType()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = Primary.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = printer.type,
                            tint = Primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = docTypeObj.findDocumentByKey(printer.documentType) ?: printer.documentType,
                        style = MaterialTheme.typography.titleMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = printer.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (!printer.address.isNullOrBlank()) {
                        Text(
                            text = if (printer.type == "BLUETOOTH") "MAC: ${printer.address}" else "IP: ${printer.address}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        InfoBadge(label = "Copias", value = printer.copyNumber.toString())
                        InfoBadge(label = "Chars", value = printer.charactersNumber.toString())
                    }
                }
                
                IconButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                tint = Color.Red,
                        contentDescription = "Eliminar Impresora",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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

@Composable
fun InfoBadge(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}