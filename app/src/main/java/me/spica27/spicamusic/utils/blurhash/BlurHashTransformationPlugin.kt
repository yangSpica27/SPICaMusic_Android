package me.spica27.spicamusic.utils.blurhash

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import com.skydoves.landscapist.plugins.ImagePlugin
import com.skydoves.landscapist.transformation.blur.BlurTransformationPlugin
import timber.log.Timber

data class BlurHashTransformationPlugin(
    private val decodeWidth: Int = 32,
    private val decodeHeight: Int = 32,
    private val punch: Float = 1f,
    private val fallbackRadius: Int = 550,
) : ImagePlugin.PainterPlugin {
    private val fallbackPlugin = BlurTransformationPlugin(radius = fallbackRadius)

    @Composable
    override fun compose(
        imageBitmap: ImageBitmap,
        painter: Painter,
    ): Painter {
        val transformedPainter =
            remember(imageBitmap, decodeWidth, decodeHeight, punch) {
                getOrCreatePainter(imageBitmap)
            }

        return transformedPainter ?: fallbackPlugin.compose(imageBitmap, painter)
    }

    private fun getOrCreatePainter(imageBitmap: ImageBitmap): Painter? {
        val cacheKey = imageBitmap.cacheKey()
        synchronized(cache) {
            cache.get(cacheKey)?.let { return it }
        }

        val painter =
            runCatching {
                val sourceBitmap = imageBitmap.asAndroidBitmap().asReadableBitmap()
                val blurHash = BlurHashEncoder.encode(sourceBitmap)
                val decodedBitmap =
                    BlurHashDecoder.decode(
                        blurHash = blurHash,
                        width = decodeWidth,
                        height = decodeHeight,
                        punch = punch,
                    )
                decodedBitmap?.let { BitmapPainter(it.asImageBitmap()) }
            }.onFailure { throwable ->
                Timber
                    .tag("BlurHashTransformation")
                    .w(throwable, "Failed to create blurhash background, falling back to blur transformation")
            }.getOrNull()

        if (painter != null) {
            synchronized(cache) {
                cache.put(cacheKey, painter)
            }
        }

        return painter
    }

    private fun ImageBitmap.cacheKey(): String = "${System.identityHashCode(this)}-$width-$height-$decodeWidth-$decodeHeight-$punch"

    private fun Bitmap.asReadableBitmap(): Bitmap =
        if (config == Bitmap.Config.ARGB_8888 && !isRecycled) {
            this
        } else {
            copy(Bitmap.Config.ARGB_8888, false)
        }

    private companion object {
        private const val CACHE_SIZE = 4
        private val cache = LruCache<String, Painter>(CACHE_SIZE)
    }
}
