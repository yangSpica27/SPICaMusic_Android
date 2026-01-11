package me.spica27.spicamusic.widget

import android.annotation.SuppressLint
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtLeast
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.lyric.LrcParser
import me.spica27.spicamusic.lyric.LyricItem
import me.spica27.spicamusic.lyric.toNormal
import me.spica27.spicamusic.repository.LyricRepository
import me.spica27.spicamusic.utils.DataStoreUtil
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsView(
    modifier: Modifier = Modifier,
    song: Song,
    currentTime: Long,
    dataStoreUtil: DataStoreUtil = koinInject<DataStoreUtil>(),
    placeHolder: @Composable () -> Unit = {},
    onScroll: (Float) -> Unit = {},
) {
    val listState = rememberLazyListState()

    val isUserScrolling by listState.interactionSource.interactions
        .filterIsInstance<DragInteraction>()
        .map { dragInteraction ->
            dragInteraction is DragInteraction.Start
        }.collectAsStateWithLifecycle(null)

    val activeIndex = remember { mutableIntStateOf(0) }

    val layoutHeight = remember { mutableIntStateOf(0) }

    val itemFontSize = dataStoreUtil.getLyricFontSize().collectAsState(18)

    val itemFontWeight = dataStoreUtil.getLyricFontWeight().collectAsState(900)

    val lyricRepository = koinInject<LyricRepository>()

    // 延迟
    val delay = lyricRepository.getDelay(song.mediaStoreId).collectAsState(0).value

    // 歌词原始数据
    val lyric = lyricRepository.getLyrics(song.mediaStoreId).collectAsState(null).value

    // 解析后的
    var currentLyric by remember { mutableStateOf(emptyList<LyricItem>()) }

    LaunchedEffect(lyric) {
        launch(Dispatchers.IO) {
            currentLyric = LrcParser.parse(lyric ?: "")
        }
    }

    LaunchedEffect(currentTime) {
        if (isUserScrolling == true) return@LaunchedEffect
        launch(Dispatchers.IO + SupervisorJob()) {
            var index = 0
            for (item in currentLyric) {
                if (item.time >= (currentTime + 125 - delay)) {
                    activeIndex.intValue = (index - 1).fastCoerceAtLeast(0)
                    return@launch
                }
                index++
            }
            activeIndex.intValue = (currentLyric.size - 1).coerceAtLeast(0)
        }
    }

    LaunchedEffect(activeIndex.intValue) {
        launch(Dispatchers.IO) {
            listState.animateScrollToItemAndCenter(
                index = activeIndex.intValue,
                offset = -layoutHeight.intValue / 6,
            )
        }
    }

    LaunchedEffect(isUserScrolling) {
        if (isUserScrolling == false && currentLyric.isNotEmpty()) {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                return@LaunchedEffect
            }

            val viewportCenterY: Int =
                layoutInfo.viewportSize.height / 2 - layoutInfo.viewportSize.height / 3

            var closestItemIndex = -1

            layoutInfo.visibleItemsInfo.forEach { itemInfo ->
                val itemCenterInViewport: Int = itemInfo.offset + (itemInfo.size / 2)
                val distance: Int =
                    Math.max(itemCenterInViewport, viewportCenterY) -
                        Math.min(
                            itemCenterInViewport,
                            viewportCenterY,
                        )

                if (distance < itemInfo.size / 2) {
                    closestItemIndex = itemInfo.index
                }
            }

            if (closestItemIndex != -1 && closestItemIndex < currentLyric.size) {
                val targetLyricItem = currentLyric[closestItemIndex]
                onScroll(targetLyricItem.time.toFloat())
            }
        }
    }

    if (currentLyric.isEmpty()) {
        placeHolder.invoke()
    } else {
        val density = LocalDensity.current

        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        layoutHeight.intValue = it.height
                    },
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                state = listState,
                userScrollEnabled = false,
                contentPadding =
                    PaddingValues(
                        vertical =
                            with(density) {
                                (layoutHeight.intValue / 3f).toDp()
                            },
                    ),
            ) {
                itemsIndexed(
                    currentLyric.map { it.toNormal() },
                    key = { index, _ ->
                        index
                    },
                ) { index, it ->
                    it?.toNormal()?.let { item ->
                        LyricsViewLine(
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            isActive = index == activeIndex.intValue,
                            content = item.content,
                            inactiveBlur =
                                (
                                    (
                                        index.coerceAtLeast(activeIndex.intValue) -
                                            index.coerceAtMost(activeIndex.intValue)
                                    ) * 0.8f
                                ).fastCoerceAtMost(4f),
                            onClick = { },
                            fontWeight = FontWeight(itemFontWeight.value),
                            fontSize = itemFontSize.value.sp,
                        )
                    }
                }
            }
            if (isUserScrolling == true) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                2.dp,
                            ).offset(
                                y =
                                    with(density) {
                                        (layoutHeight.intValue / 3f).toDp()
                                    },
                            ).background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(
                                    alpha = 0.5f,
                                ),
                            ).align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
fun LyricsViewLine(
    isActive: Boolean,
    content: String,
    contentColor: Color = Color.Black,
    fontSize: TextUnit = 18.sp,
    fontWeight: FontWeight = FontWeight.Black,
    lineHeight: TextUnit = 1.2.em,
    onClick: () -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    activeScale: Float = 1.15f,
    inactiveScale: Float = 1f,
    activeAlpha: Float = 1f,
    inactiveAlpha: Float = 0.35f,
    activeBlur: Float = 0f,
    inactiveBlur: Float = 1f,
    itemTranslationY: Float = 0f,
) {
    val scale = remember { mutableFloatStateOf(inactiveScale) }
    val alpha = remember { mutableFloatStateOf(inactiveAlpha) }
    val blur = remember { mutableFloatStateOf(0f) }

    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(
        isActive,
    ) {
        launch {
            animate(
                initialValue = scale.floatValue,
                targetValue =
                    if (isActive) {
                        activeScale
                    } else {
                        inactiveScale
                    },
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            ) { value, _ ->
                scale.floatValue = value
            }
        }

        launch {
            animate(
                initialValue = alpha.floatValue,
                targetValue =
                    if (isActive) {
                        activeAlpha
                    } else {
                        inactiveAlpha
                    },
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
            ) { value, _ ->
                alpha.floatValue = value
            }
        }
    }

    LaunchedEffect(isActive, inactiveBlur) {
        launch {
            animate(
                initialValue = blur.floatValue,
                targetValue =
                    if (isActive) {
                        activeBlur
                    } else {
                        inactiveBlur
                    },
                animationSpec =
                    tween(
                        durationMillis = 300,
                        delayMillis = 100,
                        easing = EaseInCubic,
                    ),
            ) { value, _ ->
                blur.floatValue = value
            }
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale.floatValue
                    scaleY = scale.floatValue
                    translationY = itemTranslationY
                }.padding(horizontal = 40.dp, vertical = 10.dp)
                .indication(interactionSource, indication = null)
                .blur(
                    radius = blur.floatValue.dp,
                ).pointerInput(interactionSource) {
                    detectTapGestures(
                        onPress = {
                            val press = PressInteraction.Press(it)
                            try {
                                // 其他动画进行进行的时候进制波纹动画
                                withTimeout(timeMillis = 100) {
                                    tryAwaitRelease()
                                }
                            } catch (e: TimeoutCancellationException) {
                                e.printStackTrace()
                                interactionSource.emit(press)
                                tryAwaitRelease()
                            }
                            interactionSource.emit(PressInteraction.Release(press))
                        },
                        onTap = { onClick() },
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = content,
            modifier =
                Modifier
                    .graphicsLayer {
                        this.alpha = alpha.floatValue
                    },
            style =
                TextStyle(
                    color = contentColor,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    lineHeight = lineHeight,
                    shadow =
                        Shadow(
                            color = contentColor.copy(alpha = .2f),
                            blurRadius =
                                if (isActive) {
                                    5f
                                } else {
                                    13f
                                },
                        ),
                ),
        )
    }
}

suspend fun LazyListState.animateScrollToItemAndCenter(
    index: Int,
    offset: Int,
) {
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) {
        withContext(Dispatchers.Main) {
            animateScrollToItem(index = index, scrollOffset = offset)
        }
    }
    val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }

    if (itemInfo != null) {
        val viewportCenter = layoutInfo.viewportEndOffset / 2f // Use float for precision
        val itemCenter = itemInfo.offset + itemInfo.size / 2f
        val scrollAmount = itemCenter - viewportCenter + layoutInfo.afterContentPadding
        this.animateScrollBy(
            scrollAmount,
            animationSpec =
                tween(
                    durationMillis = 500,
                    easing = EaseInOut,
                ),
        )
    } else {
        withContext(Dispatchers.Main) {
            animateScrollToItem(index = index, scrollOffset = offset)
        }
    }
}
