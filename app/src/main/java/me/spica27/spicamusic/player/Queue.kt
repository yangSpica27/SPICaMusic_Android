package me.spica.music.player

import me.spica27.spicamusic.db.entity.Song


/**
 * 播放列表
 */
class Queue {

  // 指针
  @Volatile
  var index = 0
    private set

  // 播放列表
  @Volatile
  var heap = mutableListOf<Song>()
    private set

  fun currentSong(): Song? {
    return if (heap.isNotEmpty()) {
      heap[index]
    } else {
      null
    }
  }


  fun remove(index: Int) {
    heap.removeAt(index)
  }

  fun add(song: Song): Boolean {
    if (heap.find { it.songId == song.songId } == null) {
      heap.add(0, song)
      return true
    }
    return false
  }

  fun playNextSong(): Boolean {
    if (index >= heap.size - 1) return false
    index++
    return true
  }

  fun playPreSong(): Boolean {
    if (index <= 0) return false
    index--
    return true
  }

  fun reloadNewList(song: Song, songs: List<Song>) {
    heap.clear()
    heap.addAll(songs)
    this.index = 0
    heap.forEachIndexed { index, sg ->
      run {
        if (song.songId == sg.songId) {
          this.index = index
          return
        }
      }
    }
    heap.add(0, song)
    index = 0
  }

}