package com.example.printerswanqara.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.printerswanqara.data.AppStorage
import android.content.Context
import com.example.printerswanqara.api.sales.SalesService
import com.example.printerswanqara.api.baseinfo.BaseInfoService
import com.example.printerswanqara.api.cashRegister.CashRegisterService
import com.example.printerswanqara.api.order.OrderService
import com.example.printerswanqara.api.quotes.QuotesService
import com.example.printerswanqara.api.orderPrint.OrderPrintService
// Data classes for login
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(
    val data: UserData,
)

data class UserData(
    val token: String,
    val user: UserDetails,
    val setting : Setting
)
data class UserDetails(
    val id: String,
    val name: String,
    val email: String,
    val full_image_path : String,
)

data class Setting(
    val accounting_force: Boolean,
    val active: Boolean,
    val address: String,
    val artisan_resolution: String,
    val business_name: String,
    val checkouts: Int,
    val currency: String,
    val decimals: Int,
    val default_payment_method_id: String,
    val email: String,
    val emission_type: String,
    val environment: String,

    val expiration_date: String,
    val goods_transportation: Boolean,
    val has_digital_scales: Boolean,
    val has_product_rates: Boolean,
    val has_subsidy_products: Boolean,
    val has_various_businesses: Boolean,
    val identity_code_id: String,
    val integer_weight: Int,
    val moves_inventory: Boolean,
    val phone: String,
    val printers : Printers,
    val receipts: Int,
    val regime: String,
    val ruc: String,
    val sell_without_inventory: Boolean,


    val special_taxpayer: String,
    val subscriptions_type: String,
    val subsidiaries: Int,
    val use_edocument: Boolean,
    val users: Int,
    val uses_banners: Boolean,
    val uses_color_size_product: Boolean,
    val uses_detailed_payment: Boolean,
    val uses_kds: Boolean,
    val uses_lote_product: Boolean,
    val uses_payment_accounts: Boolean,
    val uses_restaurant: Boolean,
    val uses_serie_product: Boolean,
    val warehouses: Int,
    val weight: Int,
    val withholding_resolution: String,




)

data class Printers(
    val invoice : PrinterDetails,
    val order : PrinterDetails,
    val preticket : PrinterDetails,
    val receipt : PrinterDetails,


)

data class PrinterDetails(
    val copies: Int ? = null,
    val font : String ? = null,
    val header : Boolean ? = null,
    val line_breaks : Int ? = null,
    val line_spacing: Boolean ? = null,
    val logo : Boolean ? = null,
    val observation : Boolean ? = null,
    val taxes : Boolean ? = null,
    val user : Boolean ? = null,
    val tip : Boolean ? = null,

)
data class ResetPasswordRequest(val email: String)
data class ResetPasswordResponse(val message: String)

data class VerifyRucResponse(
    val data: RucData,
    val message: String
)

data class RucData(
    val id: String,
    val ruc: String,
    val name: String
)
interface ApiService {
    @POST("apps/printers/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("general/forgot-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): ResetPasswordResponse

    @GET("tenants/verify-ruc")
    suspend fun verifyRuc(@Query("ruc") ruc: String): VerifyRucResponse
}

object ApiClient {
    private const val BASE_URL = "https://system.wanqara.app/api/v1/"

    // Provide context when building the client
    fun createApiService(context: Context, withTenant: Boolean = true): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val ruc = AppStorage.getRuc(context)
                val token = AppStorage.getToken(context)
                println("RUC from storage: $ruc") // Debug log to check RUC value
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json") // Add Accept header


                if (ruc != null) {
                    println("Adding X-tenant header with RUC: $ruc") // Debug log to confirm header addition
                    requestBuilder.addHeader("X-tenant", ruc.trim()) // Add X-tenant header if RUC is not null
                }
                if (token != null) {
                    println("Adding Authorization header with token: $token") // Debug log to confirm header addition
                    requestBuilder.addHeader("Authorization", "Bearer $token") // Add Authorization header if token is not null

                }

                chain.proceed(requestBuilder.build())
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        println("Creating ApiService with base URL: $BASE_URL") // Debug log to confirm base URL
        println("Using tenant: $withTenant") // Debug log to confirm tenant usage

        return retrofit.create(ApiService::class.java)
    }


    fun createBaseInfoService(context: Context): BaseInfoService {

        val client = OkHttpClient.Builder()

            .addInterceptor { chain ->
                val original: Request = chain.request()
                val ruc = AppStorage.getRuc(context)
                val token = AppStorage.getToken(context)
                println("RUC from storage: $ruc")
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                if (ruc != null) {
                    println("Adding X-tenant header with RUC: $ruc")
                    requestBuilder.addHeader("X-tenant", ruc.trim())
                }
                if (token != null) {
                    println("Adding Authorization header with token: $token")
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                } else {
                    println("No token found, Authorization header not added")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(BaseInfoService::class.java)
    }

    fun createSalesService(context: Context): SalesService {

        val client = OkHttpClient.Builder()

            .addInterceptor { chain ->
                val original: Request = chain.request()
                val ruc = AppStorage.getRuc(context)
                val token = AppStorage.getToken(context)
                println("RUC from storage: $ruc")
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                if (ruc != null) {
                    println("Adding X-tenant header with RUC: $ruc")
                    requestBuilder.addHeader("X-tenant", ruc.trim())
                }
                if (token != null) {
                    println("Adding Authorization header with token: $token")
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                } else {
                    println("No token found, Authorization header not added")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(SalesService::class.java)
    }

    fun createQuotesService(context: Context): QuotesService {

        val client = OkHttpClient.Builder()

            .addInterceptor { chain ->
                val original: Request = chain.request()
                val ruc = AppStorage.getRuc(context)
                val token = AppStorage.getToken(context)
                println("RUC from storage: $ruc")
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                if (ruc != null) {
                    println("Adding X-tenant header with RUC: $ruc")
                    requestBuilder.addHeader("X-tenant", ruc.trim())
                }
                if (token != null) {
                    println("Adding Authorization header with token: $token")
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                } else {
                    println("No token found, Authorization header not added")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(QuotesService::class.java)
    }

    fun createOrderService(context: Context): OrderService {

        val client = OkHttpClient.Builder()

            .addInterceptor { chain ->
                val original: Request = chain.request()
                val ruc = AppStorage.getRuc(context)
                val token = AppStorage.getToken(context)
                println("RUC from storage: $ruc")
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                if (ruc != null) {
                    println("Adding X-tenant header with RUC: $ruc")
                    requestBuilder.addHeader("X-tenant", ruc.trim())
                }
                if (token != null) {
                    println("Adding Authorization header with token: $token")
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                } else {
                    println("No token found, Authorization header not added")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(OrderService::class.java)
    }

    fun createCashRegisterService(context: Context): CashRegisterService {

        val client = OkHttpClient.Builder()

            .addInterceptor { chain ->
                val original: Request = chain.request()
                val ruc = AppStorage.getRuc(context)
                val token = AppStorage.getToken(context)
                println("RUC from storage: $ruc")
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                if (ruc != null) {
                    println("Adding X-tenant header with RUC: $ruc")
                    requestBuilder.addHeader("X-tenant", ruc.trim())
                }
                if (token != null) {
                    println("Adding Authorization header with token: $token")
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                } else {
                    println("No token found, Authorization header not added")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(CashRegisterService::class.java)
    }

    fun createOrderPrintService(context: Context): OrderPrintService {

        val client = OkHttpClient.Builder()

            .addInterceptor { chain ->
                val original: Request = chain.request()
                val ruc = AppStorage.getRuc(context)
                val token = AppStorage.getToken(context)
                println("RUC from storage: $ruc")
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                if (ruc != null) {
                    println("Adding X-tenant header with RUC: $ruc")
                    requestBuilder.addHeader("X-tenant", ruc.trim())
                }
                if (token != null) {
                    println("Adding Authorization header with token: $token")
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                } else {
                    println("No token found, Authorization header not added")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(OrderPrintService::class.java)
    }



}
