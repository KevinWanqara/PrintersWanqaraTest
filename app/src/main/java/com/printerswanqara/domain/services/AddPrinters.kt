package com.printerswanqara.domain.services

import com.printerswanqara.data.database.entities.PrintersEntity
import com.printerswanqara.data.database.repositories.PrinterRepository
import javax.inject.Inject


class AddPrinters @Inject constructor(private val repository: PrinterRepository) {
    suspend operator fun invoke(printersEntity: PrintersEntity) {
        try {
            repository.add(printersEntity)
        } catch (e: Exception) {
            println(e)
        }
    }
}