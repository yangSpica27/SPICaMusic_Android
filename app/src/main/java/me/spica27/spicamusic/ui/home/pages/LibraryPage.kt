package me.spica27.spicamusic.ui.home.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 媒体库页面
 */
@Composable
fun LibraryPage(modifier: Modifier = Modifier) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "媒体库",
                largeTitle = "媒体库", // If not specified, title value will be used
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LibraryContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            scrollBehavior,
        )
    }
}

/**
 * 媒体库内容列表
 */
@Composable
private fun LibraryContent(
    modifier: Modifier = Modifier,
    scrollBehavior: ScrollBehavior,
) {
    val backStack = LocalNavBackStack.current

    // 媒体库列表项数据
    val libraryItems =
        listOf(
            LibraryItem("所有歌曲", Icons.Default.Home, Screen.AllSongs),
            LibraryItem("歌单", Icons.AutoMirrored.Filled.List, Screen.Playlists),
            LibraryItem("专辑", Icons.Default.Star, Screen.Albums),
            LibraryItem("艺术家", Icons.Default.Person, Screen.Artists),
            LibraryItem("最近添加", Icons.Default.Add, Screen.RecentlyAdded),
            LibraryItem("最常播放", Icons.Default.Favorite, Screen.MostPlayed),
            LibraryItem("播放历史", Icons.AutoMirrored.Filled.PlaylistPlay, Screen.PlayHistory),
            LibraryItem("文件夹", Icons.Default.Home, Screen.Folders),
        )

    LazyVerticalGrid(
        modifier =
            modifier
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(libraryItems) { item ->
            LibraryItemCard(
                title = item.title,
                icon = item.icon,
                onClick = {
                    backStack.add(item.screen)
                },
            )
        }
    }
}

/**
 * 媒体库列表项卡片
 */
@Composable
private fun LibraryItemCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(vertical = 6.dp),
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.padding(end = 16.dp),
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 媒体库列表项数据类
 */
private data class LibraryItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen,
)
