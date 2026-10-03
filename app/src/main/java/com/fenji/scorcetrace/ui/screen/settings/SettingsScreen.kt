package com.fenji.scorcetrace.ui.screen.settings

import android.content.ClipData
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.component.ScreenHeader
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import com.fenji.scorcetrace.ui.theme.pressScale
import com.fenji.scorcetrace.ui.theme.rememberPressSource
import com.fenji.scorcetrace.util.AppToast
import com.fenji.scorcetrace.util.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    bottomContentPadding: Dp = Dimens.ContentBottom,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ipState by viewModel.ipState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showAboutDialog by rememberSaveable { mutableStateOf(false) }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var datePickerInitialMillis by rememberSaveable { mutableStateOf(0L) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "设置")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.PageHorizontal)
                .padding(top = 4.dp, bottom = bottomContentPadding),
            // 组与组之间拉开到 24dp，形成清晰的分组节奏
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            // ── 外观
            SettingsGroup(title = "外观") {
                SettingsRow(
                    icon = painterResource(R.drawable.ic_dark_mode),
                    title = "深色主题",
                    subtitle = "跟随此开关切换配色",
                    trailing = {
                        Switch(
                            checked = state.darkTheme,
                            onCheckedChange = viewModel::setDarkTheme,
                            colors = brandSwitchColors(),
                        )
                    },
                )
                SettingsRow(
                    icon = painterResource(R.drawable.ic_calendar_month),
                    title = "高考日期",
                    subtitle = "点击修改首页倒计时的目标日期",
                    trailing = {
                        Text(
                            text = state.gaokaoDateText,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    },
                    onClick = {
                        datePickerInitialMillis = DateUtils.utcDateFromLocalTimestamp(
                            viewModel.currentGaokaoTimestamp()
                        )
                        showDatePicker = true
                    },
                )
            }

            // ── 数据
            SettingsGroup(title = "数据") {
                SettingsRow(
                    icon = painterResource(R.drawable.ic_music_note),
                    title = "进入应用自动播放音乐",
                    subtitle = "关闭后首页不会自动开始播放",
                    trailing = {
                        Switch(
                            checked = state.autoPlayMusic,
                            onCheckedChange = viewModel::setAutoPlayMusic,
                            colors = brandSwitchColors(),
                        )
                    },
                )
                SettingsRow(
                    icon = painterResource(R.drawable.ic_delete_outline),
                    title = "清除全部数据",
                    subtitle = "删除所有学习任务与成绩记录",
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { showClearDialog = true },
                )
            }

            // ── 网络
            SettingsGroup(title = "网络") {
                val ip = ipState
                SettingsRow(
                    icon = painterResource(R.drawable.ic_wifi),
                    title = "当前网络 IP",
                    subtitle = when (ip) {
                        IpUiState.Loading -> "获取中…"
                        is IpUiState.Success -> ip.ip
                        IpUiState.Error -> "网络异常，请检查网络后重试"
                    },
                    // 只有拿到 IP 时整行可点：点击复制到剪贴板
                    onClick = (ip as? IpUiState.Success)?.let { success ->
                        {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, success.ip)))
                            }
                            AppToast.success(context, "已复制")
                        }
                    },
                    trailing = {
                        when (ip) {
                            IpUiState.Loading -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = ScoreTraceColors.AccentCyan,
                                trackColor = ScoreTraceColors.AccentCyan.copy(alpha = 0.15f),
                            )

                            is IpUiState.Success -> Text(
                                text = "复制",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 6.dp),
                            )

                            IpUiState.Error -> TextButton(onClick = viewModel::refreshIp) {
                                Text("重试")
                            }
                        }
                    },
                )
            }

            // ── 关于
            SettingsGroup(title = "关于") {
                SettingsRow(
                    icon = rememberVectorPainter(Icons.Rounded.Info),
                    title = "关于 ScoreTrace",
                    subtitle = "版本 v${state.versionName}",
                    onClick = { showAboutDialog = true },
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = datePickerInitialMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let(viewModel::saveGaokaoDate)
                        showDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于 ScoreTrace") },
            text = {
                Text(
                    text = "ScoreTrace 是一款高考备考应用，帮助你管理学习计划与成绩。\n\n" +
                        "版本：v${state.versionName}",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("知道了") }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清除全部数据？") },
            text = { Text("该操作不可撤销，将删除本机保存的所有任务与成绩。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDialog = false
                    },
                ) {
                    Text("确认清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            },
        )
    }
}

/** 分组：小标题 + 组内 12dp 间距的卡片。 */
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 4.dp, bottom = 10.dp)
                .semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.ItemGap)) {
            content()
        }
    }
}

/**
 * 设置项：左侧带浅色背景的圆角方形图标 + 标题 / 说明 + 右侧控件。
 * 可点项带按压缩放；图标为装饰，标题与说明合并朗读。
 */
@Composable
private fun SettingsRow(
    icon: Painter,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interaction = rememberPressSource()
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = ripple(),
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource = interaction)
            .then(clickModifier),
        shape = RoundedCornerShape(Dimens.ListCorner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.IconBox)
                    .clip(RoundedCornerShape(Dimens.SmallCorner))
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/** 开关统一品牌主色。 */
@Composable
private fun brandSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = MaterialTheme.colorScheme.primary,
    checkedBorderColor = Color.Transparent,
)
