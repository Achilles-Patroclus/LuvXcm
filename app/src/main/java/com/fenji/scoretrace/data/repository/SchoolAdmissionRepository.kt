package com.fenji.scoretrace.data.repository

import android.content.Context
import com.fenji.scoretrace.data.model.AdmissionScore
import com.fenji.scoretrace.data.model.ProvinceAdmission
import com.fenji.scoretrace.data.model.SchoolAdmissionData
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 院校分省录取分仓库：从 assets/school_admission_scores.json 读取。
 *
 * 数据按「院校 id → 省份 → 科类」三维组织；未覆盖的组合返回 null（UI 显示「暂无该省份录取数据」）。
 */
interface SchoolAdmissionRepository {
    /**
     * 取某院校在某省、某首选科目下的录取分。
     * @param primarySubject 首选科目名（物理/历史）；其余值按物理处理。
     */
    suspend fun getScore(schoolId: String, province: String, primarySubject: String?): AdmissionScore?
}

@Singleton
class DefaultSchoolAdmissionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson,
) : SchoolAdmissionRepository {

    private var cache: Map<String, Map<String, ProvinceAdmission>>? = null

    private suspend fun load(): Map<String, Map<String, ProvinceAdmission>> =
        withContext(Dispatchers.IO) {
            cache?.let { return@withContext it }
            val json = context.assets.open("school_admission_scores.json")
                .bufferedReader().use { it.readText() }
            val parsed = gson.fromJson(json, SchoolAdmissionData::class.java)
            val data = parsed?.data.orEmpty()
            cache = data
            data
        }

    override suspend fun getScore(
        schoolId: String,
        province: String,
        primarySubject: String?,
    ): AdmissionScore? {
        val admission = load()[schoolId]?.get(province) ?: return null
        return if (primarySubject == PRIMARY_HISTORY) admission.history else admission.physics
    }

    private companion object {
        const val PRIMARY_HISTORY = "历史"
    }
}
