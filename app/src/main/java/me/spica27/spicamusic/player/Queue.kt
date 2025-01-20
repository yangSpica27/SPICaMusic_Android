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


  fun clear() {
    synchronized(this) {
      heap.clear()
      index.set(0)
    }
  }

  fun remove(index: Int) {
    if (index < 0 || index >= heap.size) return
    synchronized(this) {
      heap.removeAt(index)

      if (index > heap.size - 1) {
        // 如果删除的是最后一个元素 避免越界
        this.index.set(heap.size - 1)
      } else if (index < this.index.get()) {
        // 如果删除的是当前播放的歌曲之前的歌曲 指针前移
        this.index.getAndUpdate { it - 1 }
      }
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