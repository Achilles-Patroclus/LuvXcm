package com.fenji.scoretrace.ui.component.score

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/** 底部录入引导卡：虚线/浅蓝底 + 相机图标，引导去 AI 页拍照录成绩。 */
@Composable
fun RecordGuideCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.04f))
            .border(
                width = 1.dp,
                color = ScoreTraceColors.BrandPrimary.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_photo_camera),
                contentDescription = null,
                tint = ScoreTraceColors.BrandPrimary,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "录入成绩？",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = ScoreTraceColors.BrandPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "去AI页上传成绩单照片，AI自动识别科目、分数与排名",
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
                lineHeight = 16.sp,
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = ScoreTraceColors.BrandPrimary,
            modifier = Modifier.size(18.dp),
        )
    }
}
