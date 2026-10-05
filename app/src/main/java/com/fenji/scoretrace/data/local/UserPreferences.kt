package com.fenji.scoretrace.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fenji.scoretrace.util.DateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
    }

    /** 今日 AI 重点文案（缓存，当天只生成一次） */
    val aiFocus: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_FOCUS]
    }

    /** 今日 AI 重点的生成日期（yyyy-MM-dd） */
    val aiFocusDate: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_AI_FOCUS_DATE]
    }

    suspend fun saveAiFocus(text: String, date: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AI_FOCUS] = text
            preferences[KEY_AI_FOCUS_DATE] = date
        }
    }

    /** 清除今日 AI 重点缓存（成绩 / 目标院校变化时调用，强制下次重新生成）。 */
    suspend fun clearAiFocus() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_AI_FOCUS)
            preferences.remove(KEY_AI_FOCUS_DATE)
        }
    }

    private companion object {
        val KEY_GAOKAO_TIMESTAMP = longPreferencesKey("gaokao_timestamp")
        val KEY_AUTO_PLAY_MUSIC = booleanPreferencesKey("auto_play_music")
        val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        val KEY_SELECTED_SUBJECTS = stringPreferencesKey("selected_subjects")
        val KEY_AI_FOCUS = stringPreferencesKey("ai_focus")
        val KEY_AI_FOCUS_DATE = stringPreferencesKey("ai_focus_date")

        const val SUBJECT_SEPARATOR = ","
        const val DEFAULT_SELECTED_SUBJECTS = "物理,化学,生物"
    }
}
