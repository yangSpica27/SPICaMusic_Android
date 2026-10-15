package me.spica27.spicamusic.storage.impl.mapper

import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.ArtistCredit
import me.spica27.spicamusic.common.entity.Lyric
import me.spica27.spicamusic.common.entity.PlayHistory
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.PlaylistWithSongs
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.impl.dao.AlbumDao
import me.spica27.spicamusic.storage.impl.dao.ArtistDao
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistWithSongsEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.index.TagJson

fun SongEntity.toCommon() = Song(
    songId = songId,
    mediaStoreId = mediaStoreId,
    path = path,
    displayName = displayName,
    artist = artist,
    size = size,
    like = like,
    duration = duration,
    sort = sort,
    sortName = sortName,
    mimeType = mimeType,
    albumId = albumId,
    sampleRate = sampleRate,
    bitRate = bitRate,
    channels = channels,
    digit = digit,
    isIgnore = isIgnore,
    codec = codec,
    album = album,
    waveformData = waveformData,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    composer = TagJson.decode(composerTags).joinToString(" / "),
    lyricist = TagJson.decode(lyricistTags).joinToString(" / "),
)

fun AlbumDao.AlbumRow.toCommon() = Album(
    id = album.albumId,
    title = album.title,
    artist = album.artist,
    year = album.year,
    numberOfSongs = songCount,
    coverMediaStoreId = coverMediaStoreId ?: 0L,
)

fun ArtistDao.ArtistRow.toCommon() = Artist(
    artistId = artist.artistId,
    name = artist.name,
    songCount = songCount,
    coverMediaStoreId = coverMediaStoreId ?: 0L,
    kind = artist.kind,
)

fun ArtistDao.CreditRow.toCommon() = ArtistCredit(
    artist =
        Artist(
            artistId = artist.artistId,
            name = artist.name,
            songCount = songCount,
            coverMediaStoreId = coverMediaStoreId ?: 0L,
            kind = artist.kind,
        ),
    role = role,
)

fun PlaylistEntity.toCommon() = Playlist(
    playlistId = playlistId,
    playlistName = playlistName,
    cover = cover,
    createTimestamp = createTimestamp,
    playTimes = playTimes,
    needUpdate = needUpdate,
)

fun Playlist.toEntity() = PlaylistEntity(
    playlistId = playlistId,
    playlistName = playlistName,
    cover = cover,
    createTimestamp = createTimestamp,
    playTimes = playTimes,
    needUpdate = needUpdate,
)

fun ExtraInfoEntity.toCommon() = Lyric(
    songId = mediaId,
    lyricContent = lyrics,
    translatedLyric = null,
    source = null,
)

fun Lyric.toEntity() = ExtraInfoEntity(
    mediaId = songId,
    lyrics = lyricContent,
    cover = "",
    delay = 0,
)

fun PlayHistoryEntity.toCommon() = PlayHistory(
    id = if (id == 0L) null else id,
    songId = mediaId,
    playTime = time,
    playCount = 1,

    userId = null,
    sessionId = if (sessionId.isBlank()) null else sessionId,
    deviceId = if (deviceId.isBlank()) null else deviceId,
    duration = duration,
    playedDuration = playedDuration,
    position = position,
    actionType = actionType,
    contextType = contextType,
    contextId = if (contextId == 0L) null else contextId,
    isCompleted = isCompleted,
    source = source,
    extra = extra,
)

fun PlayHistory.toEntity() = PlayHistoryEntity(
    id = id ?: 0,
    mediaId = songId,
    title = "",
    artist = "",
    album = "",
    duration = duration,
    playedDuration = playedDuration,
    position = position,
    actionType = actionType,
    contextType = contextType,
    contextId = contextId ?: 0,
    sessionId = sessionId ?: "",
    deviceId = deviceId ?: "",
    source = source,
    isCompleted = isCompleted,
    extra = extra,
    time = playTime,
)

fun PlaylistWithSongsEntity.toCommon() = PlaylistWithSongs(
    playlist = playlist.toCommon(),
    songs = songs.map { it.toCommon() },
)
