package me.spica27.spicamusic.ui.player.scene

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.ui.component.DialogContainer
import me.spica27.spicamusic.ui.navigation.LocalBackStack

@Composable
fun CurrentListDialogContent() {
    val backStack = LocalBackStack.current

    // 页面自带顶栏；导航栏已由对话框策略避让，这里只需让开状态栏
    DialogContainer(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        shape = RoundedCornerShape(28.dp),
        enableGlass = false,
    ) {
    }
}
