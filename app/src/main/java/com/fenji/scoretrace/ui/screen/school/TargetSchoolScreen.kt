package com.fenji.scoretrace.ui.screen.school

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fenji.scoretrace.data.model.AdmissionScore
import com.fenji.scoretrace.data.model.Major
import com.fenji.scoretrace.data.model.MajorTreeData
import com.fenji.scoretrace.data.model.SchoolInfo
import com.fenji.scoretrace.ui.screen.school.component.SchoolExpandedPanel
import com.fenji.scoretrace.ui.screen.school.component.SchoolRow
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

private data class SchoolGroup(val title: String, val schools: List<SchoolInfo>)

private val GROUP_ORDER = listOf(
    "C9 联盟", "国防七子", "985 工程", "211 工程", "双一流", "普通本科", "专科院校",
)

/** 按联盟/层次分桶：C9 → 国防七子 → 985 → 211 → 双一流 → 普通本科 → 专科。列表已按优先级排序，桶内保持原序。 */
private fun buildGroups(schools: List<SchoolInfo>): List<SchoolGroup> {
    val buckets = LinkedHashMap<String, MutableList<SchoolInfo>>()
    schools.forEach { school ->
        val key = when {
            school.tags.contains("C9") -> "C9 联盟"
            school.tags.contains("国防七子") -> "国防七子"
            school.tags.contains("985") -> "985 工程"
            school.tags.contains("211") -> "211 工程"
            school.tags.contains("双一流") -> "双一流"
            // 数据源的层次名为「本科」/「专科（高职）」
            school.level.startsWith("专科") -> "专科院校"
            else -> "普通本科"
        }
        buckets.getOrPut(key) { mutableListOf() }.add(school)
    }
    return GROUP_ORDER.mapNotNull { title ->
        buckets[title]?.takeIf { it.isNotEmpty() }?.let { SchoolGroup(title, it) }
    }
}

/**
 * 目标院校选择/编辑页。
 *
 * 全屏页，[com.fenji.scoretrace.ui.navigation.AppNavHost] 在该路由隐藏底部导航栏；
 * 标题栏自绘（返回 + 标题 + 重置），底部保存按钮自行避让手势条，不嵌套 Scaffold。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TargetSchoolScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TargetSchoolViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val groups = remember(uiState.filteredSchools) { buildGroups(uiState.filteredSchools) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TargetSchoolTopBar(onBack = onBack, onReset = viewModel::onReset)

            SchoolSearchField(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "热门院校",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScoreTraceColors.TextPrimaryLight,
                )
                SubjectTags(subjects = uiState.selectedSubjects)
            }

            when {
                uiState.isLoading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = ScoreTraceColors.BrandPrimary)
                }

                groups.isEmpty() -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "未找到相关院校",
                        fontSize = 14.sp,
                        color = ScoreTraceColors.TextSecondaryLight,
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    groups.forEach { group ->
                        stickyHeader(key = "header-${group.title}") {
                            GroupHeader(title = group.title, count = group.schools.size)
                        }
                        items(
                            items = group.schools,
                            key = { it.id },
                            contentType = { "school" },
                        ) { school ->
                            SchoolItemCard(
                                school = school,
                                isSelected = school.id == uiState.selectedSchoolId,
                                targetScore = uiState.targetScore,
                                selectedMajor = uiState.selectedMajor,
                                currentScore = uiState.currentScore,
                                majorTree = uiState.majorTree,
                                province = uiState.province,
                                primarySubject = uiState.primarySubject,
                                admission = uiState.admissionScore,
                                onSelect = { viewModel.onSchoolSelect(school) },
                                onMajorSelect = viewModel::onMajorSelect,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }

            SaveTargetBar(
                enabled = uiState.selectedSchoolId != null,
                onSave = { viewModel.onSave(onSaved) },
            )
        }
    }
}

/** 标题栏右侧：展示用户实际选科（来自用户偏好），替代原先无信息的「按你的选科可报」。 */
@Composable
private fun SubjectTags(subjects: List<String>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "选科：",
            fontSize = 12.sp,
            color = ScoreTraceColors.TextSecondaryLight,
        )
        subjects.forEach { subject ->
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = ScoreTraceColors.AccentBlue.copy(alpha = 0.10f),
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Text(
                    text = subject,
                    fontSize = 11.sp,
                    color = ScoreTraceColors.BrandPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(title: String, count: Int) {
    Text(
        text = "$title（${count} 所）",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = ScoreTraceColors.TextSecondaryLight,
        modifier = Modifier
            .fillMaxWidth()
            .background(ScoreTraceColors.PageBackgroundLight)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun TargetSchoolTopBar(onBack: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
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
            text = "目标院校",
            modifier = Modifier.weight(1f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
        Text(
            text = "重置",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onReset)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.BrandPrimary,
        )
    }
}

@Composable
private fun SchoolSearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text("搜索院校，如「浙江大学」", fontSize = 14.sp, color = ScoreTraceColors.TextTertiaryLight)
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = ScoreTraceColors.TextSecondaryLight,
                modifier = Modifier.size(20.dp),
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = ScoreTraceColors.BrandPrimary,
        ),
    )
}

/** 单张院校卡片：选中时左侧蓝条 + 展开面板（带展开/收起动画），与头部共用同一张卡片。 */
@Composable
private fun SchoolItemCard(
    school: SchoolInfo,
    isSelected: Boolean,
    targetScore: Int,
    selectedMajor: Major?,
    currentScore: Int,
    majorTree: MajorTreeData?,
    province: String,
    primarySubject: String,
    admission: AdmissionScore?,
    onSelect: () -> Unit,
    onMajorSelect: (Major) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            ScoreTraceColors.AccentBlue.copy(alpha = 0.05f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) {
                ScoreTraceColors.BrandPrimary.copy(alpha = 0.55f)
            } else {
                ScoreTraceColors.CardBorderLight
            },
        ),
    ) {
        Column {
            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(ScoreTraceColors.BrandPrimary),
                    )
                }
                SchoolRow(
                    school = school,
                    isSelected = isSelected,
                    onClick = onSelect,
                    modifier = Modifier.weight(1f),
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(220)),
                exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(180)),
            ) {
                Column {
                    HorizontalDivider(thickness = 1.dp, color = ScoreTraceColors.CardBorderLight)
                    SchoolExpandedPanel(
                        school = school,
                        targetScore = targetScore,
                        selectedMajor = selectedMajor,
                        currentScore = currentScore,
                        majorTree = majorTree,
                        province = province,
                        primarySubject = primarySubject,
                        admission = admission,
                        onMajorSelect = onMajorSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveTargetBar(enabled: Boolean, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val shape = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(shape)
                    .background(
                        if (enabled) {
                            Brush.horizontalGradient(
                                listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan),
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFC3CBD8), Color(0xFFC3CBD8)))
                        },
                    )
                    .then(if (enabled) Modifier.clickable(onClick = onSave) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "保存目标院校",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
