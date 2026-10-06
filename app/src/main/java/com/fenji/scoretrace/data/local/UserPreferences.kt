package com.fenji.scoretrace.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fenji.scoretrace.util.AppLogger
import com.fenji.scoretrace.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/** 用户偏好设置，目前承载高考日期 */
@Singleton
class UserPreferences @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** 高考日期当天 0 点 0 分 0 秒的时间戳；未设置过时给出默认值（今年或明年 6 月 7 日） */
    val gaokaoTimestamp: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_GAOKAO_TIMESTAMP] ?: DateUtils.defaultGaokaoTimestamp()
    }

    suspend fun saveGaokaoTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[KEY_GAOKAO_TIMESTAMP] = timestamp
        }
    }

    /** 进入应用是否自动播放音乐 */
    val autoPlayMusic: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_PLAY_MUSIC] ?: true
    }

    suspend fun setAutoPlayMusic(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_PLAY_MUSIC] = enabled
        }
    }

    /** 是否启用深色主题 */
    val darkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_THEME] ?: false
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = enabled
        }
    }

    /** 用户选科（3+1+2），逗号分隔存库；未设置时默认「物理,化学,生物」 */
    val selectedSubjects: Flow<List<String>> = context.dataStore.data.map { preferences ->
        (preferences[KEY_SELECTED_SUBJECTS] ?: DEFAULT_SELECTED_SUBJECTS)
            .split(SUBJECT_SEPARATOR)
            .filter { it.isNotBlank() }
    }

    suspend fun setSelectedSubjects(subjects: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_SUBJECTS] = subjects.joinToString(SUBJECT_SEPARATOR)
        }
        AppLogger.i("SubjectChange", subjects.joinToString(SUBJECT_SEPARATOR))
    }

    /**
     * 用户所在省份（省级行政区名称）；未设置时默认「云南」（兼容存量用户）。
     *
     * 注意：本字段目前仅用于 System Prompt 注入与多地区推广预留设置入口。
     * 分数线相关的展示逻辑仍按云南写定——本科线 435/465、特招线 505/545，
     * 以及云南 C9 投档线均为硬编码，未随省份切换，属于下一阶段任务。
     */
    val province: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROVINCE] ?: DEFAULT_PROVINCE
    }

    suspend fun setProvince(value: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROVINCE] = value
        }
        AppLogger.i("ProvinceChange", value)
    }

    /**
     * 是否已完成「首次启动自动定位」流程（成功、失败、用户拒绝都算完成），
     * 保证只询问一次，不重复打扰用户。
     */
    val provinceLocated: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_PROVINCE_LOCATED] ?: false
    }

    suspend fun setProvinceLocated(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PROVINCE_LOCATED] = value
        }
    }

    /** 用户是否显式设置过省份（用于区分默认「云南」与用户手动选择，避免定位覆盖用户选择）。 */
    suspend fun hasExplicitProvince(): Boolean =
        context.dataStore.data.first()[KEY_PROVINCE] != null

    /** 今日 AI 重点文案（缓存） */
    val aiFocus: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_FOCUS]
    }

    /** 今日 AI 重点的生成日期（yyyy-MM-dd） */
    val aiFocusDate: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_FOCUS_DATE]
    }

    /** 生成该 AI 重点时的数据指纹（成绩 + 目标院校）；与当前指纹不一致即视为缓存失效。 */
    val aiFocusSignal: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_FOCUS_SIGNAL]
    }

    suspend fun saveAiFocus(text: String, date: String, signal: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AI_FOCUS] = text
            preferences[KEY_AI_FOCUS_DATE] = date
            preferences[KEY_AI_FOCUS_SIGNAL] = signal
        }
    }

    /** 清空全部偏好（用于「清除全部数据」，各字段会回落到默认值）。 */
    suspend fun clearAll() {
        context.dataStore.edit { preferences -> preferences.clear() }
    }

    private companion object {
        val KEY_GAOKAO_TIMESTAMP = longPreferencesKey("gaokao_timestamp")
        val KEY_AUTO_PLAY_MUSIC = booleanPreferencesKey("auto_play_music")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_SELECTED_SUBJECTS = stringPreferencesKey("selected_subjects")
        val KEY_PROVINCE = stringPreferencesKey("province")
        val KEY_PROVINCE_LOCATED = booleanPreferencesKey("province_located")
        val KEY_AI_FOCUS = stringPreferencesKey("ai_focus")
        val KEY_AI_FOCUS_DATE = stringPreferencesKey("ai_focus_date")
        val KEY_AI_FOCUS_SIGNAL = stringPreferencesKey("ai_focus_signal")

        const val SUBJECT_SEPARATOR = ","
        const val DEFAULT_SELECTED_SUBJECTS = "物理,化学,生物"

        /** 默认省份：项目面向云南考生，存量用户无该字段时回落到此值。 */
        const val DEFAULT_PROVINCE = "云南"
    }
}
