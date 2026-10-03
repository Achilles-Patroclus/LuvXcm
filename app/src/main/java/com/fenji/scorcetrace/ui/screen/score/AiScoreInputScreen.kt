package com.fenji.scorcetrace.ui.screen.score

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors
import com.fenji.scorcetrace.util.DateUtils
import java.util.Date

/** 需要逐科录入的六科与各自满分（云南 3+1+2：语数外 150，其余 100）。 */
private val FORM_SUBJECTS = listOf(
    "语文" to 150.0,
    "数学" to 150.0,
    "英语" to 150.0,
    "物理" to 100.0,
    "化学" to 100.0,
    "生物" to 100.0,
)

/**
 * AI 录成绩页：手动录入考试名、日期、班级排名与各科分数，实时汇总总分并校验（分数不超过满分），
 * 保存后写入数据库并生成「新成绩已录入」通知。
 */
@Composable
fun AiScoreInputScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AiScoreInputViewModel = hiltViewModel(),
) {
    var examName by rememberSaveable { mutableStateOf("") }
    var examDateText by rememberSaveable { mutableStateOf(DateUtils.formatDate(Date())) }
    var classRankText by rememberSaveable { mutableStateOf("") }
    val scoreTexts = remember { mutableStateListOf(*Array(FORM_SUBJECTS.size) { "" }) }

    val entries = FORM_SUBJECTS.mapIndexed { index, (name, fullScore) ->
        ScoreEntry(name = name, score = scoreTexts[index].toDoubleOrNull() ?: 0.0, fullScore = fullScore)
    }
    val total = entries.sumOf { if (it.score > 0) it.score else 0.0 }
    val hasOverflow = entries.any { it.score > it.fullScore }
    val canSave = examName.isNotBlank() && !hasOverflow && entries.any { it.score > 0 }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AiScoreTopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionCard {
                    Text("考试信息", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = examName,
                        onValueChange = { examName = it },
                        label = { Text("考试名称") },
                        placeholder = { Text("如：高三10月月考") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = examDateText,
                        onValueChange = { examDateText = it },
                        label = { Text("考试日期（yyyy-MM-dd）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = classRankText,
                        onValueChange = { classRankText = it.filter(Char::isDigit) },
                        label = { Text("班级排名（可空）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SectionCard {
                    Text("各科分数", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ScoreTraceColors.TextPrimaryLight)
                    Spacer(modifier = Modifier.height(4.dp))
                    FORM_SUBJECTS.forEachIndexed { index, (name, fullScore) ->
                        if (index > 0) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = ScoreTraceColors.CardBorderLight,
                            )
                        }
                        val score = scoreTexts[index].toDoubleOrNull() ?: 0.0
                        val overflow = score > fullScore
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = name,
                                fontSize = 15.sp,
                                color = ScoreTraceColors.TextPrimaryLight,
                                modifier = Modifier.width(52.dp),
                            )
                            OutlinedTextField(
                                value = scoreTexts[index],
                                onValueChange = { scoreTexts[index] = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                placeholder = { Text("0", fontSize = 14.sp) },
                                isError = overflow,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "/ ${fullScore.toInt()}",
                                fontSize = 13.sp,
                                color = if (overflow) ScoreTraceColors.ErrorRed else ScoreTraceColors.TextSecondaryLight,
                            )
                        }
                    }
                }

                SectionCard {
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

                Spacer(modifier = Modifier.height(4.dp))
            }

            SaveBar(
                enabled = canSave,
                onSave = {
                    viewModel.save(
                        examName = examName.trim(),
                        examDate = DateUtils.parseDate(examDateText) ?: Date(),
                        classRank = classRankText.toIntOrNull(),
                        entries = entries.filter { it.score > 0 },
                        onSaved = onSaved,
                    )
                },
            )
        }
    }
}

@Composable
private fun AiScoreTopBar(onBack: () -> Unit) {
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
            text = "AI 录成绩",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = ScoreTraceColors.TextPrimaryLight,
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun SaveBar(enabled: Boolean, onSave: () -> Unit) {
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
                    text = "保存成绩",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}
