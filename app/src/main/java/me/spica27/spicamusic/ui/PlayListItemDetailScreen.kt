package me.spica27.spicamusic.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import me.spica27.spicamusic.viewModel.PlaylistViewModel
import me.spica27.spicamusic.wrapper.activityViewModel

@Composable
fun PlayListItemDetailScreen(
  playlistId: Long,
  songId: Long,
  playlistViewModel: PlaylistViewModel = activityViewModel(),
  navigator: NavBackStack? = null
) {




}