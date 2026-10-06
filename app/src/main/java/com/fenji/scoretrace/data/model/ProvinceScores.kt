package com.fenji.scoretrace.data.model

/**
 * 某省某一科类的录取控制线。
 *
 * @param benke 本科线（综合改革省份/老高考一本线也归入此字段）
 * @param tezhao 特殊类型招生控制线；老高考文理省份无此线时为 null
 */
data class ScoreLine(
    val benke: Int? = null,
    val tezhao: Int? = null,
)

/**
 * 一个省级行政区的高考录取控制线（数据源：assets/province_scores.json）。
 *
 * - 「3+1+2」与新高考文理省份：[physics]（物理类/理科）、[history]（历史类/文科）；
 * - 综合改革省份（京沪津浙鲁琼）：[unified]（单一本科线）。
 *
 * 三者按省份不同而择一/择二填充，其余为 null；[hasData] 为 false 表示该省无可用数据。
 */
data class ProvinceScore(
    val physics: ScoreLine? = null,
    val history: ScoreLine? = null,
    val unified: ScoreLine? = null,
    val note: String? = null,
) {
    val hasData: Boolean get() = physics != null || history != null || unified != null

    /** 按首选科目取对应科类分数线：物理→physics、历史→history，缺失时回退 unified。 */
    fun lineFor(primarySubject: String?): ScoreLine? = when (primarySubject) {
        "物理" -> physics ?: unified
        "历史" -> history ?: unified
        else -> unified ?: physics ?: history
    }

    /** 该省在给定首选科目下是否有可展示的分数线。 */
    fun hasLineFor(primarySubject: String?): Boolean = lineFor(primarySubject)?.benke != null
}

/** province_scores.json 的根结构。 */
data class ProvinceScoreData(
    val version: String,
    val source: String? = null,
    val data: Map<String, ProvinceScore> = emptyMap(),
)
