package me.spica27.spicamusic.storage.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.IAlbumRepository
import me.spica27.spicamusic.storage.impl.dao.AlbumDao
import me.spica27.spicamusic.storage.impl.mapper.toCommon

class AlbumRepositoryImpl(
    private val albumDao: AlbumDao
) : IAlbumRepository {

    companion object {
        private const val PAGE_SIZE = 30
        private const val PREFETCH_DISTANCE = 10
    }

    override fun getAllPagingFlow(): Flow<PagingData<Album>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { albumDao.getAllPaging() }
    ).flow.map { pagingData ->
        pagingData.map { it.toCommon() }
    }

    override fun getFilterAlbumsFlow(filter: String): Flow<PagingData<Album>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { albumDao.getFilteredPaging(filter) }
    ).flow.map { pagingData ->
        pagingData.map { it.toCommon() }
    }

    override fun getAlbumSongsFlow(albumId: String): Flow<List<Song>> {
        return albumDao.getAlbumSongsFlow(albumId).map { songEntities ->
            songEntities.map { it.toCommon() }
        }
    }
}