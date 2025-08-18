package me.spica27.spicamusic.media.common

import android.os.Bundle
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands

/**
 * 自定义指令
 */
sealed class CustomCommand(
  val action: String,
) {
  object ACTION_SKIP_PREV : CustomCommand("me.spica.media.ACTION_SKIP_PREV")
  object ACTION_PLAY_PAUSE : CustomCommand("me.spica.media.ACTION_PLAY_PAUSE")
  object ACTION_SKIP_NEXT : CustomCommand("me.spica.media.ACTION_SKIP_NEXT")
  object ACTION_EXIT : CustomCommand("me.spica.media.ACTION_EXIT")

  fun toSessionCommand(): SessionCommand = SessionCommand(action, Bundle.EMPTY)
}

/**
 * 将自定义指令转换为SessionCommand
 */
fun SessionCommand.toCustomCommendOrNull(): CustomCommand? {
  return when (customAction) {
    CustomCommand.ACTION_SKIP_PREV.action -> CustomCommand.ACTION_SKIP_PREV
    CustomCommand.ACTION_PLAY_PAUSE.action -> CustomCommand.ACTION_PLAY_PAUSE
    CustomCommand.ACTION_SKIP_NEXT.action -> CustomCommand.ACTION_SKIP_NEXT
    CustomCommand.ACTION_EXIT.action -> CustomCommand.ACTION_EXIT
    else -> null
  }
}

/**
 * 注册自定义指令
 */
fun SessionCommands.Builder.registerCustomCommands(): SessionCommands.Builder = apply {
  addSessionCommands(
    listOf(
      CustomCommand.ACTION_SKIP_PREV.toSessionCommand(),
      CustomCommand.ACTION_PLAY_PAUSE.toSessionCommand(),
      CustomCommand.ACTION_SKIP_NEXT.toSessionCommand(),
      CustomCommand.ACTION_EXIT.toSessionCommand()
    )
  )
}