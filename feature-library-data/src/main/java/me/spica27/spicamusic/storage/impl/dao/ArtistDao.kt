package me.spica27.spicamusic.storage.impl.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.common.entity.ArtistRole
import me.spica27.spicamusic.storage.impl.entity.ArtistEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity

/**
 * 艺人名下的歌曲 = 以演唱者署名的歌曲 ∪ 以专辑艺人身份发行的专辑曲目。
 * 只作词 / 作曲的艺人不进入艺人列表，可从歌曲详情进入。
 */
private const val ARTIST_SONG_LINKS = """
    SELECT artistId, songId FROM SongArtist WHERE role = 'ARTIST'
    UNION
    SELECT aa.artistId, ls.songId FROM AlbumArtist AS aa JOIN Song AS ls ON ls.albumId = aa.albumId
"""

private const val IS_ARTIST_SONG = """
    (EXISTS (SELECT 1 FROM SongArtist AS sa
             WHERE sa.songId = s.songId AND sa.artistId = :artistId AND sa.role = 'ARTIST')
     OR EXISTS (SELECT 1 FROM AlbumArtist AS aa WHERE aa.albumId = s.albumId AND aa.artistId = :artistId))
"""

private const val SONG_ORDER = "s.sortName = '#', s.sortName, s.displayName COLLATE NOCASE, s.songId"

@Dao
interface ArtistDao {
    data class ArtistRow(
        @Embedded val artist: ArtistEntity,
        val songCount: Int,
        val coverMediaStoreId: Long?,
    )

    data class CreditRow(
        @Embedded val artist: ArtistEntity,
        val role: ArtistRole,
        val creditOrder: Int,
        val songCount: Int,
        val coverMediaStoreId: Long?,
    )

    /** 普通艺人在前，群星 / 未知艺人在后；代表曲取 mediaStoreId 最小的一首 */
    @Query(
        """
        SELECT ar.*, COUNT(*) AS songCount, MIN(s.mediaStoreId) AS coverMediaStoreId
        FROM Artist AS ar
        JOIN ($ARTIST_SONG_LINKS) AS link ON link.artistId = ar.artistId
        JOIN Song AS s ON s.songId = link.songId AND s.isIgnore = 0
        WHERE :keyword IS NULL OR :keyword = '' OR ar.name LIKE '%' || :keyword || '%'
        GROUP BY ar.artistId
        ORDER BY ar.kind != 'NORMAL', ar.sortName = '#', ar.sortName, ar.name COLLATE NOCASE, ar.artistId
        """,
    )
    fun getArtistsPaging(keyword: String? = null): PagingSource<Int, ArtistRow>

    @Query(
        """
        SELECT ar.*, COUNT(*) AS songCount, MIN(s.mediaStoreId) AS coverMediaStoreId
        FROM Artist AS ar
        JOIN ($ARTIST_SONG_LINKS) AS link ON link.artistId = ar.artistId
        JOIN Song AS s ON s.songId = link.songId AND s.isIgnore = 0
        GROUP BY ar.artistId
        ORDER BY ar.kind != 'NORMAL', ar.sortName = '#', ar.sortName, ar.name COLLATE NOCASE, ar.artistId
        """,
    )
    fun getAllArtistsFlow(): Flow<List<ArtistRow>>

    /** 艺人名下的可见歌曲（演唱 + 专辑艺人） */
    @Query("SELECT s.* FROM Song AS s WHERE s.isIgnore = 0 AND $IS_ARTIST_SONG ORDER BY $SONG_ORDER")
    fun getArtistSongsFlow(artistId: Long): Flow<List<SongEntity>>

    /** 艺人以指定角色（作曲 / 作词）署名的可见歌曲 */
    @Query(
        """
        SELECT s.* FROM Song AS s
        WHERE s.isIgnore = 0 AND EXISTS (
            SELECT 1 FROM SongArtist AS sa
            WHERE sa.songId = s.songId AND sa.artistId = :artistId AND sa.role = :role)
        ORDER BY $SONG_ORDER
        """,
    )
    fun getSongsByRoleFlow(
        artistId: Long,
        role: String,
    ): Flow<List<SongEntity>>

    /** 一首歌的全部署名，供"查看艺人"选择 */
    @Query(
        """
        SELECT ar.*, sa.role, sa.creditOrder,
            (SELECT COUNT(*) FROM Song AS s WHERE s.isIgnore = 0 AND
                (EXISTS (SELECT 1 FROM SongArtist AS x
                         WHERE x.songId = s.songId AND x.artistId = ar.artistId AND x.role = 'ARTIST')
                 OR EXISTS (SELECT 1 FROM AlbumArtist AS y WHERE y.albumId = s.albumId AND y.artistId = ar.artistId))
            ) AS songCount,
            (SELECT MIN(s.mediaStoreId) FROM Song AS s
             JOIN SongArtist AS x ON x.songId = s.songId AND x.artistId = ar.artistId
             WHERE s.isIgnore = 0) AS coverMediaStoreId
        FROM SongArtist AS sa
        JOIN Artist AS ar ON ar.artistId = sa.artistId
        JOIN Song AS owner ON owner.songId = sa.songId
        WHERE owner.mediaStoreId = :mediaStoreId
        ORDER BY sa.role, sa.creditOrder
        """,
    )
    suspend fun getSongCredits(mediaStoreId: Long): List<CreditRow>
}
