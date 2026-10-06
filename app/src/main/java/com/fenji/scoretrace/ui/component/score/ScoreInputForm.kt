package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.DateUtils
import java.util.Date

/** 录入表单里的一科：名称 + 满分（语数外 150，选考 100）。 */
@Immutable
data class ScoreFormSubject(val name: String, val fullScore: Double)

/**
 * 依据用户选科（3+1+2）生成录入表单的科目列表：语数英固定，其后拼接用户选考三科。
 * 未设置选科时回落到「物理、化学、生物」，与 [com.fenji.scoretrace.data.local.UserPreferences] 默认值一致。
 */
fun scoreFormSubjects(selectedSubjects: List<String>): List<ScoreFormSubject> {
    val electives = selectedSubjects.ifEmpty { listOf("物理", "化学", "生物") }
    return listOf(
        ScoreFormSubject("语文", 150.0),
        ScoreFormSubject("数学", 150.0),
        ScoreFormSubject("英语", 150.0),
    ) + electives.map { ScoreFormSubject(it, 100.0) }
}

/** 表单总分 = 各科已填分数之和。 */
fun scoreFormTotal(subjects: List<ScoreFormSubject>, scoreTexts: List<String>): Double =
    subjects.indices.sumOf { index -> scoreTexts.getOrNull(index)?.toDoubleOrNull() ?: 0.0 }

/** 是否有科目超过满分。 */
fun scoreFormHasOverflow(subjects: List<ScoreFormSubject>, scoreTexts: List<String>): Boolean =
    subjects.indices.any { index ->
        (scoreTexts.getOrNull(index)?.toDoubleOrNull() ?: 0.0) > subjects[index].fullScore
    }

/** 录入页顶部栏：返回 + 标题。AI 录成绩与手动录成绩共用，仅标题不同。 */
@Composable
fun ScoreInputTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

/** 录入页统一的分组卡片容器：圆角 16dp、白底、内边距 14dp。 */
@Composable
fun ScoreSectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        content()
    }
}

/**
 * 成绩录入表单（考试信息 + 各科分数 + 总分）。
 *
 * 手动录成绩与 AI 录成绩共用本组件，差别只在页面顶部的 AI 识别区由各自页面另行渲染。
 * 日期字段为只读文本，点击弹出 Material3 [DatePickerDialog]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreEntryForm(
    examName: String,
    onExamNameChange: (String) -> Unit,
    examDate: Date,
    onExamDateChange: (Date) -> Unit,
    classRank: String,
    onClassRankChange: (String) -> Unit,
    gradeRank: String,
    onGradeRankChange: (String) -> Unit,
    subjects: List<ScoreFormSubject>,
    scoreTexts: List<String>,
    onScoreChange: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var datePickerInitial by rememberSaveable { mutableStateOf(0L) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ScoreSectionCard {
            Text(
                text = "考试信息",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = examName,
                onValueChange = onExamNameChange,
                label = { Text("考试名称") },
                placeholder = { Text("如：高三10月月考") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = DateUtils.formatChineseDate(examDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("考试日期") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "选择日期",
                            tint = ScoreTraceColors.TextSecondaryLight,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                // 覆盖一层可点区域，点击任意位置都弹出日期选择器（readOnly 输入框本身不弹键盘）
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            datePickerInitial = DateUtils.utcDateFromLocalTimestamp(examDate.time)
                            showDatePicker = true
                        },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = classRank,
                    onValueChange = { onClassRankChange(it.filter(Char::isDigit)) },
                    label = { Text("班级排名") },
                    placeholder = { Text("可空") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = gradeRank,
                    onValueChange = { onGradeRankChange(it.filter(Char::isDigit)) },
                    label = { Text("年级排名") },
                    placeholder = { Text("可空") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ScoreSectionCard {
            Text(
                text = "各科分数",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(4.dp))
            subjects.forEachIndexed { index, subject ->
                if (index > 0) {
                    HorizontalDivider(thickness = 0.5.dp, color = ScoreTraceColors.CardBorderLight)
                }
                val text = scoreTexts.getOrNull(index).orEmpty()
                val score = text.toDoubleOrNull() ?: 0.0
                val overflow = score > subject.fullScore
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = subject.name,
                        fontSize = 15.sp,
                        color = ScoreTraceColors.TextPrimaryLight,
                        modifier = Modifier.width(52.dp),
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { onScoreChange(index, it.filter { ch -> ch.isDigit() || ch == '.' }) },
                        placeholder = { Text("0", fontSize = 14.sp) },
                        isError = overflow,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "/ ${subject.fullScore.toInt()}",
                        fontSize = 13.sp,
                        color = if (overflow) ScoreTraceColors.ErrorRed else ScoreTraceColors.TextSecondaryLight,
                    )
                }
            }
        }

        val total = scoreFormTotal(subjects, scoreTexts)
        val hasOverflow = scoreFormHasOverflow(subjects, scoreTexts)
        ScoreSectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("总分", fontSize = 13.sp, color = ScoreTraceColors.TextSecondaryLight)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${total.toInt()}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScoreTraceColors.BrandPrimary,
                    )
                }
                Text(
                    text = if (hasOverflow) "有科目超过满分，请检查" else "各科分之和自动汇总",
                    fontSize = 12.sp,
                    color = if (hasOverflow) ScoreTraceColors.ErrorRed else ScoreTraceColors.TextTertiaryLight,
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = datePickerInitial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onExamDateChange(Date(DateUtils.localMidnightFromUtcDate(millis)))
                        }
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
}

/** 底部保存栏：蓝青渐变的「保存成绩」按钮，禁用时灰显。 */
@Composable
fun SaveBar(enabled: Boolean, onSave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            val shape = RoundedCornerShape(24.dp)
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
                    text = "保存成绩",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
