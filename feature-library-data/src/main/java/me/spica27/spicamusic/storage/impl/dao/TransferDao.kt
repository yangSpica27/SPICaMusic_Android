package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.entity.TransferIdentityEntity
import me.spica27.spicamusic.storage.impl.entity.TransferReceiptEntity

@Dao
interface TransferDao {
    @Query(
        """SELECT songId, mediaStoreId, path, displayName, artist, size, `like`, duration,
        sort, sortName, mimeType, albumId, album, sampleRate, bitRate, channels, digit,
        isIgnore, dateModified, codec, trackNumber, NULL AS waveformData FROM Song ORDER BY songId""",
    )
    suspend fun songs(): List<SongEntity>

    @Query("SELECT * FROM Playlist ORDER BY playlistId")
    suspend fun playlists(): List<PlaylistEntity>

    @Query("SELECT * FROM PlaylistSongCrossRef ORDER BY playlistId, sortOrder DESC, insertTime DESC, mediaId DESC")
    suspend fun entries(): List<PlaylistSongCrossRefEntity>

    @Query("SELECT * FROM extra_info")
    suspend fun lyrics(): List<ExtraInfoEntity>

    @Query("SELECT * FROM TransferIdentity ORDER BY kind, stableId")
    suspend fun identities(): List<TransferIdentityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putIdentity(identity: TransferIdentityEntity)

    @Insert
    suspend fun insertSong(song: SongEntity): Long

    @Query("SELECT * FROM TransferReceipt WHERE taskId = :taskId")
    suspend fun receipt(taskId: String): TransferReceiptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReceipt(receipt: TransferReceiptEntity)
}
