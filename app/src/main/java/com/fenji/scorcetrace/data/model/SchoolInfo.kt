package com.fenji.scorcetrace.data.model

/** 单所院校的静态信息与录取分数（数据源：assets/schools.json）。 */
data class SchoolInfo(
    val id: String,
    val name: String,
    val province: String,
    val city: String,
    /** 办学层次：本科 / 专科 */
    val level: String,
    /** 院校类型：综合类 / 理工类 / 师范类 … */
    val type: String,
    /** 办学性质：公办 / 民办 … */
    val nature: String,
    /** 层次标签：985 / 211 / 双一流 / C9 */
    val tags: List<String>,
    /** 建议目标分数（云南物理类最近一年最低分） */
    val targetScore: Int,
    /** 近三年最低分下限 */
    val minScore3Year: Int,
    /** 近三年最低分上限 */
    val maxScore3Year: Int,
    /** 按年份的云南物理类最低分 */
    val scoresByYear: Map<String, Int>,
    /** 校徽地址；在线加载失败时回退为院校首字 */
    val logoUrl: String,
)

/** schools.json 的根结构。 */
data class SchoolData(
    val version: String,
    val province: String,
    val subjectType: String,
    val totalScore: Int,
    val totalCount: Int,
    val schools: List<SchoolInfo>,
)
