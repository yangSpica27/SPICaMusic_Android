package me.spica27.spicamusic.ui.artistdetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.artwork.artwork
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.navigation.AlbumDetailRoute
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.navigation.SongMenuRoute
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.ListItemFadeInSpec
import me.spica27.spicamusic.ui.theme.ListItemFadeOutSpec
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entrance
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.IOSOverscrollEffect
import me.spica27.spicamusic.ui.widget.OtherAlbumsShelf
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.concurrent.TimeUnit
import kotlin.math.min

// ── 布局尺寸常量 ──────────────────────────────────────────────────────────────
private val HEADER_HEIGHT = 56.dp // 固定顶栏内容区高度（不含状态栏）
private val AVATAR_EXPANDED_MAX = 168.dp // 头像展开直径上限（矮窗口按可用高度 24% 钳制）
private val AVATAR_DOCKED = 36.dp // 头像停靠顶栏后的直径（顶栏内容区内垂直居中）
private val AVATAR_DOCKED_START = 56.dp // 停靠后距屏幕左缘距离（返回按钮之后）
private val AVATAR_TOP_GAP = Spacing.Small
private val AVATAR_NAME_GAP = Spacing.Large
private val AVATAR_SHADOW = 12.dp

// 顶栏小标题起点 = 停靠头像终点 + 间隙（减去 48dp 返回按钮）
private val DOCKED_TITLE_START = AVATAR_DOCKED_START + AVATAR_DOCKED + Spacing.Medium - 48.dp

// 大字名字距顶栏还剩这段距离时开始交接给顶栏小标题
private val HANDOFF_LEAD = 48.dp

private val BOTTOM_PLAYER_RESERVED = 200.dp // 悬浮迷你播放器底部预留（全项目惯例值）

private const val PULL_GROWTH = 0.25f // 顶部下拉回弹时头像随之放大的比例
private const val WASH_PARALLAX = 0.5f // 主色晕染的视差系数

private const val KEY_HERO = "hero"
private const val KEY_ACTIONS = "actions"
private const val KEY_FILLER = "filler"

private val ItemPlacementSpringSpec =
    spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

// 归位：无回弹，停下后干脆落定
private val SettleSpec =
    spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

/** 主按钮的三种语义：未在播该歌手 / 正在播 / 已暂停 */
private enum class ArtistPlayState { Idle, Playing, Paused }

/**
 * 头部折叠状态。
 *
 * 所有几何都以「列表滚过的像素」为自变量，只在 layout/draw 阶段读取 → 滚动全程零重组。
 * 折叠量程 == 头部 item（头像留白 + 名字 + meta）的实测高度。
 */
@Stable
private class ArtistHeaderState(
    private val listState: LazyListState,
) {
    var rangePx by mutableIntStateOf(0)

    /** 手指按在屏幕上时不归位，松手后才判定 */
    var pointerDown by mutableStateOf(false)

    /** 已滚过的像素，钳制在 [0, 量程] */
    val scrolledPx: Float
        get() {
            val range = rangePx
            if (range <= 0) return 0f
            return if (listState.firstVisibleItemIndex > 0) {
                range.toFloat()
            } else {
                listState.firstVisibleItemScrollOffset.coerceAtMost(range).toFloat()
            }
        }

    val progress: Float
        get() = if (rangePx <= 0) 0f else scrolledPx / rangePx

    /**
     * 标题交接进度：大字名字距顶栏 [leadPx] 时为 0，头部完全折叠时为 1。
     * [nameTopPx] 是名字顶边在头部 item 内的偏移。
     */
    fun handoff(
        nameTopPx: Float,
        leadPx: Float,
    ): Float {
        val range = rangePx
        if (range <= 0) return 0f
        val start = (nameTopPx - leadPx).coerceIn(0f, range - 1f)
        return ((scrolledPx - start) / (range - start)).coerceIn(0f, 1f)
    }
}

/**
 * 歌手详情页。
 *
 * - 圆形头像随滚动贴着内容上行、收缩，再沿平滑弧线停靠到返回按钮右侧；
 *   名字在抵达顶栏前淡出，由顶栏小标题接力；
 * - 松手后头部只会停在「展开」或「折叠」两态之一；列表末尾按需补位，
 *   歌曲再少也能完整折叠，不会卡在半途。
 */
@Composable
fun ArtistDetailScreen(artist: Artist) {
    val backStack = LocalBackStack.current
    val viewModel: ArtistDetailViewModel =
        koinViewModel(key = "ArtistDetailViewModel_${artist.name}") {
            parametersOf(artist.name)
        }
    val content by viewModel.content.collectAsStateWithLifecycle()
    val songs = content?.songs.orEmpty()
    val albums = content?.albums.orEmpty()

    val playerViewModel = LocalPlayerViewModel.current
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val playingMediaId = currentMediaItem?.mediaId
    val playingFromArtist =
        remember(playingMediaId, songs) {
            playingMediaId != null && songs.any { it.mediaStoreId.toString() == playingMediaId }
        }
    val playState =
        when {
            !playingFromArtist -> ArtistPlayState.Idle
            isPlaying -> ArtistPlayState.Playing
            else -> ArtistPlayState.Paused
        }
    val onPrimaryAction = {
        if (playingFromArtist) viewModel.togglePlayPause() else viewModel.playAll()
    }

    val coverUri = remember(artist) { artist.getCoverUri() }
    val coverArtwork = remember(artist) { artist.artwork() }
    val dominantColor =
        rememberDominantColorFromUri(
            uri = coverUri,
            fallbackColor = MaterialTheme.colorScheme.primaryContainer,
        )
    // 保持 State 形态，只在 draw 阶段读取
    val animatedDominantColor =
        animateColorAsState(
            targetValue = dominantColor,
            animationSpec = spring(stiffness = 200f),
            label = "dominantColor",
        )

    val listState = rememberLazyListState()
    val header = remember(listState) { ArtistHeaderState(listState) }
    val scope = rememberCoroutineScope()
    val overscrollEffect = remember(scope) { IOSOverscrollEffect(scope, Orientation.Vertical) }
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 首屏入场瀑布：等数据就绪后再关闸，歌曲晚到也能赶上同一拍
    var listEntrancePlay by remember { mutableStateOf(true) }
    LaunchedEffect(content != null) {
        if (content != null) {
            delay(55)
            listEntrancePlay = false
        }
    }

    // 归位：空闲（没在滚、手指没按着）时头部不允许停在半折叠态。
    // 以「空闲时的位置」而非「滚动结束」为触发源：数据增删、窗口尺寸变化让列表被动夹紧时同样会归位
    LaunchedEffect(header, reducedMotion) {
        var settleJob: Job? = null
        snapshotFlow {
            val range = header.rangePx
            val offset = listState.firstVisibleItemScrollOffset
            val idle = !listState.isScrollInProgress && !header.pointerDown
            if (idle && range > 0 && listState.firstVisibleItemIndex == 0 && offset in 1 until range) {
                offset to range
            } else {
                null
            }
        }.filterNotNull()
            .collect { (offset, range) ->
                if (settleJob?.isActive == true) return@collect
                // 底部补位保证正常情况下够滚；万一推不到底就回展开位，绝不停在中间
                val target = if (offset * 2 >= range && listState.canScrollForward) range else 0
                val delta = (target - offset).toFloat()
                // 子协程承接：被用户手势打断只取消这一次归位，不影响后续监听
                settleJob =
                    launch {
                        if (reducedMotion) {
                            listState.scrollBy(delta)
                        } else {
                            listState.animateScrollBy(delta, SettleSpec)
                        }
                    }
            }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // 只旁听按下/抬起，不消费事件
            .pointerInput(header) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    header.pointerDown = true
                    try {
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                        } while (event.changes.fastAny { it.pressed })
                    } finally {
                        header.pointerDown = false
                    }
                }
            },
    ) {
        // 展开头像：横屏/分屏矮窗口时按可用高度钳制
        val avatarExpanded = AVATAR_EXPANDED_MAX.coerceAtMost(maxHeight * 0.24f)
        val avatarBlock = AVATAR_TOP_GAP + avatarExpanded + AVATAR_NAME_GAP
        val avatarStart = (maxWidth - avatarExpanded) / 2
        val avatarTop = statusBarTop + HEADER_HEIGHT + AVATAR_TOP_GAP

        val nameTopPx = with(density) { avatarBlock.toPx() }
        val handoffLeadPx = with(density) { HANDOFF_LEAD.toPx() }
        val handoff: () -> Float =
            remember(header, nameTopPx, handoffLeadPx) {
                { header.handoff(nameTopPx, handoffLeadPx) }
            }
        // 顶栏小标题/播放键在交接后半程浮现：此时大字名字已几乎淡尽，不会出现双标题
        val titleReveal: () -> Float =
            remember(handoff) { { ((handoff() - 0.45f) / 0.55f).coerceIn(0f, 1f) } }
        val barRevealed by remember(titleReveal) { derivedStateOf { titleReveal() > 0.5f } }

        // 列表末尾补位：保证头部之后的内容至少撑满一屏可视区，从而总能滚满折叠量程。
        // 只有「操作行」与补位同时可见时才能精确测量（内容短时必然同时可见）；
        // 数据或窗口变化时清零重测。
        var fillerPx by remember(songs.size, albums.size, maxHeight) { mutableIntStateOf(0) }
        LaunchedEffect(listState, songs.size, albums.size, maxHeight) {
            snapshotFlow {
                val info = listState.layoutInfo
                val actions = info.visibleItemsInfo.fastFirstOrNull { it.key == KEY_ACTIONS }
                val filler = info.visibleItemsInfo.fastFirstOrNull { it.key == KEY_FILLER }
                if (actions == null || filler == null) {
                    null
                } else {
                    info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding -
                        (filler.offset - actions.offset)
                }
            }.filterNotNull()
                .collect { fillerPx = it.coerceAtLeast(0) }
        }

        // ── [L0] 主色晕染（视差上移并随折叠淡出）──────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTop + HEADER_HEIGHT + avatarBlock + 140.dp)
                .graphicsLayer {
                    translationY = -header.scrolledPx * WASH_PARALLAX
                    alpha = 1f - header.progress
                }.drawBehind {
                    val color = animatedDominantColor.value
                    drawRect(
                        Brush.verticalGradient(
                            0f to color.copy(alpha = 0.55f),
                            0.5f to color.copy(alpha = 0.24f),
                            1f to color.copy(alpha = 0f),
                        ),
                    )
                },
        )

        // ── [L1] 内容列表 ──────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = overscrollEffect,
            contentPadding =
                PaddingValues(
                    top = statusBarTop + HEADER_HEIGHT,
                    bottom = BOTTOM_PLAYER_RESERVED,
                ),
        ) {
            // 头像留白 + 名字 + meta：整块高度即折叠量程
            item(key = KEY_HERO, contentType = "hero") {
                ArtistHero(
                    name = artist.name,
                    meta =
                        artistMetaText(
                            songCount = if (content == null) artist.songCount else songs.size,
                            albumCount = albums.size,
                            totalDurationMs = remember(songs) { songs.sumOf { it.duration } },
                        ),
                    avatarBlock = avatarBlock,
                    textAlpha = { 1f - handoff() * 2f },
                    modifier = Modifier.onSizeChanged { header.rangePx = it.height },
                )
            }

            item(key = KEY_ACTIONS, contentType = "actions") {
                ArtistActionRow(
                    playState = playState,
                    enabled = content == null || songs.isNotEmpty(),
                    onPrimary = onPrimaryAction,
                    onShuffle = viewModel::shufflePlay,
                    modifier = Modifier.entrance(order = 2),
                )
            }

            // 专辑：多张用横向货架，仅一张时用整行卡片，避免货架只有孤零零一格
            if (albums.size > 1) {
                item(key = "albums", contentType = "album_shelf") {
                    OtherAlbumsShelf(
                        artistName = artist.name,
                        albums = albums,
                        onAlbumClick = { backStack.add(AlbumDetailRoute(it)) },
                        title = stringResource(R.string.albums_title),
                        modifier =
                            Modifier
                                .padding(top = Spacing.Small)
                                .entrance(order = 3, play = listEntrancePlay),
                    )
                }
            } else if (albums.size == 1) {
                item(key = "albums", contentType = "album_single") {
                    Column(
                        Modifier
                            .padding(top = Spacing.Small)
                            .entrance(order = 3, play = listEntrancePlay),
                    ) {
                        SectionTitle(stringResource(R.string.albums_title))
                        ArtistAlbumRow(
                            album = albums.first(),
                            onClick = { backStack.add(AlbumDetailRoute(albums.first())) },
                        )
                    }
                }
            }

            if (songs.isNotEmpty()) {
                item(key = "songs_title", contentType = "section_title") {
                    SectionTitle(
                        text = stringResource(R.string.music_tab_songs),
                        modifier =
                            Modifier
                                .padding(top = Spacing.Large)
                                .entrance(order = 4, play = listEntrancePlay),
                    )
                }
            }

            items(
                count = songs.size,
                key = { index -> songs[index].mediaStoreId },
                contentType = { "song" },
            ) { index ->
                val song = songs[index]
                val isCurrent = playingMediaId == song.mediaStoreId.toString()
                ArtistSongRow(
                    song = song,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onClick = { viewModel.playSongInList(song) },
                    onMore = { backStack.add(SongMenuRoute(song)) },
                    modifier =
                        Modifier
                            .animateItem(
                                fadeInSpec = ListItemFadeInSpec,
                                placementSpec = ItemPlacementSpringSpec,
                                fadeOutSpec = ListItemFadeOutSpec,
                            ).entrance(
                                order = min(5 + index, 9),
                                play = listEntrancePlay,
                            ),
                )
            }

            item(key = KEY_FILLER, contentType = "filler") {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(with(density) { fillerPx.toDp() }),
                )
            }
        }

        // ── [L2] 顶栏底色 + 发丝线（随交接浮现）────────────────────────────────
        val barColor = MaterialTheme.colorScheme.background
        val hairlineColor = MaterialTheme.colorScheme.outlineVariant
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTop + HEADER_HEIGHT)
                .drawBehind {
                    val a = handoff()
                    drawRect(barColor.copy(alpha = a))
                    drawRect(
                        color = hairlineColor.copy(alpha = 0.14f * a),
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                },
        )

        // ── [L3] 浮动头像（全部运动收敛在一个 graphicsLayer）─────────────────────
        Box(
            Modifier
                .padding(start = avatarStart, top = avatarTop)
                .size(avatarExpanded)
                .entrance(order = 0)
                .graphicsLayer {
                    val expandedPx = avatarExpanded.toPx()
                    val dockedPx = AVATAR_DOCKED.toPx()
                    val expandedBottom = (avatarTop + avatarExpanded).toPx()
                    val dockedBottom = (statusBarTop + (HEADER_HEIGHT + AVATAR_DOCKED) / 2).toPx()
                    val dockDistance = expandedBottom - dockedBottom

                    val scrolled = header.scrolledPx
                    val t = (scrolled / dockDistance).coerceIn(0f, 1f)
                    // 顶部下拉时与内容一起下移并轻微放大
                    val pull = overscrollEffect.overscrollOffset.takeIf { it > 0f } ?: 0f
                    val diameter = lerp(expandedPx, dockedPx, t) + pull * PULL_GROWTH
                    // 底边与内容 1:1 同步上行（名字永远不会钻到头像下面），抵达顶栏后停住
                    val bottom = expandedBottom - scrolled.coerceAtMost(dockDistance) + pull
                    // 水平走 smoothstep：先随内容上浮再滑入顶栏，起止都没有速度突变
                    val ease = t * t * (3f - 2f * t)
                    val centerX =
                        lerp(
                            avatarStart.toPx() + expandedPx / 2f,
                            AVATAR_DOCKED_START.toPx() + dockedPx / 2f,
                            ease,
                        )

                    val scale = diameter / expandedPx
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = scale
                    scaleY = scale
                    translationX = centerX - diameter / 2f - avatarStart.toPx()
                    translationY = bottom - diameter - avatarTop.toPx()
                    shape = CircleShape
                    clip = true
                    // 主色投影：停靠时收敛为零，顶栏里不拖一团阴影
                    shadowElevation = (1f - t) * AVATAR_SHADOW.toPx()
                    ambientShadowColor = animatedDominantColor.value
                    spotShadowColor = animatedDominantColor.value
                },
        ) {
            AudioCover(
                artwork = coverArtwork,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── [L4] 顶栏内容：返回 / 小标题 / 播放 ─────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = statusBarTop)
                .height(HEADER_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { backStack.removeLastOrNull() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = artist.name,
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = DOCKED_TITLE_START, end = Spacing.Small)
                        .graphicsLayer {
                            val a = titleReveal()
                            alpha = a
                            translationY = (1f - a) * 4.dp.toPx()
                        },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            DockedPlayButton(
                playState = playState,
                enabled = barRevealed && (content == null || songs.isNotEmpty()),
                reveal = titleReveal,
                onClick = onPrimaryAction,
                modifier = Modifier.padding(end = Spacing.Small),
            )
        }
    }
}

// ── 头部：名字 + meta（头像是浮动层，这里只留位）───────────────────────────────

@Composable
private fun ArtistHero(
    name: String,
    meta: String,
    avatarBlock: Dp,
    textAlpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.Small),
    ) {
        Spacer(Modifier.height(avatarBlock))
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.ExtraLarge)
                .entrance(order = 1)
                .graphicsLayer { alpha = textAlpha().coerceIn(0f, 1f) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.ExtraSmall))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** meta 行：N 首歌曲 · M 张专辑 · 总时长 */
@Composable
private fun artistMetaText(
    songCount: Int,
    albumCount: Int,
    totalDurationMs: Long,
): String {
    val countPart = stringResource(R.string.songs_count, songCount)
    val albumPart = if (albumCount > 0) stringResource(R.string.artist_albums_count, albumCount) else null
    val hours = TimeUnit.MILLISECONDS.toHours(totalDurationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMs) % 60
    val durationPart =
        when {
            totalDurationMs <= 0L -> null
            hours > 0 -> stringResource(R.string.hours_minutes, hours, minutes)
            minutes > 0 -> stringResource(R.string.minutes, minutes)
            else -> stringResource(R.string.less_than_1_minute)
        }
    return listOfNotNull(countPart, albumPart, durationPart).joinToString(" · ")
}

// ── 操作行：播放（随播放状态切换语义）/ 随机 ───────────────────────────────────

@Composable
private fun ArtistActionRow(
    playState: ArtistPlayState,
    enabled: Boolean,
    onPrimary: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        ArtistActionPill(
            primary = true,
            enabled = enabled,
            onClick = onPrimary,
            modifier = Modifier.weight(1f),
        ) { contentColor ->
            AnimatedContent(
                targetState = playState,
                transitionSpec = { materialSharedAxisZ(forward = true) },
                contentAlignment = Alignment.Center,
                label = "artistPlayState",
            ) { state ->
                PillLabel(
                    icon = if (state == ArtistPlayState.Playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    text =
                        stringResource(
                            when (state) {
                                ArtistPlayState.Idle -> R.string.play_all
                                ArtistPlayState.Playing -> R.string.pause
                                ArtistPlayState.Paused -> R.string.play
                            },
                        ),
                    color = contentColor,
                )
            }
        }
        ArtistActionPill(
            primary = false,
            enabled = enabled,
            onClick = onShuffle,
            modifier = Modifier.weight(1f),
        ) { contentColor ->
            PillLabel(
                icon = Icons.Default.Shuffle,
                text = stringResource(R.string.shuffle_play),
                color = contentColor,
            )
        }
    }
}

@Composable
private fun ArtistActionPill(
    primary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (contentColor: Color) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier =
            modifier
                .heightIn(min = 44.dp)
                .alpha(if (enabled) 1f else 0.38f)
                .clip(CircleShape)
                .background(if (primary) colors.primary else colors.secondaryContainer)
                .clickHighlight(enabled = enabled, onClick = onClick)
                .padding(horizontal = Spacing.Large),
        contentAlignment = Alignment.Center,
    ) {
        content(if (primary) colors.onPrimary else colors.onSecondaryContainer)
    }
}

@Composable
private fun PillLabel(
    icon: ImageVector,
    text: String,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
        )
    }
}

/** 折叠后停靠在顶栏末端的播放键：大按钮滚出视野后主操作仍触手可及 */
@Composable
private fun DockedPlayButton(
    playState: ArtistPlayState,
    enabled: Boolean,
    reveal: () -> Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .minimumInteractiveComponentSize()
                .size(36.dp)
                .graphicsLayer {
                    val a = reveal()
                    alpha = a
                    val s = lerp(0.8f, 1f, a)
                    scaleX = s
                    scaleY = s
                }.clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                // 未浮现时不挂点击、不暴露语义：防误触，不拦截下方列表拖动，读屏也不会念出隐形按钮
                .then(
                    if (enabled) Modifier.clickHighlight(onClick = onClick) else Modifier.clearAndSetSemantics {},
                ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = playState == ArtistPlayState.Playing,
            transitionSpec = { materialSharedAxisZ(forward = true) },
            label = "dockedPlayIcon",
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(if (playing) R.string.pause else R.string.play_all),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── 分区标题（与专辑货架标题同款）───────────────────────────────────────────────

@Composable
private fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Large)
                .padding(bottom = Spacing.Small),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

// ── 单张专辑：整行卡片 ─────────────────────────────────────────────────────────

@Composable
private fun ArtistAlbumRow(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artwork = remember(album.id) { album.artwork() }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Small)
                .clip(Shapes.LargeCornerBasedShape)
                .clickHighlight(onClick = onClick)
                .padding(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AudioCover(
            artwork = artwork,
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(Shapes.MediumCornerBasedShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title.ifBlank { stringResource(R.string.unknown_album) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    listOfNotNull(
                        album.year.takeIf { it > 0 }?.toString(),
                        stringResource(R.string.songs_count_format, album.numberOfSongs),
                    ).joinToString(" · "),
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── 歌曲行：封面（当前曲目压暗 + 律动条）+ 标题 / 专辑 + 时长 + 更多 ──────────────

@Composable
private fun ArtistSongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // 透明端用同色零透明度，避免插值途中泛黑
    val highlight =
        animateColorAsState(
            targetValue = colors.primaryContainer.copy(alpha = if (isCurrent) 0.30f else 0f),
            animationSpec = tween(durationMillis = 220, easing = EaseOutEmphasized),
            label = "songRowHighlight",
        )
    val titleColor by animateColorAsState(
        targetValue = if (isCurrent) colors.primary else colors.onSurface,
        animationSpec = tween(durationMillis = 220, easing = EaseOutEmphasized),
        label = "songRowTitle",
    )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Small)
                .clip(Shapes.MediumCornerBasedShape)
                .drawBehind { drawRect(highlight.value) }
                .clickHighlight(onClick = onClick)
                .padding(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        // 每行缓存封面模型，长列表 fling 时不逐行分配
        val songArtwork = remember(song.mediaStoreId) { song.artwork() }
        Box(
            Modifier
                .size(48.dp)
                .clip(Shapes.SmallCornerBasedShape),
        ) {
            AudioCover(
                artwork = songArtwork,
                modifier = Modifier.fillMaxSize(),
            )
            // 外层是 RowScope，显式限定以调用无作用域版本
            androidx.compose.animation.AnimatedVisibility(
                visible = isCurrent,
                enter = fadeIn(tween(durationMillis = 220, easing = EaseOutEmphasized)),
                exit = fadeOut(tween(durationMillis = 160, easing = EaseOutEmphasized)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.36f)),
                    contentAlignment = Alignment.Center,
                ) {
                    PlayingBars(animate = isPlaying, color = Color.White)
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.album.ifBlank { stringResource(R.string.unknown_album) },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = song.getFormattedDuration(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

private val StaticBarHeights: List<() -> Float> = listOf({ 8f }, { 13f }, { 6f })

/** 律动条：播放中三根错相起伏，暂停或降级动效时静止；高度只在 draw 阶段读取 */
@Composable
private fun PlayingBars(
    animate: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val heights: List<() -> Float> =
        if (animate && !reducedMotion) {
            val transition = rememberInfiniteTransition(label = "artistPlayingBars")
            List(3) { i ->
                val bar =
                    transition.animateFloat(
                        initialValue = 4f,
                        targetValue = 14f,
                        animationSpec =
                            infiniteRepeatable(
                                animation =
                                    tween(
                                        durationMillis = 420 + i * 130,
                                        easing = FastOutSlowInEasing,
                                    ),
                                repeatMode = RepeatMode.Reverse,
                            ),
                        label = "bar$i",
                    )
                val height: () -> Float = { bar.value }
                height
            }
        } else {
            StaticBarHeights
        }
    Spacer(
        modifier
            .size(width = 13.dp, height = 14.dp)
            .drawBehind {
                val barWidth = 3.dp.toPx()
                val gap = 2.dp.toPx()
                heights.forEachIndexed { i, height ->
                    val barHeight = height().dp.toPx()
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f),
                    )
                }
            },
    )
}
