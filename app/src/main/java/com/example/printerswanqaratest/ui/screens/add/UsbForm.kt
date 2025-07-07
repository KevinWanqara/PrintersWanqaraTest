package com.example.printerswanqaratest.ui.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.text.isDigit
import kotlin.text.takeWhile
import kotlin.text.toIntOrNull

@Composable
private fun UsbForm() {
    var name by remember { mutableStateOf("") }
    var charNumber by remember { mutableStateOf("") }
    var copyNumber by remember { mutableStateOf("") }
    var docTypeExpanded by remember { mutableStateOf(false) }
    val docTypes = listOf("DNI", "RUC", "Passport")
    var selectedDoc by remember { mutableStateOf(docTypes.first()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp), // Add some horizontal padding to the whole form content
        verticalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing a bit
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("USB Printer Name") },
            modifier = Modifier.fillMaxWidth() // Ensure text fields also use full width
        )
        OutlinedTextField(
            value = charNumber,
            onValueChange = { charNumber = it.takeWhile { c -> c.isDigit() } },
            label = { Text("Characters Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = copyNumber,
            onValueChange = { copyNumber = it.takeWhile { c -> c.isDigit() } },
            label = { Text("Copy Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.fillMaxWidth()) { // Make Box take full width for centering dropdown if needed
            OutlinedButton(
                onClick = { docTypeExpanded = true },
                modifier = Modifier.align(Alignment.Center) // Center the button in the Box
            ) {
                Text("Tipo Documento: $selectedDoc")
            }
            DropdownMenu(
                expanded = docTypeExpanded,
                onDismissRequest = { docTypeExpanded = false }
            ) {
                docTypes.forEach { type ->
                    DropdownMenuItem(text = { Text(type) }, onClick = {
                        selectedDoc = type; docTypeExpanded = false
                    })
                }
            }
        }
        Button(
            onClick = {
                saveUsbPrinter(
                    name,
                    charNumber.toIntOrNull() ?: 0,
                    copyNumber.toIntOrNull() ?: 0,
                    selectedDoc
                )
            },
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f) // Takes 80% of the column width
                .padding(top = 16.dp), // More space above the button
            contentPadding = PaddingValues(
                vertical = 12.dp,
                horizontal = 24.dp
            ) // Decent internal padding
        ) {
            Text("Save USB Printer")
        }
    }
}