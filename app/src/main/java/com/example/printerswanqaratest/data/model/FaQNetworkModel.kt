package com.example.printerswanqaratest.data.model

import com.example.printerswanqaratest.domain.models.FaQ
import com.google.gson.annotations.SerializedName

data class FaQNetworkModel(
    @SerializedName("id") private val id: Int,
    @SerializedName("question") private val question: String,
    @SerializedName("answer") private val answer: String,
    @SerializedName("url") private val url: String?
) {
    fun createFaQFromFaQNetworkModel(): FaQ {
        return FaQ(
            this.id,
            this.question,
            this.answer,
            this.url
        )
    }
}