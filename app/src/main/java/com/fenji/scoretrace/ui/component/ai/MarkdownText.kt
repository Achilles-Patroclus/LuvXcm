package com.fenji.scoretrace.ui.component.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fenji.scoretrace.ui.theme.ScoreTraceColors

/**
 * 轻量 Markdown 渲染。支持：加粗 / 斜体 / 行内代码、H1-H3 标题、有序与无序列表、
 * 表格（带边框与分隔线）、代码块（灰底等宽）、引用块、分隔线。
 *
 * 解析为「块列表」后逐块渲染；行内样式用 [AnnotatedString] 承载。解析结果按原文
 * [remember] 缓存，重组（如滚动、动画）时不会重复解析。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        blocks.forEach { block -> MarkdownBlock(block) }
    }
}

// ── 块数据模型 ────────────────────────────────────────────────

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Numbered(val number: String, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Code(val code: String) : MdBlock
    data class Table(val header: List<String>, val rows: List<List<String>>) : MdBlock
    data object Divider : MdBlock
}

private val HEADING_REGEX = Regex("^(#{1,6})\\s+(.*)$")
private val BULLET_REGEX = Regex("^[-*+]\\s+(.*)$")
private val NUMBERED_REGEX = Regex("^(\\d{1,3})[.)]\\s+(.*)$")
private val SEPARATOR_CELL_REGEX = Regex("^:?-+:?$")

private val BoldStyle = SpanStyle(fontWeight = FontWeight.Bold)
private val ItalicStyle = SpanStyle(fontStyle = FontStyle.Italic)
private val InlineCodeStyle = SpanStyle(
    fontFamily = FontFamily.Monospace,
    background = Color(0xFFF0F1F4),
    color = Color(0xFFB4285C),
)

// ── 解析 ─────────────────────────────────────────────────────

private fun parseMarkdown(source: String): List<MdBlock> {
    if (source.isBlank()) return emptyList()
    val lines = source.replace("\r\n", "\n").replace('\r', '\n').split('\n')
    val blocks = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        val text = paragraph.toString().trim()
        if (text.isNotEmpty()) blocks += MdBlock.Paragraph(text)
        paragraph.clear()
    }

    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()

        // 代码块：``` 到下一个 ```（未闭合则吃到结尾）
        if (trimmed.startsWith("```")) {
            flushParagraph()
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                if (code.isNotEmpty()) code.append('\n')
                code.append(lines[i])
                i++
            }
            if (i < lines.size) i++ // 跳过结束围栏
            blocks += MdBlock.Code(code.toString())
            continue
        }

        if (trimmed.isEmpty()) {
            flushParagraph()
            i++
            continue
        }

        if (isHorizontalRule(trimmed)) {
            flushParagraph()
            blocks += MdBlock.Divider
            i++
            continue
        }

        val heading = HEADING_REGEX.find(trimmed)
        if (heading != null) {
            flushParagraph()
            val level = heading.groupValues[1].length.coerceAtMost(3)
            blocks += MdBlock.Heading(level, heading.groupValues[2].trim())
            i++
            continue
        }

        // 表格：当前行是表头且下一行是分隔行
        val headerCells = parseTableRow(trimmed)
        val nextTrimmed = lines.getOrNull(i + 1)?.trim()
        if (headerCells != null && nextTrimmed != null && isTableSeparator(nextTrimmed)) {
            flushParagraph()
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size) {
                val rowLine = lines[i].trim()
                if (rowLine.isEmpty()) break
                if (isTableSeparator(rowLine)) {
                    i++
                    continue
                }
                val cells = parseTableRow(rowLine) ?: break
                rows += cells
                i++
            }
            blocks += MdBlock.Table(headerCells, rows)
            continue
        }

        // 引用：连续 > 行合并为一块
        if (trimmed.startsWith(">")) {
            flushParagraph()
            val quote = StringBuilder()
            while (i < lines.size && lines[i].trim().startsWith(">")) {
                if (quote.isNotEmpty()) quote.append('\n')
                quote.append(lines[i].trim().removePrefix(">").trim())
                i++
            }
            blocks += MdBlock.Quote(quote.toString())
            continue
        }

        val bullet = BULLET_REGEX.find(trimmed)
        if (bullet != null) {
            flushParagraph()
            blocks += MdBlock.Bullet(bullet.groupValues[1].trim())
            i++
            continue
        }

        val numbered = NUMBERED_REGEX.find(trimmed)
        if (numbered != null) {
            flushParagraph()
            blocks += MdBlock.Numbered(numbered.groupValues[1], numbered.groupValues[2].trim())
            i++
            continue
        }

        // 普通段落：连续正文行合并为一块
        if (paragraph.isNotEmpty()) paragraph.append('\n')
        paragraph.append(trimmed)
        i++
    }
    flushParagraph()
    return blocks
}

private fun isHorizontalRule(line: String): Boolean {
    val s = line.replace(" ", "")
    if (s.length < 3) return false
    return s.all { it == '-' } || s.all { it == '*' } || s.all { it == '_' }
}

private fun parseTableRow(line: String): List<String>? {
    if (!line.contains('|')) return null
    var s = line.trim()
    if (s.startsWith("|")) s = s.substring(1)
    if (s.endsWith("|")) s = s.dropLast(1)
    return s.split('|').map { it.trim() }
}

private fun isTableSeparator(line: String): Boolean {
    val cells = parseTableRow(line) ?: return false
    return cells.isNotEmpty() && cells.all { SEPARATOR_CELL_REGEX.matches(it) }
}

// ── 行内样式 ─────────────────────────────────────────────────

/**
 * 单遍扫描构造行内 [AnnotatedString]，用深度计数天然支持嵌套（如 `**粗 *斜* 粗**`）。
 * 未闭合的标记不会抛异常：对应的深度保持开启直到文本结束。
 */
private fun buildInline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    var bold = 0
    var italic = 0
    val buffer = StringBuilder()

    fun styleFor(): SpanStyle {
        var style = SpanStyle()
        if (bold > 0) style = style.merge(BoldStyle)
        if (italic > 0) style = style.merge(ItalicStyle)
        return style
    }

    fun flush() {
        if (buffer.isNotEmpty()) {
            withStyle(styleFor()) { append(buffer.toString()) }
            buffer.clear()
        }
    }

    while (i < text.length) {
        val ch = text[i]
        if (ch == '`') {
            val end = text.indexOf('`', i + 1)
            if (end > i) {
                flush()
                withStyle(InlineCodeStyle) { append(text.substring(i + 1, end)) }
                i = end + 1
                continue
            }
        }
        if (i + 1 < text.length) {
            val two = text.substring(i, i + 2)
            if (two == "**" || two == "__") {
                flush()
                if (bold > 0) bold-- else bold++
                i += 2
                continue
            }
        }
        if (ch == '*' || ch == '_') {
            flush()
            if (italic > 0) italic-- else italic++
            i++
            continue
        }
        buffer.append(ch)
        i++
    }
    flush()
}

// ── 渲染 ─────────────────────────────────────────────────────

@Composable
private fun MarkdownBlock(block: MdBlock) {
    when (block) {
        is MdBlock.Heading -> Text(
            text = remember(block.text) { buildInline(block.text) },
            fontSize = when (block.level) {
                1 -> 19.sp
                2 -> 17.sp
                else -> 15.sp
            },
            fontWeight = FontWeight.Bold,
            lineHeight = when (block.level) {
                1 -> 26.sp
                2 -> 23.sp
                else -> 21.sp
            },
            color = ScoreTraceColors.TextPrimaryLight,
        )

        is MdBlock.Paragraph -> Text(
            text = remember(block.text) { buildInline(block.text) },
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = ScoreTraceColors.TextPrimaryLight,
        )

        is MdBlock.Bullet -> ListRow(marker = "•", text = block.text)
        is MdBlock.Numbered -> ListRow(marker = "${block.number}.", text = block.text)
        is MdBlock.Quote -> QuoteBlock(block.text)
        is MdBlock.Code -> CodeBlock(block.code)
        is MdBlock.Table -> MarkdownTable(block)
        MdBlock.Divider -> HorizontalDivider(
            thickness = 1.dp,
            color = ScoreTraceColors.CardBorderLight,
        )
    }
}

@Composable
private fun ListRow(marker: String, text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = marker,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            modifier = Modifier.widthIn(min = 22.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = remember(text) { buildInline(text) },
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = ScoreTraceColors.TextPrimaryLight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QuoteBlock(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(ScoreTraceColors.BrandPrimary.copy(alpha = 0.45f)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = remember(text) { buildInline(text) },
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = ScoreTraceColors.TextSecondaryLight,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF3F4F6))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Color(0xFF1F2937),
        )
    }
}

@Composable
private fun MarkdownTable(block: MdBlock.Table) {
    val colCount = maxOf(block.header.size, block.rows.maxOfOrNull { it.size } ?: 0).coerceAtLeast(1)
    val border = ScoreTraceColors.CardBorderLight
    // 表格按容器宽度均分列宽。不可套 horizontalScroll：横向无界约束会让 weight 列宽塔陷成一条竖线。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, border, RoundedCornerShape(8.dp)),
    ) {
        Column {
            TableRow(cells = block.header, colCount = colCount, isHeader = true, border = border, showTop = false)
            block.rows.forEach { row ->
                TableRow(cells = row, colCount = colCount, isHeader = false, border = border, showTop = true)
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    colCount: Int,
    isHeader: Boolean,
    border: Color,
    showTop: Boolean,
) {
    Column {
        if (showTop) HorizontalDivider(thickness = 1.dp, color = border)
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            for (column in 0 until colCount) {
                if (column > 0) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(border),
                    )
                }
                val inline = remember(cells.getOrNull(column)) { buildInline(cells.getOrNull(column).orEmpty()) }
                Text(
                    text = inline,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
                    color = ScoreTraceColors.TextPrimaryLight,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isHeader) Color(0xFFF6F8FC) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}
