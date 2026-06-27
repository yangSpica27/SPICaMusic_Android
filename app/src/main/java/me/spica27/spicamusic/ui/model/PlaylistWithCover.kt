package me.spica27.spicamusic.ui.model

import androidx.compose.runtime.Immutable
import me.spica27.spicamusic.common.entity.Playlist

/**
 * 歌单 + 封面/歌曲数的聚合 UI 模型。
 *
 * 封面专辑 id 与歌曲数原本是在每个列表项内部各自订阅一条 Flow（N 项 × 2 条），
 * 滚动时会反复重建订阅。改为在 ViewModel 内一次性聚合后，列表项只做纯展示，
 * 既消除了滚动期间的数据库订阅抖动，也让列表项可被 Compose 跳过（skippable）。
 */
@Immutable
data class PlaylistWithCover(
    val playlist: Playlist,
    val coverAlbumIds: List<Long>,
    val songCount: Int,
)
