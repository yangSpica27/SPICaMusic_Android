package me.spica27.spicamusic.ui.player.scene

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.component.DialogContainer
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.player.pages.CurrPlaylistPage

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
        CurrPlaylistPage(
            onNavigateBack = { backStack.removeLastOrNull() },
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navigationContentDescription = stringResource(R.string.back),
            // 与 DialogContainer 的 Surface(tonalElevation = 6.dp) 同色
            chromeColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
            contentWindowInsets = WindowInsets(0),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
