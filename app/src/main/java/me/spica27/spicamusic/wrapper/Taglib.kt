package me.spica27.spicamusic.wrapper

import androidx.annotation.Keep

// 参考LMusic https://github.com/cy745/lmusic
@Keep
object Taglib {

    external suspend fun retrieveMetadataWithFD(fileDescriptor: Int): Metadata?
    external suspend fun getLyricWithFD(fileDescriptor: Int): String?
    external suspend fun getPictureWithFD(fileDescriptor: Int): ByteArray?

    // TODO 加suspend 会异常
    external fun writeLyricInto(fileDescriptor: Int, lyric: String): Boolean
}


