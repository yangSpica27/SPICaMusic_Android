package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 串行执行扫描与导入，避免歌曲映射过期。 */
class LibraryAccessGate {
    private val mutex = Mutex()

    suspend fun <T> withAccess(block: suspend () -> T): T = mutex.withLock { block() }
}
