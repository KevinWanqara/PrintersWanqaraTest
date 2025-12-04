package com.example.printerswanqara.api.cashRegister


import com.example.printerswanqara.api.UserDetails
import com.example.printerswanqara.api.sales.PaymentMethod
import com.example.printerswanqara.api.sales.Sales
import com.example.printerswanqara.api.sales.Subsidiary


data class CashRegisterApiResponse (
    val data: CashRegisterData,
)


data class CashRegisterData(
    val id : String,
    val active : Boolean,
    val cash_movements : List<CashMovements>? = null,
    val checkout : Checkout,
    val checkout_id : String,
    val difference : Double,
    val expenses : Double,
    val observation : String? = null,
    val opened_at : String? = null,
    val closed_at : String? = null,
    val opening : Double,
    val received_cash : Double,
    val revenue : Double,
    val sales : List <Sales>? = null,
    val sales_cash : Double,
    val sales_total : Double,
    val sequential : String,
    val subsidiary : Subsidiary,
    val subsidiary_id : String,
    val summary : Summary,
    val total_cash : Double,
    val user : UserDetails,
    val user_id : String,


    )


data class CashMovements(
    val id : String,
    val date : String,
    val description : String,
    val observation : String? = null,
    val responsible_name : String,
    val total : String,
    val type : String,
    val user_id : String,
)


data class Checkout(
    val id : String,
    val code : String,
    val name : String,
    val observation : String? = null,
    val sequential : String,
    val show_name : String,
    val subsidiary_id : String,

)


data class Summary(
    val totals: Totals,
    val payment_methods: List<PaymentMethodSummary>,
    val payment_methods_totals: PaymentMethodsTotals,
    val sales_by_product: List<SalesByProduct>,
    val sales_by_hour: SalesByHour,
    val controls: Controls,
    val movements: List<Movements>,
    val logbook: List<LogbookEntry>,
    val tips: Tips,
)

data class Movements(
    val concept: String,
    val datetime: String,
    val total : Double,
    val type: String,

)
data class Totals(
    val opening: Double,
    val sales_total_valid: Double,
    val sales_total_canceled: Double,
    val sales_total_net: Double,
    val sales_cash: Double,
    val sales_cash_cancelation: Double,
    val sales_cash_net: Double,
    val debt_amount: Double,
    val revenue: Double,
    val expenses: Double,
    val cancelations_total: Double,
    val cancelations_count: Int,
    val expected_cash: Double,
    val received_cash: Double,
    val difference: Double,
    val additional_tip_total: Double,
)

data class PaymentMethodSummary(
    val payment_method_id: String,
    val name: String,
    val is_cash: Boolean,
    val valid: CountAmount,
    val canceled: CountAmount,
    val net_amount: Double,
)

data class CountAmount(
    val amount: Double,
    val count: Int,
)

data class PaymentMethodsTotals(
    val valid: AmountCash,
    val canceled: AmountCash,
    val net: AmountCash,
)

data class AmountCash(
    val amount: Double,
    val cash: Double,
)

data class SalesByHour(
    val meta: SalesByHourMeta,
    val totals: SalesByHourTotals,
    val buckets: List<SalesByHourBucket>,
)

data class SalesByHourMeta(
    val same_day: Boolean,
    val range_start: String,
    val range_end: String,
)

data class SalesByHourTotals(
    val valid: CountAmount,
    val canceled: CountAmount,
    val overall: CountAmount,
    val net: NetAmount,
)

data class NetAmount(
    val amount: Double,
)

data class SalesByHourBucket(
    val hour: String,
    val valid_amount: Double,
    val canceled_amount: Double,
    val total_amount: Double,
    val valid_count: Int,
    val canceled_count: Int,
    val total_count: Int,
)

data class Controls(
    val cash_breakdown: CashBreakdown,
    val cancelations_total_pd: Double,
    val cancelations_total_cr: Double,
)

data class CashBreakdown(
    val opening: Double,
    val sales_cash_valid: Double,
    val sales_cash_canceled: Double,
    val revenue: Double,
    val expenses: Double,
)

data class LogbookEntry(
    val payment_detail_id: String,
    val payment_method: String,
    val paymentable_type: String,
    val paymentable_name: String,
    val document: Document,
    val type: String,
    val datetime: String,
    val amount: Double,
    val is_canceled: Boolean,
    val cancelation: Any? = null,
)

data class Document(
    val number: String,
    val date: String,
)

data class Tips(
    val additional_total: Double,
)


data class SalesByProduct(
    val product_id : String,
    val product_name : String,
    val total_discount : Double,
    val total_quantity : Double,
    val total_sold : Double,
    val total_sold_with_taxes : Double,

)