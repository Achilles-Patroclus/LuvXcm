package com.fenji.scoretrace.ui.screen.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.util.AppToast

private const val MAX_CONTENT_LENGTH = 500

private val FEEDBACK_TYPES = listOf("功能建议", "Bug 反馈", "内容错误", "其他")

/**
 * 意见反馈页（界面完整、提交暂为占位）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    var type by rememberSaveable { mutableStateOf(FEEDBACK_TYPES.first()) }
    var content by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScoreTraceColors.PageBackgroundLight,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            MineSubPageTopBar(title = "意见反馈", onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                FieldLabel("反馈类型")
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FEEDBACK_TYPES.forEach { item ->
                        TypeChip(
                            text = item,
                            selected = item == type,
                            onClick = { type = item },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                FieldLabel("反馈内容")
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= MAX_CONTENT_LENGTH) content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    placeholder = { Text("请描述你遇到的问题或建议…") },
                    supportingText = { Text("${content.length}/$MAX_CONTENT_LENGTH") },
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ScoreTraceColors.CardBorderLight, RoundedCornerShape(12.dp))
                        .clickable { AppToast.info("即将支持") }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = ScoreTraceColors.TextSecondaryLight,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("添加图片（即将支持）", fontSize = 14.sp, color = ScoreTraceColors.TextSecondaryLight)
                }

                Spacer(modifier = Modifier.height(20.dp))

                FieldLabel("联系方式（选填）")
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("邮箱 / QQ，便于我们回复你") },
                )

                Spacer(modifier = Modifier.height(28.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(ScoreTraceColors.BrandPrimary, ScoreTraceColors.AccentCyan),
                        ),
                    )
                    .clickable {
                        AppToast.info("感谢反馈，功能即将上线")
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("提交", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = ScoreTraceColors.TextPrimaryLight)
}

@Composable
private fun TypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                if (selected) ScoreTraceColors.BrandPrimary.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
            )
            .border(
                width = 1.dp,
                color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.CardBorderLight,
                shape = CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) ScoreTraceColors.BrandPrimary else ScoreTraceColors.TextSecondaryLight,
        )
    }
}
