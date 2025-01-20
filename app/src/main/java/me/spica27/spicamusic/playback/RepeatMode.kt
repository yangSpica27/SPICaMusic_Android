package me.spica27.spicamusic.playback


// 循环方式
enum class RepeatMode {

  NONE,// 顺序播放
  ALL, // 循环
  TRACK; // 单曲循环

  // 切换循环方式
  fun increment() =
    when (this) {
      NONE -> ALL
      ALL -> TRACK
      TRACK -> NONE
    }


  val icon: Int
    get() =
      when (this) {
        NONE -> -1
        ALL -> -1
        TRACK -> -1
      }


}
