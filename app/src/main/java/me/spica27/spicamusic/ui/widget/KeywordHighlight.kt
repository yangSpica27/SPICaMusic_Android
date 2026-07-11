package me.spica27.spicamusic.ui.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * 把命中关键词的部分用主题色加粗高亮（忽略大小写，多次命中全部高亮）。
 * 搜索类列表（全局搜索、歌单内搜索）共享的文本工具。
 */
@Composable
fun highlightKeyword(
    text: String,
    keyword: String,
): AnnotatedString =
    buildAnnotatedString {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) {
            append(text)
            return@buildAnnotatedString
        }
        val highlightStyle =
            SpanStyle(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        var start = 0
        while (start < text.length) {
            val hit = text.indexOf(trimmed, startIndex = start, ignoreCase = true)
            if (hit < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, hit))
            withStyle(highlightStyle) {
                append(text.substring(hit, hit + trimmed.length))
            }
            start = hit + trimmed.length
        }
    }
