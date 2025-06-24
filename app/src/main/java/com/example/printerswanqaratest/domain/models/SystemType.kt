package com.example.printerswanqaratest.domain.models

class SystemType (
    private var id: Int = 1,
    private var name: String,
    private var isRestaurant: Boolean,
) {
    fun getName(): String {
        return this.name
    }
    fun getIsRestaurant(): Boolean {
        return this.isRestaurant
    }
}