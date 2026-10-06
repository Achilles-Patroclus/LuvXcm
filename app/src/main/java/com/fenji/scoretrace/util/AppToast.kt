package com.fenji.scoretrace.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 轻提示的四种语义类型。 */
enum class AppToastType { Success, Error, Warning, Info }

/** 带类型的一次提示事件，供 ViewModel 经 Flow 上报，由页面转交 [AppToast]。 */
data class ToastMessage(val text: String, val type: AppToastType)

/** 一条轻提示。[leaving] 为 true 时正在播放退出动画，动画结束后由 [AppToast] 出队。 */
class ToastItem internal constructor(
    val id: Long,
    val text: String,
    val type: AppToastType,
) {
    var leaving by mutableStateOf(false)
        internal set
}

/**
 * 全局轻提示（自绘，零依赖）。
 *
 * 用法——任何地方一行调用，无需传递 HostState：
 *   AppToast.success("已保存")
 *   AppToast.error("网络异常，请重试")
 *   AppToast.warning("定位权限被拒绝")
 *   AppToast.info("已复制到剪贴板")
 *
 * 状态通过 [visible] 暴露，交由界面层的 `AppToastHost` 渲染成底部居中、自下而上堆叠的卡片。
 * 同时最多展示 [MAX_VISIBLE] 条，超出的排队等待，前面的关闭后自动补位。
 */
object AppToast {

    private const val MAX_VISIBLE = 3
    private const val EXIT_MILLIS = 220L

    private val _visible = mutableStateListOf<ToastItem>()

    /** 当前正在展示（含正在退出）的提示，供 `AppToastHost` 观察。 */
    val visible: List<ToastItem> get() = _visible

    private val pending = ArrayDeque<ToastItem>()
    private var nextId = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun show(message: CharSequence, type: AppToastType = AppToastType.Info) {
        val item = ToastItem(nextId++, message.toString(), type)
        if (_visible.size < MAX_VISIBLE) showNow(item) else pending.addLast(item)
    }

    fun show(event: ToastMessage) = show(event.text, event.type)

    fun success(message: CharSequence) = show(message, AppToastType.Success)

    fun error(message: CharSequence) = show(message, AppToastType.Error)

    fun warning(message: CharSequence) = show(message, AppToastType.Warning)

    fun info(message: CharSequence) = show(message, AppToastType.Info)

    fun dismiss(id: Long) {
        val item = _visible.firstOrNull { it.id == id } ?: return
        if (item.leaving) return
        item.leaving = true
        scope.launch {
            delay(EXIT_MILLIS)
            _visible.remove(item)
            pending.removeFirstOrNull()?.let { showNow(it) }
        }
    }

    private fun showNow(item: ToastItem) {
        _visible.add(item)
        scope.launch {
            delay(durationMillis(item.type))
            dismiss(item.id)
        }
    }

    private fun durationMillis(type: AppToastType): Long = when (type) {
        AppToastType.Error, AppToastType.Warning -> 3200L
        AppToastType.Success, AppToastType.Info -> 2200L
    }
}
