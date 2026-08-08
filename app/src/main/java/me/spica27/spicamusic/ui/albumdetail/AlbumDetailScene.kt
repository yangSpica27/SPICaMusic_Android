package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.runtime.Composable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.common.entity.Album

class AlbumDetailScene(
    private val album: Album,
) : StackScene() {
    @Composable
    override fun Content() {
        AlbumDetailScreen(album = album)
    }

    override suspend fun onPush() {
        super.onPush()
        geometryTransitions.forEach { it.reset() }
    }

    override suspend fun onAppear() {
        coroutineScope {
            launch { super.onAppear() }
            geometryTransitions.forEach { transition ->
                launch { transition.animateForward() }
            }
        }
    }

    /**
     * 退场：同时执行屏幕滑出动画（super）和共享元素反向飞行动画（[geometryTransitions]），
     * 封面与标题会飞回列表中的源位置。
     */
    override suspend fun onDisappear() {
        coroutineScope {
            launch { super.onDisappear() }
            geometryTransitions.forEach { transition ->
                launch { transition.animateReverse() }
            }
        }
    }
}
