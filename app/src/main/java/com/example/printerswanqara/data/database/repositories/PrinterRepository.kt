package com.example.printerswanqara.data.database.repositories

import com.example.printerswanqara.data.database.dao.PrinterDAO
import com.example.printerswanqara.data.database.entities.PrintersEntity
import javax.inject.Inject

class PrinterRepository @Inject constructor(
    private val printerDAO: PrinterDAO
) {
    suspend fun add(printersEntity: PrintersEntity) {

        printersEntity.id = null
        val printersList = printerDAO.getAll()
        if (printersList.isNotEmpty()){
            printersList.forEach lit@{ it ->
                println("Checking existing printer with document type: ${it.documentType}")
                if (it.documentType == printersEntity.documentType){
                    println("Printer with document type ${it.documentType} already exists with ID ${it.id}. Updating ID of the new printer entity.")
                    // If a printer with the same document type exists, update its ID

                    printersEntity.id = it.id
                    return@lit
                }
            }
        }

        // Always insert as new: set id to null so Room generates a new id

        println(
            "Inserting new printer entity with document type ${printersEntity.documentType}."
        )
        printerDAO.insertAll(printersEntity)
    }

    suspend fun getAll(): List<PrintersEntity> {
        return printerDAO.getAll()
    }
    suspend fun delete(printer: Int) {
        return printerDAO.delete(printer)
    }

    suspend fun findPrinterById(id:String): PrintersEntity {
        return printerDAO.findById(id)
    }
    suspend fun findPrinterByDocumentType(document:String): PrintersEntity? {
        return printerDAO.getPrinterByDocumentType(document)
    }
    suspend fun update(printersEntity: PrintersEntity) {
        printerDAO.update(printersEntity)
    }
}