package me.spica27.spicamusic.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri

class SongMenuScene(
    val song: Song,
) : DialogScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val density = LocalDensity.current
        // 预先在 Composition 阶段把 dp 转成 px，避免在 graphicsLayer 里读取 CompositionLocal
        val slideOffsetPx = with(density) { 72.dp.toPx() }

        Box(
            Modifier
                .zIndex(3f)
                .fillMaxSize(),
        ) {
            // ── 半透明遮罩：随进度渐显，点击关闭 ──
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = enterProgress.value }
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { path.pop(scene) },
            )

            // ── 卡片：从底部上滑
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val p = enterProgress.value
                            translationY = (1f - p) * slideOffsetPx
                            alpha = p
                        },
            ) {
                DialogContent()
            }
        }
    }

    @Composable
    override fun DialogContent() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                vertical = 15.dp,
                                horizontal = 20.dp,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LandscapistImage(
                        imageModel = { song.getCoverUri() },
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.small),
                    )
                    Column(
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp),
                    ) {
                        Text(
                            text = song.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    IconButton(
                        onClick = { },
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭")
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(16.dp)
                            .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ControlButon(
                        title = "下一首播放",
                        icon = Icons.Default.PlayArrow,
                        modifier = Modifier.weight(1f),
                    )
                    ControlButon(
                        title = "添加到播放列表",
                        icon = Icons.Default.PlaylistPlay,
                        modifier = Modifier.weight(1f),
                    )
                    ControlButon(
                        title = "收藏",
                        icon = Icons.Default.Favorite,
                        modifier = Modifier.weight(1f),
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                )
                ControlItem(
                    title = "添加到播放列表",
                    icon = Icons.AutoMirrored.Default.PlaylistAdd,
                )
                ControlItem(
                    title = "查看专辑",
                    icon = Icons.Default.Album,
                )
                ControlItem(
                    title = "查看歌手",
                    icon = Icons.Default.SportsMartialArts,
                )
                ControlItem(
                    title = "查看歌曲信息",
                    icon = Icons.Default.Info,
                )
                Spacer(
                    modifier = Modifier.height(55.dp),
                )
            }
        }
    }
}

@Composable
private fun ControlButon(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
            }.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ControlItem(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable {
            }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
    }
}
