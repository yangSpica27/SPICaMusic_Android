package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skydoves.landscapist.image.LandscapistImage

@Composable
fun AudioCover(
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { },
    uri: Uri? = null,
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
            placeHolder()
        },
    )
}
