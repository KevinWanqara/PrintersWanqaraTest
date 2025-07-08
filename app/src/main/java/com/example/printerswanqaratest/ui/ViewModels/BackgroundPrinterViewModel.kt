package com.example.printerswanqaratest.ui.ViewModels

import androidx.lifecycle.ViewModel
import com.example.printerswanqaratest.domain.models.Printers
import com.example.printerswanqaratest.domain.models.SystemType
import com.example.printerswanqaratest.domain.services.GetAllPrinters
import com.example.printerswanqaratest.domain.services.GetPrinter
import com.example.printerswanqaratest.api.sales.SalesService
import com.example.printerswanqaratest.api.sales.Sales
import com.example.printerswanqaratest.api.sales.SalesApiResponse

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