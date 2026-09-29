package me.spica27.spicamusic.feature.transfer.data

import androidx.room.withTransaction
import me.spica27.spicamusic.storage.api.StoredLyrics
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.entity.TransferIdentityEntity
import java.util.UUID

internal const val TRACK_IDENTITY = "track"
internal const val PLAYLIST_IDENTITY = "playlist"

internal data class LocalLibrary(
    val songs: List<SongEntity>,
    val playlists: List<PlaylistEntity>,
    val entries: List<PlaylistSongCrossRefEntity>,
    val lyrics: Map<Long, ExtraInfoEntity>,
    val identities: List<TransferIdentityEntity>,
) {
    private val canonicalIds = identities.groupBy { it.kind to it.localId }.mapValues { it.value.first().stableId }

    fun id(
        kind: String,
        localId: Long,
    ): String = canonicalIds.getValue(kind to localId)
}

internal suspend fun AppDatabase.readTransferSnapshot(assignIdentities: Boolean = false): LocalLibrary =
    withTransaction {
        val dao = transferDao()
        val songs = dao.songs()
        val playlists = dao.playlists()
        val identities = dao.identities().toMutableList()
        if (assignIdentities) {
            val existing = identities.map { it.kind to it.localId }.toSet()
            val missing =
                songs.map { TRACK_IDENTITY to requireNotNull(it.songId) } +
                    playlists.map { PLAYLIST_IDENTITY to requireNotNull(it.playlistId) }
            missing.filterNot { it in existing }.forEach { (kind, localId) ->
                val identity = TransferIdentityEntity(kind, UUID.randomUUID().toString(), localId)
                dao.putIdentity(identity)
                identities.add(identity)
            }
        }
        LocalLibrary(songs, playlists, dao.entries(), dao.lyrics().associateBy { it.mediaId }, identities)
    }

internal fun ExtraInfoEntity.stored() =
    StoredLyrics(
        mediaId,
        lyrics,
        cover,
        delay,
        lyricSourceName,
        sourceType,
        isManual,
        sourceUri,
        restoredSnapshot,
        lyricsSuppressed,
    )
