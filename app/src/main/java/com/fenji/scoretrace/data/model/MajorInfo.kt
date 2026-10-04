package com.fenji.scoretrace.data.model

/**
 * 专业三级目录（数据源：assets/majors_v2.json，教育部《普通高等学校本科专业目录（2026年）》）。
 * 层级：学科门类（2 位代码）→ 专业类（4 位代码）→ 专业（6 位代码，含 T/K 后缀）。
 */
data class Major(
    val code: String,
    val name: String,
) {
    /** 统一展示格式：专业代码 + 空格 + 专业名称，例「080901 计算机科学与技术」。 */
    val display: String get() = "$code $name"
}

/** 专业类（如 0809 计算机类）。 */
data class MajorSubCategory(
    val code: String,
    val name: String,
    val majors: List<Major>,
)

/** 学科门类（如 08 工学）。 */
data class MajorCategory(
    val code: String,
    val name: String,
    val subCategories: List<MajorSubCategory>,
)

/** majors_v2.json 的根结构。 */
data class MajorTreeData(
    val version: String,
    val categoryCount: Int,
    val subCategoryCount: Int,
    val majorCount: Int,
    val categories: List<MajorCategory>,
)
