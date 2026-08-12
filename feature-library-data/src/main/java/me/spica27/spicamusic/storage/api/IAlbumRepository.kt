package me.spica27.spicamusic.storage.api

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Song

/**
 * 专辑仓库接口
 * 提供专辑数据的增删改查操作
 */
interface IAlbumRepository {

    /**
     * 获取所有专辑的 Flow，包含分页数据
     */
    fun getAllPagingFlow(): Flow<PagingData<Album>>

    /**
     * 根据过滤条件获取专辑的 Flow，包含分页数据
     */
    fun getFilterAlbumsFlow(filter: String): Flow<PagingData<Album>>

    /**
     * 根据专辑ID获取专辑内的歌曲列表 Flow
     */
    fun getAlbumSongsFlow(albumId: String): Flow<List<Song>>

    /**
     * 获取同一歌手名下的全部专辑 Flow（按年份倒序），用于详情页"来自某歌手的其他内容"
     */
    fun getAlbumsByArtistFlow(artist: String): Flow<List<Album>>

}