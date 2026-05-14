package me.spica27.spicamusic.di

import com.linc.amplituda.Amplituda
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import me.spica27.spicamusic.core.preferences.PreferencesManager
import me.spica27.spicamusic.feature.library.domain.AlbumUseCases
import me.spica27.spicamusic.feature.library.domain.MusicScanUseCases
import me.spica27.spicamusic.feature.library.domain.PlayHistoryUseCases
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.ScanFolderUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.feature.lyrics.domain.LyricsUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.feature.settings.domain.SettingsUseCases
import me.spica27.spicamusic.ui.album.AlbumViewModel
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailViewModel
import me.spica27.spicamusic.ui.allsong.AllSongsViewModel
import me.spica27.spicamusic.ui.artist.ArtistViewModel
import me.spica27.spicamusic.ui.audioeffects.AudioEffectsViewModel
import me.spica27.spicamusic.ui.favorite.FavoriteViewModel
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.library.LibraryPageViewModel
import me.spica27.spicamusic.ui.listeningstats.ListeningStatsViewModel
import me.spica27.spicamusic.ui.mostedplayed.MostPlayedViewModel
import me.spica27.spicamusic.ui.player.CurrentPlaylistPanelViewModel
import me.spica27.spicamusic.ui.player.LyricsViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.playlist.PlaylistViewModel
import me.spica27.spicamusic.ui.playlistdetail.PlaylistDetailViewModel
import me.spica27.spicamusic.ui.search.SearchViewModel
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.settings.SettingsViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidApplication
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
                    player = get<PlayerUseCases>(),
                    songRepository = get<SongUseCases>(),
                )
            }

            // 歌词页面 ViewModel
            viewModel {
                LyricsViewModel(
                    player = get<PlayerUseCases>(),
                    lyricsUseCases = get<LyricsUseCases>(),
                )
            }

            // 当前播放列表面板 ViewModel
            viewModel {
                CurrentPlaylistPanelViewModel(
                    playlistRepository = get<PlaylistUseCases>(),
                    songRepository = get<SongUseCases>(),
                )
            }

            // 首页 ViewModel
            viewModel {
                HomeViewModel(
                    songRepository = get<SongUseCases>(),
                    playlistRepository = get<PlaylistUseCases>(),
                )
            }

            // 搜索页面 ViewModel
            viewModel {
                SearchViewModel(
                    songRepository = get<SongUseCases>(),
                )
            }

            // 设置页面 ViewModel
            viewModel {
                SettingsViewModel(
                    settingsUseCases = get<SettingsUseCases>(),
                )
            }

            // 媒体库来源页面 ViewModel
            viewModel {
                MediaLibrarySourceViewModel(
                    app = androidApplication(),
                    scanService = get<MusicScanUseCases>(),
                    folderRepository = get<ScanFolderUseCases>(),
                )
            }

            // 所有歌曲页面 ViewModel
            viewModel {
                AllSongsViewModel(
                    songRepository = get<SongUseCases>(),
                    player = get<PlayerUseCases>(),
                )
            }

            // 歌单页面 ViewModel
            viewModel {
                PlaylistViewModel(
                    playlistRepository = get<PlaylistUseCases>(),
                )
            }

            // 歌单详情页面 ViewModel
            viewModel { parameters ->
                PlaylistDetailViewModel(
                    playlistId = parameters.get<Long>(),
                    playlistRepository = get<PlaylistUseCases>(),
                    player = get<PlayerUseCases>(),
                    songRepository = get<SongUseCases>(),
                )
            }

            // 专辑详情页面 ViewModel
            viewModel { parameters ->
                AlbumDetailViewModel(
                    albumId = parameters.get<String>(),
                    albumRepository = get<AlbumUseCases>(),
                    player = get<PlayerUseCases>(),
                )
            }

            // 媒体库
            viewModel {
                LibraryPageViewModel(
                    songRepositoryImpl = get<SongUseCases>(),
                    albumRepositoryImpl = get<AlbumUseCases>(),
                    playlistRepositoryImpl = get<PlaylistUseCases>(),
                    historyRepository = get<PlayHistoryUseCases>(),
                )
            }

            // 专辑页面 ViewModel
            viewModel {
                AlbumViewModel(
                    albumRepository = get<AlbumUseCases>(),
                )
            }

            // 歌手页面 ViewModel
            viewModel {
                ArtistViewModel(
                    songUseCases = get<SongUseCases>(),
                )
            }

            // 音效配置页面 ViewModel
            viewModel {
                AudioEffectsViewModel(
                    settingsUseCases = get<SettingsUseCases>(),
                    player = get<PlayerUseCases>(),
                )
            }

            // 听歌统计页面 ViewModel
            viewModel {
                ListeningStatsViewModel(
                    historyRepository = get<PlayHistoryUseCases>(),
                    songRepository = get<SongUseCases>(),
                )
            }

            // 最常播放页面 ViewModel
            viewModel {
                MostPlayedViewModel(
                    app = androidApplication(),
                    historyRepository = get<PlayHistoryUseCases>(),
                    songRepository = get<SongUseCases>(),
                    playlistRepository = get<PlaylistUseCases>(),
                    player = get<PlayerUseCases>(),
                )
            }

            // 我的收藏页面 ViewModel
            viewModel {
                FavoriteViewModel(
                    app = androidApplication(),
                    songRepository = get<SongUseCases>(),
                    player = get<PlayerUseCases>(),
                    playlistRepository = get<PlaylistUseCases>(),
                )
            }
        }
}
