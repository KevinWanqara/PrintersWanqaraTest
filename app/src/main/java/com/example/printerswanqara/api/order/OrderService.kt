package com.example.printerswanqara.api.order






import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface OrderService {
    @GET("restaurant/orders/{id}")
    suspend fun getOrderById(

        @Path("id") id: String,
        @Query("include") include: String? = "table,details,tickets,tickets.details,details.productRelation,details.productRelation.taxes,table.area,subsidiary,taxes,subsidiary.image,orderPrints,orderPrints.orderPrintDetails.printers,deliveryRecord,deliveryRecord.delivery,"
        ): OrderApiResponse
}



