package com.example.printerswanqara.api.quotes



import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query




interface QuotesService {
    @GET("billing/quotes/{id}")
    suspend fun getQuoteById(

        @Path("id") id: String,

    ): QuotesApiResponse
}



