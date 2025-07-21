package com.example.printerswanqara.api.cashRegister

import com.example.printerswanqara.api.UserData
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
    val difference : Double ,
    val expenses : Double ,
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
    val user : UserData,
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


data class Summary (
    val debt_amount : Double,
    val expenses : Double,
    val payment_methods : List<PaymentMethod>,
    val revenue : Double,

    val sales_by_product : List<SalesByProduct>,
    val sales_cash : Double,
    val sales_total : Double,
    val total_cash : Double,
)


data class SalesByProduct(
    val product_id : String,
    val product_name : String,
    val total_discount : Double,
    val total_quantity : Double,
    val total_sold : Double,
    val total_sold_with_taxes : Double,

)