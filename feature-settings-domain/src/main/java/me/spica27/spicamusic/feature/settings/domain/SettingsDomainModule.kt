package me.spica27.spicamusic.feature.settings.domain

import org.koin.dsl.module

val settingsDomainModule =
    module {
        single { SettingsUseCases(get()) }
    }
