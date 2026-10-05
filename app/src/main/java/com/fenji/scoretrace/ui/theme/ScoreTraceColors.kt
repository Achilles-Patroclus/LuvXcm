package com.fenji.scoretrace.ui.theme

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

    /** 浅色页面上的青蓝强调（文字 / 图标）：压深以保证 ≥4.5:1 对比度 */
    val AccentCyanOnLight = Color(0xFF00707C)

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
    val TextTertiaryLight = Color(0xFF9CA3AF)

    // ── 描边
    val CardBorder = Color(0xFFFFFFFF).copy(alpha = 0.20f)
    val CardBorderLight = Color(0xFFE5E7EB)
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

    /** 当前分低于目标分时的警示色（橙，比纯红柔和；用于目标院校卡分数） */
    val ScoreWarn = Color(0xFFFF9800)

    // ── 浅色主题（设计稿 v1）
    /** 页面背景：浅灰蓝，比纯白多一层呼吸感 */
    val PageBackgroundLight = Color(0xFFEEF1F8)

    /** 卡片背景：纯白 */
    val CardBackgroundLight = Color(0xFFFFFFFF)

    /** 品牌主色（设计稿青蓝） */
    val BrandPrimary = Color(0xFF3B82F6)
    val BrandPrimaryLight = Color(0xFF60A5FA)
    val BrandPrimaryDark = Color(0xFF2563EB)

    /** 品牌渐变（倒计时卡 / 音乐胶囊用） */
    val BrandGradient = listOf(Color(0xFF3B82F6), Color(0xFF5BB8E8))

    /** AI 品牌渐变（蓝 → 青绿）：AI 图标容器与发送按钮专用 */
    val AiGradient = listOf(Color(0xFF4A90E2), Color(0xFF2ECC71))

    /** 目标院校紫色 */
    val SchoolPurple = Color(0xFF7C5CFC)
    val SchoolPurpleLight = Color(0xFF9B7FFF)

    /** 文字层级（浅色主题） */
    val TextPrimaryLight = Color(0xFF1A1A2E)
    val TextSecondaryLight = Color(0xFF6B7280)

    /** 语义色（浅色） */
    val SuccessGreen = Color(0xFF10B981)
    val WarningOrange = Color(0xFFF59E0B)
    val ErrorRed = Color(0xFFEF4444)

    /** 六科进度条配色中的专属色（语数用品牌蓝、英语用 SuccessGreen、化学用 WarningOrange） */
    val SubjectPhysicsGreen = Color(0xFF059669)
    val SubjectBiologyTeal = Color(0xFF14B8A6)

    /** 快捷功能四色 */
    val QuickActionBlue = Color(0xFF3B82F6)
    val QuickActionGreen = Color(0xFF10B981)
    val QuickActionOrange = Color(0xFFF59E0B)
    val QuickActionPurple = Color(0xFF8B5CF6)

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
