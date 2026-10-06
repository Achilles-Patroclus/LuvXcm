package com.fenji.scoretrace.ui.screen.mine

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.BuildConfig
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.component.mine.ExportSheet
import com.fenji.scoretrace.ui.component.mine.SettingDangerItem
import com.fenji.scoretrace.ui.component.mine.SettingDivider
import com.fenji.scoretrace.ui.component.mine.SettingGroup
import com.fenji.scoretrace.ui.component.mine.SettingGroupTitle
import com.fenji.scoretrace.ui.component.mine.SettingNavigateItem
import com.fenji.scoretrace.ui.component.mine.SettingSwitchItem
import com.fenji.scoretrace.ui.component.mine.ThemeModeSheet
import com.fenji.scoretrace.ui.component.mine.UserProfileCard
import com.fenji.scoretrace.ui.component.mine.themeModeLabel
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

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
    onOpenAbout: () -> Unit = {},
    onOpenFeedback: () -> Unit = {},
    onNavigateHome: () -> Unit = {},
    viewModel: MineViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val provinceStatus by viewModel.provinceStatus.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showClearDataDialog by rememberSaveable { mutableStateOf(false) }
    var showEditProfile by rememberSaveable { mutableStateOf(false) }
    var showThemeSheet by rememberSaveable { mutableStateOf(false) }
    var showExportSheet by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.refreshProvince() else viewModel.onProvincePermissionDenied()
    }

    LaunchedEffect(Unit) {
        viewModel.toast.collect { AppToast.show(it) }
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
                nickname = state.nickname,
                avatarPath = state.avatarPath,
                targetSchool = state.targetSchool,
                examYear = state.examYear,
                studyDays = state.studyDays,
                examCount = state.examCount,
                checkInDays = state.continuousCheckIn,
                onEditProfile = { showEditProfile = true },
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
                    showChevron = false,
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
                    subtitle = provinceSubtitle(provinceStatus),
                    showChevron = false,
                    trailing = {
                        if (provinceStatus == ProvinceStatus.Locating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = ScoreTraceColors.BrandPrimary,
                            )
                        } else {
                            IconButton(onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    viewModel.refreshProvince()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "刷新定位",
                                    tint = ScoreTraceColors.BrandPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    },
                )
            }

            // ── 偏好
            SettingGroupTitle("偏好")
            SettingGroup {
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_dark_mode),
                    iconBgColor = GraySlate,
                    title = "主题",
                    value = themeModeLabel(state.themeMode),
                    onClick = { showThemeSheet = true },
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
            }

            // ── 数据
            SettingGroupTitle("数据")
            SettingGroup {
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_upload),
                    iconBgColor = ScoreTraceColors.SuccessGreen,
                    title = "导出成绩",
                    value = "先选格式",
                    onClick = { showExportSheet = true },
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
                    onClick = onOpenAbout,
                )
                SettingDivider()
                SettingNavigateItem(
                    icon = painterResource(R.drawable.ic_feedback),
                    iconBgColor = ScoreTraceColors.WarningOrange,
                    title = "意见反馈",
                    value = "",
                    onClick = onOpenFeedback,
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
                        onNavigateHome()
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

    if (showEditProfile) {
        EditProfileSheet(
            nickname = state.nickname,
            avatarPath = state.avatarPath,
            onSaveNickname = viewModel::setNickname,
            onAvatarPicked = viewModel::saveAvatar,
            onDismiss = { showEditProfile = false },
        )
    }

    if (showThemeSheet) {
        ThemeModeSheet(
            current = state.themeMode,
            onSelect = viewModel::setThemeMode,
            onDismiss = { showThemeSheet = false },
        )
    }

    if (showExportSheet) {
        ExportSheet(
            onSelectFormat = { format ->
                viewModel.exportScores(format)
                showExportSheet = false
            },
            onDismiss = { showExportSheet = false },
        )
    }
}

/** 省份项副标题：随定位状态变化。 */
private fun provinceSubtitle(status: ProvinceStatus): String = when (status) {
    ProvinceStatus.Locating -> "正在定位…"
    ProvinceStatus.Success -> "根据 GPS 自动获取"
    ProvinceStatus.Failed -> "定位失败，点击刷新重试"
    ProvinceStatus.PermissionDenied -> "定位权限被拒，点击刷新重新授权"
}
