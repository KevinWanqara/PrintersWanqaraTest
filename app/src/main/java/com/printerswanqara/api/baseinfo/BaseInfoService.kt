package com.printerswanqara.api.baseinfo

import com.printerswanqara.api.Setting
import retrofit2.http.GET


interface BaseInfoService {
    @GET("general/settings/base-info")
    suspend fun getBaseInfo():   BaseInfoResponse
}



data class BaseInfoResponse(
    val data: Setting
)