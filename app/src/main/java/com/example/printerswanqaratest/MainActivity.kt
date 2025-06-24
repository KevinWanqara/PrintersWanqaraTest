package com.example.printerswanqaratest

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.printerswanqaratest.ui.theme.PrintersWanqaraTestTheme
import androidx.compose.material3.Icon
import com.example.printerswanqaratest.data.TokenDatabaseHelper
import com.example.printerswanqaratest.ui.screens.LoginScreen
import com.example.printerswanqaratest.ui.screens.DomainValidationScreen
import com.example.printerswanqaratest.data.AppStorage
import com.example.printerswanqaratest.core.print.test.PrintBluetoothTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    //Main Activity
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PrintersWanqaraTestTheme {
                val navController = rememberNavController()
                val context = this
                var isLoggedIn by remember { mutableStateOf(false) }
                var isDomainValidated by remember { mutableStateOf(false) }
                val tokenDb = remember { TokenDatabaseHelper(context) }
                // Check for token and RUC on launch
                LaunchedEffect(Unit) {
                    val ruc = AppStorage.getRuc(context)
                    isDomainValidated = !ruc.isNullOrBlank()
                    isLoggedIn = tokenDb.getToken() != null
                }
                when {
                    !isDomainValidated -> {
                        DomainValidationScreen(onDomainValidated = {
                            isDomainValidated = true

                        },
                                logoResId = R.drawable.ic_wanqara_logo_foreground

                        )
                    }
                    !isLoggedIn -> {
                        LoginScreen(
                            onLoginSuccess = { token ->
                                tokenDb.saveToken(token)
                                isLoggedIn = true
                            },
                            onDomainValidationRequested = {
                                isDomainValidated = false
                            },
                            logoResId = R.drawable.ic_wanqara_logo_foreground
                        )
                    }
                    else -> {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp
                        var menuExpanded by remember { mutableStateOf(false) }
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                CenterAlignedTopAppBar(
                                    title = { Text("Printers Wanqara") },
                                    navigationIcon = {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Home") },
                                                onClick = {
                                                    navController.navigate("home")
                                                    menuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Add Printer") },
                                                onClick = {
                                                    navController.navigate("add_printer")
                                                    menuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Bluetooth Test") },
                                                onClick = {
                                                    menuExpanded = false
                                                    navController.navigate("add_printer")
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Print Bluetooth test") },
                                                onClick = {
                                                    if (!PrintBluetoothTest(context)("60:6E:41:59:BD:6F")) {
                                                        CoroutineScope(Dispatchers.Main).launch {
                                                            Toast.makeText(context, "Impresora no vinculada", Toast.LENGTH_SHORT).show()
                                                        }

                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Message") },
                                                onClick = {
                                                    navController.navigate("message")
                                                    menuExpanded = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Printer Test") },
                                                onClick = {
                                                    navController.navigate("printer_test")
                                                    menuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                composable("home") { HomeScreen(navController) }
                                composable("add_printer") { AddPrinterScreen() }
                                composable("edit_printer") { EditPrinterScreen() }
                                composable("message") { MessageScreen() }
                                composable("printer_test") { PrinterTestScreen() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, name = "Main Activity Preview")
@Composable
fun MainActivityPreview() {
    PrintersWanqaraTestTheme {
        val navController = rememberNavController()
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        var menuExpanded by remember { mutableStateOf(false) }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Printers Wanqara") },
                    navigationIcon = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Home") },
                                onClick = {
                                    navController.navigate("home")
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Printer") },
                                onClick = {
                                    navController.navigate("add_printer")
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Printer") },
                                onClick = {
                                    navController.navigate("edit_printer")
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Message") },
                                onClick = {
                                    navController.navigate("message")
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Printer Test") },
                                onClick = {
                                    navController.navigate("printer_test")
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Printer Test") },
                                onClick = {
                                    navController.navigate("printer_test")
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            // You can preview a single screen here, e.g. HomeScreen(navController)
            HomeScreen(navController)
        }
    }
}
