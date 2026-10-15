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
import me.spica27.spicamusic.common.entity.ArtistRole
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.AlbumUseCases
import me.spica27.spicamusic.feature.library.domain.ArtistUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction

/** 歌手页内容：各分区同时就绪后才下发，避免专辑晚到把歌曲列表整体顶下去 */
@Immutable
data class ArtistDetailContent(
    /** 演唱的歌曲及其作为专辑艺人的专辑曲目 */
    val songs: List<Song>,
    /** 主要作品：作为专辑艺人的专辑 */
    val albums: List<Album>,
    /** 参与作品：在别人的专辑里演唱 */
    val appearances: List<Album>,
    val composedSongs: List<Song>,
    val writtenSongs: List<Song>,
)

@Stable
class ArtistDetailViewModel(
    private val artistId: Long,
    private val artistUseCases: ArtistUseCases,
    private val albumUseCases: AlbumUseCases,
    private val player: PlayerUseCases,
) : ViewModel() {
    /** null 表示首屏数据尚未就绪（区别于"确实没有内容"） */
    val content: StateFlow<ArtistDetailContent?> =
        combine(
            artistUseCases.getArtistSongsFlow(artistId),
            albumUseCases.getAlbumsByArtistFlow(artistId),
            albumUseCases.getAppearancesFlow(artistId),
            artistUseCases.getSongsByRoleFlow(artistId, ArtistRole.COMPOSER),
            artistUseCases.getSongsByRoleFlow(artistId, ArtistRole.LYRICIST),
            ::ArtistDetailContent,
        ).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /** 主操作播放的列表：演唱歌曲；只作词 / 作曲的艺人退回到对应作品 */
    private val songs: List<Song>
        get() =
            content.value
                ?.let { content ->
                    content.songs.ifEmpty { content.composedSongs }.ifEmpty { content.writtenSongs }
                }.orEmpty()

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

    /** 在歌曲所在的分区内从这首开始播放 */
    fun playSongInList(
        song: Song,
        songList: List<Song>,
    ) {
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
