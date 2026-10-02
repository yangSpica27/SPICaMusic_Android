package me.spica27.spicamusic.service

import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.SettableFuture
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.model.CachePolicy
import com.skydoves.landscapist.core.model.ImageResult
import me.spica27.spicamusic.artwork.artwork
import java.io.IOException

/**
 * MediaSession 的封面加载器：锁屏、系统媒体控件、Android Auto 走和应用内同一套解析与内存缓存。
 * 不写磁盘缓存：来源在本机，落盘只是复制。
 */
@UnstableApi
class ArtworkBitmapLoader(
    private val landscapist: Landscapist = Landscapist.getInstance(),
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = mimeType.startsWith("image/")

    /** 字节数组没有稳定的缓存键，解码一次即丢，避免每次调用都在缓存里留下新条目 */
    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = load(data, cacheInMemory = false)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = load(uri, cacheInMemory = true)

    override fun loadBitmapFromMetadata(metadata: MediaMetadata): ListenableFuture<Bitmap>? {
        metadata.artwork()?.let { return load(it, cacheInMemory = true) }
        return super.loadBitmapFromMetadata(metadata)
    }

    private fun load(
        model: Any,
        cacheInMemory: Boolean,
    ): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        val request =
            ImageRequest
                .builder()
                .model(model)
                .size(ARTWORK_EDGE, ARTWORK_EDGE)
                .diskCachePolicy(CachePolicy.DISABLED)
                .memoryCachePolicy(if (cacheInMemory) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .build()
        val disposable =
            landscapist.enqueue(request) { result ->
                when (result) {
                    is ImageResult.Success ->
                        if (result.isFinal) {
                            val bitmap = result.data as? Bitmap
                            if (bitmap != null) {
                                future.set(bitmap)
                            } else {
                                future.setException(IOException("Unexpected artwork type ${result.data::class.simpleName}"))
                            }
                        }

                    is ImageResult.Failure ->
                        future.setException(result.throwable ?: IOException(result.message ?: "Artwork load failed"))

                    ImageResult.Loading -> Unit
                }
            }
        future.addListener({ if (future.isCancelled) disposable.dispose() }, MoreExecutors.directExecutor())
        return future
    }

    private companion object {
        /** 锁屏与系统媒体控件实际渲染尺寸在 400 到 600 px 之间 */
        const val ARTWORK_EDGE = 640
    }
}
