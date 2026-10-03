package com.fenji.scorcetrace.data.repository

import android.content.Context
import com.fenji.scorcetrace.data.model.MajorTreeData
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** 专业三级目录仓库：从 assets/majors_v2.json 读取，进程内缓存。 */
interface MajorRepository {
    suspend fun loadMajorTree(): MajorTreeData
}

@Singleton
class DefaultMajorRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val gson: Gson,
) : MajorRepository {

    private var cached: MajorTreeData? = null

    override suspend fun loadMajorTree(): MajorTreeData = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }
        val json = context.assets.open("majors_v2.json").bufferedReader().use { it.readText() }
        val data = gson.fromJson(json, MajorTreeData::class.java)
        cached = data
        data
    }
}
