package com.example.printerswanqara.api.sales


import com.example.printerswanqara.api.Setting
import com.example.printerswanqara.api.UserDetails
import com.example.printerswanqara.api.order.Order

// Main API response for a sales request
data class SalesApiResponse(
    val data: Sales?,
    val message: String? = null
)


data class Sales(
    val date: String,
    val payment_methods: List<PaymentMethod>,
    val observation: String,
    val document_type: String,
    val support_code: String,
    val customer_id: String,
    val subsidiary_id: String,
    val checkout_id: String,
    val warehouse_id: String,
    val user_id: String,
    val user: UserDetails?,
    val sequential: String,
    val access_key: String,
    val number: String,
    val id: String,
    val discount: Double,
    val subtotal: Double,
    val total: Double,
    val delivered: Boolean,
    val details: List<Detail>,
    val creation_type: String?,
    val subsidiary: Subsidiary,
    val warehouse: Warehouse,
    val customer: Customer?,
    val payment_account: PaymentAccount,
    val taxes: List<Tax>,
    //val edocument: List<EDocument>,
    val settings: Setting,
    val summary: Summary,
    val is_debt: Boolean,
    val debt_amount: Double,
    val is_canceled: Boolean,
    val title: String,
    val quote_id: String?,
    val created_at: String?,
    val label: String?,
    val description: String?,
    val complete: Boolean?,
    val change_amount : Double?,
    val inventory_records: List<SaleInventoryRecord>?,
    val order : Order,

    )


data class OrderPrints(
    val sequential: String,
    val order_print_details: List<OrderPrintDetails>
)

data class OrderPrintDetails(
    val type : String,
    val sequential: String,
    val product_name : String,
    val amount : Double,
    val printers : List<Printers>,
)

data class Printers (
    val id: String,
    val code : String,
    val name : String,
    val observation : String?,

)

data class Table(
    val id: String,
    val name: String,
)
data class SaleInventoryRecord(
    val id: String,
    val date: String,
    val type: String,
    val model_origin: String,
    val document_number: String,
    val sequential: String,
    val warehouse_id: String,
    val warehouse: Warehouse,
    val details: List<Detail>,
    val person_id: String,
    val person_identity: String,
    val person_name: String,
    val responsible_name: String,
    val user_id: String,
    val observation: String?,
    val location_description: String?,
    val requested_at: String?,
    val requested_by_name: String?
)

data class Summary(
    val discount: Double,
    val showDiscount: Double?,
    val subtotal: Double,
    val showSubtotal: Double?,
    val subtotal_rate: List<SubtotalRate>?,
    val iva_rate: List<IvaRate>?,
    val total: Double
)

data class SubtotalRate(
    val rate: Double,
    val subtotal: Double,
    val showSubtotalRate: Double?
)

data class IvaRate(
    val rate: Double,
    val iva: Double,
    val showIvaRate: Double?
)

data class PaymentMethod(
    val code: String,
    val default: Boolean,
    val display_name: String,
    val end_date: String?,
    val id: String,
    val name: String,
    val start_date: String,
    val amount: String?,
    val unit: String?,
    val deadline: String?,
    val code_sri: String?,
    val type: String?
)

data class Detail(
    val id: String,
    val amount: Double,
    val price: Double,
    val discount: Double,
    val total: Double,
    val description: String,
    val additional_information: String,
    val spent: Boolean,
    val product: Product,
    val product_id: String,
    val total_amount: Double?,
    val pending_amount: Double?,
    val moved_stock_amount: Double?
)

data class Subsidiary(
    val id: String,
    val commercial_name: String,
    val code: String,
    val address: String,
    val phone: String,
    val active: Boolean,
    val dispatch_inventory: Boolean,
    val differentiated_billing: Boolean,
    val city_id: String,
    val image : SubsidiaryImage,
    val observation : String? = null,
)

data class SubsidiaryImage(
    val full_path: String,

)

data class Warehouse(
    val id: String,
    val name: String,
    val code: String,
    val address: String,
    val description: String,
    val active: Boolean,
    val inventory: Boolean,
    val latitude: Double?,
    val longitude: Double?,
    val is_main: Boolean
)

data class Customer(
    val id: String,
    val code: String,
    val name: String,
    val commercial_name: String,
    val identity: String,
    val address: String,
    val active: Boolean,
    val phones: List<String>,
    val emails: List<String>,
    val roles: List<String>,
    val foreign: Boolean,
    val artisan_resolution: String?,
    val special_taxpayer: String?,
    val person_type: String?,
    val gender: String?,
    val civil_status: String?,
    val payment_foreign: String,
    val regimen_type: String?,
    val quota: String,
    val credit: String,
    val related: Boolean,
    val accounting_force: Boolean,
    val tax_support: String?,
    val country_id: String?,
    val seller_id: String?,
    val identity_code_id: String,
    val identity_code: List<IdentityCode>,
    val addresses: List<Address>
)

data class IdentityCode(
    val id: String,
    val code: String,
    val name: String,
    val show: Boolean,
    val identity_types: List<IdentityType>
)

data class IdentityType(
    val id: String,
    val code: String,
    val transaction_type: String,
    val start_date: String,
    val end_date: String,
    val identity_code_id: String
)

data class Address(
    val id: String,
    val name: String,
    val address: String,
    val observation: String,
    val city_id: String,
    val latitude: Double?,
    val longitude: Double?,
    val person_id: String
)

data class PaymentAccount(
    val id: String,
    val amount: Double,
    val balance: Double,
    val type: String,
    val movement_type: String,
    val supported: Boolean,
    val closed: Boolean,
    val expiration_date: String,
    val person_id: String,
    val is_detail: Boolean
)



data class EDocument(
    val id: String,
    val access_key: String,
    val date: String,
    val path: String,
    val code: String,
    val messages: String,
    val status: Boolean,
    val environment: Int,
    val edocumentable_id: String,
    val edocumentable_type: String,
    val authorized: Boolean,
    val send: Boolean,
    val signed: Boolean,
    val created_at: String,
    val updated_at: String
)

