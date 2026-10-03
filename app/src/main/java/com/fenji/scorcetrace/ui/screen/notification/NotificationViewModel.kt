package com.fenji.scorcetrace.ui.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fenji.scorcetrace.data.local.entity.NotificationEntity
import com.fenji.scorcetrace.data.repository.NotificationRepository
import com.fenji.scorcetrace.data.repository.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 通知中心顶部分类 Tab；[type] 为 null 表示「全部」。 */
enum class NotificationTab(val label: String, val type: String?) {
    ALL("全部", null),
    STUDY("学习", NotificationType.STUDY),
    SCORE("成绩", NotificationType.SCORE),
    AI("AI建议", NotificationType.AI),
    SYSTEM("系统", NotificationType.SYSTEM),
}

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> = notificationRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = notificationRepository.observeUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _selectedTab = MutableStateFlow(NotificationTab.ALL)
    val selectedTab: StateFlow<NotificationTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: NotificationTab) {
        _selectedTab.value = tab
    }

    fun markRead(id: Long) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead() }
    }

    fun delete(id: Long) {
        viewModelScope.launch { notificationRepository.delete(id) }
    }

    fun clearAll() {
        viewModelScope.launch { notificationRepository.clearAll() }
    }
}
