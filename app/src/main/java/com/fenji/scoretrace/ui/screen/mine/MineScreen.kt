package com.fenji.scoretrace.ui.screen.mine

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.font.FontWeight
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
    onOpenSubjectConfig: () -> Unit = {},
    viewModel: MineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val networkIp by viewModel.networkIp.collectAsStateWithLifecycle()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showClearDataDialog by rememberSaveable { mutableStateOf(false) }
    var showProvinceDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { AppToast.success(it) }
    }

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
                    onClick = onOpenSubjectConfig,
                )
                SettingDivider()
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_public),
                    iconBgColor = ScoreTraceColors.AccentCyanOnLight,
                    title = "省份",
                    value = state.province,
                    onClick = { showProvinceDialog = true },
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
                    onClick = { showClearDataDialog = true },
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

    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("清除全部数据？") },
            text = {
                Text("将删除本机保存的成绩、目标院校、任务、通知、计时记录与 AI 对话，并重置所有设置。该操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataDialog = false
                    },
                ) {
                    Text("确认清除", color = ScoreTraceColors.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) { Text("取消") }
            },
        )
    }

    if (showProvinceDialog) {
        ProvincePickerDialog(
            current = state.province,
            onSelect = { province ->
                viewModel.setProvince(province)
                showProvinceDialog = false
            },
            onDismiss = { showProvinceDialog = false },
        )
    }
}

/** 省份选择器：全国 34 个省级行政区，当前项高亮。 */
@Composable
private fun ProvincePickerDialog(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择省份") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                CHINA_PROVINCES.forEach { province ->
                    val selected = province == current
                    Text(
                        text = province,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextPrimaryLight,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) ScoreTraceColors.BrandPrimary.copy(alpha = 0.10f) else Color.Transparent,
                            )
                            .clickable { onSelect(province) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 全国省级行政区（34 个）。 */
private val CHINA_PROVINCES = listOf(
    "北京", "天津", "河北", "山西", "内蒙古",
    "辽宁", "吉林", "黑龙江", "上海", "江苏",
    "浙江", "安徽", "福建", "江西", "山东",
    "河南", "湖北", "湖南", "广东", "广西",
    "海南", "重庆", "四川", "贵州", "云南",
    "西藏", "陕西", "甘肃", "青海", "宁夏",
    "新疆", "香港", "澳门", "台湾",
)
