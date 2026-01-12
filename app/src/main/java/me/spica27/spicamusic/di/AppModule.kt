package me.spica27.spicamusic.di

import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.pages.SearchViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
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

            // TODO: 添加更多 ViewModel
        }
}
