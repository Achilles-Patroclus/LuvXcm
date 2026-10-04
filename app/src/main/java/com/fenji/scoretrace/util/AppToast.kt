package com.fenji.scoretrace.util

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 全局轻提示（基于 Material3 Snackbar，取代旧的自定义 Toast）。
 *
 * 用法：
 *   AppToast.success("已复制")
 *   AppToast.error("网络异常，请重试")
 *   AppToast.info("再按一次退出应用")
 *
 * 由 AppNavHost 在根 Scaffold 提供 [SnackbarHostState]，并通过 [attach] 注入。
 * success / error 会在文案前加 ✓ / ✕ 前缀，由 SnackbarHost 的自定义样式渲染成对应图标。
 */
object AppToast {

    private var hostState: SnackbarHostState? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun attach(state: SnackbarHostState) {
        hostState = state
    }

    fun show(message: CharSequence, duration: SnackbarDuration = SnackbarDuration.Short) {
        val host = hostState ?: return
        scope.launch { host.showSnackbar(message.toString(), duration = duration) }
    }

    fun success(message: CharSequence) = show("$SUCCESS_PREFIX $message")

    fun error(message: CharSequence) = show("$ERROR_PREFIX $message")

    fun info(message: CharSequence) = show(message.toString())

    fun warning(message: CharSequence) = show(message.toString())

    const val SUCCESS_PREFIX = "✓"
    const val ERROR_PREFIX = "✕"
}
