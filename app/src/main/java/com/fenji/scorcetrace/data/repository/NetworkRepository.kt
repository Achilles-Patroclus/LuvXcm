package com.fenji.scorcetrace.data.repository

import com.fenji.scorcetrace.data.remote.ApiService
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkRepository {
    /** 拉取本机公网 IP；失败（无网络 / 超时 / 接口不可达）时返回 null */
    suspend fun fetchUserIp(): String?
}

@Singleton
class DefaultNetworkRepository @Inject constructor(
    private val apiService: ApiService,
) : NetworkRepository {

    override suspend fun fetchUserIp(): String? = runCatching {
        apiService.getUserIp(IPIFY_URL).ip
    }.getOrNull()?.takeIf { it.isNotBlank() }

    private companion object {
        /** 完整地址：@Url 传绝对 URL，不走 Retrofit 的 baseUrl */
        const val IPIFY_URL = "https://api.ipify.org?format=json"
    }
}
