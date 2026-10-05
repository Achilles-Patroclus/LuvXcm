package com.fenji.scoretrace.data.repository

/** 拍照识分流程的统一返回类型。 */
sealed class VisionResult {
    /** 个人成绩单（或已按姓名提取出的单人数据） */
    data class Personal(val data: ScoreSheetData) : VisionResult()

    /** 检测到班级排名表，需要用户补充姓名后再提取 */
    data class ClassRanking(val message: String) : VisionResult()

    /** 与成绩无关的图片 */
    data class Other(val reason: String) : VisionResult()

    /** 识别出错（含错误码，如 NOT_FOUND / 1305 / 1301） */
    data class Error(val code: String, val message: String) : VisionResult()
}

/** 识别阶段，用于 UI 分阶段 loading 文案。 */
enum class VisionStage { DETECTING, EXTRACTING, SEARCHING }
