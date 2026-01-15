package com.example.printerswanqara.ui.screens

import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.printerswanqara.api.ApiClient
import com.example.printerswanqara.data.AppStorage
import com.example.printerswanqara.ui.theme.Primary
import kotlinx.coroutines.launch


@Composable
fun DomainValidationScreen(
    onDomainValidated: () -> Unit,
    modifier: Modifier = Modifier,
    logoResId: Int
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var ruc by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = ApiClient.createApiService(context)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (true) 0.9f else 0.9f)
                .wrapContentHeight()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(id = logoResId),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Bienvenido a Wanqara",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Primary,
                textAlign =  TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Por favor, ingrese el dominio de su empresa para continuar.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            // Label on top
            Text(
                "Ingresa tu número de RUC",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Start),
                color = Color.Gray
            )
            OutlinedTextField(
                value = ruc,
                onValueChange = { ruc = it },
                placeholder = { Text("Ingrese el dominio de su empresa") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Domain, contentDescription = null, tint = Primary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Primary,
                    focusedLabelColor = Primary,
                    cursorColor = Primary
                ),

                visualTransformation = VisualTransformation.None
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        loading = true
                        errorMessage = null
                        try {
                            val response = apiService.verifyRuc(ruc)
                            if (response.data.ruc.isNotBlank()) {
                                AppStorage.saveRuc(context, response.data.id)
                                val toast = Toast(context)
                                val textView = TextView(context)
                                textView.text = "Dominio validado correctamente"
                                val background = GradientDrawable()
                                background.cornerRadius = 16f
                                background.setColor(Primary.toArgb())
                                textView.background = background
                                textView.setPadding(32, 16, 32, 16)
                                textView.setTextColor(android.graphics.Color.WHITE)
                                toast.view = textView
                                toast.duration = Toast.LENGTH_SHORT
                                toast.show()
                                onDomainValidated()
                            } else {
                                errorMessage = "Dominio inválido. Por favor, verifica e intenta nuevamente."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error al validar el dominio: ${e.localizedMessage}"
                            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ruc.isNotBlank() && !loading,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Validar", style = MaterialTheme.typography.titleMedium)
            }
            if (loading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "¿Tienes problemas para acceder?",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Contáctanos",
                color = Primary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clickable { uriHandler.openUri("https://wanqara.com/contacto/") }
                    .padding(top = 4.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}
