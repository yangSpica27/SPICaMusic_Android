package me.spica27.spicamusic.ui.player.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.player.pages.CurrPlaylistPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentListDialogContent() {
    val backStack = LocalBackStack.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    colors =
                        TopAppBarDefaults.topAppBarColors().copy(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                backStack.removeLastOrNull()
                            },
                            colors =
                                IconButtonDefaults.iconButtonColors().copy(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    title = {
                        Text(stringResource(R.string.now_playinglist))
                    },
                )
            },
        ) {
            Box(
                modifier = Modifier.padding(it),
            ) {
                CurrPlaylistPage(
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
