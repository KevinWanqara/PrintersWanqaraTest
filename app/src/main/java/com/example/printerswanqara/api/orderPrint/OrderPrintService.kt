package com.example.printerswanqara.api.orderPrint

import com.example.printerswanqara.api.sales.SalesApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query




interface OrderPrintService {
    @GET("restaurant/order-printers/{id}")
    //Get order print detail function
    suspend fun getOrderPrintById(

        @Path("id") id: String,
    ): OrderPrintApiResponse
}



