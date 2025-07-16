package com.example.printerswanqara.api.order

import com.example.printerswanqara.api.sales.Detail
import com.example.printerswanqara.api.sales.Subsidiary
import com.example.printerswanqara.api.sales.Tax


data class OrderApiResponse(
    val data: Order
)


data class Order (
    val  id : String,
    val date : String,
    val details: List<Detail>,
    val discount : String,
    val observation : String ?= null,
    val pax : String,
    val sequential : String,
    val status : String,
    val subsidiary_id : String,
    val subsidiary : Subsidiary,
    val table : Table,
    val table_id : String,
    val tickets : List<Ticket>,
    val total : String,
    val subtotal : String,
    val waiter : String,
    val type : String,
    val tip : String,
    val turn : String,
    val alias : String,
    val taxes : List<Tax>? = null,





    )


data class Table (
    val id: String,
    val area : Area,
    val area_id : String,
    val joined : Boolean,
    val main_table_id : String? = null,
    val name: String,
    val pax : Int,
    val status : String,


)

data class Area (
    val id: String,
    val name: String,
    val subsidiary_id : String,
    val floor : Int,
    val description: String? = null,
)


data class Ticket (
    val id: String,
    val cancel : Boolean,
    val details : List<Detail>,
    val end : String? = null,
    val is_urgent : Boolean,
    val modified : Boolean,
    val observation : String? = null,
    val order_id : String,
    val reprint : Boolean,
    val start : String,
    val status : String,
    val subsidiary_id : String,
    val user_id : String,

)