package com.fenji.scorcetrace.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fenji.scorcetrace.util.DateUtils
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

    private companion object {
        val KEY_GAOKAO_TIMESTAMP = longPreferencesKey("gaokao_timestamp")
        val KEY_AUTO_PLAY_MUSIC = booleanPreferencesKey("auto_play_music")
    }
}
