package me.spica27.spicamusic.module

import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.spica27.spicamusic.network.LyricApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  /**
   * 注入ohHttpClient
   */
  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient =
    OkHttpClient
      .Builder()
      .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
      .retryOnConnectionFailure(true)
      .connectTimeout(5000L, TimeUnit.MILLISECONDS)
      .readTimeout(3000L, TimeUnit.MILLISECONDS)
      .callTimeout(3000L, TimeUnit.MILLISECONDS)
      .writeTimeout(3000L, TimeUnit.MILLISECONDS)
      .build()

  @Provides
  @Singleton
  fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
    Retrofit
      .Builder()
      .client(okHttpClient)
      .baseUrl("http://api.spica27.site/api/v1/lyrics/")
      .addConverterFactory(MoshiConverterFactory.create().withNullSerialization())
      .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
      .build()

  @Provides
  @Singleton
  fun provideLyricApi(retrofit: Retrofit): LyricApi = retrofit.create(LyricApi::class.java)

}