package me.spica27.spicamusic.artwork

import android.content.Context
import com.skydoves.landscapist.core.Landscapist
import com.skydoves.landscapist.core.LandscapistConfig
import com.skydoves.landscapist.core.cache.DiskLruCache
import com.skydoves.landscapist.core.fetcher.AndroidContextProvider
import com.skydoves.landscapist.core.fetcher.AndroidFetchers
import com.skydoves.landscapist.core.network.CompositeFetcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

/**
 * 进程唯一的图片加载器：Compose、通知栏、MediaSession 共用同一套缓存。
 * 在 Application.onCreate 调用 [install]，之后通过 Landscapist.getInstance() 取用。
 *
 * 曲库封面只走内存缓存（来源在本机，落盘只是复制）；磁盘缓存仅供零散的普通 Uri 请求，
 * 上限调小后 DiskLruCache 打开时会顺带把旧版本留下的大条目淘汰掉。
 */
object ArtworkLoader {
    private const val DISK_CACHE_BYTES = 32L * 1024 * 1024
    private const val MAX_MEMORY_CACHE_BYTES = 256L * 1024 * 1024
    private const val TREE_ROOTS_TIMEOUT_MILLIS = 2_000L

    /** null 表示额外目录尚未加载；首个请求会等它就绪，避免早期请求以错误结果写入缓存 */
    private val treeRoots = MutableStateFlow<List<ArtworkTreeRoot>?>(null)

    /** 参与缓存键；额外目录变化后递增，让内存缓存与负缓存里以缩略图兜底的条目重新解析 */
    @Volatile
    var generation: Int = 0
        private set

    fun install(context: Context): Landscapist {
        val app = context.applicationContext
        AndroidContextProvider.initialize(app)
        val config =
            LandscapistConfig(
                memoryCacheSize = (Runtime.getRuntime().maxMemory() / 4).coerceAtMost(MAX_MEMORY_CACHE_BYTES),
                diskCacheSize = DISK_CACHE_BYTES,
            )
        val diskCache =
            DiskLruCache.create(
                directory = File(app.cacheDir, "landscapist_cache").toOkioPath(),
                maxSize = DISK_CACHE_BYTES,
                fileSystem = FileSystem.SYSTEM,
            )
        val fetcher =
            MusicArtworkFetcher(app) {
                withTimeoutOrNull(TREE_ROOTS_TIMEOUT_MILLIS) { treeRoots.filterNotNull().first() } ?: emptyList()
            }
        val landscapist =
            Landscapist
                .builder()
                .config(config)
                .diskCache(diskCache)
                .fetcher(CompositeFetcher(listOf(fetcher, AndroidFetchers.createDefault())))
                .build()
        Landscapist.setInstance(landscapist)
        return landscapist
    }

    fun updateTreeRoots(roots: List<ArtworkTreeRoot>) {
        val previous = treeRoots.value
        treeRoots.value = roots
        if (previous != null && previous != roots) generation++
    }
}
