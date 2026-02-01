package me.spica27.spicamusic.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 从 URI 提取主色调
 */
suspend fun extractDominantColorFromUri(
    context: Context,
    uri: Uri?,
    fallbackColor: Color = Color(0xFF2196F3),
): Color =
    withContext(Dispatchers.IO) {
        if (uri == null) return@withContext fallbackColor

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                val palette = Palette.from(bitmap).generate()
                bitmap.recycle()

                // 优先使用鲜艳色，其次是主色
                val dominantSwatch =
                    palette.vibrantSwatch
                        ?: palette.dominantSwatch
                        ?: palette.mutedSwatch

                dominantSwatch?.let {
                    Color(it.rgb)
                } ?: fallbackColor
            } else {
                fallbackColor
            }
        } catch (e: Exception) {
            fallbackColor
        }
    }

/**
 * Composable 函数：记忆化的封面主色提取
 */
@Composable
fun rememberDominantColorFromUri(
    uri: Uri?,
    fallbackColor: Color = Color(0xFF2196F3),
): Color {
    val context = LocalContext.current
    var dominantColor by remember(uri) { mutableStateOf(fallbackColor) }

    LaunchedEffect(uri) {
        dominantColor = extractDominantColorFromUri(context, uri, fallbackColor)
    }

    return dominantColor
}

/**
 * 计算颜色亮度
 */
fun calculateLuminance(color: Color): Float {
    val red = color.red
    val green = color.green
    val blue = color.blue
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
