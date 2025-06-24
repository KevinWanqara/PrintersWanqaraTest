package com.example.printerswanqaratest.data.model.reponse

import com.example.printerswanqaratest.data.model.FaQNetworkModel
import com.google.gson.annotations.SerializedName

data class FaQApiResponse(
    @SerializedName("data") private val faQNetworkModel: List<FaQNetworkModel>
) {
    fun getFaQNetworkModel(): List<FaQNetworkModel> {
        return this.faQNetworkModel
    }
}
