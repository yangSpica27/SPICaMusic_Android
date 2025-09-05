package me.spica27.spicamusic.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import me.spica27.spicamusic.db.entity.Song

@Composable
fun CoverWidget(
    song: Song,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val coverPainter =
        rememberAsyncImagePainter(
            model = ImageRequest.Builder(context).data(song.getCoverUri()).build(),
        )

    val coverPainterState = coverPainter.state.collectAsStateWithLifecycle()

    Box(
        modifier = modifier,
    ) {
        if (coverPainterState.value is AsyncImagePainter.State.Success) {
            Image(
                painter = coverPainter,
                contentDescription = "Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                modifier = Modifier.rotate(45f),
                text = song.displayName,
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.W900,
                    ),
            )
        }
    }
}
