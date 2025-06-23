package com.example.printerswanqaratest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printerswanqaratest.api.ApiClient
import com.example.printerswanqaratest.api.LoginRequest
import com.example.printerswanqaratest.api.ResetPasswordRequest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier,
    logoResId: Int
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isLargeScreen = screenWidth > 600.dp
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isReset by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiService = remember { ApiClient.createApiService(context) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isLargeScreen) 0.4f else 0.9f)
                .wrapContentHeight()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            if (!isReset) {
                TextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            loading = true
                            errorMessage = null
                            try {
                                val response = apiService.login(LoginRequest(username, password))
                                onLoginSuccess(response.token)
                            } catch (e: Exception) {
                                errorMessage = "Login failed: ${e.localizedMessage}"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Login", fontSize = 18.sp)
                }
                TextButton(onClick = { isReset = true }) {
                    Text("Forgot password?")
                }
            } else {
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            loading = true
                            errorMessage = null

                            try {
                                val response = apiService.resetPassword(ResetPasswordRequest(email))
                                errorMessage = response.message
                            } catch (e: Exception) {
                                errorMessage = "Reset failed: ${e.localizedMessage}"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Reset Password", fontSize = 18.sp)
                }
                TextButton(onClick = { isReset = false }) {
                    Text("Back to Login")
                }
            }
            if (loading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = Color.Red)
            }
        }
    }
}
