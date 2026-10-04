package com.fenji.scoretrace.data.remote.dto

import com.google.gson.annotations.SerializedName

/** ipify 的返回体：{"ip":"1.2.3.4"} */
data class IpResponse(
    @SerializedName("ip") val ip: String,
)
