package me.spica27.navkit.scene

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 用于 SaveableStateHolder 的键集合，区分场景主内容区与浮层内容区。
 * 实现 Parcelable 以确保在进程重建时可被 rememberSaveable 持久化。
 */
sealed interface SceneKeySet : Parcelable {

    /**
     * 场景的主内容区键，由 sceneId 唯一标识。
     * @param key 场景 ID（NavigationPath 分配的自增整数）
     */
    @Parcelize
    data class Content(val key: Int) : SceneKeySet

    /**
     * 场景的浮层内容区键（用于 Dialog / ActionSheet 等浮层场景）。
     * @param key 场景 ID
     */
    @Parcelize
    data class FloatingContent(val key: Int) : SceneKeySet
}
