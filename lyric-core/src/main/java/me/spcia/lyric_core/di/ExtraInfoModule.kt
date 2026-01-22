package me.spcia.lyric_core.di

import me.spcia.lyric_core.ApiClient
import org.koin.dsl.module


val extraInfoModule = module {


  single {
    ApiClient(get())
  }


}