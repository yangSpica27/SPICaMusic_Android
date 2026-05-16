package me.spica27.navkit.stack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import me.spica27.navkit.geometry.GeometryTransition
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.path.NavigationPath
import me.spica27.navkit.scene.DialogScene
import me.spica27.navkit.scene.Scene
import me.spica27.navkit.scene.SceneKeySet
import me.spica27.navkit.scene.SceneStage
import me.spica27.navkit.scene.StackScene
import me.spica27.navkit.viewmodel.EntryViewModel

/**
 * 导航栈的顶层容器 Composable。
 *
 * ## 职责
 * 1. 创建并持有 [NavigationPath]（含 animationScope、EntryViewModel）
 * 2. 将路径通过 [LocalNavigationPath] 注入到子树
 * 3. 遍历 [NavigationPath.scenes]，为每个场景：
 *    - 提供 [SaveableStateHolder] 隔离 rememberSaveable 状态
 *    - 提供 [LocalScene] 注入当前场景实例
 *    - 应用 graphicsLayer 进退场变换（StackScene 专用）
 *    - 在首次布局后通知场景 placed（解锁进场动画）
 * 4. 注册 [BackHandler]，在栈深度 > 1 时拦截系统返回键
 *
 * ## 使用示例
 * ```kotlin
 * NavigationStack(initial = { MyRootScene() }) { path ->
 *     // path 可通过 LocalNavigationPath.current 在任意子 Composable 中访问
 * }
 * ```
 *
 * @param modifier 外层容器的 Modifier
 * @param initialScene 构造初始场景（根场景）；在 NavigationPath 首次创建时调用一次
 * @param content 导航栈的内容区，接收 [NavigationPath] 作为参数（通常不需要直接使用，
 *   通过 [LocalNavigationPath] 访问即可）
 */
@Composable
fun NavigationStack(
    modifier: Modifier = Modifier,
    initialScene: (() -> Scene)? = null,
    content: @Composable (NavigationPath) -> Unit = {}
) {
    val animationScope = rememberCoroutineScope()
    val saveableStateHolder = rememberSaveableStateHolder()

    // EntryViewModel 作用域：跟随调用处的 ViewModelStoreOwner（通常是 Activity）
    val entryViewModel = viewModel<EntryViewModel>()

    val path = remember {
        NavigationPath(
            animationScope = animationScope,
            initialScenes = if (initialScene != null) listOf(initialScene()) else emptyList()
        )
    }

    // 栈深度 > 1 时拦截系统返回键
    BackHandler(enabled = path.canPop) {
        path.popTop()
    }

    CompositionLocalProvider(LocalNavigationPath provides path) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // 渲染所有未 Disappeared 的场景（包括正在退场的，以保证退场动画可见）
            val visibleScenes = path.scenes.filter { scene ->
                scene.stage.value != SceneStage.Disappeared
            }

            visibleScenes.forEach { scene ->
                key(scene.id) {
                    // 每个场景隔离 rememberSaveable 状态
                    saveableStateHolder.SaveableStateProvider(
                        key = SceneKeySet.Content(scene.id)
                    ) {
                        CompositionLocalProvider(LocalScene provides scene) {
                            SceneContainer(
                                scene = scene,
                                path = path,
                                entryViewModel = entryViewModel
                            )
                        }
                    }
                }
            }

            // 几何过渡浮层（最顶层 z 序，渲染在所有场景之上）
            // 遍历可见场景，为持有 geometryTransition 的场景渲染飞行封面浮层
            visibleScenes.forEach { scene ->
                scene.geometryTransition?.let { t ->
                    key("overlay_${scene.id}") {
                        GeometryOverlay(transition = t) { scene.FloatingContent() }
                    }
                }
            }

            // 可扩展：在此叠加几何过渡浮层（GeometryTransition overlay）
            content(path)
        }
    }
}

/**
 * 单个场景的渲染容器，负责：
 * - 应用 graphicsLayer 变换（StackScene 的进退场缩放/位移）
 * - 通知场景已完成首帧布局（触发 waitAppear 解除）
 *
 * @param scene 要渲染的场景实例
 * @param path  当前 NavigationPath，用于读取全局 scenes 列表（计算 aheadEnterProgress）
 * @param entryViewModel 场景 ViewModel 作用域管理器
 */
@Composable
private fun SceneContainer(
    scene: Scene,
    path: NavigationPath,
    entryViewModel: EntryViewModel
) {
    // StackScene / DialogScene：从 Draw 阶段读取动画进度，避免 Composition-phase 重组
    val sceneModifier = when (scene) {
        is StackScene -> Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                // 首次布局完成时通知场景，解锁进场动画
                scene.notifyPlaced()
            }
            .graphicsLayer {
                // 在 Draw 阶段读取进度值（不触发重组）
                val enter = scene.enterProgress.value
                val ahead = scene.aheadEnterProgress(path.scenes)
                val fgProgress = scene.dialogForegroundProgress(path.scenes)

                // ── 自身进场动画 ──────────────────────────────────────────
                alpha = enter
                translationX = (1f - enter) * size.width * ENTER_SLIDE_FRACTION

                // ── StackScene 入场：背景压缩 + 左移 ─────────────────────
                val compressionAhead = ahead.coerceIn(0f, 1f)
                val compressionScale = COMPRESS_SCALE_MIN +
                        (1f - COMPRESS_SCALE_MIN) * (1f - compressionAhead)
                scaleX = compressionScale
                scaleY = compressionScale
                translationX -= compressionAhead * size.width * COMPRESS_TRANSLATE_FRACTION

                // ── StackScene 入场：背景模糊（API < 31 时 Compose 自动忽略）
                if (compressionAhead > 0f) {
                    val blurSigma = compressionAhead * density * BLUR_MAX_DP
                    renderEffect = BlurEffect(blurSigma, blurSigma, TileMode.Clamp)
                }

                // ── DialogScene 入场：背景变暗 + 去饱和度 ──────────────────
                if (fgProgress > 0f) {
                    // alpha 1.0 → 0.3，乘以自身进度保持进场同步
                    alpha *= lerp(1f, 0.3f, fgProgress)
                    // Rec. 601 灰度化 ColorMatrix
                    val g = fgProgress
                    val c = 1f - g
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix(
                            floatArrayOf(
                                0.213f * g + c, 0.715f * g, 0.072f * g, 0f, 0f,
                                0.213f * g, 0.715f * g + c, 0.072f * g, 0f, 0f,
                                0.213f * g, 0.715f * g, 0.072f * g + c, 0f, 0f,
                                0f, 0f, 0f, 1f, 0f,
                            )
                        )
                    )
                }
            }

        is DialogScene -> Modifier
            .fillMaxSize()
            .onGloballyPositioned { scene.notifyPlaced() }

        else -> Modifier.fillMaxSize()
    }

    // 将 EntryViewModel 的 ViewModelStoreOwner 注入到子树，
    // 子场景内调用 viewModel() 时将使用场景独立的 ViewModelStore
    val sceneOwner = remember(scene.id) {
        entryViewModel.viewModelStoreOwnerFor(scene.id)
    }

    // 场景 pop（Disappeared）后清理其 ViewModelStore
    DisposableEffect(scene.id) {
        onDispose {
            if (scene.stage.value == SceneStage.Disappeared) {
                entryViewModel.clearScene(scene.id)
            }
        }
    }

    CompositionLocalProvider(
        LocalViewModelStoreOwner provides sceneOwner
    ) {
        Box(modifier = sceneModifier) {
            scene.Content()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 几何过渡浮层
// ──────────────────────────────────────────────────────────────────────────

/**
 * 渲染飞行中的共享元素浮层。
 *
 * - 从 [transition] 读取当前进度，计算插值后的边界矩形
 * - 用 [absoluteOffset] + [size] 将内容定位到正确的屏幕位置/尺寸
 * - 进度达到 1f（动画完成）后自动隐藏
 */
@Composable
private fun GeometryOverlay(
    transition: GeometryTransition,
    content: @Composable () -> Unit,
) {
    // 读取动画进度（Compose 状态，每帧触发本 composable 重组）
    val progress = transition.progress.value
    if (progress >= 1f) return

    val bounds = transition.getBounds()
    val density = LocalDensity.current
    with(density) {
        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = bounds.left.toDp(),
                    y = bounds.top.toDp(),
                )
                .size(
                    width = bounds.width.coerceAtLeast(1f).toDp(),
                    height = bounds.height.coerceAtLeast(1f).toDp(),
                )
                .clip(RoundedCornerShape(16.dp)),
        ) {
            content()
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 动画常量
// ──────────────────────────────────────────────────────────────────────────

/** 进场时从右侧偏移的宽度比例（1.0f = 完整屏宽） */
private const val ENTER_SLIDE_FRACTION = 0.15f

/** 背景压缩最小缩放比（下层场景在上层完全进场时的最小缩放） */
private const val COMPRESS_SCALE_MIN = 0.94f

/** 背景压缩时的左移偏移比例 */
private const val COMPRESS_TRANSLATE_FRACTION = 0.04f

/** StackScene 进场时对背景施加的最大模糊半径（dp） */
private const val BLUR_MAX_DP = 24f
