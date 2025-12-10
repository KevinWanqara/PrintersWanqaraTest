package com.example.printerswanqara.api.orderPrint

data class OrderPrintApiResponse(
    val data: OrderPrintData
)



data class OrderPrintData(
    val id: String,
    val sequential: String,
    val responsible_name: String,
    val observation: String?,
    val is_initial: Boolean,
    val order_type : String,
    val is_urgent: Boolean,
    val order_id: String,
    val user_id: String,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String?,
    val subsidiary_id: String,
    val order_print_details: List<OrderPrintDetailItem>,
    val order: OrderPayloadDetails,
    val printers: List<PrinterInfoAssociated>

)

data class OrderPrintDetailItem(
    val id: String,
    val sequential: String,
    val amount: Int,
    val type: String,
    val product_name: String,
    val additional_information: List<Any>, // Or List<Any> if more complex/unknown
    val is_initial: Boolean,
    val order_print_id: String,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String?,
    val printers: List<PrinterInDetailItem>
)

data class PrinterInDetailItem(
    val id: String,
    val name: String,
    val code: String,
    val observation: String?,
    val pivot: PivotOrderDetailPrinterItem
)

data class PivotOrderDetailPrinterItem(
    val order_print_detail_id: String,
    val printer_id: String,
    val created_at: String,
    val updated_at: String
)

data class OrderPayloadDetails(
    val id: String,
    val date: String,
    val status: String,
    val observation: String?,
    val sequential: String,
    val waiter: String,
    val alert: Boolean,
    val pax: Int,
    val subtotal: String,
    val discount: String,
    val tip: String,
    val total: String,
    val user_id: String,
    val table_id: String,
    val alias: String?,
    val type: String,
    val subsidiary_id: String,
    val turn: String?,
    val table: TableDetails?,
    val subsidiary: SubsidiaryDetails?
)

data class TableDetails(
    val id: String,
    val name: String,
    val pax: Int,
    val status: String,
    val area_id: String,
    val joined: Boolean,
    val main_table_id: String?,
    val area: AreaDetails?
)

data class AreaDetails(
    val id: String,
    val name: String,
    val description: String?,
    val floor: Int,
    val subsidiary_id: String
)

data class SubsidiaryDetails(
    val id: String,
    val commercial_name: String,
    val code: String,
    val name: String,
    val address: String,
    val phone: String,
    val active: Boolean,
    val dispatch_inventory: Boolean,
    val differentiated_billing: Boolean,
    val apply_tip: Boolean,
    val email: String,
    val default_receipt_type: String?,
    val observation: String,
    val city_id: String?,
    val group: String,
    val print_logo: Boolean,
    val font_small: Boolean,
    val copies: Int,
    val print_observation: Boolean,
    val quotes_available_days: Int,
    val uses_kds: Boolean,
    val bussiness_type: String,
    val use_edocument: Boolean,
    val uses_turn: Boolean,
    val uses_customer_quota: Boolean,
    val uses_debt_expiration: Boolean,
    val default_price: String,
    val show_full_price: Boolean,
    val display_name: String,
    val has_open_cash_register: Boolean,
    val open_cash_register: OpenCashRegisterDetails?
)

data class OpenCashRegisterDetails(
    val id: String,
    val sequential: String,
    val active: Boolean,
    val opening: Double,
    val revenue: Double,
    val expenses: Double,
    val total_cash: Double,
    val sales_cash: Double,
    val received_cash: Double,
    val sales_total: Double,
    val difference: Double,
    val opened_at: String,
    val closed_at: String?,
    val observation: String?,
    val user_id: String,
    val checkout_id: String,
    val subsidiary_id: String,
    val checkout: CheckoutDetails?
)

data class CheckoutDetails(
    val id: String,
    val code: String,
    val name: String,
    val active: Boolean,
    val observation: String?,
    val sequential: String?,
    val subsidiary_id: String,
    val deleted_at: String?,
    val show_name: String
)

data class PrinterInfoAssociated(
    val id: String,
    val name: String,
    val code: String,
    val observation: String?,
    val pivot: PivotOrderPrinterItemAssociated
)

data class PivotOrderPrinterItemAssociated(
    val order_print_id: String,
    val printer_id: String,
    val sequential: String,
    val date: String,
    val created_at: String,
    val updated_at: String
)
