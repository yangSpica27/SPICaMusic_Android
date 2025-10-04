package me.spica27.spicamusic.ui.main.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.VisaulizerMode
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.clickableNoRippleWithVibration
import me.spica27.spicamusic.utils.pressable
import me.spica27.spicamusic.utils.scrollEndHaptic
import me.spica27.spicamusic.visualiser.MusicVisualiser
import me.spica27.spicamusic.widget.CoverWidget
import me.spica27.spicamusic.widget.LocalMenuState
import me.spica27.spicamusic.widget.ObserveLifecycleEvent
import me.spica27.spicamusic.widget.VisualizerView
import me.spica27.spicamusic.widget.blur.progressiveBlur
import me.spica27.spicamusic.widget.materialSharedAxisYIn
import me.spica27.spicamusic.widget.materialSharedAxisYOut
import org.koin.compose.koinInject

@Composable
fun VisaulizerWidget(song: Song) {
    var isActive by rememberSaveable { mutableStateOf(true) }

    val currentMode =
        koinInject<DataStoreUtil>().getVisualizerMode().collectAsStateWithLifecycle(null).value

    ObserveLifecycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> isActive = true
            Lifecycle.Event.ON_PAUSE -> isActive = false
            else -> {}
        }
    }

//    val overlyState = LocalPlayerWidgetState.current

    val menuState = LocalMenuState.current

    if (currentMode != null && isActive) {
        AnimatedContent(
            targetState = currentMode,
            modifier =
                Modifier
                    .fillMaxSize()
                    .clickable {
                        menuState.show { ModeSelectPanel() }
                    }.pressable(),
            contentKey = { it.value },
            transitionSpec = {
                materialSharedAxisYIn(
                    true,
                    durationMillis = 450,
                ) togetherWith scaleOut() + materialSharedAxisYOut(true, durationMillis = 450)
            },
        ) { currentMode ->
            when (currentMode) {
                VisaulizerMode.BOTTOM -> Bottom(song)
                VisaulizerMode.CIRCLE -> Circle(song)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun Circle(song: Song) {
    val context = LocalContext.current

    val lineColor = MaterialTheme.colorScheme.onSurface

    var isActive by rememberSaveable { mutableStateOf(true) }

    ObserveLifecycleEvent { event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> isActive = true
            Lifecycle.Event.ON_PAUSE -> isActive = false
            else -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isActive) {
            AndroidView(
                factory = { context ->
                    VisualizerView(context)
                },
                update = { view ->
                    view.setThemeColor(lineColor.toArgb())
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(60.dp + 12.dp)
                    .clip(CircleShape),
        ) {
            AnimatedContent(
                contentKey = { it.songId },
                targetState = song,
                label = "cover_transition",
                transitionSpec = {
                    materialSharedAxisYIn(
                        true,
                        durationMillis = 450,
                    ) togetherWith scaleOut() + materialSharedAxisYOut(true, durationMillis = 450)
                },
            ) { song ->
                val coverPainter =
                    rememberAsyncImagePainter(
                        model =
                            ImageRequest
                                .Builder(context)
                                .data(song.getCoverUri())
                                .transformations(
                                    CircleCropTransformation(),
                                ).build(),
                    )
                val coverPainterState = coverPainter.state.collectAsStateWithLifecycle()
                val infiniteTransition = rememberInfiniteTransition(label = "infinite")
                val rotateState =
                    infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec =
                            infiniteRepeatable(
                                animation = tween(10000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart,
                            ),
                        label = "",
                    )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                            .clip(CircleShape)
                            .border(
                                12.dp,
                                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                                CircleShape,
                            ).rotate(rotateState.value),
                    contentAlignment = Alignment.Center,
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
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun Bottom(song: Song) {
    val o =
        remember { List(MusicVisualiser.FREQUENCY_BAND_LIMITS.size * 2) { 0f }.toMutableStateList() }

    val lineColor = MaterialTheme.colorScheme.onSurface.copy(.3f)

    val drawData =
        o.map {
            animateFloatAsState(
                it,
                label = "black_line",
                animationSpec =
                    tween(
                        durationMillis = 100,
                        easing = EaseInOut,
                    ),
            )
        }

    DisposableEffect(Unit) {
        val musicVisualiser = MusicVisualiser()
        musicVisualiser.setListener(
            object : MusicVisualiser.Listener {
                override fun getDrawData(list: List<Float>) {
                    for ((index, f) in list.withIndex()) {
                        o[index] = f
                    }
                }
            },
        )
        musicVisualiser.ready()
        onDispose {
            musicVisualiser.dispose()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val xs = MutableList(MusicVisualiser.FREQUENCY_BAND_LIMITS.size * 2) { 0f }
                    val ys = MutableList(MusicVisualiser.FREQUENCY_BAND_LIMITS.size * 2) { 0f }

                    drawData.forEachIndexed { index, f ->
                        xs[index] = size.width * (index + 1) / (drawData.size)
                        ys[index] = size.height - size.height / 3 * f.value - 12.dp.toPx()
                    }

                    onDrawWithContent {
                        drawContent()

                        for (i in 0 until MusicVisualiser.FREQUENCY_BAND_LIMITS.size * 2) {
                            drawLine(
                                color = lineColor,
                                start = Offset(xs[i], size.height),
                                end = Offset(xs[i], ys[i]),
                                strokeWidth = size.width / (MusicVisualiser.FREQUENCY_BAND_LIMITS.size * 2) - 1.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }
                },
    ) {
        AnimatedContent(
            song,
            contentKey = { it.mediaStoreId },
        ) { song ->
            CoverWidget(
                song = song,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .progressiveBlur(),
            )
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.primaryContainer,
                                    ),
                            ),
                    ),
        )
    }
}

@Composable
fun ModeSelectPanel() {
    val dataStoreUtil = koinInject<DataStoreUtil>()
    val currentMode = dataStoreUtil.getVisualizerMode().collectAsStateWithLifecycle(null).value
    val modes = listOf(VisaulizerMode.BOTTOM, VisaulizerMode.CIRCLE)
    val coroutineScope = rememberCoroutineScope()
    val menuState = LocalMenuState.current
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .scrollEndHaptic(),
    ) {
        items(modes.size) { index ->
            val item = modes[index]
            val isSelected = item == currentMode
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 12.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            color =
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        ),
                    modifier =
                        Modifier
                            .weight(1f)
                            .pressable()
                            .clickableNoRippleWithVibration {
                                coroutineScope.launch {
                                    dataStoreUtil.setVisualizerMode(item)
                                    menuState.dismiss()
                                }
                            },
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    modifier = Modifier.size(24.dp),
                    imageVector = Icons.Filled.Check,
                    tint =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                    contentDescription = null,
                )
            }
        }
    }
}
