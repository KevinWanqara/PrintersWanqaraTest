package com.example.printerswanqaratest.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.with
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.printerswanqaratest.api.ApiClient
import com.example.printerswanqaratest.api.LoginRequest
import com.example.printerswanqaratest.api.ResetPasswordRequest
import com.example.printerswanqaratest.data.AppStorage
import com.example.printerswanqaratest.ui.theme.Primary
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: (String) -> Unit,
    onDomainValidationRequested: () -> Unit,
    modifier: Modifier = Modifier,
    logoResId: Int
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isLargeScreen = screenWidth > 600.dp
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isReset by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val apiService = ApiClient.createApiService(context)
    val savedRuc = remember { AppStorage.getRuc(context) }

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
                savedRuc?.let {
                    Text(
                        text = buildAnnotatedString {
                            append("Accede a ")
                            withStyle(SpanStyle(color = Primary, fontWeight = FontWeight.Bold)) {
                                append(it)
                            }
                            append(" usando tu correo electrónico y contraseña")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                // Correo label
                Text(
                    text = "Correo electrónico",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start),
                    color = Color.Gray
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Ingresa tu correo electrónico") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Primary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Primary,
                        focusedLabelColor = Primary,
                        cursorColor = Primary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Contraseña label
                Text(
                    text = "Contraseña",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start),
                    color = Color.Gray
                )
                var passwordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Ingresa tu contraseña") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Primary)
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = Primary,
                        focusedLabelColor = Primary,
                        cursorColor = Primary
                    ),
                    trailingIcon = {
                        AnimatedContent(
                            targetState = passwordVisible,
                            transitionSpec = {
                                slideInVertically(initialOffsetY = { it }) with slideOutVertically(targetOffsetY = { -it })
                            }
                        ) { visible ->
                            val image = if (visible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = if (visible) "Hide password" else "Show password")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            loading = true
                            errorMessage = null
                            try {
                                println( "Logging in with email: $email and password: $password")
                                val response = apiService.login(LoginRequest(email, password))
                                println("Login response: $response")
                                println("Login successful, token: ${response.data.token}")
                                onLoginSuccess(response.data.token)
                                AppStorage.saveToken(context, response.data.token)
                                AppStorage.saveSettings(context,response.data.setting)
                                AppStorage.saveUserData(context, response.data.user)


                            } catch (e: Exception) {
                                errorMessage = "Login failed: ${e.localizedMessage}"
                            } finally {
                                loading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Acceder", fontSize = 18.sp)
                }
                TextButton(onClick = { isReset = true }) {
                    Text("¿Olvidaste tu contraseña?")
                }
                Spacer(modifier = Modifier.height(32.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "¿Esta no es tu empresa?",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        AppStorage.saveRuc(context, "")
                        onDomainValidationRequested()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Empieza de nuevo", color = Primary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                }
            } else {
                OutlinedTextField(
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
