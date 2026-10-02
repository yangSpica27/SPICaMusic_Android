package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.model.CachePolicy
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.spicamusic.R
import me.spica27.spicamusic.artwork.MusicArtwork

/** 封面来源都在本机，只走内存缓存；写磁盘缓存只会把原图再复制一份 */
private val ArtworkRequestBuilder: ImageRequest.Builder.() -> Unit = { diskCachePolicy(CachePolicy.DISABLED) }

/**
 * 统一的默认音乐封面。
 *
 * 使用真实位图而非随容器尺寸重排的图标组合，因此列表、播放器和共享元素动画
 * 只需缩放外层封面容器即可。
 */
@Composable
fun DefaultMusicCover(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.default_cover),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}

/**
 * 歌曲 / 专辑 / 歌手封面。
 * 回退链在 [MusicArtwork] 的 fetcher 里完成；加载期间显示容器底色，失败时渲染 [placeHolder]。
 */
@Composable
fun AudioCover(
    artwork: MusicArtwork?,
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { DefaultMusicCover() },
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        if (artwork == null || artwork.isEmpty) {
            placeHolder()
        } else {
            LandscapistImage(
                imageModel = { artwork },
                modifier = Modifier.fillMaxSize(),
                requestBuilder = ArtworkRequestBuilder,
                failure = { placeHolder() },
            )
        }
    }
}

@Preview
@Composable
private fun DefaultMusicCoverPreview() {
    DefaultMusicCover(modifier = Modifier.size(160.dp))
}
