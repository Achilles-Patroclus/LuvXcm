package com.fenji.scoretrace.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 轻提示的四种语义类型。 */
enum class AppToastType { Success, Error, Warning, Info }

/** 带类型的一次提示事件，供 ViewModel 经 Flow 上报，由页面转交 [AppToast]。 */
data class ToastMessage(val text: String, val type: AppToastType)

/**
 * 全局轻提示（基于 Material3 Snackbar）。
 *
 * 用法：
 *   AppToast.success("已复制")
 *   AppToast.error("网络异常，请重试")
 *   AppToast.warning("暂无可导出的成绩")
 *   AppToast.info("再按一次退出应用")
 *
 * 由 AppNavHost 在根 Scaffold 提供 [SnackbarHostState]，并通过 [attach] 注入。
 * 文案前会加一个类型前缀（✓ / ✕ / ⚠ / ℹ），由 SnackbarHost 的自定义样式渲染成对应图标与配色。
 */
object AppToast {

    private var hostState: SnackbarHostState? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun attach(state: SnackbarHostState) {
        hostState = state
    }

    fun show(
        message: CharSequence,
        type: AppToastType = AppToastType.Info,
        duration: SnackbarDuration = SnackbarDuration.Short,
    ) {
        val host = hostState ?: return
        scope.launch { host.showSnackbar("${prefixOf(type)} $message", duration = duration) }
    }

    fun show(event: ToastMessage) = show(event.text, event.type)

    fun success(message: CharSequence) = show(message, AppToastType.Success)

    fun error(message: CharSequence) = show(message, AppToastType.Error)

    fun warning(message: CharSequence) = show(message, AppToastType.Warning)

    fun info(message: CharSequence) = show(message, AppToastType.Info)

    private fun prefixOf(type: AppToastType): String = when (type) {
        AppToastType.Success -> SUCCESS_PREFIX
        AppToastType.Error -> ERROR_PREFIX
        AppToastType.Warning -> WARNING_PREFIX
        AppToastType.Info -> INFO_PREFIX
    }

    const val SUCCESS_PREFIX = "✓"
    const val ERROR_PREFIX = "✕"
    const val WARNING_PREFIX = "⚠"
    const val INFO_PREFIX = "ℹ"
}
