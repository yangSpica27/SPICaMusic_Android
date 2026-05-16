package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.runtime.Composable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.spica27.navkit.geometry.GeometryTransition
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.common.entity.Album

class AlbumDetailScene(
    private val album: Album,
    /** 封面飞行过渡；为 null 时跳过共享元素动画，仅执行普通滑入 */
    private val transition: GeometryTransition? = null,
) : StackScene() {
    /** 提供给 NavigationStack 的过渡驱动器；非 null 时自动触发 FloatingContent 渲染 */
    override val geometryTransition: GeometryTransition? = transition

    /** 飞行过渡期间渲染的浮层内容（专辑封面图像） */
    @Composable
    override fun FloatingContent() {
        AlbumCoverContent(album)
    }

    @Composable
    override fun Content() {
        AlbumDetailScreen(album = album, geometryTransition = transition)
    }

    /**
     * push 开始时将过渡进度重置为 0f，
     * 保证同一专辑多次打开时每次都能完整播放飞行动画。
     */
    override suspend fun onPush() {
        super.onPush()
        geometryTransition?.reset()
    }

    /**
     * 进场：同时执行屏幕滑入动画（super）和封面飞行动画（[geometryTransition]）。
     * 两者并发运行，互不阻塞。
     */
    override suspend fun onAppear() {
        coroutineScope {
            launch { super.onAppear() }
            launch { geometryTransition?.animateForward() }
        }
    }

    /**
     * 退场：同时执行屏幕滑出动画（super）和封面飞行反向动画（[geometryTransition]）。
     * 两者并发运行，封面会飞回列表中的源位置。
     */
    override suspend fun onDisappear() {
        coroutineScope {
            launch { super.onDisappear() }
            launch { geometryTransition?.animateReverse() }
        }
    }
}
