package me.spica27.spicamusic.feature.player.domain

import org.koin.dsl.module

val playerDomainModule =
    module {
        single { PlayerUseCases(get()) }
    }
