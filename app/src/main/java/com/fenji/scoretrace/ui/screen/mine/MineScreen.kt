package com.fenji.scoretrace.ui.screen.mine

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.component.mine.SettingCopyItem
import com.fenji.scoretrace.ui.component.mine.SettingDangerItem
import com.fenji.scoretrace.ui.component.mine.SettingDivider
import com.fenji.scoretrace.ui.component.mine.SettingGroup
import com.fenji.scoretrace.ui.component.mine.SettingGroupTitle
import com.fenji.scoretrace.ui.component.mine.SettingNavigateItem
import com.fenji.scoretrace.ui.component.mine.SettingSwitchItem
import com.fenji.scoretrace.ui.component.mine.UserProfileCard
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast
import com.fenji.scoretrace.util.Constants
import kotlinx.coroutines.launch

private val GraySlate = Color(0xFF64748B)
private val MusicSky = Color(0xFF0EA5E9)

/**
 * 「我的」页面。
 * 顶部蓝色渐变用户信息卡 + 学习 / 偏好 / 数据 / 关于四组设置 + 底部版权。
 * 全页可滚动；不使用 Scaffold（系统栏内边距已由外层 AppNavHost 处理）。
 */
@Composable
fun MineScreen(
    onOpenTargetSchool: () -> Unit = {},
    viewModel: MineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val networkIp by viewModel.networkIp.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showSubjectDialog by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            UserProfileCard(
                targetSchool = state.targetSchool,
                examYear = state.examYear,
                streakDays = state.streakDays,
                studyDays = state.studyDays,
                scoreCount = state.scoreRecordCount,
                taskCount = state.taskCompletedCount,
                onEditProfile = { /* TODO 编辑个人资料 */ },
            )

            // ── 学习
            SettingGroupTitle("学习")
            SettingGroup {
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_school),
                    iconBgColor = ScoreTraceColors.BrandPrimary,
                    title = "目标院校",
                    value = state.targetSchool.ifBlank { "未设置" },
                    onClick = onOpenTargetSchool,
                )
                SettingDivider()
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_calendar_month),
                    iconBgColor = ScoreTraceColors.SuccessGreen,
                    title = "高考日期",
                    value = state.gaokaoDateText,
                    onClick = { /* TODO 弹出 DatePicker */ },
                )
                SettingDivider()
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_subject),
                    iconBgColor = ScoreTraceColors.SchoolPurple,
                    title = "选科配置",
                    value = state.selectedSubjects.joinToString(" · ").ifBlank { "未设置" },
                    onClick = { showSubjectDialog = true },
                )
            }

            // ── 偏好
            SettingGroupTitle("偏好")
            SettingGroup {
                SettingSwitchItem(
                    icon = painterResource(R.drawable.ic_dark_mode),
                    iconBgColor = GraySlate,
                    title = "深色主题",
                    subtitle = "跟随系统时自动切换",
                    checked = state.darkTheme,
                    onCheckedChange = viewModel::setDarkTheme,
                )
                SettingDivider()
                SettingSwitchItem(
                    icon = painterResource(R.drawable.ic_music_note),
                    iconBgColor = MusicSky,
                    title = "自动播放音乐",
                    subtitle = "打开 App 时继续上次播放",
                    checked = state.autoPlayMusic,
                    onCheckedChange = viewModel::setAutoPlayMusic,
                )
                SettingDivider()
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_text_fields),
                    iconBgColor = ScoreTraceColors.WarningOrange,
                    title = "字体大小",
                    value = state.fontSize,
                    onClick = { /* TODO 字体大小选择 */ },
                )
            }

            // ── 数据
            SettingGroupTitle("数据")
            SettingGroup {
                SettingCopyItem(
                    icon = painterResource(R.drawable.ic_public),
                    iconBgColor = GraySlate,
                    title = "网络 IP",
                    subtitle = "当前出口地址，点击复制",
                    value = networkIp,
                    onCopy = {
                        if (networkIp == "获取中…") return@SettingCopyItem
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(ClipData.newPlainText(null, networkIp))
                            )
                        }
                        AppToast.success("已复制")
                    },
                )
                SettingDivider()
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_upload),
                    iconBgColor = ScoreTraceColors.SuccessGreen,
                    title = "导出成绩",
                    value = "",
                    onClick = { /* TODO 导出为 CSV / PDF */ },
                )
                SettingDivider()
                SettingDangerItem(
                    icon = painterResource(R.drawable.ic_delete_sweep),
                    iconBgColor = ScoreTraceColors.ErrorRed,
                    title = "清除全部数据",
                    subtitle = "成绩、目标与设置将被清空",
                    onClick = { /* TODO 二次确认弹窗 */ },
                )
            }

            // ── 关于
            SettingGroupTitle("关于")
            SettingGroup {
                SettingNavigateItem(
                    icon = rememberVectorPainter(Icons.Filled.Info),
                    iconBgColor = ScoreTraceColors.BrandPrimary,
                    title = "关于 ScoreTrace",
                    value = "v${BuildConfig.VERSION_NAME}",
                    onClick = { /* TODO 关于弹窗 */ },
                )
                SettingDivider()
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_feedback),
                    iconBgColor = ScoreTraceColors.WarningOrange,
                    title = "意见反馈",
                    value = "",
                    onClick = { /* TODO 反馈渠道 */ },
                )
                SettingDivider()
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_code),
                    iconBgColor = GraySlate,
                    title = "开源许可",
                    value = "",
                    onClick = { /* TODO 开源许可列表 */ },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 底部版权
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "ScoreTrace v${BuildConfig.VERSION_NAME} · 为 2027 届考生而做",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "数据仅保存在本机，愿你所求皆如愿",
                    fontSize = 12.sp,
                    color = ScoreTraceColors.TextTertiaryLight,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSubjectDialog) {
        SubjectSelectionDialog(
            current = state.selectedSubjects,
            onDismiss = { showSubjectDialog = false },
            onConfirm = { selected ->
                viewModel.updateSelectedSubjects(selected)
                showSubjectDialog = false
            },
        )
    }
}

/** 选科配置对话框：新高考 3+1+2 —— 首选二选一，再选四选二。 */
@Composable
private fun SubjectSelectionDialog(
    current: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    var primary by remember {
        mutableStateOf(Constants.PRIMARY_SUBJECT_NAMES.firstOrNull { it in current })
    }
    val secondary = remember {
        mutableStateListOf<String>().apply {
            addAll(Constants.SECONDARY_SUBJECT_NAMES.filter { it in current })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选科配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("首选科目（二选一）", style = MaterialTheme.typography.labelLarge)
                Constants.PRIMARY_SUBJECT_NAMES.forEach { name ->
                    SelectableSubjectRow(
                        label = name,
                        selected = primary == name,
                        onClick = { primary = name },
                        radio = true,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("再选科目（四选二）", style = MaterialTheme.typography.labelLarge)
                Constants.SECONDARY_SUBJECT_NAMES.forEach { name ->
                    val selected = name in secondary
                    SelectableSubjectRow(
                        label = name,
                        selected = selected,
                        onClick = {
                            if (selected) {
                                secondary.remove(name)
                            } else if (secondary.size < SECONDARY_PICK_COUNT) {
                                secondary.add(name)
                            }
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = primary != null && secondary.size == SECONDARY_PICK_COUNT,
                onClick = {
                    val ordered = Constants.SECONDARY_SUBJECT_NAMES.filter { it in secondary }
                    onConfirm(listOfNotNull(primary) + ordered)
                },
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

@Composable
private fun SelectableSubjectRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    radio: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (radio) {
            RadioButton(selected = selected, onClick = onClick)
        } else {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 15.sp, color = ScoreTraceColors.TextPrimaryLight)
    }
}

/** 3+1+2 中「再选」需选定的科目数 */
private const val SECONDARY_PICK_COUNT = 2
