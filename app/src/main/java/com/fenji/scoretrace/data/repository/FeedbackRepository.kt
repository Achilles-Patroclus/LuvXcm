package com.fenji.scoretrace.data.repository

/**
 * 意见反馈提交接口（预留）。
 *
 * 当前版本只完成了反馈页的界面与本地校验，尚未接入服务端。
 * 后续接入时：实现本接口、在 DI 中绑定，并由反馈页的 ViewModel 调用提交方法。
 *
 * TODO: 定义提交方法，例如
 *   suspend fun submit(
 *       type: String,
 *       content: String,
 *       contact: String?,
 *       imageUris: List<String>,
 *   ): Result<Unit>
 */
interface FeedbackRepository
