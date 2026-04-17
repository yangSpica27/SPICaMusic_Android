package me.spica27.spicamusic.storage.impl.mapper

import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Lyric
import me.spica27.spicamusic.common.entity.PlayHistory
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.PlaylistWithSongs
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.impl.entity.AlbumEntity
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistWithSongsEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity

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
    codec = codec
)

fun AlbumEntity.toCommon() = Album(
    id = id,
    title = title,
    artist = artist,
    artworkUri = artworkUri,
    year = year,
    numberOfSongs = numberOfSongs,
    dateModified = dateModified,
)

fun Song.toEntity() = SongEntity(
    songId = songId,
    mediaStoreId = mediaStoreId,
    path = path,
    displayName = displayName,
    artist = artist,
    size = size,
    like = like,
    duration = duration,
    sort = sort,
    mimeType = mimeType,
    albumId = albumId,
    sampleRate = sampleRate,
    bitRate = bitRate,
    channels = channels,
    digit = digit,
    isIgnore = isIgnore,
    sortName = sortName,
    codec = codec
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
