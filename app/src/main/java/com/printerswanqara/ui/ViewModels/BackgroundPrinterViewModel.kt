package com.printerswanqara.ui.ViewModels

import androidx.lifecycle.ViewModel
import com.printerswanqara.domain.models.Printers
import com.printerswanqara.domain.services.GetAllPrinters
import com.printerswanqara.domain.services.GetPrinter
import com.printerswanqara.api.sales.SalesService
import com.printerswanqara.api.sales.Sales
import com.printerswanqara.api.sales.SalesApiResponse

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BackgroundPrinterViewModel @Inject constructor(
    private val getPrinter: GetPrinter,
    private val getAllPrinters: GetAllPrinters,
    private val salesService: SalesService // Inject SalesService
) :ViewModel() {

    suspend fun onCreate(document:String): Printers? {
        return getPrinter.getPrinterByDocument(document)
    }

    suspend fun getAll():List<Printers>{
        println("Fetching all printers...")
        return getAllPrinters.getAll()
    }

    suspend fun getSaleById(saleId: String): Sales? {
        println("Fetching sale with ID: $saleId")

        return try {
            val response: SalesApiResponse = salesService.getSalesById(saleId)
            response.data
        } catch (e: Exception) {
            null
        }
    }
}