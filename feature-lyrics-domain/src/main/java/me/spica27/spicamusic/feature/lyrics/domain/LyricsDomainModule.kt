package me.spica27.spicamusic.feature.lyrics.domain

import org.koin.dsl.module

val lyricsDomainModule =
    module {
        single { LyricsUseCases(get(), get()) }
    }
