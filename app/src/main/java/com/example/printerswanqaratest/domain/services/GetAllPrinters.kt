package com.example.printerswanqaratest.domain.services

import com.example.printerswanqaratest.data.database.repositories.PrinterRepository
import com.example.printerswanqaratest.domain.models.Printers
import javax.inject.Inject

class GetAllPrinters @Inject constructor(private val repository: PrinterRepository) {
    suspend fun getAll(): List<Printers> {
        return repository.getAll().map { it -> it.getPrinterModelFromPrinterEntity() }
    }

}