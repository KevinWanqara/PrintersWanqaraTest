package com.example.printerswanqaratest.domain.services

import com.example.printerswanqaratest.data.database.repositories.PrinterRepository
import com.example.printerswanqaratest.domain.models.Printers
import javax.inject.Inject

class GetPrinter @Inject constructor(private val printerRepository: PrinterRepository) {

    suspend operator fun invoke(id:String): Printers {
        return printerRepository.findPrinterById(id).getPrinterModelFromPrinterEntity()
    }
    suspend fun getPrinterByDocument(document:String): Printers? {
        return printerRepository.findPrinterByDocumentType(document)?.getPrinterModelFromPrinterEntity()
    }
}