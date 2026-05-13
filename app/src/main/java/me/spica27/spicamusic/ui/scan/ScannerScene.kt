package me.spica27.spicamusic.ui.scan

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.widget.primaryClickable
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 扫描界面
 */
class ScannerScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val mediaLibrarySourceViewModel: MediaLibrarySourceViewModel = koinActivityViewModel()
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                path.popTop()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    title = {
                        Text(
                            text = "扫描音乐",
                        )
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier =
                    Modifier
                        .animateContentSize()
                        .fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        top = paddingValues.calculateTopPadding(),
                        bottom = paddingValues.calculateBottomPadding(),
                        start = 16.dp,
                        end = 16.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .height(250.dp),
                    ) {
                    }
                }
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .primaryClickable {
                                    mediaLibrarySourceViewModel.startMediaStoreScan()
                                }.padding(
                                    vertical = 8.dp,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "自动扫描",
                            modifier = Modifier,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                item {
                    ElevatedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            mediaLibrarySourceViewModel.startMediaStoreScan()
                        },
                    ) {
                        Text(
                            text = "自定义扫描",
                        )
                    }
                }
            }
        }
    }
}
