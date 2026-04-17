package me.spica27.spicamusic.feature.player.domain

import me.spica27.spicamusic.player.api.IMusicPlayer

class PlayerUseCases(
    private val player: IMusicPlayer,
) : IMusicPlayer by player
