package com.example.printerswanqara.api.cashRegister



import retrofit2.http.GET
import retrofit2.http.Path


interface CashRegisterService {
    @GET("general/cash-registers/{id}")
    suspend fun getCashRegisterById(

        @Path("id") id: String,

    ): CashRegisterApiResponse
}



