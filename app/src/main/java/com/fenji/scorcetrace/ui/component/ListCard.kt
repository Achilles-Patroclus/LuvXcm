package com.fenji.scorcetrace.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fenji.scorcetrace.ui.theme.Dimens
import com.fenji.scorcetrace.ui.theme.pressScale
import com.fenji.scorcetrace.ui.theme.rememberPressSource

/**
 * 统一列表卡片：左侧科目色条 + 中间标题/副标题/附加内容 + 右侧状态或操作。
 *
 * - [accent] 传 `Color(subject.color)` 即得到科目色条；不传则用主题主色。
 * - [onClick] / [onLongClick] 用 `combinedClickable` 承接，默认均为 null（不可点）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = Color.Unspecified,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    titleDecoration: TextDecoration? = null,
    titleMaxLines: Int = 2,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val resolvedAccent =
        if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else accent

    val card: @Composable () -> Unit = {
        ListCardSurface(
            title = title,
            subtitle = subtitle,
            accent = resolvedAccent,
            titleStyle = titleStyle,
            titleDecoration = titleDecoration,
            titleMaxLines = titleMaxLines,
            leading = leading,
            trailing = trailing,
            content = content,
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = modifier,
        )
    }

    card()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListCardSurface(
    title: String,
    subtitle: String?,
    accent: Color,
    titleStyle: TextStyle,
    titleDecoration: TextDecoration?,
    titleMaxLines: Int,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    content: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    modifier: Modifier,
) {
    val interaction = rememberPressSource()
    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = ripple(),
            onLongClick = onLongClick,
            onClick = { onClick?.invoke() },
        )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource = interaction)
            .then(clickModifier),
        shape = RoundedCornerShape(Dimens.ListCorner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 科目色条
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.45f))),
                    ),
            )
            Spacer(modifier = Modifier.width(12.dp))

            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(4.dp))
            }

            // 文字块合并朗读，右侧操作按钮保持独立可聚焦
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {},
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    textDecoration = titleDecoration,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = titleMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (content != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    content()
                }
            }

            if (trailing != null) {
                Spacer(modifier = Modifier.width(4.dp))
                trailing()
            }
        }
    }
}
