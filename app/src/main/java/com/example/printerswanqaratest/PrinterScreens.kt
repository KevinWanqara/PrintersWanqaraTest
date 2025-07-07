package com.example.printerswanqaratest
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.example.printerswanqaratest.ui.theme.*
import androidx.navigation.NavHostController
import com.example.printerswanqaratest.core.print.test.PrintBluetoothTest
import com.example.printerswanqaratest.core.print.test.PrintWifiTest
import com.example.printerswanqaratest.core.print.test.PrintUSBTest

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.MutableLiveData

@Composable
fun HomeScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
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

var port = MutableLiveData<String>("9100")
var ipAddress = MutableLiveData<String>("192.168.100.90")
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

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)

@Composable

fun PrinterTestScreen(


) {
    val context = LocalContext.current

    Scaffold(

        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),

    ) {
        Text("Printer Test", color = Primary, style = MaterialTheme.typography.headlineMedium)
        var menuExpanded by remember { mutableStateOf(false) }
        Column (modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center


        ) {

                Button(
                    onClick = {
                        if (!PrintBluetoothTest(context)("60:6E:41:59:BD:6F")) {
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(context, "Impresora no vinculada", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Bluetooth Test")
                    Spacer(Modifier.width(8.dp))
                    Text("Bluetooth Test")

                }
            Button(
                onClick = {


                    // Check if port and ipAddress are not null or empty
                    println( "Port: ${port.value}, IP Address: ${ipAddress.value}")


                    if (!port.value.isNullOrEmpty() && !ipAddress.value.isNullOrEmpty()) {
                        PrintWifiTest(
                            ipAddress.value!!.trim(),
                            port.value!!.toInt(),
                            "B"
                        )()
                    } else {
                        Toast.makeText(context, "Debe ingresar la IP y el Puerto", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Bluetooth Test")
                Spacer(Modifier.width(8.dp))
                Text("Wifi Test")

            }

            Button(
                onClick = {


                    if (!PrintUSBTest(context)()) {
                        Toast.makeText(context, "Impresora no conectada", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Info, contentDescription = "Bluetooth Test")
                Spacer(Modifier.width(8.dp))
                Text("USB Test")

            }



        }
    }
}
