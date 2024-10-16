package me.spica27.spicamusic.player

import androidx.lifecycle.AtomicReference
import me.spica27.spicamusic.db.entity.Song


/**
 * 播放列表
 */
@Suppress("unused")
class Queue {

  // 指针
  private val index = AtomicReference(0)

  // 播放列表

  private val heap = ArrayList<Song>()

  fun getPlayList(): List<Song> {
    return ArrayList(heap)
  }

  fun getIndex(): Int {
    return index.get()
  }


  fun currentSong(): Song? {
    return if (heap.isNotEmpty()) {
      heap[index.get()]
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
      if (index.get() >= heap.size - 1) return false
      index.getAndUpdate { it + 1 }
      return true
    }
  }

  fun playPreSong(): Boolean {
    synchronized(this) {
      if (index.get() <= 0) return false
      index.getAndUpdate { it - 1 }
      return true
    }
  }

  fun reloadNewList(song: Song, songs: List<Song>) {
    synchronized(this) {
      heap.clear()
      heap.addAll(songs)
      this.index.set(0)
      heap.forEachIndexed { index, sg ->
        run {
          if (song.songId == sg.songId) {
            this.index.set(index)
            return
          }
        }
      }
      heap.add(0, song)
      index.set(0)
    }
  }

}