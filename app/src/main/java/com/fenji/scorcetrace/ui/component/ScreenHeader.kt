package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fenji.scorcetrace.ui.theme.Dimens

/**
 * 页面标题栏。
 * 外层 AppNavHost 的 Scaffold 已处理状态栏与底部导航栏内边距，这里不再嵌套 Scaffold，
 * 避免系统栏内边距被重复计算。
 *
 * [horizontalPadding] / [actionsEndPadding] 默认对齐项目页面基准；首页需要与自身 16dp 内容边距
 * 对齐时单独传入，不影响其它页面。
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = Dimens.PageHorizontal,
    actionsEndPadding: Dp = 8.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = horizontalPadding,
                end = actionsEndPadding,
                top = 16.dp,
                bottom = 8.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // 作为页面级标题，供屏幕阅读器按标题跳转
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        actions()
    }
}
