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

import com.example.printerswanqara.BuildConfig
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
    val cash_register : CashRegisterDetails,



)

data class CashRegisterDetails(
    val payment_methods: Boolean? = null,
    val movements: Boolean? = null,
    val logbook: Boolean? = null,
    val sales_by_product: Boolean? = null,
    val sales_resume: Boolean? = null,
    val font: String? = null,
    val logo: Boolean? = null,
    val user: Boolean? = null,
    val line_breaks: Int? = null,
    val line_spacing: Boolean? = null,
    val copies: Int? = null,
    val tip: Boolean? = null,
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

data class PrintRequest(
    val address: String,
    val data: com.google.gson.JsonObject,
    val type: String
)

interface DynamicPrinterService {
    @POST("api/print/discrimination")
    suspend fun print(@Body request: PrintRequest): okhttp3.ResponseBody
}

data class ServerPrintRequest(
    val data: com.google.gson.JsonObject ? = null,
    val printType: String? = null,
    val openDrawer: Boolean? = null
)

interface ServerPrinterService {
    @POST("receiptPrinter/invoice-ticket")
    suspend fun printInvoiceTicket(@Body request: ServerPrintRequest): okhttp3.ResponseBody

    @POST("receiptPrinter/ticket-a")
    suspend fun printTicketA(@Body request: ServerPrintRequest): okhttp3.ResponseBody

    @POST("receiptPrinter/ticket-b")
    suspend fun printTicketB(@Body request: ServerPrintRequest): okhttp3.ResponseBody

    @POST("receiptPrinter/ticket-c")
    suspend fun printTicketC(@Body request: ServerPrintRequest): okhttp3.ResponseBody

    @POST("receiptPrinter/preticket")
    suspend fun printPreticket(@Body request: ServerPrintRequest): okhttp3.ResponseBody

    @POST("receiptPrinter/quote")
    suspend fun printQuote(@Body request: ServerPrintRequest): okhttp3.ResponseBody

    @POST("receiptPrinter/cashregister")
    suspend fun printCashRegister(@Body request: ServerPrintRequest): okhttp3.ResponseBody
}

object ApiClient {
    private const val BASE_URL = BuildConfig.BASE_URL

    //private const val BASE_URL = "https://system.wanqara.org/api/v1/"
    //private const val BASE_URL = "https://system.wanqara.app/api/v1/"

    private fun getUnsafeOkHttpClientBuilder(): OkHttpClient.Builder {
        try {
            // Create a trust manager that does not validate certificate chains
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }

                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            })

            // Install the all-trusting trust manager
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            // Create an ssl socket factory with our all-trusting manager
            val sslSocketFactory = sslContext.socketFactory

            val builder = OkHttpClient.Builder()
            builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            builder.hostnameVerifier { _, _ -> true }
            return builder
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // Provide context when building the client
    fun createApiService(context: Context, withTenant: Boolean = true): ApiService {
        val client = getUnsafeOkHttpClientBuilder()
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

        val client = getUnsafeOkHttpClientBuilder()

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

        val client = getUnsafeOkHttpClientBuilder()

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

        val client = getUnsafeOkHttpClientBuilder()

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

        val client = getUnsafeOkHttpClientBuilder()

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

        val client = getUnsafeOkHttpClientBuilder()

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

        val client = getUnsafeOkHttpClientBuilder()

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

    fun createDynamicPrinterService(context: Context, baseUrl: String): DynamicPrinterService {
        val client = getUnsafeOkHttpClientBuilder()
            .addInterceptor { chain ->
                val original: Request = chain.request()
                // We might not need headers for local IP, but keeping consistent with user request to "use this"
                // If it fails due to headers, we can remove them. 
                // However, usually local services ignore unknown headers.
                // Let's include them to be safe/consistent.
                val ruc = AppStorage.getRuc(context)
                val token = AppStorage.getToken(context)
                val requestBuilder = original.newBuilder()
                    .addHeader("Accept", "application/json")
                if (ruc != null) {
                    requestBuilder.addHeader("X-tenant", ruc.trim())
                }
                if (token != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(DynamicPrinterService::class.java)
    }

    fun createServerPrinterService(context: Context, baseUrl: String): ServerPrinterService {
        val client = getUnsafeOkHttpClientBuilder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original: Request = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Content-Type", "application/json")
                chain.proceed(requestBuilder.build())
            }
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(ServerPrinterService::class.java)
    }

}
