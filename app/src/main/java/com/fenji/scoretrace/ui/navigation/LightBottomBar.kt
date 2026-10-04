package com.fenji.scoretrace.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 底部导航项：图标用 [Painter]（core 图标走 rememberVectorPainter，缺失图标走本地矢量 drawable）。 */
data class BottomBarTab(
    val screen: Screen,
    @param:StringRes val labelRes: Int,
    val icon: Painter,
)

/**
 * 普通浅色底栏（替代液态玻璃 [FloatingBottomBar]）。
 * 选中项：浅蓝圆角背景 + 品牌蓝图标文字；未选中：中性灰。
 *
 * 顶部分隔线与内容区分开；底栏自身用 [navigationBarsPadding] 避让系统手势条。
 */
@Composable
fun LightBottomBar(
    tabs: List<BottomBarTab>,
    currentRoute: String?,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // 顶部分隔线：区分内容区与底栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(Color(0xFFE2E8F0)),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp, bottom = 6.dp)
                // 关键：避让系统导航栏/手势条
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                LightBottomBarItem(
                    tab = tab,
                    selected = tab.screen.route == currentRoute,
                    onClick = { onSelect(tab.screen) },
                )
            }
        }
    }
}

@Composable
private fun LightBottomBarItem(
    tab: BottomBarTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            // 上下各 6dp：保证选中背景的圆角有完整空间，不被底栏边界截断
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            painter = tab.icon,
            contentDescription = stringResource(tab.labelRes),
            tint = contentColor,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(tab.labelRes),
            fontSize = 11.sp,
            color = contentColor,
        )
    }
}
