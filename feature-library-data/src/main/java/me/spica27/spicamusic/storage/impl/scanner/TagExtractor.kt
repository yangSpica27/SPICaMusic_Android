package me.spica27.spicamusic.storage.impl.scanner

import android.content.ContentResolver
import android.net.Uri
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.TagLib
import timber.log.Timber

/** TagLib 读取结果；读取失败的部分为 null */
data class ExtractedTags(
    val properties: Map<String, List<String>>?,
    val audio: ExtractedAudio?,
)

data class ExtractedAudio(
    val durationMs: Long,
    val sampleRate: Int,
    val bitRate: Int,
    val channels: Int,
)

/** 按 content URI 读取完整标签与音频参数，只打开一次文件 */
class TagExtractor(
    private val resolver: ContentResolver,
) {
    fun extract(uri: Uri): ExtractedTags {
        var properties: Map<String, List<String>>? = null
        var audio: ExtractedAudio? = null
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                // dupPfd 包裹在 use{} 中：detachFd() 之前出错时由 use{} 关闭复制出来的 fd；
                // detachFd() 后 fd 所有权转交 TagLib，由它负责关闭
                pfd.dup().use { dupPfd ->
                    val metadataFd = pfd.detachFd()
                    val audioFd = dupPfd.detachFd()
                    try {
                        properties =
                            TagLib
                                .getMetadata(fd = metadataFd, readPictures = false)
                                ?.propertyMap
                                ?.mapValues { (_, values) -> values.toList() }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "标签读取失败: $uri")
                    }
                    try {
                        audio =
                            TagLib.getAudioProperties(audioFd, readStyle = AudioPropertiesReadStyle.Accurate)?.let {
                                ExtractedAudio(
                                    durationMs = it.length.toLong(),
                                    sampleRate = it.sampleRate,
                                    bitRate = it.bitrate * 1000, // kbps → bps
                                    channels = it.channels,
                                )
                            }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "音频参数读取失败: $uri")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "无法打开音频文件: $uri")
        }
        return ExtractedTags(properties, audio)
    }

    private companion object {
        const val TAG = "TagExtractor"
    }
}
