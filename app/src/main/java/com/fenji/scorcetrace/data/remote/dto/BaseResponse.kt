package com.fenji.scorcetrace.data.remote.dto

import com.google.gson.annotations.SerializedName

/** 统一响应包装，字段名按后端约定调整。 */
data class BaseResponse<T>(
    @SerializedName("code") val code: Int = 0,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
) {
    val isSuccess: Boolean get() = code == 0
}
