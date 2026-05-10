package me.spica27.navkit.scene

/**
 * 场景的生命周期状态，对应进场/驻留/退场各阶段。
 *
 * 状态流转顺序：
 *   Uninitialized → Appearing → Appeared → Disappearing → Disappeared
 */
enum class SceneStage {
    /** 场景已创建但尚未开始进场动画 */
    Uninitialized,

    /** 正在执行进场动画 */
    Appearing,

    /** 进场动画结束，场景完全可见 */
    Appeared,

    /** 正在执行退场动画 */
    Disappearing,

    /** 退场动画结束，场景已从栈中移除 */
    Disappeared
}
