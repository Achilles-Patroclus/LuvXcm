package com.fenji.scoretrace.data.repository

import android.content.Context
import com.fenji.scoretrace.data.model.SchoolData
import com.fenji.scoretrace.data.model.SchoolInfo
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 院校录取数据仓库：从 assets/schools.json 读取，进程内缓存。 */
interface SchoolRepository {
    suspend fun loadSchools(): SchoolData

    /** 按院校名 / 城市 / 标签模糊匹配；[query] 为空返回全部 */
    suspend fun searchSchools(query: String): List<SchoolInfo>

    suspend fun getSchoolById(id: String): SchoolInfo?

    /** 按院校全名精确匹配（保存的目标院校只存了校名，用它反查校徽等信息） */
    suspend fun findByName(name: String): SchoolInfo?
}

@Singleton
class DefaultSchoolRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson,
) : SchoolRepository {

    private var cachedData: SchoolData? = null

    override suspend fun loadSchools(): SchoolData = withContext(Dispatchers.IO) {
        cachedData?.let { return@withContext it }
        val json = context.assets.open("schools.json").bufferedReader().use { it.readText() }
        val data = gson.fromJson(json, SchoolData::class.java)
        cachedData = data
        data
    }

    override suspend fun searchSchools(query: String): List<SchoolInfo> {
        val data = loadSchools()
        if (query.isBlank()) return data.schools
        return data.schools.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.city.contains(query, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(query, ignoreCase = true) }
        }
    }

    override suspend fun getSchoolById(id: String): SchoolInfo? =
        loadSchools().schools.find { it.id == id }

    override suspend fun findByName(name: String): SchoolInfo? =
        loadSchools().schools.find { it.name == name }
}
