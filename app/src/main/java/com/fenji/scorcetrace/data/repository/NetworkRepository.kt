package com.fenji.scorcetrace.data.repository

import com.fenji.scorcetrace.data.remote.ApiService
import kotlinx.coroutines.delay
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

    override suspend fun fetchUserIp(): String? {
        repeat(MAX_ATTEMPTS) { attempt ->
            val ip = runCatching { apiService.getUserIp(IPIFY_URL).ip }.getOrNull()
            if (!ip.isNullOrBlank()) return ip
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
        }
        return null
    }

    private companion object {
        /** 完整地址：@Url 传绝对 URL，不走 Retrofit 的 baseUrl */
        const val IPIFY_URL = "https://api.ipify.org?format=json"

        /** 网络抖动/瞬时超时时最多再试一次 */
        const val MAX_ATTEMPTS = 2
        const val RETRY_DELAY_MS = 300L
    }
}
