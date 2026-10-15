package me.spica27.spicamusic.storage.impl.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.ArtistCredit
import me.spica27.spicamusic.common.entity.ArtistRole
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.IArtistRepository
import me.spica27.spicamusic.storage.impl.dao.ArtistDao
import me.spica27.spicamusic.storage.impl.mapper.toCommon

class ArtistRepositoryImpl(
    private val artistDao: ArtistDao,
) : IArtistRepository {
    override fun getArtistsPagingFlow(keyword: String?): Flow<PagingData<Artist>> =
        Pager(
            config =
                PagingConfig(
                    pageSize = PAGE_SIZE,
                    prefetchDistance = PREFETCH_DISTANCE,
                    enablePlaceholders = false,
                ),
            pagingSourceFactory = { artistDao.getArtistsPaging(keyword) },
        ).flow.map { pagingData -> pagingData.map { it.toCommon() } }

    override fun getAllArtistsFlow(): Flow<List<Artist>> =
        artistDao.getAllArtistsFlow().map { rows -> rows.map { it.toCommon() } }.distinctUntilChanged()

    override fun getArtistSongsFlow(artistId: Long): Flow<List<Song>> =
        artistDao.getArtistSongsFlow(artistId).map { list -> list.map { it.toCommon() } }

    override fun getSongsByRoleFlow(
        artistId: Long,
        role: ArtistRole,
    ): Flow<List<Song>> = artistDao.getSongsByRoleFlow(artistId, role.name).map { list -> list.map { it.toCommon() } }

    override suspend fun getSongCredits(mediaStoreId: Long): List<ArtistCredit> =
        withContext(Dispatchers.IO) {
            artistDao.getSongCredits(mediaStoreId).map { it.toCommon() }
        }

    private companion object {
        const val PAGE_SIZE = 30
        const val PREFETCH_DISTANCE = 10
    }
}
