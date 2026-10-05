package com.fenji.scoretrace.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * 通过系统 [LocationManager] + [Geocoder] 获取当前省份（不依赖 Google Play 服务）。
 *
 * 策略：先取各 provider 的最新「最后已知位置」，没有则请求一次单点定位；全程 5 秒超时。
 * 任何失败（无权限 / provider 关闭 / 无网络 / 反查失败）都返回 null，由调用方回退默认省份。
 * 返回结果已归一化为省级简称（云南省 → 云南）。
 */
object LocationHelper {

    private const val TIMEOUT_MS = 5_000L

    private val PROVIDERS = listOf(
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )

    /** 全国 34 个省级行政区简称，用于把 Geocoder 返回的全称归一化。 */
    private val PROVINCES = listOf(
        "北京", "天津", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江",
        "上海", "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南",
        "湖北", "湖南", "广东", "广西", "海南", "重庆", "四川", "贵州",
        "云南", "西藏", "陕西", "甘肃", "青海", "宁夏", "新疆",
        "香港", "澳门", "台湾",
    )

    suspend fun getCurrentProvince(context: Context): String? = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return@withContext null
        val location = lastKnownLocation(manager) ?: requestSingleLocation(context, manager)
            ?: return@withContext null
        val adminArea = reverseGeocode(context, location.latitude, location.longitude)
            ?: return@withContext null
        normalizeProvince(adminArea)
    }

    private fun lastKnownLocation(manager: LocationManager): Location? =
        PROVIDERS.asSequence()
            .filter { providerEnabled(manager, it) }
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }

    private fun providerEnabled(manager: LocationManager, provider: String): Boolean =
        runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)

    private suspend fun requestSingleLocation(context: Context, manager: LocationManager): Location? {
        val provider = PROVIDERS.firstOrNull { providerEnabled(manager, it) } ?: return null
        return withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        runCatching { manager.removeUpdates(this) }
                        if (continuation.isActive) continuation.resume(location)
                    }
                }
                val accepted = runCatching {
                    manager.requestLocationUpdates(
                        provider,
                        0L,
                        0f,
                        ContextCompat.getMainExecutor(context),
                        listener,
                    )
                    true
                }.getOrDefault(false)
                if (!accepted) {
                    if (continuation.isActive) continuation.resume(null)
                    return@suspendCancellableCoroutine
                }
                continuation.invokeOnCancellation { runCatching { manager.removeUpdates(listener) } }
            }
        }
    }

    private suspend fun reverseGeocode(context: Context, latitude: Double, longitude: Double): String? =
        withTimeoutOrNull(TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val geocoder = Geocoder(context, Locale.getDefault())
                val listener = object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses.firstOrNull()?.adminArea)
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
                val accepted = runCatching {
                    geocoder.getFromLocation(latitude, longitude, 1, listener)
                    true
                }.getOrDefault(false)
                if (!accepted && continuation.isActive) continuation.resume(null)
            }
        }

    /** 归一化省级名称：云南省 → 云南、内蒙古自治区 → 内蒙古、北京市 → 北京。 */
    fun normalizeProvince(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        return PROVINCES.firstOrNull { value.startsWith(it) }
    }
}
