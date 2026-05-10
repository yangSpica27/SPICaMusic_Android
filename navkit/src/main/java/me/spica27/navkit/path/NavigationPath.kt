package me.spica27.navkit.path

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.spica27.navkit.scene.Scene
import me.spica27.navkit.scene.SceneStage

/**
 * CompositionLocal：提供最近的 [NavigationPath] 实例。
 * 使用 staticCompositionLocalOf 避免每次路径更新时触发重组（路径本身是稳定引用）。
 */
val LocalNavigationPath = compositionLocalOf<NavigationPath> {
    error("No NavigationPath provided. Wrap your content with NavigationStack { }.")
}

/**
 * CompositionLocal：提供当前场景实例。
 * static 版本避免场景切换时不相关的子树重组。
 */
val LocalScene = compositionLocalOf<Scene> {
    error("No Scene provided. This composable must be called inside a Scene's content lambda.")
}

/**
 * 导航栈的核心状态持有者。
 *
 * ## 职责
 * - 维护 [scenes]：Compose 可观察的场景栈（SnapshotStateList）
 * - 通过 [push] / [pop] 驱动场景生命周期（协程 + 互斥锁）
 * - 持有 [saveableStateHolder] 供场景隔离 rememberSaveable 状态
 * - 持有 [viewModelStore] 用于注册 [EntryViewModel]
 *
 * ## 线程安全
 * [push] 和 [pop] 都在 [animationScope] 中执行，每个场景用 [Scene.stageMutex]
 * 保证生命周期钩子串行，不会出现并发进退场。
 *
 * @param animationScope 用于驱动进退场协程的 CoroutineScope，
 *   通常来自 rememberCoroutineScope()，持有 MonotonicFrameClock
 * @param initialScenes 初始场景列表（通常只含一个根场景）
 */
@Stable
class NavigationPath(
    internal val animationScope: CoroutineScope,
    initialScenes: List<Scene> = emptyList()
) {

    /** Compose 可观察的场景栈；NavigationStack 直接读取此列表渲染 UI */
    val scenes: SnapshotStateList<Scene> = mutableStateListOf()

    /** 自增场景 ID 计数器 */
    private var nextId = 0

    /** 导航路径级别的 ViewModelStore，用于存放 EntryViewModel */
    internal val viewModelStore = ViewModelStore()

    init {
        // 初始场景走完整 push 流程，保证 StackScene.enterProgress 动画到 1f 后可见
        initialScenes.forEach { push(it) }
    }

    // ──────────────────────────────────────────────────────────────────────
    // 导航操作
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 将 [scene] 压入导航栈并启动进场动画。
     *
     * 流程（在 animationScope 协程中串行执行）：
     * 1. 分配 ID，设置 Appearing
     * 2. 添加到 scenes 列表（触发 Compose 重组，场景开始渲染）
     * 3. [Scene.onPush]：进度 snap 到初始值
     * 4. [Scene.waitAppear]：等待场景首帧布局完成
     * 5. 设置 Appeared
     * 6. [Scene.onAppear]：执行进场动画
     */
    fun push(scene: Scene) {
        animationScope.launch {
            scene.withStageLock {
                scene.id = nextId++
                scene.stage.value = SceneStage.Appearing
                scenes.add(scene)
                scene.onPush()
                scene.waitAppear()
                scene.stage.value = SceneStage.Appeared
                scene.onAppear()
            }
        }
    }

    /**
     * 将 [scene] 从导航栈弹出并启动退场动画。
     *
     * 流程（在 animationScope 协程中串行执行）：
     * 1. 设置 Disappearing
     * 2. [Scene.waitDisappear]：等待下层场景准备就绪
     * 3. [Scene.onDisappear]：执行退场动画
     * 4. 设置 Disappeared，从 scenes 列表移除
     * 5. [Scene.onPop]：释放资源
     */
    fun pop(scene: Scene) {
        animationScope.launch {
            scene.withStageLock {
                scene.stage.value = SceneStage.Disappearing
                scene.waitDisappear()
                scene.onDisappear()
                scene.stage.value = SceneStage.Disappeared
                scenes.remove(scene)
                scene.onPop()
            }
        }
    }

    /**
     * 弹出栈顶场景（如果栈内多于一个场景）。
     * @return 是否执行了 pop（栈顶存在且可弹出）
     */
    fun popTop(): Boolean {
        val top = scenes.lastOrNull() ?: return false
        if (scenes.size <= 1) return false
        pop(top)
        return true
    }

    /** 栈内是否有可以弹出的场景（scene 数量 > 1） */
    val canPop: Boolean get() = scenes.size > 1
}
