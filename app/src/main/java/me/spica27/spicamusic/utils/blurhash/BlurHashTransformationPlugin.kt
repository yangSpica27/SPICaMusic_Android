package me.spica27.spicamusic.utils.blurhash

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import com.skydoves.landscapist.plugins.ImagePlugin
import com.skydoves.landscapist.transformation.blur.BlurTransformationPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        val cacheKey =
            remember(imageBitmap, decodeWidth, decodeHeight, punch) {
                imageBitmap.cacheKey()
            }
        val cachedImage = remember(cacheKey) { getCachedImage(cacheKey) }
        val transformedImage by
            produceState<ImageBitmap?>(initialValue = cachedImage, key1 = cacheKey) {
                if (value == null) {
                    value =
                        withContext(Dispatchers.Default) {
                            getOrCreateImage(imageBitmap, cacheKey)
                        }
                }
            }
        val transformedPainter =
            transformedImage?.let { image ->
                remember(image) { BitmapPainter(image) }
            }

        return transformedPainter ?: fallbackPlugin.compose(imageBitmap, painter)
    }

    private fun getCachedImage(cacheKey: String): ImageBitmap? =
        synchronized(cache) {
            cache.get(cacheKey)
        }

    private fun getOrCreateImage(
        imageBitmap: ImageBitmap,
        cacheKey: String,
    ): ImageBitmap? {
        getCachedImage(cacheKey)?.let { return it }

        val transformedImage =
            runCatching {
                val sourceBitmap = imageBitmap.asAndroidBitmap().asReadableBitmap()
                val blurHash = BlurHashEncoder.encode(sourceBitmap)
                BlurHashDecoder
                    .decode(
                        blurHash = blurHash,
                        width = decodeWidth,
                        height = decodeHeight,
                        punch = punch,
                    )?.asImageBitmap()
            }.onFailure { throwable ->
                Timber
                    .tag("BlurHashTransformation")
                    .w(throwable, "Failed to create blurhash background, falling back to blur transformation")
            }.getOrNull()

        if (transformedImage != null) {
            synchronized(cache) {
                cache.put(cacheKey, transformedImage)
            }
        }

        return transformedImage
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
        private val cache = LruCache<String, ImageBitmap>(CACHE_SIZE)
    }
}
