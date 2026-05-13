package me.spica27.spicamusic.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.ui.player.formatTime

class SongInfoScene(
    val song: Song,
) : DialogScene() {
    /**
     * 重写 Content()，将默认的"从中心缩放"替换为"从底部上滑 + 淡入/淡出"。
     * enterProgress 由父类 DialogScene 驱动（push→1f，pop→0f），无需额外声明。
     */
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
                        .graphicsLayer { alpha = enterProgress.value * 0.5f }
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { path.pop(scene) },
            )

            // ── 卡片：从底部上滑 + 淡入/淡出 ──
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val p = enterProgress.value
                            alpha = p
                            // p=0 时向下偏移 slideOffsetPx，p=1 时归位
                            translationY = (1f - p) * slideOffsetPx
                        },
            ) {
                DialogContent()
            }
        }
    }

    @Composable
    override fun DialogContent() {
        val path = LocalNavigationPath.current
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 24.dp,
                        ).clip(MaterialTheme.shapes.large)
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
            ) {
                Column(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("信息", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(5.dp))
                    Item("歌曲名称", song.displayName)
                    Item("歌手", song.artist)
                    Item("专辑", song.album)
                    Item("时长", formatTime(song.duration))
                    Item("文件路径", song.path)
                    Item("文件大小", "${song.size / 1024 / 1024} MB")
                    Item("文件格式", song.codec)

                    Spacer(Modifier.height(20.dp))
                    ElevatedButton(
                        onClick = {
                            path.popTop()
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

@Composable
private fun Item(
    title: String,
    content: String,
) {
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
