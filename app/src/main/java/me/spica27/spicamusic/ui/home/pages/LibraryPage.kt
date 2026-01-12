package me.spica27.spicamusic.ui.home.pages

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 媒体库页面
 */
@Composable
fun LibraryPage(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "媒体库",
            )
        },
    ) { paddingValues ->
        LibraryContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        )
    }
}

/**
 * 媒体库内容列表
 */
@Composable
private fun LibraryContent(modifier: Modifier = Modifier) {
    val backStack = LocalNavBackStack.current

    // 媒体库列表项数据
    val libraryItems =
        listOf(
            LibraryItem("所有歌曲", Icons.Default.Home, Screen.AllSongs),
            LibraryItem("歌单", Icons.Default.List, Screen.Playlists),
            LibraryItem("专辑", Icons.Default.Star, Screen.Albums),
            LibraryItem("艺术家", Icons.Default.Person, Screen.Artists),
            LibraryItem("最近添加", Icons.Default.Add, Screen.RecentlyAdded),
            LibraryItem("最常播放", Icons.Default.Favorite, Screen.MostPlayed),
            LibraryItem("播放历史", Icons.Default.List, Screen.PlayHistory),
            LibraryItem("文件夹", Icons.Default.Home, Screen.Folders),
        )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
        androidx.compose.foundation.layout.Row(
            modifier =
                Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
