package com.fenji.scoretrace.ui.component.mine

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.R
import com.fenji.scoretrace.ui.theme.ScoreTraceColors
import com.fenji.scoretrace.ui.theme.pressScale
import com.fenji.scoretrace.ui.theme.rememberPressSource

/** 分组标题（「学习」「偏好」「数据」「关于」）。 */
@Composable
fun SettingGroupTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = ScoreTraceColors.TextTertiaryLight,
        modifier = modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

/** 分组容器：白色圆角卡片，内含多个设置项。 */
@Composable
fun SettingGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        content()
    }
}

/** 跳转项：图标 + 标题 + 右侧值 + 箭头。 */
@Composable
fun SettingNavigateItem(
    icon: Painter,
    iconBgColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = rememberPressSource()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon = icon, bgColor = iconBgColor)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = ScoreTraceColors.TextSecondaryLight,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = ScoreTraceColors.TextTertiaryLight,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** Switch 项：图标 + 标题 + 副标题 + Switch。 */
@Composable
fun SettingSwitchItem(
    icon: Painter,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon = icon, bgColor = iconBgColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ScoreTraceColors.BrandPrimary,
                checkedBorderColor = Color.Transparent,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1),
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** 复制项：图标 + 标题 + 副标题 + 值 + 复制按钮。 */
@Composable
fun SettingCopyItem(
    icon: Painter,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    value: String,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon = icon, bgColor = iconBgColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.TextPrimaryLight,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            modifier = Modifier.padding(end = 8.dp),
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ScoreTraceColors.PageBackgroundLight)
                .clickable(onClick = onCopy),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_content_copy),
                contentDescription = "复制",
                tint = ScoreTraceColors.TextSecondaryLight,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 危险项（红色标题，如「清除全部数据」）。 */
@Composable
fun SettingDangerItem(
    icon: Painter,
    iconBgColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = rememberPressSource()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource = interaction)
            .clickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon = icon, bgColor = iconBgColor)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScoreTraceColors.ErrorRed,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ScoreTraceColors.TextSecondaryLight,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = ScoreTraceColors.TextTertiaryLight,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** 通用图标（圆角方块背景 + 同色图标）。 */
@Composable
private fun SettingIcon(icon: Painter, bgColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = bgColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 分组内分隔线（左侧让出图标宽度）。 */
@Composable
fun SettingDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .padding(start = 62.dp)
            .background(ScoreTraceColors.CardBorderLight),
    )
}
