package com.fenji.scorcetrace.util

import java.util.Calendar

object Constants {

    const val DATABASE_NAME = "score_trace.db"

    /** 满分缺省值 */
    const val DEFAULT_FULL_SCORE = 150.0

    /** 高考固定日期（月/日），倒计时默认目标；具体年份由 DateUtils.defaultGaokaoTimestamp 推导 */
    const val GAOKAO_MONTH = Calendar.JUNE
    const val GAOKAO_DAY = 7

    /** 首页展示的最近成绩条数 */
    const val HOME_RECENT_SCORE_LIMIT = 3

    /** 首页音乐播放器的默认音源 */
    const val DEFAULT_MUSIC_URL = "https://www.lequxiang.com.cn/view.php/80e6affab3677c4e9264648b4cb150b2.mp3"
    const val DEFAULT_MUSIC_TITLE = "Whisper Of Hope"
    const val DEFAULT_MUSIC_ARTIST = "备考轻音乐"

    const val DATE_PATTERN = "yyyy-MM-dd"
    const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"

    /** 数据库首次创建时写入的默认科目 */
    val DEFAULT_SUBJECT_NAMES = listOf(
        "语文", "数学", "英语",
        "物理", "化学", "生物",
        "政治", "历史", "地理",
    )

    /**
     * 新高考「3+1+2」科目分组。
     *
     * - 3：必考科目（[REQUIRED_SUBJECT_NAMES]）
     * - 1：首选科目，物理/历史二选一（[PRIMARY_SUBJECT_NAMES]）
     * - 2：再选科目，四选二（[SECONDARY_SUBJECT_NAMES]）
     *
     * 与 [DEFAULT_SUBJECT_NAMES] 是同一批 9 个科目的不同视角：后者用于建库播种（扁平全量），
     * 前者用于表单分组选择。二者必须保持名称逐字一致。
     */

    /** 3：必考科目 */
    val REQUIRED_SUBJECT_NAMES = listOf("语文", "数学", "英语")

    /** 1：首选科目（二选一） */
    val PRIMARY_SUBJECT_NAMES = listOf("物理", "历史")

    /** 2：再选科目（四选二） */
    val SECONDARY_SUBJECT_NAMES = listOf("化学", "生物", "政治", "地理")

    /** 科目可选配色（ARGB，以 Long 存库） */
    val SUBJECT_COLORS = listOf(
        0xFF1565C0L, 0xFFC62828L, 0xFF2E7D32L,
        0xFF6A1B9AL, 0xFFEF6C00L, 0xFF00838FL,
        0xFF4E342EL, 0xFFAD1457L, 0xFF37474FL,
    )
}
