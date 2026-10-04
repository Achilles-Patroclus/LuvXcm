package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scorcetrace.R
import com.fenji.scorcetrace.ui.screen.home.CountdownUiState
import com.fenji.scorcetrace.ui.theme.ScoreTraceColors

/**
 * 蓝色渐变倒计时卡。
 * 左：日历图标 + 「距离XXXX年高考还有」+ 大数字天数
 * 右：时/分/秒 三个半透明方块
 * 底部：年度进度条 + 「已过XX%」
 *
 * 卡片外壳不读取任何数值，各数值由独立子组件各自订阅自己的那一位，
 * 因此秒级 tick 只重组秒数单元，不会带动整卡。
 */
@Composable
fun CountdownCard(
    countdown: State<CountdownUiState>,
    yearPassedPercent: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(ScoreTraceColors.BrandGradient))
            .padding(16.dp),
    ) {
        CountdownHeader(countdown)

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DaysValue(countdown)

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeUnitBox(countdown, selector = { it.hours }, label = "时")
                TimeUnitBox(countdown, selector = { it.minutes }, label = "分")
                TimeUnitBox(countdown, selector = { it.seconds }, label = "秒", highlight = true)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        YearProgress(yearPassedPercent)
    }
}

/** 顶部：日历图标 + 「距离XXXX年高考还有」 */
@Composable
private fun CountdownHeader(countdown: State<CountdownUiState>) {
    val targetYear by remember {
        derivedStateOf { countdown.value.targetDateText.take(4).toIntOrNull() ?: 2027 }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar_month),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "距离${targetYear}年高考还有",
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}

/** 大天数 + 「天」 */
@Composable
private fun DaysValue(countdown: State<CountdownUiState>) {
    val days by remember { derivedStateOf { countdown.value.days } }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = days.toString(),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 56.sp,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "天",
            fontSize = 17.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

/** 底部年度进度 */
@Composable
private fun YearProgress(yearPassedPercent: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "年度进度",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "已过 $yearPassedPercent%",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.9f),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((yearPassedPercent / 100f).coerceIn(0f, 1f))
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White),
            )
        }
    }
}

/**
 * 时/分/秒有状态包装：只订阅自己那一位，补零后交给无状态叶子 [CountdownUnit]。
 * 只有自己那一位变化时才重组，不波及天/时/分。
 */
@Composable
private fun TimeUnitBox(
    countdown: State<CountdownUiState>,
    selector: (CountdownUiState) -> Long,
    label: String,
    highlight: Boolean = false,
) {
    val value by remember(selector) {
        derivedStateOf { selector(countdown.value).toString().padStart(2, '0') }
    }
    CountdownUnit(label = label, value = value, highlight = highlight)
}

/**
 * 时/分/秒单元叶子（无状态）：入参只有不可变基本值，全部稳定，Compose 可跳过。
 * 秒位每秒变化时只重组这一个盒子。
 */
@Composable
private fun CountdownUnit(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            // 半透明白底：秒比时分略亮，形成层级；alpha 提高以拉开白色数字的对比度
            .background(Color.White.copy(alpha = if (highlight) 0.35f else 0.25f))
            .padding(horizontal = 11.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f),
        )
    }
}
