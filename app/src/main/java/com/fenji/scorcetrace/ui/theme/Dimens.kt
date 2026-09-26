package com.fenji.scorcetrace.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 统一间距 / 圆角 / 尺寸规范。
 * 间距按 4 / 8 / 12 / 16 / 20 / 24 的节奏递进。
 */
object Dimens {

    // ── 页面与区块
    /** 页面左右外边距 */
    val PageHorizontal = 20.dp
    /** 卡片之间的间距 */
    val CardGap = 16.dp
    /** 分组 / 区块之间的间距 */
    val SectionGap = 24.dp
    /** 列表项之间的间距 */
    val ItemGap = 12.dp
    /** 紧凑间距 */
    val TightGap = 8.dp
    /** 列表底部留白（避免被底部导航遮挡） */
    val ContentBottom = 24.dp

    // ── 圆角
    /** 主卡片圆角 */
    val CardCorner = 28.dp
    /** 次级元素圆角（数字块、封面、图标底） */
    val SubCorner = 16.dp
    /** 列表卡圆角（略小于主卡片，避免密集列表过于「药丸化」） */
    val ListCorner = 24.dp
    /** 小元素圆角（小图标底） */
    val SmallCorner = 12.dp

    // ── 内边距
    /** 卡片内边距 */
    val CardPadding = 18.dp

    // ── 尺寸
    /** 设置项图标底尺寸 */
    val IconBox = 40.dp
    /** 无障碍最小触摸热区 */
    val MinTouchTarget = 48.dp
}
