package me.spica27.spicamusic.player

import me.spica27.spicamusic.db.entity.Song
import timber.log.Timber


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
  var heap = ArrayList<Song>()
    private set

  fun currentSong(): Song? {
    return if (heap.isNotEmpty()) {
      heap[index]
    } else {
      null
    }
  }


  fun remove(index: Int) {
    synchronized(this) {
      heap.removeAt(index)
    }
  }

  fun add(song: Song): Boolean {
    synchronized(this) {
      if (heap.find { it.songId == song.songId } == null) {
        heap.add(0, song)
        return true
      }
      return false
    }
  }

  fun playNextSong(): Boolean {
    synchronized(this) {
      if (index >= heap.size - 1) return false
      index++
      return true
    }
  }

  fun playPreSong(): Boolean {
    synchronized(this) {
      if (index <= 0) return false
      index--
      return true
    }
  }

  fun reloadNewList(song: Song, songs: List<Song>) {
    synchronized(this) {
      Timber.tag("QUEUE2.5").d("播放歌曲${song.displayName}")
      Timber.tag("QUEUE2.5").d("播放列表${songs}")
      heap.clear()
      heap.addAll(songs)
      this.index = 0
      Timber.tag("QUEUE3").d("播放歌曲${song.displayName}")
      Timber.tag("QUEUE3").d("播放列表${songs}")
      heap.forEachIndexed { index, sg ->
        run {
          if (song.songId == sg.songId) {
            this.index = index
            Timber.tag("Queue").e("reloadNewList: $index")
            return
          }
        }
      }
      heap.add(0, song)
      index = 0
    }
  }

}