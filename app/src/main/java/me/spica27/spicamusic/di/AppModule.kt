package me.spica27.spicamusic.di

import com.linc.amplituda.Amplituda
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.storage.api.IAlbumRepository
import me.spica27.spicamusic.storage.api.IMusicScanService
import me.spica27.spicamusic.storage.api.IPlayHistoryRepository
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailViewModel
import me.spica27.spicamusic.ui.audioeffects.AudioEffectsViewModel
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.pages.SearchViewModel
import me.spica27.spicamusic.ui.library.AlbumViewModel
import me.spica27.spicamusic.ui.library.AllSongsViewModel
import me.spica27.spicamusic.ui.library.LibraryPageViewModel
import me.spica27.spicamusic.ui.library.PlaylistDetailViewModel
import me.spica27.spicamusic.ui.library.PlaylistViewModel
import me.spica27.spicamusic.ui.player.CurrentPlaylistPanelViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.settings.SettingsViewModel
import me.spica27.spicamusic.utils.PreferencesManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * App 模块的依赖注入配置
 */
object AppModule {
    val appModule =
        module {
            // PreferencesManager
            single { PreferencesManager(androidContext()) }

            single<OkHttpClient>(
                createdAtStart = true,
            ) {
                OkHttpClient
                    .Builder()
                    .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                    .retryOnConnectionFailure(true)
                    .connectTimeout(3000L, TimeUnit.MILLISECONDS)
                    .readTimeout(3000L, TimeUnit.MILLISECONDS)
                    .callTimeout(3000L, TimeUnit.MILLISECONDS)
                    .writeTimeout(3000L, TimeUnit.MILLISECONDS)
                    .build()
            }
            single<Retrofit>(
                createdAtStart = true,
            ) {
                Retrofit
                    .Builder()
                    .client(get())
                    .baseUrl("http://api.spica27.site/api/v1/lyrics/")
                    .addConverterFactory(
                        MoshiConverterFactory
                            .create(Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build())
                            .withNullSerialization(),
                    ).addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
                    .build()
            }

            // Amplituda 分析工具
            single { Amplituda(androidContext()) }

            // 播放器 ViewModel - 全局共享
            viewModel {
                PlayerViewModel(
                    player = get<IMusicPlayer>(),
                    songRepository = get<ISongRepository>(),
                )
            }

            // 当前播放列表面板 ViewModel
            viewModel {
                CurrentPlaylistPanelViewModel(
                    playlistRepository = get<IPlaylistRepository>(),
                    songRepository = get<ISongRepository>(),
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
                    player = get<IMusicPlayer>(),
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
                    songRepository = get<ISongRepository>(),
                )
            }

            // 专辑详情页面 ViewModel
            viewModel { parameters ->
                AlbumDetailViewModel(
                    albumId = parameters.get<String>(),
                    albumRepository = get<IAlbumRepository>(),
                    player = get<IMusicPlayer>(),
                )
            }

            // 媒体库
            viewModel {
                LibraryPageViewModel(
                    songRepositoryImpl = get<ISongRepository>(),
                    albumRepositoryImpl = get<IAlbumRepository>(),
                    playlistRepositoryImpl = get<IPlaylistRepository>(),
                    historyRepository = get<IPlayHistoryRepository>(),
                )
            }

            // 专辑页面 ViewModel
            viewModel {
                AlbumViewModel(
                    albumRepository = get<IAlbumRepository>(),
                )
            }

            // 音效配置页面 ViewModel
            viewModel {
                AudioEffectsViewModel(
                    preferencesManager = get<PreferencesManager>(),
                    player = get<IMusicPlayer>(),
                )
            }
        }
}
