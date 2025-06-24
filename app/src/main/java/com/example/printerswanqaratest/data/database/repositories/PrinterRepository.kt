package com.example.printerswanqaratest.data.database.repositories

import com.example.printerswanqaratest.data.database.dao.PrinterDAO
import com.example.printerswanqaratest.data.database.entities.PrintersEntity
import javax.inject.Inject

class PrinterRepository @Inject constructor(
    private val printerDAO: PrinterDAO
) {
    suspend fun add(printersEntity: PrintersEntity) {
        val printersList = printerDAO.getAll()
        if (printersList.isNotEmpty()){
            printersList.forEach lit@{ it ->
                if (it.documentType == printersEntity.documentType){
                    printersEntity.id = it.id
                    return@lit
                }
            }
        }
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