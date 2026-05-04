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
            // 第一次 pass：仅读取图片尺寸，不分配像素内存
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            // 计算 inSampleSize，目标尺寸 128×128（调色板提取不需要高分辨率）
            val sampleSize = calculateInSampleSize(bounds, 128, 128)

            // 第二次 pass：按比例缩放解码，大幅降低内存占用
            val options =
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 // 节省一半内存
                }
            val bitmap =
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }

            if (bitmap != null) {
                val palette = Palette.from(bitmap).maximumColorCount(16).generate()
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

/** 计算合适的 inSampleSize，使解码后尺寸不超过 [reqWidth]×[reqHeight] */
private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val (height, width) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
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
