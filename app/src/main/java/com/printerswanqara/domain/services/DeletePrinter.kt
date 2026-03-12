package com.printerswanqara.domain.services

import com.printerswanqara.data.database.repositories.PrinterRepository
import javax.inject.Inject

class DeletePrinter  @Inject constructor(private val repository: PrinterRepository) {

    suspend operator fun invoke(printers: Int){
        repository.delete(printers)
    }
}