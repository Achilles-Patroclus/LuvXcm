package com.fenji.scoretrace.data.repository

import android.content.Context
import com.fenji.scoretrace.data.model.ProvinceScore
import com.fenji.scoretrace.data.model.ProvinceScoreData
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 各省分数线仓库：从 assets/province_scores.json 读取，进程内缓存。 */
interface ProvinceScoreRepository {
    /** 全部省级行政区（key 为省份名）→ 分数线。 */
    suspend fun getAll(): Map<String, ProvinceScore>

    /** 取某省分数线；无数据返回 null。 */
    suspend fun getScores(province: String): ProvinceScore?
}

@Singleton
class DefaultProvinceScoreRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson,
) : ProvinceScoreRepository {

    private var cache: Map<String, ProvinceScore>? = null

    override suspend fun getAll(): Map<String, ProvinceScore> = withContext(Dispatchers.IO) {
        cache?.let { return@withContext it }
        val json = context.assets.open("province_scores.json").bufferedReader().use { it.readText() }
        val parsed = gson.fromJson(json, ProvinceScoreData::class.java)
        val data = parsed?.data.orEmpty()
        cache = data
        data
    }

    override suspend fun getScores(province: String): ProvinceScore? = getAll()[province]
}
