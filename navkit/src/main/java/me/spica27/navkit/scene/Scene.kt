package me.spica27.navkit.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.spica27.navkit.geometry.GeometryTransition

/**
 * 所有场景的抽象基类。
 *
 * 每个场景实例对应导航栈中的一个条目，持有：
 * - [id]：由 [me.spica27.navkit.path.NavigationPath] 分配的唯一自增整数
 * - [stage]：Compose 可观察的生命周期状态
 * - [stageMutex]：保证生命周期钩子串行执行的互斥锁
 *
 * 子类通过重写 [onPush] / [onAppear] / [waitAppear] /
 * [onDisappear] / [waitDisappear] / [onPop] 来实现进退场动画逻辑。
 */
@Stable
abstract class Scene {

    /** 由 NavigationPath 在 push 时赋值的场景唯一 ID */
    var id: Int = 0
        internal set

    /** 当前生命周期阶段，Compose 可观察 */
    val stage: MutableState<SceneStage> = mutableStateOf(SceneStage.Uninitialized)

    /**
     * 保证 push/pop 生命周期钩子串行的互斥锁。
     * 协程在 push 流程或 pop 流程期间持有此锁，防止并发触发。
     */
    internal val stageMutex = Mutex()

    /**
     * 场景的 Composable 内容渲染函数。
     * 子类必须实现此方法以提供 UI 内容。
     *
     * 调用时 [LocalScene][me.spica27.navkit.path.LocalScene] 已注入当前场景实例，
     * [LocalNavigationPath][me.spica27.navkit.path.LocalNavigationPath] 已注入导航路径。
     */
    @Composable
    abstract fun Content()

    /**
     * 场景持有的几何过渡（共享元素动画驱动器）。非 null 时，
     * [me.spica27.navkit.stack.NavigationStack] 在场景可见期间自动调用 [FloatingContent]
     * 渲染浮层（飞行封面动画）。子类重写此属性以提供自定义过渡。
     */
    open val geometryTransition: GeometryTransition? get() = null

    /**
     * 几何过渡期间渲染的浮层内容（飞行封面动画中的图像）。
     * 仅当 [geometryTransition] 非 null 时由 NavigationStack 调用。
     * 子类重写此方法以提供自定义浮层内容。
     */
    @Composable
    open fun FloatingContent() {}

    // ──────────────────────────────────────────────────────────────────────
    // 生命周期钩子（由 NavigationPath 的协程调用，子类可重写）
    // ──────────────────────────────────────────────────────────────────────

    /** push 开始时立即调用；可在此将动画进度 snap 到初始值（如 0f） */
    open suspend fun onPush() {}

    /**
     * 阻塞直到场景首帧已完成 Compose 布局（placed）。
     * StackScene 重写此方法等待 [me.spica27.navkit.scene.StackScene.placed] 变为 true。
     */
    open suspend fun waitAppear() {}

    /** 场景首帧就绪后调用；可在此启动进场动画（如将进度从 0f 动画到 1f） */
    open suspend fun onAppear() {}

    /**
     * 阻塞直到进场动画完成；用于等待下层场景准备好接受退场触发。
     * 默认实现直接返回（无等待），StackScene 可重写。
     */
    open suspend fun waitDisappear() {}

    /** 退场动画阶段；可在此将进度从 1f 动画到 0f */
    open suspend fun onDisappear() {}

    /** 场景已从栈中移除后调用；可在此释放资源或重置状态 */
    open suspend fun onPop() {}

    // ──────────────────────────────────────────────────────────────────────
    // 内部工具方法
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 在 [stageMutex] 锁内执行 [block]，用于 push/pop 流程的原子化控制。
     * NavigationPath 通过此方法保证同一时刻只有一个流程持有场景锁。
     */
    internal suspend fun <T> withStageLock(block: suspend () -> T): T =
        stageMutex.withLock { block() }
}
