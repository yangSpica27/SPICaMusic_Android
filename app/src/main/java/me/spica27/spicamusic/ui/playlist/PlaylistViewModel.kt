package me.spica27.spicamusic.ui.playlist

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.ui.model.PlaylistWithCover
import timber.log.Timber

/** 创建歌单页「顺便选几首」的候选歌曲数量上限 */
private const val CREATOR_CANDIDATE_LIMIT = 30

/**
 * 歌单页面 ViewModel
 */
@Stable
class PlaylistViewModel(
    private val playlistRepository: PlaylistUseCases,
    private val songRepository: SongUseCases,
) : ViewModel() {
    // 所有歌单列表
    val playlists: StateFlow<List<Playlist>> =
        playlistRepository.getAllPlaylistsFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /**
     * 创建歌单页的候选歌曲：常听优先，不足 [CREATOR_CANDIDATE_LIMIT] 时用全部歌曲补齐。
     *
     * 只在创建页订阅，`WhileSubscribed` 会在离开页面后自动停掉上游。
     */
    val creatorCandidates: StateFlow<List<Song>> =
        combine(
            songRepository.getOftenListenSong10Flow(),
            songRepository.getAllSongsFlow(),
        ) { often, all ->
            val ordered = LinkedHashMap<Long, Song>(CREATOR_CANDIDATE_LIMIT)
            (often + all).forEach { song ->
                if (ordered.size >= CREATOR_CANDIDATE_LIMIT) return@forEach
                ordered.putIfAbsent(song.mediaStoreId, song)
            }
            ordered.values.toList()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistsWithCover: StateFlow<List<PlaylistWithCover>> =
        playlists
            .flatMapLatest { list ->
                if (list.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        list.map { playlist ->
                            val id = playlist.playlistId ?: 0L
                            combine(
                                playlistRepository.getPlaylistCoverAlbumIds(id),
                                playlistRepository.getSongSizeInPlaylist(id),
                            ) { albumIds, size ->
                                PlaylistWithCover(playlist, ImmutableList.copyOf(albumIds), size)
                            }
                        },
                    ) { it.toList() }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    // 是否显示创建歌单对话框
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog = _showCreateDialog.asStateFlow()

    // 是否显示删除确认对话框
    private val _showDeleteDialog = MutableStateFlow<Playlist?>(null)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    /**
     * 显示创建歌单对话框
     */
    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    /**
     * 隐藏创建歌单对话框
     */
    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    /**
     * 创建新歌单，并可选地把 [mediaIds] 一次性加进去。
     *
     * 建单与加歌串在同一个协程里：`createPlaylist` 返回新歌单 id 后立刻批量写入，
     * 批量接口是单事务，只触发一次数据失效，列表不会先闪一个空歌单再跳成 N 首。
     */
    fun createPlaylist(
        name: String,
        mediaIds: List<Long> = emptyList(),
    ) {
        if (name.isBlank()) {
            Timber.w("歌单名称不能为空")
            return
        }

        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.createPlaylist(name.trim())
                if (mediaIds.isNotEmpty()) {
                    playlistRepository.addSongsToPlaylist(playlistId, mediaIds)
                }
                Timber.d("创建歌单成功: $name (ID: $playlistId, 歌曲 ${mediaIds.size} 首)")
                hideCreateDialog()
            } catch (e: Exception) {
                Timber.e(e, "创建歌单失败")
            }
        }
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteDialog(playlist: Playlist) {
        _showDeleteDialog.value = playlist
    }

    /**
     * 隐藏删除确认对话框
     */
    fun hideDeleteDialog() {
        _showDeleteDialog.value = null
    }

    /**
     * 删除歌单
     */
    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                playlist.playlistId?.let {
                    playlistRepository.deletePlaylist(it)
                    Timber.d("删除歌单成功: ${playlist.playlistName}")
                }
                hideDeleteDialog()
            } catch (e: Exception) {
                Timber.e(e, "删除歌单失败")
            }
        }
    }
}
