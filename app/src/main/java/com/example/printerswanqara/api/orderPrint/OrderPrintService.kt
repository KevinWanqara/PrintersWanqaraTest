package com.example.printerswanqara.api.orderPrint

import com.example.printerswanqara.api.sales.SalesApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query




interface OrderPrintService {
    @GET("/restaurant/order_printers/{id}")
    //Get order print detail function
    suspend fun getOrderPrintById(

        @Path("id") id: String,
        @Query("include") include: String? = "orderPrintDetails.printers,order,printers,order.table,order.table.area,order.subsidiary"
    ): OrderPrintApiResponse
}



