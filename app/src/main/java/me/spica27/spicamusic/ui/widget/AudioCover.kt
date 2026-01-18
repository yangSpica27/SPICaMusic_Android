package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent

@Composable
fun AudioCover(
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { },
    uri: Uri? = null,
) {
    SubcomposeAsyncImage(
        model = uri,
        contentDescription = "audio cover",
        modifier = modifier,
    ) {
        val state by painter.state.collectAsState()
        when (state) {
            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent()
            }

            is AsyncImagePainter.State.Error -> {
                placeHolder()
            }

            is AsyncImagePainter.State.Loading -> {
                placeHolder()
            }

            else -> {
                placeHolder()
            }
        }
    }
}
