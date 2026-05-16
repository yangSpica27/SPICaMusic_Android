package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
            ShowOnIdleContent(true) {
                Image(
                    painter = painter,
                    contentDescription = "Loaded image",
                    contentScale = ContentScale.Crop,
                )
            }
        },
        failure = {
            placeHolder()
        },
    )
}
