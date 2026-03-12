package com.printerswanqara.api.paymentaccount

import com.printerswanqara.api.UserDetails
import com.printerswanqara.api.quotes.Person
import com.printerswanqara.api.sales.Detail
import com.printerswanqara.api.sales.OrderPrints
import com.printerswanqara.api.sales.PaymentMethod
import com.printerswanqara.api.sales.Subsidiary
import com.printerswanqara.api.sales.Tax


data class PaymentAccountApiResponse(
    val data: PaymentAccount
)




data class PaymentAccount(
    val id: String,
    val sequential: String,
    val number :String,
    val user_id: String,
    val user: UserDetails?,
    val interest: String? = null,
    val amount: String,
    val balance: String,
    val type: String,
    val movement_type: String,
    val supported: Boolean,
    val closed: Boolean,
    val expiration_date: String,
    val person_id: String,
    val person: Person?,
    val is_detail: Boolean,
    val payment_accountable: PaymentAccountable,
    val payments: List<PaymentAccountPayments>,
    val observation: String? = null,
    val status: String,
    val reference: String,
    val is_advance: Boolean,
    val is_canceled: Boolean,
    val payed_amount: Double,
    val payment_account_details: List<PaymentDetails>? = null
)

data class PaymentDetails(
    val id: String,
    val payment_account_id: String,
    val paymentable_id: String,
    val paymentable_type: String,
    val model: String,
    val amount: Double,
    val account_amount: Double,
    val new_amount: Double,
    val date: String,
    val observation: String? = null,
    val user_id: String,
    val user: UserDetails? = null,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String? = null,
    val paymentable: Paymentable? = null,
    val is_canceled: Boolean,
    val type: String? = null
)

data class Paymentable(
    val id: String,
    val date: String,
    val amount: String? = null,
    val amount_available: String? = null,
    val account_type: String? = null,
    val active: Boolean? = null,
    val assumed_payment: Boolean? = null,
    val auto_consumed: Boolean? = null,
    val balance: String? = null,
    val cash_register_id: String? = null,
    val checkout_id: String? = null,
    val expiration_date: String? = null,
    val financial_institution_id: String? = null,
    val is_previous: Boolean? = null,
    val observation: String? = null,
    val origin_account: String? = null,
    val payment_entity: String? = null,
    val payment_id: String? = null,
    val payment_method_id: String? = null,
    val payment_method_type: String? = null,
    val payment_method_name: String? = null,
    val paymentable: Paymentable? = null,
    val paymentable_name: String? = null,
    val person_id: String? = null,
    val person: Person? = null,
    val reference: String? = null,
    val user_id: String? = null,
    val user: UserDetails? = null,
    val description: String? = null
)

data class PaymentAccountable(
    val id: String,
    val date: String,
    val user_id: String,
    val customer_id: String,
    val subtotal: Double,
    val discount: Double,
    val total: Double,
    val tip: Double,
    val delivered: Boolean,
    val document_type: String,
    val support_code: String? = null,
    val sequential: String,
    val number: String,
    val complete: Boolean,
    val access_key: String,
    val additional_information: String? = null,
    val observation: String? = null,
    val shipping_guide: String? = null,
    val payment_methods: List<PaymentMethod>,
    val is_canceled: Boolean,
    val received_payment: String,
    val sri_environment: String,
    val is_debt: Boolean,
    val debt_amount: Double,
    val checkout_id: String,
    val subsidiary_id: String,
    val warehouse_id: String,
    val order_id: String? = null,
    val change_amount: Double? = null,
    val description: String? = null
)

data class PaymentAccountPayments(
    val id_detail: String,
    val date: String,
    val amount: String,
    val payment_method_type: String,
    val paymentable_name: String,
    val reference: String? = null
)