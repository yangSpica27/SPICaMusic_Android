package me.spica27.navkit.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

/**
 * 为导航栈中的每个场景维护独立的 [ViewModelStore]。
 *
 * 该 ViewModel 存储在导航路径级别的 [ViewModelStore] 中（生命周期跟随
 * NavigationPath），当场景被 pop 时，对应的 store 会被清除，
 * 场景内的所有 ViewModel 也随之销毁。
 *
 * ## 使用方式
 * 在 Compose 树中通过 `LocalEntryViewModel.current` 获取实例，
 * 然后调用 [viewModelStoreOwnerFor] 以 [LocalScene][me.spica27.navkit.path.LocalScene]
 * 的 id 作为 key，传入 `koinViewModel` 或 `viewModel()` 的 `viewModelStoreOwner` 参数。
 */
@Immutable
class EntryViewModel : ViewModel() {

    /**
     * 以场景 ID 为 key 的 ViewModelStore 映射表。
     * LinkedHashMap 保证插入顺序，便于按场景顺序调试。
     */
    private val owners = LinkedHashMap<Int, ViewModelStore>()

    /**
     * 返回指定场景 ID 对应的 [ViewModelStoreOwner]；
     * 如果尚不存在则自动创建并注册。
     *
     * @param sceneId 场景的唯一整数 ID，来自 [Scene.id][me.spica27.navkit.scene.Scene.id]
     */
    fun viewModelStoreOwnerFor(sceneId: Int): ViewModelStoreOwner {
        val store = owners.getOrPut(sceneId) { ViewModelStore() }
        return object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    /**
     * 清除并移除指定场景的 [ViewModelStore]，触发该 store 内所有
     * ViewModel 的 [ViewModel.onCleared]。
     * 由 NavigationPath 在 pop 完成后调用。
     *
     * @param sceneId 要清除的场景 ID
     */
    internal fun clearScene(sceneId: Int) {
        owners.remove(sceneId)?.clear()
    }

    /**
     * ViewModel 被销毁时（NavigationPath 所在的 Activity/Fragment 销毁）
     * 清除所有场景的 ViewModelStore。
     */
    override fun onCleared() {
        owners.values.forEach { it.clear() }
        owners.clear()
    }
}
