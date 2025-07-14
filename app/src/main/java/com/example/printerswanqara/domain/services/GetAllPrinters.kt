package com.example.printerswanqara.domain.services

import com.example.printerswanqara.data.database.repositories.PrinterRepository
import com.example.printerswanqara.domain.models.Printers
import javax.inject.Inject

class GetAllPrinters @Inject constructor(private val repository: PrinterRepository) {
    suspend fun getAll(): List<Printers> {
        return repository.getAll().map { it -> it.getPrinterModelFromPrinterEntity() }
    }

}