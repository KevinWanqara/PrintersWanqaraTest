







import com.printerswanqara.api.paymentaccount.PaymentAccountApiResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface PaymentAccountService {
    @GET("accounting/payments/payment-account/{accountId}")
    suspend fun fetchAccount(
        @Path("accountId") accountId: String,
        @Query("include") include: String? = "person,paymentAccountable,paymentAccountDetails.paymentable.user,paymentAccountDetails,paymentDetails,paymentAccountDetails.paymentable,paymentAccountDetails.paymentable.person"
    ): PaymentAccountApiResponse
}


