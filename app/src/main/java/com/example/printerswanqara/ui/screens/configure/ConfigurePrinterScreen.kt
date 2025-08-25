package com.example.printerswanqara.ui.screens.configure

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.printerswanqara.data.AppStorage
import com.google.gson.Gson
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.printerswanqara.api.Setting
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.FormatLineSpacing
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.example.printerswanqara.ui.theme.Primary

@Composable
fun ConfigurePrinterScreen() {
    val context = LocalContext.current
    val settingsJson = AppStorage.getSettings(context)
    val settings: Setting? = settingsJson?.let { Gson().fromJson(it, Setting::class.java) }

    if (settings?.printers != null) {
        val printerTabs = listOf("Factura","Recibo", "Comanda", "Pre-Ticket")
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        var previousTabIndex by remember { mutableIntStateOf(0) }
        val printers = settings.printers
        val printerDetailsList = listOf(
            printers.invoice to "Factura",
            printers.receipt to "Recibo",
            printers.order to "Comanda",
            printers.preticket to "Pre-Ticket"
        )
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Configuración de Impresoras",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            TabRow(
                selectedTabIndex = selectedTabIndex,
                //containerColor = Primary,
                //contentColor = Primary,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Primary
                    )
                }
            ) {
                printerTabs.forEachIndexed { index, tabTitle ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            previousTabIndex = selectedTabIndex
                            selectedTabIndex = index
                        },
                        text = { Text(tabTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        //icon = { Icon(Icons.Filled.Print, contentDescription = null) },
                        selectedContentColor = Primary,
                        unselectedContentColor = Color.Black
                    )
                }
            }
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()) togetherWith (slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()) togetherWith (slideOutHorizontally { width -> width } + fadeOut())
                    }
                },
                label = "tab-slide"
            ) { tabIndex ->
                val (tabDetails, tabTitle) = printerDetailsList[tabIndex]
                PrinterProfileCard(title = tabTitle, details = tabDetails)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No hay impresoras configuradas.", modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
fun PrinterProfileCard(title: String, details: com.example.printerswanqara.api.PrinterDetails?) {
    Card(
        modifier = Modifier.padding(top = 16.dp, start = 8.dp, end = 8.dp).fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Icon(Icons.Filled.Print, contentDescription = null, tint = Primary)
                Text(text = title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (details != null) {
                // Numeric/String values section
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    ProfileRow(label = "Fuente", value = details.font, icon = Icons.Filled.TextFields)
                    ProfileRow(label = "Saltos de línea", value = details.line_breaks?.toString(), icon = Icons.AutoMirrored.Filled.WrapText)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                // Boolean values section
                Text(text = "Opciones", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                Column {
                    BooleanRow(label = "Encabezado", value = details.header, icon = Icons.Filled.Title)
                    BooleanRow(label = "Interlineado", value = details.line_spacing, icon = Icons.Filled.FormatLineSpacing)
                    BooleanRow(label = "Logo", value = details.logo, icon = Icons.Filled.Image)
                    BooleanRow(label = "Observación", value = details.observation, icon = Icons.AutoMirrored.Filled.Notes)
                    BooleanRow(label = "Desglosar Impuestos", value = details.taxes, icon = Icons.Filled.Receipt)
                    BooleanRow(label = "Vendedor", value = details.user, icon = Icons.Filled.Person)
                }
            } else {
                Text(text = "Sin detalles para esta impresora.", color = Color.Gray)
            }
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String?, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    if (value == null) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFF616161), modifier = Modifier.padding(end = 8.dp))
        Text(text = "$label:", fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 4.dp))
        Text(text = value)
    }
}

@Composable
fun BooleanRow(label: String, value: Boolean?, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    if (value == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        // Left column: Icon and label
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF616161), modifier = Modifier.padding(end = 8.dp))
            Text(text = "$label:", fontWeight = FontWeight.Medium)
        }

        // Right column: Yes/No icon and text
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Sí", tint = Color(0xFF388E3C))
            } else {
                Icon(Icons.Filled.Cancel, contentDescription = "No", tint = Color(0xFFD32F2F))
            }
        }
    }
}