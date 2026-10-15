package me.spica27.spicamusic.storage.impl.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.AlbumEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity

/** 专辑列 + 可见歌曲数 + 代表曲（首碟首曲）；只统计未忽略的歌曲 */
private const val ALBUM_COLUMNS = """
    a.*,
    (SELECT COUNT(*) FROM Song AS s WHERE s.albumId = a.albumId AND s.isIgnore = 0) AS songCount,
    (SELECT s.mediaStoreId FROM Song AS s WHERE s.albumId = a.albumId AND s.isIgnore = 0
     ORDER BY s.discNumber = 0, s.discNumber, s.trackNumber = 0, s.trackNumber, s.songId LIMIT 1) AS coverMediaStoreId
"""

/** 至少有一首未忽略的歌曲；全部被忽略的专辑不出现在列表中 */
private const val HAS_VISIBLE_SONG =
    "EXISTS (SELECT 1 FROM Song AS v WHERE v.albumId = a.albumId AND v.isIgnore = 0)"

/** 首字母排序，# 组排在最后 */
private const val ALBUM_ORDER = "a.sortName = '#', a.sortName, a.title COLLATE NOCASE, a.albumId"

@Dao
interface AlbumDao {
    data class AlbumRow(
        @Embedded val album: AlbumEntity,
        val songCount: Int,
        val coverMediaStoreId: Long?,
    )

    /** 专辑列表：只列出至少有一首可见歌曲的专辑 */
    @Query(
        """
        SELECT $ALBUM_COLUMNS FROM Album AS a
        WHERE $HAS_VISIBLE_SONG
          AND (:keyword IS NULL OR :keyword = ''
               OR a.title LIKE '%' || :keyword || '%' OR a.artist LIKE '%' || :keyword || '%')
        ORDER BY $ALBUM_ORDER
        """,
    )
    fun getAlbumsPaging(keyword: String? = null): PagingSource<Int, AlbumRow>

    @Query("SELECT $ALBUM_COLUMNS FROM Album AS a WHERE $HAS_VISIBLE_SONG ORDER BY $ALBUM_ORDER")
    fun getAllAlbumsFlow(): Flow<List<AlbumRow>>

    @Query("SELECT $ALBUM_COLUMNS FROM Album AS a WHERE a.albumId = :albumId")
    fun getAlbumFlow(albumId: Long): Flow<AlbumRow?>

    /** 专辑曲目：按碟号、曲号排序，未知序号排在后面 */
    @Query(
        """
        SELECT * FROM Song
        WHERE albumId = :albumId AND isIgnore = 0
        ORDER BY discNumber = 0, discNumber, trackNumber = 0, trackNumber, displayName COLLATE NOCASE, songId
        """,
    )
    fun getAlbumSongsFlow(albumId: Long): Flow<List<SongEntity>>

    /** 主要作品：该艺人是专辑艺人 */
    @Query(
        """
        SELECT $ALBUM_COLUMNS FROM Album AS a
        WHERE EXISTS (SELECT 1 FROM AlbumArtist AS aa WHERE aa.albumId = a.albumId AND aa.artistId = :artistId)
          AND $HAS_VISIBLE_SONG
        ORDER BY a.year DESC, a.sortName, a.albumId
        """,
    )
    fun getAlbumsByArtistFlow(artistId: Long): Flow<List<AlbumRow>>

    /** 参与作品：不是专辑艺人，但在该专辑有自己的可见演唱曲目 */
    @Query(
        """
        SELECT $ALBUM_COLUMNS FROM Album AS a
        WHERE NOT EXISTS (SELECT 1 FROM AlbumArtist AS aa WHERE aa.albumId = a.albumId AND aa.artistId = :artistId)
          AND EXISTS (
              SELECT 1 FROM Song AS v JOIN SongArtist AS sa ON sa.songId = v.songId
              WHERE v.albumId = a.albumId AND v.isIgnore = 0
                AND sa.artistId = :artistId AND sa.role = 'ARTIST')
        ORDER BY a.year DESC, a.sortName, a.albumId
        """,
    )
    fun getAppearancesFlow(artistId: Long): Flow<List<AlbumRow>>

    /** 同一专辑艺人的其他专辑；群星、未知艺人不算同一艺人 */
    @Query(
        """
        SELECT $ALBUM_COLUMNS FROM Album AS a
        WHERE a.albumId != :albumId
          AND EXISTS (
              SELECT 1 FROM AlbumArtist AS other
              JOIN AlbumArtist AS self ON self.artistId = other.artistId AND self.albumId = :albumId
              JOIN Artist AS ar ON ar.artistId = other.artistId
              WHERE other.albumId = a.albumId AND ar.kind = 'NORMAL')
          AND $HAS_VISIBLE_SONG
        ORDER BY a.year DESC, a.sortName, a.albumId
        """,
    )
    fun getRelatedAlbumsFlow(albumId: Long): Flow<List<AlbumRow>>
}
