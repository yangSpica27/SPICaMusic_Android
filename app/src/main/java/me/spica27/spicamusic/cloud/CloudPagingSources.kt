package me.spica27.spicamusic.cloud

import androidx.paging.PagingSource
import androidx.paging.PagingState

class RemoteMusicPagingSource(
    private val clients: RemoteMusicClientRegistry,
    private val account: RemoteMusicAccount,
    private val query: String,
) : PagingSource<Int, RemoteSong>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, RemoteSong> =
        try {
            val offset = params.key ?: 0
            val page =
                clients.listSongs(
                    account = account,
                    query = query,
                    offset = offset,
                    limit = params.loadSize.coerceIn(1, PAGE_SIZE),
                )
            LoadResult.Page(
                data = page.songs,
                prevKey = null,
                nextKey = page.nextOffset,
            )
        } catch (error: Throwable) {
            LoadResult.Error(error)
        }

    override fun getRefreshKey(state: PagingState<Int, RemoteSong>): Int? = 0

    companion object {
        const val PAGE_SIZE = 80
    }
}

class MediaServerPagingSource(
    private val client: MediaServerClient,
    private val account: MediaServerAccount,
    private val query: String,
) : PagingSource<Int, CloudSong>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CloudSong> {
        val start = params.key ?: 0
        return client
            .getSongs(
                account = account,
                startIndex = start,
                limit = params.loadSize.coerceIn(1, MediaServerClient.PAGE_SIZE),
                searchTerm = query,
            ).fold(
                onSuccess = { page ->
                    LoadResult.Page(
                        data = page.songs,
                        prevKey = null,
                        nextKey = page.nextStartIndex,
                        itemsBefore = start,
                        itemsAfter = (page.totalCount - start - page.songs.size).coerceAtLeast(0),
                    )
                },
                onFailure = { error -> LoadResult.Error<Int, CloudSong>(error) },
            )
    }

    override fun getRefreshKey(state: PagingState<Int, CloudSong>): Int? = null
}

class TelegramPagingSource(
    private val repository: TelegramRepository,
    private val chatId: Long,
) : PagingSource<Long, TelegramSong>() {
    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, TelegramSong> =
        runCatching {
            val page =
                repository.getAudioPage(
                    chatId = chatId,
                    fromMessageId = params.key ?: 0L,
                    limit = params.loadSize.coerceIn(1, TelegramRepository.PAGE_SIZE),
                )
            LoadResult.Page(
                data = page.songs,
                prevKey = null,
                nextKey = page.nextFromMessageId,
            )
        }.getOrElse { error -> LoadResult.Error<Long, TelegramSong>(error) }

    override fun getRefreshKey(state: PagingState<Long, TelegramSong>): Long? = null
}
