package com.fenji.scorcetrace.data.repository

import com.fenji.scorcetrace.data.local.dao.NotificationDao
import com.fenji.scorcetrace.data.local.entity.NotificationEntity
import com.fenji.scorcetrace.util.DateUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** 通知类型标识（存库为字符串）。 */
object NotificationType {
    const val STUDY = "study"
    const val SCORE = "score"
    const val AI = "ai"
    const val SYSTEM = "system"
}

interface NotificationRepository {
    fun observeAll(): Flow<List<NotificationEntity>>
    fun observeUnreadCount(): Flow<Int>

    /** 新增一条通知，并顺带清理 30 天前的旧记录 */
    suspend fun add(type: String, title: String, content: String)

    /** 用新内容覆盖当天该类型的通知：先删当天旧记录，再插入（保证每天至多一条） */
    suspend fun addReplacingToday(type: String, title: String, content: String)

    suspend fun markRead(id: Long)
    suspend fun markAllRead()
    suspend fun delete(id: Long)
    suspend fun clearAll()
}

@Singleton
class DefaultNotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao,
) : NotificationRepository {

    override fun observeAll(): Flow<List<NotificationEntity>> = notificationDao.observeAll()

    override fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()

    override suspend fun add(type: String, title: String, content: String) {
        notificationDao.insert(
            NotificationEntity(
                type = type,
                title = title,
                content = content,
                timestamp = System.currentTimeMillis(),
            )
        )
        notificationDao.deleteOlderThan(System.currentTimeMillis() - RETAIN_MILLIS)
    }

    override suspend fun addReplacingToday(type: String, title: String, content: String) {
        notificationDao.deleteByTypeSince(type, DateUtils.startOfDay().time)
        add(type, title, content)
    }

    override suspend fun markRead(id: Long) = notificationDao.markRead(id)

    override suspend fun markAllRead() = notificationDao.markAllRead()

    override suspend fun delete(id: Long) = notificationDao.deleteById(id)

    override suspend fun clearAll() = notificationDao.clearAll()

    private companion object {
        const val RETAIN_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
