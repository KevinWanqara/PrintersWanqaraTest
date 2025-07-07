package com.example.printerswanqaratest.ui.screens.list

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.printerswanqaratest.data.database.DatabaseProvider
import com.example.printerswanqaratest.data.database.repositories.PrinterRepository
import com.example.printerswanqaratest.domain.models.Printers
import com.example.printerswanqaratest.domain.services.GetAllPrinters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ListPrintersScreen() {
    val context = LocalContext.current
    var printers by remember { mutableStateOf<List<Printers>>(emptyList()) }

    LaunchedEffect(Unit) {
        printers = withContext(Dispatchers.IO) {
            val db = DatabaseProvider.getDatabase(context)
            val repository = PrinterRepository(db.printersDAO())
            val getAll = GetAllPrinters(repository)
            getAll.getAll()
        }
    }

    if (printers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No printers saved")
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            items(printers) { printer ->
                Text(text = printer.name)
            }
        }
    }
}
