package com.example.printerswanqara.api.quotes


import com.example.printerswanqara.api.UserDetails
import com.example.printerswanqara.api.sales.Address

import com.example.printerswanqara.api.sales.Detail

import com.example.printerswanqara.api.sales.IdentityCode
import com.example.printerswanqara.api.sales.Sales

import com.example.printerswanqara.api.sales.Subsidiary

import com.example.printerswanqara.api.sales.Tax
import com.example.printerswanqara.api.sales.Warehouse


data class QuotesApiResponse(
    val data: Quotes?,
    val message: String? = null
)

data class Quotes(
    val id: String,
    val date: String,
    val expiration_date : String,
    val observation: String,
    val subsidiary_id: String,
    val subsidiary: Subsidiary,
    val checkout_id: String,
    val user: UserDetails?,
    val user_id: String,
    val sequential: String,
    val number: String,
    val discount: Double,
    val subtotal: Double,
    val total: Double,
    val details: List<Detail>,
    val Person: Person?,
    val taxes: List<Tax>,
    val sale_id: String?,
    val sale : Sales?,
    val created_at: String?,
    val payment_terms : String? = null,

    )


data class Person(
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