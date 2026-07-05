package me.spica27.spicamusic.ui.favorite

import androidx.compose.runtime.Composable
import me.spica27.navkit.scene.StackScene

/**
 * 我的收藏 独立页面（全屏）
 */
class FavoriteScene : StackScene() {
    @Composable
    override fun Content() {
        FavoriteScreen()
    }
}
