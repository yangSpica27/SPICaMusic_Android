package me.spica27.spicamusic.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.spica27.spicamusic.R
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.utils.navSharedBounds
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 艺术家页面
 */
@Composable
fun ArtistsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier =
            modifier
                .navSharedBounds(Screen.Artists)
                .fillMaxSize(),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.artists_title),
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(R.string.artists_list))
        }
    }
}
