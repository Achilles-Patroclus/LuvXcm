package com.fenji.scoretrace.ui.component.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * 「我的」页顶部用户信息卡：蓝色渐变背景 + 头像 + 备考人信息 + 连续打卡 + 三统计数据。
 * 底部圆角 24dp，右上角为编辑入口。
 */
@Composable
fun UserProfileCard(
    targetSchool: String,
    examYear: String,
    streakDays: Int,
    studyDays: Int,
    scoreCount: Int,
    taskCount: Int,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val schoolText = targetSchool.ifBlank { "未设置目标院校" }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        ScoreTraceColors.BrandPrimary,
                        Color(0xFF2F9FE8),
                        Color(0xFF06B6D4),
                    )
                )
            )
            .drawBehind {
                // 装饰性半透明光斑，呼应设计稿右上的浅色圆
                drawCircle(
                    color = Color.White.copy(alpha = 0.08f),
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.92f, -size.height * 0.05f),
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = size.minDimension * 0.32f,
                    center = Offset(size.width * 0.78f, size.height * 0.42f),
                )
            }
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp),
    ) {
        // 编辑按钮（右上角）
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
                    .clickable(onClick = onEditProfile),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑资料",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // 头像 + 信息
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = ScoreTraceColors.BrandPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "备考人",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$examYear 届 · 目标 $schoolText",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_fire),
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "连续打卡 $streakDays 天",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 三个统计数据
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatItem(
                label = "学习天数",
                value = studyDays.toString(),
                valueColor = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f),
            )
            StatItem(
                label = "成绩记录",
                value = scoreCount.toString(),
                valueColor = ScoreTraceColors.SuccessGreen,
                modifier = Modifier.weight(1f),
            )
            StatItem(
                label = "任务完成",
                value = taskCount.toString(),
                valueColor = ScoreTraceColors.WarningOrange,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = ScoreTraceColors.TextSecondaryLight,
        )
    }
}
