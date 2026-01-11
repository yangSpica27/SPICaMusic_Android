package me.spica27.spicamusic.di

import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * App 模块的依赖注入配置
 */
object AppModule {
    val appModule =
        module {
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

            // TODO: 添加更多 ViewModel
        }
}
