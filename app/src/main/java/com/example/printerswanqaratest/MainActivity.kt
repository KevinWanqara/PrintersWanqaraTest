package com.example.printerswanqaratest

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.printerswanqaratest.ui.theme.PrintersWanqaraTestTheme
import com.example.printerswanqaratest.data.TokenDatabaseHelper
import com.example.printerswanqaratest.ui.screens.LoginScreen
import com.example.printerswanqaratest.ui.screens.DomainValidationScreen
import com.example.printerswanqaratest.ui.screens.add.AddPrinterScreen
import com.example.printerswanqaratest.ui.screens.edit.EditPrinterScreen
import com.example.printerswanqaratest.ui.screens.list.ListPrintersScreen
import com.example.printerswanqaratest.ui.screens.configure.ConfigurePrinterScreen
import com.example.printerswanqaratest.ui.screens.tests.PrinterTestScreen
import com.example.printerswanqaratest.ui.screens.message.MessageScreen
import com.example.printerswanqaratest.data.AppStorage
import com.example.printerswanqaratest.ui.screens.home.HomeScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TopAppBarDefaults
import com.example.printerswanqaratest.ui.theme.Primary

class MainActivity : ComponentActivity() {
    //Main Activity
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
                val navEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navEntry?.destination?.route
                when {
                    !isDomainValidated -> {
                        DomainValidationScreen(
                            onDomainValidated = {
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
                        Scaffold(
                             modifier = Modifier.fillMaxSize(),
                            topBar = {
                                 CenterAlignedTopAppBar(
                                     title = { Text("Printers Wanqara", color = Color.White) },
                                     colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                         containerColor = Primary,
                                         navigationIconContentColor = Color.White,
                                         actionIconContentColor = Color.White
                                     ),
                                     navigationIcon = {
                                        if (currentRoute != "home") IconButton(onClick = { navController.navigate("home") { popUpTo("home") } }) {
                                             Icon(Icons.Filled.Home, contentDescription = "Home")
                                         }
                                     },
                                     actions = {
                                         if (currentRoute == "home") {
                                            IconButton(onClick = { navController.navigate("edit_printer") }) {
                                                Icon(
                                                    Icons.Filled.Edit,
                                                    contentDescription = "Edit"
                                                )
                                            }
                                            IconButton(onClick = { navController.navigate("list_printers") }) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.List,
                                                    contentDescription = "List"
                                                )
                                            }
                                            IconButton(onClick = { navController.navigate("configure_printer") }) {
                                                Icon(
                                                    Icons.Filled.Settings,
                                                    contentDescription = "Configure"
                                                )
                                            }
                                            IconButton(onClick = { navController.navigate("printer_test") }) {
                                                Icon(
                                                    Icons.Filled.Print,
                                                    contentDescription = "Test"
                                                )
                                            }
                                            IconButton(onClick = { navController.navigate("message") }) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.Message,
                                                    contentDescription = "Message"
                                                )
                                            }
                                        }
                                    }
                                )
                            },
                            floatingActionButton = {
                                if (currentRoute == "home") {
                                    FloatingActionButton(
                                        onClick = { navController.navigate("add_printer") },
                                        containerColor = Primary,
                                        contentColor = Color.White
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Add Printer" )
                                    }
                                }
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
                                composable("list_printers") { ListPrintersScreen() }
                                composable("configure_printer") { ConfigurePrinterScreen() }
                                composable("printer_test") { PrinterTestScreen() }
                                composable("message") { MessageScreen() }
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
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("Printers Wanqara") }
                    )
                }
            ) { innerPadding ->
                HomeScreen(navController, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}