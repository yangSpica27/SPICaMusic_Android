package me.spica27.spicamusic.module

import android.app.Application
import androidx.room.Room
import com.linc.amplituda.Amplituda
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import me.spica27.spicamusic.db.AppDatabase
import me.spica27.spicamusic.db.dao.LyricDao
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.network.LyricApi
import me.spica27.spicamusic.repository.LyricRepository
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.viewModel.LyricViewModel
import me.spica27.spicamusic.viewModel.MusicSearchViewModel
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.PlaylistViewModel
import me.spica27.spicamusic.viewModel.SelectSongViewModel
import me.spica27.spicamusic.viewModel.SettingViewModel
import me.spica27.spicamusic.viewModel.SongViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.dsl.onClose
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object InjectModules {


  /**
   * 网络的注入
   */
  val networkModule = module {
    single<OkHttpClient> {
      OkHttpClient
        .Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .retryOnConnectionFailure(true)
        .connectTimeout(5000L, TimeUnit.MILLISECONDS)
        .readTimeout(3000L, TimeUnit.MILLISECONDS)
        .callTimeout(3000L, TimeUnit.MILLISECONDS)
        .writeTimeout(3000L, TimeUnit.MILLISECONDS)
        .build()
    }
    single<Retrofit> {
      Retrofit
        .Builder()
        .client(get())
        .baseUrl("http://api.spica27.site/api/v1/lyrics/")
        .addConverterFactory(MoshiConverterFactory.create().withNullSerialization())
        .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .build()
    }
    single<LyricApi> {
      get<Retrofit>().create(LyricApi::class.java)
    }
  }

  /**
   * 持久化
   */
  val persistenceModule = module {
    single<AppDatabase> {
      Room
        .databaseBuilder(
          get<Application>(), AppDatabase::class.java,
          "spica_music.db"
        )
        .fallbackToDestructiveMigration(false)
        .build()
    }

    single<LyricDao> {
      get<AppDatabase>().lyricDao()
    }

    single<SongDao> {
      get<AppDatabase>().songDao()
    }

    single<PlaylistDao> {
      get<AppDatabase>().playlistDao()
    }
  }

  /**
   * 工具类
   */
  val utilsModule = module {
    single<Amplituda> {
      Amplituda(get<Application>())
    } onClose {
      it?.clearCache()
    }
    single<DataStoreUtil> {
      DataStoreUtil(get<Application>())
    }
  }

  val repositoryModule = module {
    single<LyricRepository> {
      LyricRepository(
        lyricApi = get(),
        lyricDao = get()
      )
    }
  }

  /**
   * ViewModel
   */
  val viewModelModule = module {
    viewModel {
      SongViewModel(
        songDao = get<SongDao>(),
        playlistDao = get<PlaylistDao>()
      )
    }
    viewModel {
      PlaylistViewModel(
        playlistDao = get<PlaylistDao>(),
        songDao = get<SongDao>()
      )
    }
    viewModel {
      LyricViewModel(
        lyricRepository = get<LyricRepository>()
      )
    }
    viewModel {
      SettingViewModel(
        dataStoreUtil = get<DataStoreUtil>()
      )
    }
    viewModel {
      SelectSongViewModel(
        songDao = get<SongDao>(),
        playlistDao = get<PlaylistDao>()
      )
    }
    viewModel {
      PlayBackViewModel(
        songDao = get<SongDao>(),
        amplituda = get<Amplituda>(),
        lyricDao = get<LyricDao>()
      )
    }
    viewModel {
      MusicSearchViewModel(
        songDao = get<SongDao>()
      )
    }
  }

}