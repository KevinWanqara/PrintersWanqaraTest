package com.example.printerswanqara.data.model.reponse

import com.example.printerswanqara.data.model.FaQNetworkModel
import com.google.gson.annotations.SerializedName

data class FaQApiResponse(
    @SerializedName("data") private val faQNetworkModel: List<FaQNetworkModel>
) {
    fun getFaQNetworkModel(): List<FaQNetworkModel> {
        return this.faQNetworkModel
    }
}
