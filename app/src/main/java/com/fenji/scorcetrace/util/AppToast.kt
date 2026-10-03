package com.fenji.scorcetrace.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.fenji.scorcetrace.R

/**
 * 全局自定义 Toast。
 *
 * 用法：
 *   AppToast.show(context, "保存成功")
 *   AppToast.success(context, "已添加到收藏")
 *   AppToast.error(context, "网络异常，请重试")
 *   AppToast.warning(context, "即将清除全部数据")
 *   AppToast.info(context, "已切换到深色主题")
 */
object AppToast {

    enum class Type(@DrawableRes val iconRes: Int) {
        INFO(R.drawable.ic_toast_info),
        SUCCESS(R.drawable.ic_toast_success),
        ERROR(R.drawable.ic_toast_error),
        WARNING(R.drawable.ic_toast_warning),
        DEFAULT(0), // 无图标
    }

    // 缓存当前 Toast，避免连续点击时多个 Toast 排队
    private var currentToast: Toast? = null

    fun show(
        context: Context,
        message: CharSequence,
        type: Type = Type.DEFAULT,
        duration: Int = Toast.LENGTH_SHORT,
    ) {
        // 取消上一个 Toast，避免叠加
        currentToast?.cancel()

        val toast = Toast(context.applicationContext)
        toast.duration = duration

        // 自定义布局
        val view = LayoutInflater.from(context).inflate(R.layout.toast_custom, null)
        val iconView = view.findViewById<ImageView>(R.id.toastIcon)
        val textView = view.findViewById<TextView>(R.id.toastText)

        textView.text = message

        if (type.iconRes != 0) {
            iconView.visibility = View.VISIBLE
            iconView.setImageResource(type.iconRes)
        } else {
            // 无图标时去掉与图标间距，避免左侧留白比右侧宽
            iconView.visibility = View.GONE
            (textView.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
        }

        toast.view = view

        // 位置：底部上方 80dp，水平居中
        val density = context.resources.displayMetrics.density
        toast.setGravity(
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            0,
            (80 * density).toInt(),
        )

        currentToast = toast
        toast.show()
    }

    // ── 便捷方法 ──

    fun info(context: Context, message: CharSequence) =
        show(context, message, Type.INFO)

    fun success(context: Context, message: CharSequence) =
        show(context, message, Type.SUCCESS)

    fun error(context: Context, message: CharSequence) =
        show(context, message, Type.ERROR)

    fun warning(context: Context, message: CharSequence) =
        show(context, message, Type.WARNING)

    // ── StringRes 版本 ──

    fun show(context: Context, @StringRes resId: Int, type: Type = Type.DEFAULT) =
        show(context, context.getText(resId), type)

    fun info(context: Context, @StringRes resId: Int) =
        show(context, resId, Type.INFO)

    fun success(context: Context, @StringRes resId: Int) =
        show(context, resId, Type.SUCCESS)

    fun error(context: Context, @StringRes resId: Int) =
        show(context, resId, Type.ERROR)

    fun warning(context: Context, @StringRes resId: Int) =
        show(context, resId, Type.WARNING)
}
