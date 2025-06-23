package com.example.printerswanqaratest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.printerswanqaratest.ui.theme.*
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.navigate("add_printer") },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Printer")
            Spacer(Modifier.width(8.dp))
            Text("Add Printer")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("edit_printer") },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Printer")
            Spacer(Modifier.width(8.dp))
            Text("Edit Printer")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("message") },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.Info, contentDescription = "Message")
            Spacer(Modifier.width(8.dp))
            Text("Message")
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { navController.navigate("printer_test") },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.Info, contentDescription = "Printer Test")
            Spacer(Modifier.width(8.dp))
            Text("Printer Test")
        }
    }
}

@Composable
fun AddPrinterScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add a new printer", color = Primary, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { /* TODO: Add WiFi printer logic */ },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) { Text("Add WiFi Printer") }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { /* TODO: Add USB printer logic */ },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) { Text("Add USB Printer") }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { /* TODO: Add Bluetooth printer logic */ },
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) { Text("Add Bluetooth Printer") }
    }
}

@Composable
fun EditPrinterScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Edit selected printer", color = Primary, style = MaterialTheme.typography.headlineMedium)
        // TODO: Add printer selection and editing UI
    }
}

@Composable
fun MessageScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Info, contentDescription = "Info", tint = Primary, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Message goes here", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
fun PrinterTestScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Printer Test", color = Primary, style = MaterialTheme.typography.headlineMedium)
        // TODO: Add printer test UI
    }
}
