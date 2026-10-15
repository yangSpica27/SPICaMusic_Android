package me.spica27.spicamusic.storage.api

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Song

/**
 * 专辑仓库接口。专辑由应用按文件标签建立，只列出至少有一首未忽略歌曲的专辑。
 */
interface IAlbumRepository {

    /**
     * 获取所有专辑的 Flow，包含分页数据
     */
    fun getAllPagingFlow(): Flow<PagingData<Album>>

    /**
     * 根据过滤条件（专辑名 / 专辑艺人）获取专辑的 Flow，包含分页数据
     */
    fun getFilterAlbumsFlow(filter: String): Flow<PagingData<Album>>

    /**
     * 全部专辑（不分页），供曲库页在内存中排序、筛选
     */
    fun getAllAlbumsFlow(): Flow<List<Album>>

    /**
     * 单张专辑；专辑已不存在时为 null
     */
    fun getAlbumFlow(albumId: Long): Flow<Album?>

    /**
     * 专辑内的歌曲，按碟号、曲号排序
     */
    fun getAlbumSongsFlow(albumId: Long): Flow<List<Song>>

    /**
     * 艺人的专辑（主要作品：该艺人是专辑艺人），按年份倒序
     */
    fun getAlbumsByArtistFlow(artistId: Long): Flow<List<Album>>

    /**
     * 艺人参与的专辑：不是专辑艺人，但在该专辑有自己的演唱曲目
     */
    fun getAppearancesFlow(artistId: Long): Flow<List<Album>>

    /**
     * 同一专辑艺人的其他专辑，用于详情页"来自某歌手的其他内容"
     */
    fun getRelatedAlbumsFlow(albumId: Long): Flow<List<Album>>

}
