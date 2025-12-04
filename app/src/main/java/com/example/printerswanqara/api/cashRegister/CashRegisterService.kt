package com.example.printerswanqara.api.cashRegister



import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface CashRegisterService {
    @GET("general/cash-registers/{id}")
    suspend fun getCashRegisterById(

        @Path("id") id: String,
        @Query("include") include: String? = "user,subsidiary,checkout"

    ): CashRegisterApiResponse
}



