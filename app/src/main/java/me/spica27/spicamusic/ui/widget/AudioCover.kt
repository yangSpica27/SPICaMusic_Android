package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.skydoves.landscapist.image.LandscapistImage

@Composable
fun AudioCover(
    modifier: Modifier = Modifier,
    placeHolder: @Composable () -> Unit = { },
    uri: Uri? = null,
) {
    LandscapistImage(
        modifier = modifier,
        imageModel = { uri },
        success = { state, painter ->
            Image(
                painter = painter,
                contentDescription = "Loaded image",
            )
        },
        failure = {
            placeHolder()
        },
    )
}
