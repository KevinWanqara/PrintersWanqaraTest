package com.example.printerswanqara

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.printerswanqara.ui.theme.PrintersWanqaraTestTheme
import com.example.printerswanqara.data.TokenDatabaseHelper
import com.example.printerswanqara.ui.screens.LoginScreen
import com.example.printerswanqara.ui.screens.DomainValidationScreen
import com.example.printerswanqara.ui.screens.add.AddPrinterScreen
import com.example.printerswanqara.ui.screens.edit.EditPrinterScreen
import com.example.printerswanqara.ui.screens.list.ListPrintersScreen
import com.example.printerswanqara.ui.screens.configure.ConfigurePrinterScreen
import com.example.printerswanqara.ui.screens.tests.PrinterTestScreen
import com.example.printerswanqara.ui.screens.message.MessageScreen
import com.example.printerswanqara.data.AppStorage
import com.example.printerswanqara.ui.screens.home.HomeScreen
import androidx.navigation.compose.NavHost
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.printerswanqara.ui.theme.Primary
import coil.compose.rememberAsyncImagePainter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale


import com.google.accompanist.navigation.animation.composable

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import com.example.printerswanqara.api.ApiClient

class MainActivity : ComponentActivity() {
    //Main Activity
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrintersWanqaraTestTheme {
                val navController = rememberNavController()
                val context = this
                val baseInfoService = ApiClient.createBaseInfoService(context)

                var isLoggedIn by remember { mutableStateOf(false) }
                var isDomainValidated by remember { mutableStateOf(false) }
                val tokenDb = remember { TokenDatabaseHelper(context) }
                // Check for token and RUC on launch
                LaunchedEffect(Unit) {
                    val ruc = AppStorage.getRuc(context)
                    isDomainValidated = !ruc.isNullOrBlank()
                    isLoggedIn = tokenDb.getToken() != null

                    if (isLoggedIn) {
                        try {
                            val response = baseInfoService.getBaseInfo().data // Replace with the actual method
                            println("Saving Base Info: $response")
                            AppStorage.saveSettings(context, response) // Save the data locally
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                val navEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navEntry?.destination?.route
                when {
                    !isDomainValidated -> {
                        DomainValidationScreen(
                            onDomainValidated = {
                                isDomainValidated = true
                                isLoggedIn = false

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
                                var profileMenuExpanded by remember { mutableStateOf(false) }
                                val showBack = currentRoute == "add_printer" || (currentRoute != null && currentRoute != "list_printers")
                                CenterAlignedTopAppBar(
                                    title = {
                                        when (currentRoute) {
                                            "add_printer" -> Text("Agregar", color = Color.White)
                                            "edit_printer/{printerId}" -> Text("Actualizar", color = Color.White)
                                            "list_printers", null -> {
                                                Row(verticalAlignment = Alignment.CenterVertically ) {
                                                    Icon(
                                                        painterResource(id = R.drawable.ic_wanqara_logo_foreground),
                                                        contentDescription = "Wanqara Logo",
                                                        modifier = Modifier.size(32.dp),
                                                        tint = Color.White
                                                    )
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Wanqara Printers", color = Color.White)
                                                }
                                            }
                                            else -> Text("")
                                        }
                                    },
                                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                        containerColor = Primary,
                                        navigationIconContentColor = Color.White,
                                        actionIconContentColor = Color.White,
                                        titleContentColor = Color.White,

                                    ),
                                    navigationIcon = {
                                        if (showBack) {
                                            IconButton(onClick = { navController.popBackStack() }) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back"
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        Box {
                                            val userData = remember { mutableStateOf(AppStorage.getUserData(context)) }
                                            val profileImageUrl = userData.value?.full_image_path
                                            IconButton(onClick = { profileMenuExpanded = true }) {
                                                if (!profileImageUrl.isNullOrEmpty()) {
                                                    Image(
                                                        painter = rememberAsyncImagePainter(profileImageUrl),
                                                        contentDescription = "Profile Image",
                                                        modifier = Modifier
                                                            .size(32.dp)
                                                            .clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(Icons.Filled.Person, contentDescription = "Profile")
                                                }
                                            }
                                            DropdownMenu(
                                                expanded = profileMenuExpanded,
                                                onDismissRequest = { profileMenuExpanded = false }
                                            ) {
                                                // User info header
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (!profileImageUrl.isNullOrEmpty()) {
                                                        Image(
                                                            painter = rememberAsyncImagePainter(profileImageUrl),
                                                            contentDescription = "Profile Image",
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Icon(
                                                            Icons.Filled.Person,
                                                            contentDescription = "Profile",
                                                            modifier = Modifier
                                                                .size(48.dp)
                                                                .clip(CircleShape)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(userData.value?.name ?: "User Name", style = MaterialTheme.typography.titleMedium)
                                                        Text(userData.value?.email ?: "user@email.com", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                                    }

                                                }
                                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                                DropdownMenuItem(

                                                    text = { Text("Configuraciones") },
                                                    leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = "configs") },
                                                    onClick = {
                                                        profileMenuExpanded = false
                                                        navController.navigate("configure_printer")

                                                    }

                                                )
                                                DropdownMenuItem(

                                                    text = { Text("Cerrar Sesión") },
                                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout") },
                                                    onClick = {
                                                        profileMenuExpanded = false
                                                        AppStorage.clearSession(context)
                                                        isLoggedIn = false
                                                        isDomainValidated = false
                                                    }

                                                )
                                            }
                                        }
                                    }
                                )
                            },
                            floatingActionButton = {
                                if (currentRoute == "list_printers") {
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
                                startDestination = "list_printers",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding),
                                enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
                                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) },
                                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
                                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
                            ) {
                                composable("home") { HomeScreen(navController) }
                                composable("add_printer") { AddPrinterScreen(navController) }
                                composable("edit_printer/{printerId}") { backStackEntry ->
                                    val printerId = backStackEntry.arguments?.getString("printerId")
                                    EditPrinterScreen(printerId,navController)
                                }
                                composable("list_printers") { ListPrintersScreen(
                                    navController
                                ) }
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
                        title = { Text("Wanqara Printers") },

                    )

                }
            ) { innerPadding ->
                HomeScreen(navController, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}