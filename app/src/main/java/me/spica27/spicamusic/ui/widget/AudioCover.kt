package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

@Composable
fun AudioCover(
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { },
    uri: Uri? = null,
) {
    // 使用 rememberAsyncImagePainter + 普通 Image 替代 SubcomposeAsyncImage，
    // 避免子组合（subcomposition）在 sharedBounds 动画帧期间的重测量开销。
    val painter = rememberAsyncImagePainter(model = uri)
    val state by painter.state.collectAsState()

    Box(modifier = modifier) {
        if (state !is AsyncImagePainter.State.Success) {
            placeHolder()
        } else {
            Image(
                painter = painter,
                contentDescription = "audio cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
