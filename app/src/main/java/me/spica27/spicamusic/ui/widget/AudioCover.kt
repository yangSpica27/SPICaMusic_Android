package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.spicamusic.R

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
 * 歌曲/专辑封面。
 * [uri] 加载失败时先尝试 [fallbackUri]（歌曲本体无内嵌封面时回退专辑图），仍失败才渲染 [placeHolder]。
 */
@Composable
fun AudioCover(
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { DefaultMusicCover() },
    uri: Uri? = null,
    fallbackUri: Uri? = null,
    progressiveEnabled: Boolean = false,
) {
    LandscapistImage(
        modifier = modifier,
        requestBuilder = {
            this
                .model(uri)
                .tag(uri.toString())
                .progressiveEnabled(progressiveEnabled)
                .build()
        },
        imageModel = { uri },
        failure = {
            if (fallbackUri != null && fallbackUri != uri) {
                CoverFallback(
                    fallbackUri = fallbackUri,
                    modifier = Modifier.fillMaxSize(),
                    placeHolder = placeHolder,
                )
            } else {
                placeHolder()
            }
        },
    )
}

/**
 * 封面兜底层：加载 [fallbackUri]，失败（或为 null）时渲染 [placeHolder]。
 * 供 AudioCover 与各处直接使用 LandscapistImage 的 failure 槽复用。
 */
@Composable
fun CoverFallback(
    fallbackUri: Uri?,
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { DefaultMusicCover() },
) {
    if (fallbackUri == null) {
        placeHolder()
        return
    }
    LandscapistImage(
        modifier = modifier,
        requestBuilder = {
            this
                .model(fallbackUri)
                .tag(fallbackUri.toString())
                .progressiveEnabled(false)
                .build()
        },
        imageModel = { fallbackUri },
        success = { _, painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
        failure = {
            placeHolder()
        },
    )
}

@Preview
@Composable
private fun DefaultMusicCoverPreview() {
    DefaultMusicCover(modifier = Modifier.size(160.dp))
}
