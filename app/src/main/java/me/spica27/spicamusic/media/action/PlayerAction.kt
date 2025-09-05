package me.spica27.spicamusic.media.action

import androidx.annotation.Keep
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.media.SpicaPlayer
import me.spica27.spicamusic.media.common.PlayMode
import org.koin.java.KoinJavaComponent.getKoin

@UnstableApi
@Keep
sealed class PlayerAction : Action() {
    private val spicaPlayer = getKoin().get<SpicaPlayer>()

    override fun action() {
        spicaPlayer.doAction(this)
    }

    // 播放
    data object Play : PlayerAction()

    // 暂停
    data object Pause : PlayerAction()

    // 下一曲
    data object SkipToNext : PlayerAction()

    // 上一曲
    data object SkipToPrevious : PlayerAction()

    // 播放歌曲
    data class PlayById(
        val mediaId: String,
    ) : PlayerAction()

    // 进度到
    data class SeekTo(
        val positionMs: Long,
    ) : PlayerAction()

    // 播放玩后暂停
    data class PauseWhenCompletion(
        val cancel: Boolean = false,
    ) : PlayerAction()

    // 设置播放模式
    data class SetPlayMode(
        val playMode: PlayMode,
    ) : PlayerAction()

    // 添加到下一曲
    data class AddToNext(
        val mediaId: String,
    ) : PlayerAction()

    // 删除列表歌曲
    data class RemoveWithMediaId(
        val mediaId: String,
    ) : PlayerAction()

    // 更新播放列表
    data class UpdateList(
        val mediaIds: List<String>,
        val mediaId: String? = null,
        val start: Boolean = false,
    ) : PlayerAction()

    // 自定义指令
    sealed class CustomAction(
        val name: String,
    ) : PlayerAction()

    // 播放或暂停
    data object PlayOrPause : CustomAction(PlayOrPause::class.java.name)

    // 从头开始
    data object ReloadAndPlay : CustomAction(ReloadAndPlay::class.java.name)

    companion object {
        fun of(name: String): CustomAction? {
            return when (name) {
                PlayOrPause::class.java.name -> return PlayOrPause
                ReloadAndPlay::class.java.name -> return ReloadAndPlay
                else -> null
            }
        }
    }
}
