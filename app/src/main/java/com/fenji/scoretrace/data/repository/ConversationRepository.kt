package com.fenji.scoretrace.data.repository

import com.fenji.scoretrace.data.local.dao.ConversationDao
import com.fenji.scoretrace.data.local.entity.ConversationEntity
import com.fenji.scoretrace.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

interface ConversationRepository {
    /** 全部会话，按最近消息时间倒序 */
    fun observeConversations(): Flow<List<ConversationEntity>>

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>>

    /** 新建会话，返回会话 id */
    suspend fun createConversation(title: String): Long

    suspend fun addMessage(conversationId: Long, role: String, content: String)

    suspend fun touchConversation(id: Long)

    /** 更新会话标题（如 AI 自动生成的短标题） */
    suspend fun updateTitle(conversationId: Long, title: String)

    /** 删除会话及其全部消息 */
    suspend fun deleteConversation(id: Long)

    /** 清空全部会话与消息 */
    suspend fun clearAll()
}

@Singleton
class DefaultConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
) : ConversationRepository {

    override fun observeConversations(): Flow<List<ConversationEntity>> =
        conversationDao.observeConversations()

    override fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        conversationDao.observeMessages(conversationId)

    override suspend fun createConversation(title: String): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insertConversation(
            ConversationEntity(title = title, createdAt = now, updatedAt = now),
        )
    }

    override suspend fun addMessage(conversationId: Long, role: String, content: String) {
        conversationDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis(),
            )
        )
    }

    override suspend fun touchConversation(id: Long) {
        conversationDao.touchConversation(id, System.currentTimeMillis())
    }

    override suspend fun updateTitle(conversationId: Long, title: String) {
        conversationDao.updateConversation(conversationId, title, System.currentTimeMillis())
    }

    override suspend fun deleteConversation(id: Long) {
        conversationDao.deleteMessages(id)
        conversationDao.deleteConversation(id)
    }

    override suspend fun clearAll() {
        conversationDao.clearConversations()
    }
}
