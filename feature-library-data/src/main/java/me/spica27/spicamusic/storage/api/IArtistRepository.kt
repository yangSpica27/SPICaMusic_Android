package me.spica27.spicamusic.storage.api

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.ArtistCredit
import me.spica27.spicamusic.common.entity.ArtistRole
import me.spica27.spicamusic.common.entity.Song

/**
 * 艺人仓库接口。艺人由应用按文件标签建立；
 * 艺人列表只含演唱者与专辑艺人，只作词 / 作曲的艺人从歌曲署名进入。
 */
interface IArtistRepository {
    /**
     * 分页获取艺人列表（支持按名称过滤）
     */
    fun getArtistsPagingFlow(keyword: String? = null): Flow<PagingData<Artist>>

    /**
     * 全部艺人（不分页），供曲库页在内存中排序、筛选
     */
    fun getAllArtistsFlow(): Flow<List<Artist>>

    /**
     * 艺人名下的歌曲：演唱的歌曲及其作为专辑艺人的专辑曲目
     */
    fun getArtistSongsFlow(artistId: Long): Flow<List<Song>>

    /**
     * 艺人以作曲 / 作词身份署名的歌曲
     */
    fun getSongsByRoleFlow(
        artistId: Long,
        role: ArtistRole,
    ): Flow<List<Song>>

    /**
     * 一首歌的全部署名（按角色、署名顺序）
     */
    suspend fun getSongCredits(mediaStoreId: Long): List<ArtistCredit>
}
