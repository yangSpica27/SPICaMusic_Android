package me.spica27.spicamusic.ui.player.scene

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.spica27.navkit.geometry.GeometryTransition
import me.spica27.navkit.geometry.geometryTarget
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.LyricsPanel
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import kotlin.math.roundToInt

/**
 * 全屏歌词页面。
 *
 */
class LyricScene(
    private val heroArtworkUri: Uri? = null,
    private val coverTransition: GeometryTransition? = null,
) : StackScene() {
    override val geometryTransitions: List<GeometryTransition> =
        listOfNotNull(coverTransition)

    /** push 开始时重置全部过渡进度，保证多次进入都能完整播放飞行动画 */
    override suspend fun onPush() {
        super.onPush()
        geometryTransitions.forEach { it.reset() }
    }

    /** 进场：屏幕滑入与三个共享元素的飞行并发执行 */
    override suspend fun onAppear() {
        coroutineScope {
            launch { super.onAppear() }
            geometryTransitions.forEach { transition ->
                launch { transition.animateForward() }
            }
        }
    }

    /** 退场：屏幕滑出与共享元素反向飞回并发执行 */
    override suspend fun onDisappear() {
        coroutineScope {
            launch { super.onDisappear() }
            geometryTransitions.forEach { transition ->
                launch { transition.animateReverse() }
            }
        }
    }

    @Composable
    override fun FloatingContent(key: String) {
        val playerViewModel = LocalPlayerViewModel.current
        val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
        val artworkUri = currentMediaItem?.mediaMetadata?.artworkUri ?: heroArtworkUri
        when (key) {
            coverTransition?.key -> FlyingCover(uri = artworkUri)
        }
    }

    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current

        BackHandler(true) {
            path.popTop()
        }

        val enterAnimEnd = enterAnimEnd.collectAsState()

        val playerViewModel = LocalPlayerViewModel.current
        val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

        // header 展示实时播放内容（歌曲切换时跟随更新），飞行浮层则使用 push 时捕获的快照
        val title =
            currentMediaItem
                ?.mediaMetadata
                ?.title
                ?.toString()
                ?: stringResource(R.string.unknown_song)
        val artist =
            currentMediaItem
                ?.mediaMetadata
                ?.artist
                ?.toString()
                ?: stringResource(R.string.unknown_artist)
        val artworkUri = currentMediaItem?.mediaMetadata?.artworkUri ?: heroArtworkUri

        Scaffold(
            topBar = {
                LyricsHeader(
                    title = title,
                    artist = artist,
                    artworkUri = artworkUri,
                    coverTransition = coverTransition,
                    enterProgressProvider = { enterProgress.value },
                    onBack = { path.popTop() },
                )
            },
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                ShowOnIdleContent(enterAnimEnd.value) {
                    LyricsPanel(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

// ============================================
// Header
// ============================================

/**
 * 全屏歌词 header：返回按钮 + 封面缩略图 + 歌名 / 作者。
 *
 * 封面、歌名、作者是三个几何过渡的目标节点；
 * 飞行期间由浮层接管显示（[GeometryTransition.shouldShowTarget]），落定后才显示本体。
 */
@Composable
private fun LyricsHeader(
    title: String,
    artist: String,
    artworkUri: Uri?,
    coverTransition: GeometryTransition?,
    enterProgressProvider: () -> Float,
    onBack: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .offset {
                    IntOffset(0, ((1f - enterProgressProvider()) * 24.dp.toPx()).roundToInt())
                }.padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        IconButton(
            onClick = onBack,
            colors =
                IconButtonDefaults.iconButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(20.dp),
            )
        }

        // 封面缩略图（飞行目标）
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .graphicsLayer {
                        alpha =
                            if (coverTransition == null || coverTransition.shouldShowTarget()) 1f else 0f
                    }.then(
                        if (coverTransition != null) {
                            Modifier.geometryTarget(coverTransition)
                        } else {
                            Modifier
                        },
                    ).clip(Shapes.MediumCornerBasedShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            AudioCover(
                uri = artworkUri,
                placeHolder = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = stringResource(R.string.cover_placeholder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
        ) {
            // 歌名（飞行目标）——与播放器页面同一文字样式，飞行为纯位移，无字号突变
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier =
                    Modifier
                        .basicMarquee(),
            )
            Spacer(modifier = Modifier.height(2.dp))
            // 作者（飞行目标）
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier =
                    Modifier
                        .basicMarquee(),
            )
        }
    }
}

// ============================================
// 飞行浮层内容
// ============================================

/** 飞行中的封面：跟随浮层矩形缩放，与源/目标使用同一图像模型保证视觉连续 */
@Composable
private fun FlyingCover(uri: Uri?) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        AudioCover(
            progressiveEnabled = false,
            uri = uri,
            placeHolder = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
