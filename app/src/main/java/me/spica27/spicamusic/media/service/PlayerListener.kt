package me.spica27.spicamusic.media.service

import androidx.media3.common.Player
import me.spica27.spicamusic.media.common.PlayMode
import me.spica27.spicamusic.media.utils.PlayerKVUtils
import org.koin.java.KoinJavaComponent.getKoin
import timber.log.Timber

class PlayerListener(
    private val player: Player,
) : Player.Listener {
    private val playerKVUtils = getKoin().get<PlayerKVUtils>()

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        val playMode =
            PlayMode.of(
                repeatMode = player.repeatMode,
                shuffleModeEnabled = shuffleModeEnabled,
            )
        playerKVUtils.setPlayMode(playMode.name)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        val playMode =
            PlayMode.of(
                repeatMode = repeatMode,
                shuffleModeEnabled = player.shuffleModeEnabled,
            )
        playerKVUtils.setPlayMode(playMode.name)
    }

    override fun onEvents(
        player: Player,
        events: Player.Events,
    ) {
        super.onEvents(player, events)
        for (i in 0 until events.size()) {
            Timber.tag("PlayerListener").e("onEvents: ${events.get(i)}")
        }
    }
}
