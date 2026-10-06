package com.fenji.scoretrace.util

/** 固定展示顺序的「精英层次标签」；C9 / 国防七子 不参与展示。 */
private val LEVEL_TAG_ORDER = listOf("985", "211", "双一流")

/**
 * 首页目标院校卡展示的层次标签：优先展示 985 / 211 / 双一流；都没有时按办学层次回退为
 * 「本科」/「专科」。已显示精英标签时不再重复显示「本科」，避免冗余。
 */
fun homeSchoolTags(level: String, tags: List<String>): List<String> {
    val elite = LEVEL_TAG_ORDER.filter { it in tags }
    return elite.ifEmpty { listOf(if (level.startsWith("专科")) "专科" else "本科") }
}

/** 院校选择列表行展示的层次标签：剔除 C9 / 国防七子后按优先级排序，最多 4 项、超出以 +N 收尾。 */
fun pickerSchoolTags(tags: List<String>): List<String> {
    val sorted = tags
        .filter { it != "C9" && it != "国防七子" }
        .distinct()
        .sortedBy { tag -> LEVEL_TAG_ORDER.indexOf(tag).let { if (it < 0) LEVEL_TAG_ORDER.size else it } }
    return if (sorted.size <= 4) sorted else sorted.take(3) + "+${sorted.size - 3}"
}
