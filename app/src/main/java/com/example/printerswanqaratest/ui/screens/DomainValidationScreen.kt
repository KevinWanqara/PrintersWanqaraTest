package com.example.printerswanqaratest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.printerswanqaratest.api.ApiClient
import com.example.printerswanqaratest.data.AppStorage
import kotlinx.coroutines.launch


@Composable
fun DomainValidationScreen(
    onDomainValidated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var ruc by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = remember { ApiClient.createApiService(context) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Enter your RUC/domain", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = ruc,
                onValueChange = { ruc = it },
                label = { Text("RUC") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        loading = true
                        errorMessage = null
                        try {
                            val response = apiService.verifyRuc(ruc)
                            if (response.valid) {
                                AppStorage.saveRuc(context, response.ruc)
                                onDomainValidated()
                            } else {
                                errorMessage = "Invalid RUC/domain."
                            }
                        } catch (e: Exception) {
                            errorMessage = "Validation failed: ${e.localizedMessage}"
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ruc.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Validate", style = MaterialTheme.typography.titleMedium)
            }
            if (loading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
