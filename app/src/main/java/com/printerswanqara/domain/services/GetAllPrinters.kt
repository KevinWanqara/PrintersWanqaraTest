package com.printerswanqara.domain.services

import com.printerswanqara.data.database.repositories.PrinterRepository
import com.printerswanqara.domain.models.Printers
import javax.inject.Inject

class GetAllPrinters @Inject constructor(private val repository: PrinterRepository) {
    suspend fun getAll(): List<Printers> {
        return repository.getAll().map { it -> it.getPrinterModelFromPrinterEntity() }
    }

}