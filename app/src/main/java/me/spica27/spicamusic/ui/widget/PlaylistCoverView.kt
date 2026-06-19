package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────────────────────────────────────
// 公共入口：接受 albumId Flow（来自 PlaylistUseCases.getPlaylistCoverAlbumIds）
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 歌单封面马赛克组件。
 *
 * 使用方式（歌单列表 item 示例）：
 * ```kotlin
 * val coverIds by viewModel.getPlaylistCoverAlbumIds(playlist.playlistId!!).collectAsState(emptyList())
 * PlaylistCoverView(albumIds = coverIds, modifier = Modifier.size(56.dp))
 * ```
 * 或直接传入 Flow：
 * ```kotlin
 * PlaylistCoverView(
 *     albumIdsFlow = playlistUseCases.getPlaylistCoverAlbumIds(playlistId),
 *     modifier = Modifier.size(56.dp),
 * )
 * ```
 *
 * @param albumIds   最多 4 个专辑 ID，长度决定渲染策略（0/1-3/4）
 * @param iconSize   空歌单占位音符图标大小，默认 32.dp
 */
@Composable
fun PlaylistCoverView(
    albumIds: List<Long>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
) {
    when {
        albumIds.isEmpty() -> EmptyPlaylistCover(modifier, iconSize)
        albumIds.size < 4 -> SingleAlbumCover(albumIds.first(), modifier)
        else -> MosaicCover(albumIds.take(4), modifier)
    }
}

/**
 * 接受 Flow 版本，内部 collectAsState；适合直接传 use case 的 Flow 而不想在外部 collect 的场景。
 */
@Composable
fun PlaylistCoverView(
    albumIdsFlow: Flow<List<Long>>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
) {
    val albumIds by albumIdsFlow.collectAsState(initial = emptyList())
    PlaylistCoverView(albumIds = albumIds, modifier = modifier, iconSize = iconSize)
}

// ─────────────────────────────────────────────────────────────────────────────
// 内部渲染分支
// ─────────────────────────────────────────────────────────────────────────────

/** 歌单为空时的占位图（音符图标居中） */
@Composable
private fun EmptyPlaylistCover(
    modifier: Modifier,
    iconSize: Dp,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 歌单少于 4 首时，直接显示第一首的专辑封面 */
@Composable
private fun SingleAlbumCover(
    albumId: Long,
    modifier: Modifier,
) {
    LandscapistImage(
        imageModel = { "content://media/external/audio/albumart/$albumId".toUri() },
        modifier = modifier,
        success = { _, painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
        failure = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        },
    )
}

/** 歌单达到 4 首时，拼 2×2 马赛克封面 */
@Composable
private fun MosaicCover(
    albumIds: List<Long>,
    modifier: Modifier,
) {
    Row(modifier = modifier) {
        // 左列：第 0、2 张
        Column(modifier = Modifier.weight(1f)) {
            MosaicCell(albumIds[0], Modifier.weight(1f).fillMaxWidth())
            MosaicCell(albumIds[2], Modifier.weight(1f).fillMaxWidth())
        }
        // 右列：第 1、3 张
        Column(modifier = Modifier.weight(1f)) {
            MosaicCell(albumIds[1], Modifier.weight(1f).fillMaxWidth())
            MosaicCell(albumIds[3], Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun MosaicCell(
    albumId: Long,
    modifier: Modifier,
) {
    LandscapistImage(
        imageModel = { "content://media/external/audio/albumart/$albumId".toUri() },
        modifier = modifier,
        failure = {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        },
    )
}
