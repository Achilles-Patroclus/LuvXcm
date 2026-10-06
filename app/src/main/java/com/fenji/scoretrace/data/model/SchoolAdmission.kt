package com.fenji.scoretrace.data.model

/** 某省某科类的院校录取最低分与最低位次；缺失字段为 null。 */
data class AdmissionScore(
    val min: Int? = null,
    val rank: Int? = null,
)

/** 一所院校在某省的录取数据（按科类）。 */
data class ProvinceAdmission(
    val physics: AdmissionScore? = null,
    val history: AdmissionScore? = null,
)

/**
 * school_admission_scores.json 的根结构。
 *
 * [data] 为「院校 id → 省份 → 科类录取分」的三层映射；院校 id 与 assets/schools.json 一致。
 */
data class SchoolAdmissionData(
    val version: String,
    val source: String? = null,
    val data: Map<String, Map<String, ProvinceAdmission>> = emptyMap(),
)
