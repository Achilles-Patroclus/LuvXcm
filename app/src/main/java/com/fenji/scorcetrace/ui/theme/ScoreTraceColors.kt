package com.fenji.scorcetrace.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * ScoreTrace 统一设计色板。
 *
 * 与首页音乐播放器卡片既有语汇保持一致：
 * 深色卡片渐变 1E1E2E → 2D2D44、亮青强调 36D1DC、封面渐变 5B86E5 → 36D1DC。
 * 所有取值集中在此，方便统一调整。
 */
object ScoreTraceColors {

    // ── 品牌强调色
    val AccentCyan = Color(0xFF36D1DC)
    val AccentBlue = Color(0xFF5B86E5)
    val BrandBlue = Color(0xFF1565C0)

    // ── 页面底渐变（比卡片再深一档，让卡片「浮」起来）
    val PageGradientDark = listOf(Color(0xFF12121C), Color(0xFF1D1D2B))
    val PageGradientLight = listOf(Color(0xFFF6F8FC), Color(0xFFE9EDF5))

    // ── 卡片面渐变（深色直接沿用音乐卡既有值）
    val CardGradientDark = listOf(Color(0xFF1E1E2E), Color(0xFF2D2D44))
    val CardGradientLight = listOf(Color(0xFFFFFFFF), Color(0xFFF2F5FB))

    // 倒计时卡片：更亮的蓝紫过渡，呼应封面渐变的冷暖对比
    val HighlightGradientDark = listOf(Color(0xFF2A2A46), Color(0xFF1B1B2A))

    // ── 封面 / 手绘插画渐变（沿用既有封面）
    val CoverGradient = listOf(AccentBlue, AccentCyan)

    // ── 深色卡片内的文字层级（alpha 分级做层级，避免无谓的色相跳变）
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFFFFFFF).copy(alpha = 0.70f)
    val TextTertiary = Color(0xFFFFFFFF).copy(alpha = 0.50f)

    // 浅色页面上的弱化文字
    val TextTertiaryLight = Color(0xFF44474F).copy(alpha = 0.75f)

    // ── 描边
    val CardBorder = Color(0xFFFFFFFF).copy(alpha = 0.20f)
    val CardBorderLight = BrandBlue.copy(alpha = 0.10f)
    val HairlineBorder = Color(0xFFFFFFFF).copy(alpha = 0.12f)
    val HairlineBorderLight = Color(0xFF1565C0).copy(alpha = 0.14f)

    // ── 语义色
    val SuccessDark = Color(0xFF4ADE80)
    val SuccessLight = Color(0xFF2E7D32)
    val WarningDark = Color(0xFFFBBF24)
    val WarningLight = Color(0xFFEF6C00)
    val ErrorSoft = Color(0xFFFF8A80)
    val ErrorDeep = Color(0xFFBA1A1A)

    // ── 目标院校卡片强调色：暖橙。作为次级强调色，与倒计时卡的冷青/蓝紫拉开区分，
    //    避免首页两张大卡抢焦点；深/浅主题下都保证 ≥3:1 的图标对比度。
    val AccentAmber = Color(0xFFE0912F)
    val AccentAmberDeep = Color(0xFFB26A00)

    // ── 强调色上的前景色（保证对比度）
    val OnAccentDeep = Color(0xFF0B1B23)
    val OnLightDeep = Color(0xFF16161F)

    /** 带浅色背景的圆角方形图标底：主色 12%~18% alpha。 */
    fun iconContainer(accent: Color = BrandBlue, alpha: Float = 0.14f): Color =
        accent.copy(alpha = alpha)

    /** 页面底渐变笔刷。 */
    fun pageBrush(dark: Boolean): Brush =
        Brush.verticalGradient(if (dark) PageGradientDark else PageGradientLight)

    /** 卡片面渐变笔刷。 */
    fun cardBrush(dark: Boolean): Brush =
        Brush.linearGradient(if (dark) CardGradientDark else CardGradientLight)

    /** 封面 / 插画渐变笔刷。 */
    fun coverBrush(): Brush = Brush.linearGradient(CoverGradient)
}
