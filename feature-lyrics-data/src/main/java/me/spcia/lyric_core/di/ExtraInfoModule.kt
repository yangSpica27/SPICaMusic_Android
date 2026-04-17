package me.spcia.lyric_core.di

import me.spcia.lyric_core.ApiClient
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 歌词模块 DI 配置
 */
val extraInfoModule = module {

    // ApiClient - 歌词API客户端
    single {
        ApiClient(
            context = androidContext(),
            retrofit = get()
        )
    }
}
