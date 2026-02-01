package me.spcia.lyric_core.di

import me.spcia.lyric_core.ApiClient
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


val extraInfoModule = module {

  // 提供 OkHttpClient（如果尚未在其他模块提供）
  single {
    OkHttpClient.Builder()
      .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
      .followRedirects(true)
      .build()
  }

  single {
    ApiClient(
      context = androidContext(),
      retrofit = get(),
      okHttpClient = get()
    )
  }


}