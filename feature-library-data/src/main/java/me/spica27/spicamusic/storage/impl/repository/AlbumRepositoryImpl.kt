package me.spica27.spicamusic.storage.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    override fun getAllPagingFlow(): Flow<PagingData<Album>> = pagedAlbums(null)

    override fun getFilterAlbumsFlow(filter: String): Flow<PagingData<Album>> = pagedAlbums(filter)

    private fun pagedAlbums(keyword: String?): Flow<PagingData<Album>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { albumDao.getAlbumsPaging(keyword) }
    ).flow.map { pagingData ->
        pagingData.map { it.toCommon() }
    }

    override fun getAllAlbumsFlow(): Flow<List<Album>> =
        albumDao.getAllAlbumsFlow().map { rows -> rows.map { it.toCommon() } }.distinctUntilChanged()

    override fun getAlbumFlow(albumId: Long): Flow<Album?> =
        albumDao.getAlbumFlow(albumId).map { it?.toCommon() }.distinctUntilChanged()

    override fun getAlbumSongsFlow(albumId: Long): Flow<List<Song>> =
        albumDao.getAlbumSongsFlow(albumId).map { songEntities ->
            songEntities.map { it.toCommon() }
        }

    override fun getAlbumsByArtistFlow(artistId: Long): Flow<List<Album>> =
        albumDao.getAlbumsByArtistFlow(artistId).map { rows -> rows.map { it.toCommon() } }.distinctUntilChanged()

    override fun getAppearancesFlow(artistId: Long): Flow<List<Album>> =
        albumDao.getAppearancesFlow(artistId).map { rows -> rows.map { it.toCommon() } }.distinctUntilChanged()

    override fun getRelatedAlbumsFlow(albumId: Long): Flow<List<Album>> =
        albumDao.getRelatedAlbumsFlow(albumId).map { rows -> rows.map { it.toCommon() } }.distinctUntilChanged()
}
