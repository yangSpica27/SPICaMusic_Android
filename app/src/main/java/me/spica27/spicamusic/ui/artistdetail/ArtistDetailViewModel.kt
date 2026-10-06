package me.spica27.spicamusic.ui.artistdetail

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.SongFilter
import me.spica27.spicamusic.feature.library.domain.AlbumUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction

/** 歌手页内容：歌曲与专辑同时就绪后才下发，避免专辑晚到把歌曲列表整体顶下去 */
@Immutable
data class ArtistDetailContent(
    val songs: List<Song>,
    val albums: List<Album>,
)

@Stable
class ArtistDetailViewModel(
    private val artistName: String,
    private val songUseCases: SongUseCases,
    private val albumUseCases: AlbumUseCases,
    private val player: PlayerUseCases,
) : ViewModel() {
    /** null 表示首屏数据尚未就绪（区别于"确实没有内容"） */
    val content: StateFlow<ArtistDetailContent?> =
        combine(
            songUseCases.getSongsFlow(filter = SongFilter(artists = listOf(artistName))),
            albumUseCases.getAlbumsByArtistFlow(artistName),
            ::ArtistDetailContent,
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    private val songs: List<Song>
        get() = content.value?.songs.orEmpty()

    fun playAll() {
        val songList = songs
        if (songList.isEmpty()) return
        viewModelScope.launch {
            player.doAction(
                PlayerAction.UpdateList(
                    songList.map { it.mediaStoreId.toString() },
                    start = true,
                ),
            )
        }
    }

    /** 打乱后整列播放，从打乱后的第一首开始 */
    fun shufflePlay() {
        val ids = songs.map { it.mediaStoreId.toString() }.shuffled()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            player.doAction(
                PlayerAction.UpdateList(
                    mediaIds = ids,
                    mediaId = ids.first(),
                    start = true,
                ),
            )
        }
    }

    /** 正在播放该歌手的歌曲时用于继续/暂停，不重置队列 */
    fun togglePlayPause() {
        viewModelScope.launch {
            player.doAction(PlayerAction.PlayOrPause)
        }
    }

    fun playSongInList(song: Song) {
        val songList = songs
        viewModelScope.launch {
            player.doAction(
                PlayerAction.UpdateList(
                    songList.map { it.mediaStoreId.toString() },
                    mediaId = song.mediaStoreId.toString(),
                    start = true,
                ),
            )
        }
    }
}
