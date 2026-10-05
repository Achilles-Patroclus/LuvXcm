package com.fenji.scoretrace.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 监听当前网络是否可用：存在「已校验通过的公网连接」时发射 true，否则 false。
 *
 * 用 [ConnectivityManager.registerDefaultNetworkCallback] 感知网络变化，覆盖
 * 连接建立/断开/能力变化三种时机。仅依赖 Android 框架，无需额外依赖。
 */
fun Context.observeOnlineStatus(): Flow<Boolean> = callbackFlow {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun hasValidatedInternet(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            trySend(hasValidatedInternet())
        }

        override fun onLost(network: Network) {
            trySend(hasValidatedInternet())
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            trySend(hasValidatedInternet())
        }
    }

    trySend(hasValidatedInternet())
    connectivityManager.registerDefaultNetworkCallback(callback)
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
