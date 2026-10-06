package com.fenji.scoretrace

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fenji.scoretrace.data.local.UserPreferences
import com.fenji.scoretrace.ui.navigation.AppNavHost
import com.fenji.scoretrace.ui.theme.ScoreTraceTheme
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.LocationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        lifecycleScope.launch { resolveProvince(granted) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppLogger.i("AppStart", "MainActivity onCreate")
        setContent {
            ScoreTraceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost()
                }
            }
        }
        autoDetectProvinceOnFirstLaunch()
    }

    /**
     * 首次启动（且用户从未设置过省份）时申请粗定位权限并定位一次，写入省份偏好。
     * 用 [UserPreferences.provinceLocated] 标记保证只询问一次；拒绝或失败时保持默认「云南」。
     */
    private fun autoDetectProvinceOnFirstLaunch() {
        lifecycleScope.launch {
            if (userPreferences.provinceLocated.first()) return@launch
            if (userPreferences.hasExplicitProvince()) {
                userPreferences.setProvinceLocated(true)
                return@launch
            }
            if (hasCoarseLocationPermission()) {
                resolveProvince(granted = true)
            } else {
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    private suspend fun resolveProvince(granted: Boolean) {
        if (granted) {
            LocationHelper.getCurrentProvince(this)?.let { userPreferences.setProvince(it) }
        }
        // 无论授权、拒绝还是定位失败都标记完成，避免下次启动重复打扰
        userPreferences.setProvinceLocated(true)
    }

    private fun hasCoarseLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
