package com.example.printerswanqaratest.data.database.repositories

import com.example.printerswanqaratest.data.database.dao.PrinterDAO
import com.example.printerswanqaratest.data.database.entities.PrintersEntity
import javax.inject.Inject

class PrinterRepository @Inject constructor(
    private val printerDAO: PrinterDAO
) {
    suspend fun add(printersEntity: PrintersEntity) {

        /*
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
        */
        // Always insert as new: set id to null so Room generates a new id
        printersEntity.id = null
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

    suspend fun findPrinterById(id:Int): PrintersEntity {
        return printerDAO.findById(id)
    }
    suspend fun findPrinterByDocumentType(document:String): PrintersEntity? {
        return printerDAO.getPrinterByDocumentType(document)
    }
}