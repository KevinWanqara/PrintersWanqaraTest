package com.example.printerswanqaratest.domain.models

data class BluetoothDomain(
    private val name: String?,
    private val address: String
){
    fun getAddress(): String {
        return this.address
    }
    fun getName(): String? {
        return this.name
    }
}