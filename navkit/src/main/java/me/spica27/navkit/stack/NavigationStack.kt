package me.spica27.navkit.stack

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.path.NavigationPath
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
    // StackScene 专用：从 Draw 阶段读取动画进度，避免 Composition-phase 重组
    val sceneModifier = if (scene is StackScene) {
        Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                // 首次布局完成时通知场景，解锁进场动画
                scene.notifyPlaced()
            }
            .graphicsLayer {
                // 在 Draw 阶段读取进度值（不触发重组）
                val enter = scene.enterProgress.value
                val ahead = scene.aheadEnterProgress(path.scenes)

                // 进场：整体 alpha 随进度渐显
                alpha = enter

                // 进场：从右侧滑入（translationX 从 +100dp → 0）
                translationX = (1f - enter) * size.width * ENTER_SLIDE_FRACTION

                // 背景压缩：下层场景随上层场景入场而轻微缩小并左移
                val compressionScale = COMPRESS_SCALE_MIN +
                        (1f - COMPRESS_SCALE_MIN) * (1f - ahead.coerceIn(0f, 1f))
                scaleX = compressionScale
                scaleY = compressionScale
                translationX -= ahead.coerceIn(0f, 1f) * size.width * COMPRESS_TRANSLATE_FRACTION
            }
            .drawWithCache {
                val ahead = scene.aheadEnterProgress(path.scenes)
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        color = Color.Black.copy(alpha = 0.2f * (ahead.coerceIn(0f, 1f)))
                    )
                }
            }
    } else {
        Modifier.fillMaxSize()
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
// 动画常量
// ──────────────────────────────────────────────────────────────────────────

/** 进场时从右侧偏移的宽度比例（1.0f = 完整屏宽） */
private const val ENTER_SLIDE_FRACTION = 0.15f

/** 背景压缩最小缩放比（下层场景在上层完全进场时的最小缩放） */
private const val COMPRESS_SCALE_MIN = 0.94f

/** 背景压缩时的左移偏移比例 */
private const val COMPRESS_TRANSLATE_FRACTION = 0.04f
