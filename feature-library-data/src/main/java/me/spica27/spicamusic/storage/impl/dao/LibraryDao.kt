package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import me.spica27.spicamusic.storage.impl.entity.AlbumArtistEntity
import me.spica27.spicamusic.storage.impl.entity.AlbumEntity
import me.spica27.spicamusic.storage.impl.entity.ArtistEntity
import me.spica27.spicamusic.storage.impl.entity.LibraryStateEntity
import me.spica27.spicamusic.storage.impl.entity.SongArtistEntity

/** 曲库写入专用（LibraryWriter），调用方负责事务 */
@Dao
interface LibraryDao {
    // ===== 曲库状态 =====

    @Query("SELECT * FROM LibraryState WHERE id = 1")
    suspend fun state(): LibraryStateEntity?

    @Insert
    suspend fun insertState(state: LibraryStateEntity)

    @Query("UPDATE LibraryState SET indexPolicy = :policy WHERE id = 1")
    suspend fun updateIndexPolicy(policy: String): Int

    @Query("SELECT EXISTS(SELECT 1 FROM PlaylistSongCrossRef)")
    suspend fun hasPlaylistEntries(): Boolean

    /**
     * 是否有歌曲还没建立关系。重建后每首歌至少有一条演唱署名（缺失时为未知艺人），
     * 所以没有署名就说明上次分批提交后被取消或进程退出，关系重建没跑到。
     */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM Song AS s WHERE NOT EXISTS (SELECT 1 FROM SongArtist AS sa WHERE sa.songId = s.songId))",
    )
    suspend fun hasUnindexedSongs(): Boolean

    // ===== 索引输入 =====

    data class IndexRow(
        val songId: Long,
        val path: String,
        val album: String,
        val artistTags: String,
        val albumArtistTags: String,
        val composerTags: String,
        val lyricistTags: String,
        val year: Int,
        val isCompilation: Boolean,
        val albumId: Long?,
    )

    @Query(
        "SELECT songId, path, album, artistTags, albumArtistTags, composerTags, lyricistTags, year, isCompilation, albumId FROM Song",
    )
    suspend fun indexRows(): List<IndexRow>

    // ===== 艺人 / 专辑 =====

    @Query("SELECT * FROM Artist")
    suspend fun artists(): List<ArtistEntity>

    @Insert
    suspend fun insertArtists(artists: List<ArtistEntity>): List<Long>

    @Update
    suspend fun updateArtists(artists: List<ArtistEntity>)

    @Query("SELECT * FROM Album")
    suspend fun albums(): List<AlbumEntity>

    @Insert
    suspend fun insertAlbums(albums: List<AlbumEntity>): List<Long>

    @Update
    suspend fun updateAlbums(albums: List<AlbumEntity>)

    @Query("UPDATE Song SET albumId = :albumId WHERE songId = :songId")
    suspend fun setSongAlbum(
        songId: Long,
        albumId: Long?,
    )

    // ===== 署名关系 =====

    @Query("SELECT * FROM SongArtist")
    suspend fun songArtists(): List<SongArtistEntity>

    @Upsert
    suspend fun upsertSongArtists(credits: List<SongArtistEntity>)

    @Delete
    suspend fun deleteSongArtists(credits: List<SongArtistEntity>)

    @Query("SELECT * FROM AlbumArtist")
    suspend fun albumArtists(): List<AlbumArtistEntity>

    @Upsert
    suspend fun upsertAlbumArtists(credits: List<AlbumArtistEntity>)

    @Delete
    suspend fun deleteAlbumArtists(credits: List<AlbumArtistEntity>)

    @Query("DELETE FROM Album WHERE NOT EXISTS (SELECT 1 FROM Song AS s WHERE s.albumId = Album.albumId)")
    suspend fun deleteOrphanAlbums()

    @Query(
        """
        DELETE FROM Artist
        WHERE NOT EXISTS (SELECT 1 FROM SongArtist AS sa WHERE sa.artistId = Artist.artistId)
          AND NOT EXISTS (SELECT 1 FROM AlbumArtist AS aa WHERE aa.artistId = Artist.artistId)
        """,
    )
    suspend fun deleteOrphanArtists()

    // ===== v22 → v23 重建 =====

    @Query("SELECT mediaStoreId FROM LegacySongCarryOver WHERE isImported = 1")
    suspend fun carriedImportedIds(): List<Long>

    /** 每批歌曲写入后在同一事务执行；幂等 */
    @Query(
        """
        UPDATE Song SET `like` = 1
        WHERE `like` = 0 AND mediaStoreId IN (SELECT mediaStoreId FROM LegacySongCarryOver WHERE isLiked = 1)
        """,
    )
    suspend fun applyCarriedLikes()

    @Query(
        """
        UPDATE Song SET isImported = 1
        WHERE isImported = 0 AND mediaStoreId IN (SELECT mediaStoreId FROM LegacySongCarryOver WHERE isImported = 1)
        """,
    )
    suspend fun applyCarriedImports()

    @Query(
        """
        UPDATE Playlist SET needUpdate = 1
        WHERE playlistId IN (
            SELECT DISTINCT playlistId FROM PlaylistSongCrossRef
            WHERE mediaId NOT IN (SELECT mediaStoreId FROM Song))
        """,
    )
    suspend fun markPlaylistsWithMissingSongs()

    @Query("DELETE FROM PlaylistSongCrossRef WHERE mediaId NOT IN (SELECT mediaStoreId FROM Song)")
    suspend fun deleteMissingPlaylistEntries(): Int

    @Query("DELETE FROM LegacySongCarryOver")
    suspend fun clearCarryOver()

    @Query("UPDATE LibraryState SET pendingRebuild = 0 WHERE id = 1")
    suspend fun clearPendingRebuild()
}
