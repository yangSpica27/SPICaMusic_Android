package me.spica27.spicamusic.di

import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.storage.api.IMusicScanService
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.pages.SearchViewModel
import me.spica27.spicamusic.ui.library.AllSongsViewModel
import me.spica27.spicamusic.ui.library.PlaylistDetailViewModel
import me.spica27.spicamusic.ui.library.PlaylistViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.settings.SettingsViewModel
import me.spica27.spicamusic.utils.PreferencesManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * App 模块的依赖注入配置
 */
object AppModule {
    val appModule =
        module {
            // PreferencesManager
            single { PreferencesManager(androidContext()) }

            // 播放器 ViewModel - 全局共享
            viewModel {
                PlayerViewModel(
                    player = get<IMusicPlayer>(),
                )
            }

            // 首页 ViewModel
            viewModel {
                HomeViewModel(
                    songRepository = get<ISongRepository>(),
                    playlistRepository = get<IPlaylistRepository>(),
                )
            }

            // 搜索页面 ViewModel
            viewModel {
                SearchViewModel(
                    songRepository = get<ISongRepository>(),
                )
            }

            // 设置页面 ViewModel
            viewModel {
                SettingsViewModel(
                    preferencesManager = get<PreferencesManager>(),
                )
            }

            // 媒体库来源页面 ViewModel
            viewModel {
                MediaLibrarySourceViewModel(
                    scanService = get<IMusicScanService>(),
                )
            }

            // 所有歌曲页面 ViewModel
            viewModel {
                AllSongsViewModel(
                    songRepository = get<ISongRepository>(),
                )
            }

            // 歌单页面 ViewModel
            viewModel {
                PlaylistViewModel(
                    playlistRepository = get<IPlaylistRepository>(),
                )
            }

            // 歌单详情页面 ViewModel
            viewModel { parameters ->
                PlaylistDetailViewModel(
                    playlistId = parameters.get<Long>(),
                    playlistRepository = get<IPlaylistRepository>(),
                    player = get<IMusicPlayer>(),
                )
            }
        }
}
