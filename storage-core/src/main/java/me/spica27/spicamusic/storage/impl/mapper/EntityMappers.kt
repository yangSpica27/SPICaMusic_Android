package me.spica27.spicamusic.storage.impl.mapper

import me.spica27.spicamusic.common.entity.Lyric
import me.spica27.spicamusic.common.entity.PlayHistory
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.PlaylistWithSongs
import me.spica27.spicamusic.common.entity.Song
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
  sortName = sortName
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
  id = id,
  songId = mediaId,
  playTime = time,
  playCount = 1,
)

fun PlayHistory.toEntity() = PlayHistoryEntity(
  id = id ?: 0,
  mediaId = songId,
  title = "",
  artist = "",
  album = "",
  time = playTime,
)

fun PlaylistWithSongsEntity.toCommon() = PlaylistWithSongs(
  playlist = playlist.toCommon(),
  songs = songs.map { it.toCommon() },
)
