package com.example.printerswanqaratest.domain.models

import com.example.printerswanqaratest.core.document.documentType
import com.example.printerswanqaratest.data.database.entities.PrintersEntity

class Printers {
    val id: Int?
    val fontSize: String
    val name: String
    val documentType: String
    val copyNumber: Int
    val charactersNumber: Int
    val type: String
    var address: String?
    var port: Int?

    constructor(
        id: Int?,
        fontSize: String,
        name: String,
        documentType: String,
        copyNumber: Int,
        charactersNumber: Int,
        type: String,
        address: String?,
        port: Int?
    ) {
        this.id = id
        this.name = name
        this.fontSize = fontSize
        this.documentType = documentType
        this.copyNumber = copyNumber
        this.charactersNumber = charactersNumber
        this.type = type
        this.address = address
        this.port = port
    }
    fun createPrinterEntityFromPrinterModel(): PrintersEntity {
        val documentType:documentType = documentType()
        return PrintersEntity(
            this.id,
            this.name,
            this.fontSize,
            documentType.findKeyByDocument(this.documentType) ?: this.documentType,
            this.copyNumber,
            this.charactersNumber,
            this.type,
            this.address,
            this.port
        )
    }
    fun createEntityFromPrinterModel(): PrintersEntity {
        return PrintersEntity(
            this.id,
            this.name,
            this.fontSize,
            this.documentType,
            this.copyNumber,
            this.charactersNumber,
            this.type,
            this.address,
            this.port
        )
    }
}
