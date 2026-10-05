package com.fenji.scoretrace.ui.screen.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.R
import com.fenji.scoretrace.data.local.entity.NotificationEntity
import com.fenji.scoretrace.data.repository.NotificationType
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val SystemGray = Color(0xFF64748B)

/**
 * 通知中心页：顶部栏（返回 + 未读角标 + 全部已读 + 清空）+ 分类 Tab +
 * 可按左滑删除的通知列表 + 底部提示。全屏页，由 AppNavHost 隐藏底部导航栏。
 */
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = hiltViewModel(),
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadCount.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

    val filtered = remember(notifications, selectedTab) {
        selectedTab.type?.let { type -> notifications.filter { it.type == type } } ?: notifications
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            NotificationTopBar(
                unreadCount = unreadCount,
                onBack = onBack,
                onMarkAllRead = viewModel::markAllRead,
                onClear = viewModel::clearAll,
            )

            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ScoreTraceColors.BrandPrimary,
            ) {
                NotificationTab.entries.forEach { tab ->
                    val count = tab.type
                        ?.let { type -> notifications.count { it.type == type } }
                        ?: notifications.size
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        selectedContentColor = ScoreTraceColors.BrandPrimary,
                        unselectedContentColor = ScoreTraceColors.TextSecondaryLight,
                    ) {
                        Text(
                            text = if (count > 0) "${tab.label} $count" else tab.label,
                            fontSize = 13.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "暂无通知",
                        fontSize = 14.sp,
                        color = ScoreTraceColors.TextTertiaryLight,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(filtered, key = { it.id }) { notification ->
                        SwipeableNotificationRow(
                            notification = notification,
                            onDelete = { viewModel.delete(notification.id) },
                            onClick = { viewModel.markRead(notification.id) },
                        )
                    }
                }
            }

            Text(
                text = "只保留最近 30 天的通知 · 已到底部",
                fontSize = 12.sp,
                color = ScoreTraceColors.TextTertiaryLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NotificationTopBar(
    unreadCount: Int,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                tint = ScoreTraceColors.TextPrimaryLight,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "通知",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        if (unreadCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(ScoreTraceColors.ErrorRed),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else "$unreadCount",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onMarkAllRead) {
            Text("全部已读", color = ScoreTraceColors.BrandPrimary, fontSize = 14.sp)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete_sweep),
                contentDescription = "清空通知",
                tint = ScoreTraceColors.TextSecondaryLight,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SwipeableNotificationRow(
    notification: NotificationEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ScoreTraceColors.ErrorRed)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("删除", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        },
    ) {
        NotificationRow(notification = notification, onClick = onClick)
    }
}

@Composable
private fun NotificationRow(notification: NotificationEntity, onClick: () -> Unit) {
    val (icon, accent) = iconFor(notification.type)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ScoreTraceColors.TextPrimaryLight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notification.content,
                        fontSize = 13.sp,
                        color = ScoreTraceColors.TextSecondaryLight,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRelativeTime(notification.timestamp),
                        fontSize = 11.sp,
                        color = ScoreTraceColors.TextTertiaryLight,
                    )
                }
            }

            // 未读蓝点：图标底右侧顶部，与首页铃铛红点（未读计数）区分
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 14.dp, end = 16.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ScoreTraceColors.BrandPrimary),
                )
            }
        }
    }
}

/** 类型 → （图标, 强调色）。 */
@Composable
private fun iconFor(type: String): Pair<Painter, Color> = when (type) {
    NotificationType.SCORE -> painterResource(R.drawable.ic_edit_note) to ScoreTraceColors.BrandPrimary
    NotificationType.STUDY -> painterResource(R.drawable.ic_calendar_month) to ScoreTraceColors.WarningOrange
    NotificationType.AI -> painterResource(R.drawable.ic_ai_logo) to ScoreTraceColors.AccentCyan
    else -> rememberVectorPainter(Icons.Filled.Settings) to SystemGray
}

/** 相对时间：刚刚 / N分钟前 / N小时前 / 昨天 / M月d日。 */
private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minute = 60_000L
    val hour = 60 * minute
    return when {
        diff < minute -> "刚刚"
        diff < hour -> "${diff / minute} 分钟前"
        diff < 24 * hour -> "${diff / hour} 小时前"
        isYesterday(timestamp) -> "昨天"
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun isYesterday(timestamp: Long): Boolean {
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    return yesterday.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
        yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
}
