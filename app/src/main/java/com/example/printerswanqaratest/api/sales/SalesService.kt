package com.example.printerswanqaratest.api.sales

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query




interface SalesService {
    @GET("billing/sales/{id}")
    suspend fun getSalesById(
        @Path("id") id: String,
        @Query("include") include: String? = null
    ): SalesApiResponse
}



