package me.spica27.spicamusic.ui.player.pages

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueueEditTest {
    @Test
    fun `reported index disambiguates duplicate songs`() {
        val items = listOf(mediaItem("10"), mediaItem("10"), mediaItem("20"))

        assertEquals(1, resolveCurrentQueueIndex(items, reportedIndex = 1, currentMediaId = "10"))
    }

    @Test
    fun `stale reported index falls back to first match`() {
        val items = listOf(mediaItem("10"), mediaItem("10"), mediaItem("20"))

        assertEquals(0, resolveCurrentQueueIndex(items, reportedIndex = 5, currentMediaId = "10"))
        assertEquals(2, resolveCurrentQueueIndex(items, reportedIndex = 0, currentMediaId = "20"))
    }

    @Test
    fun `no current media resolves to minus one`() {
        val items = listOf(mediaItem("10"))

        assertEquals(-1, resolveCurrentQueueIndex(items, reportedIndex = 0, currentMediaId = null))
        assertEquals(-1, resolveCurrentQueueIndex(items, reportedIndex = 0, currentMediaId = "99"))
    }

    @Test
    fun `move without hidden items maps to visible positions`() {
        val keys = listOf("a", "b", "c", "d")

        assertEquals(0 to 2, resolveQueueMove(keys, listOf("b", "c", "a", "d"), "a"))
        assertEquals(3 to 0, resolveQueueMove(keys, listOf("d", "a", "b", "c"), "d"))
        assertEquals(1 to 3, resolveQueueMove(keys, listOf("a", "c", "d", "b"), "b"))
    }

    @Test
    fun `unchanged order produces no move`() {
        val keys = listOf("a", "b", "c")

        assertNull(resolveQueueMove(keys, keys, "b"))
        assertNull(resolveQueueMove(listOf("a"), listOf("a"), "a"))
    }

    @Test
    fun `move skips over hidden items in the source`() {
        // "h" 已从界面移除但仍在播放器里等待撤销
        val source = listOf("a", "h", "b", "c")

        assertEquals(3 to 0, resolveQueueMove(source, listOf("c", "a", "b"), "c"))
        assertEquals(0 to 3, resolveQueueMove(source, listOf("b", "c", "a"), "a"))
        assertEquals(2 to 0, resolveQueueMove(source, listOf("b", "a", "c"), "b"))
    }

    @Test
    fun `applying the resolved move reproduces the visible order`() {
        val source = listOf("a", "h1", "b", "c", "h2", "d")
        val hidden = setOf("h1", "h2")
        val visible = source.filterNot { it in hidden }
        for (moved in visible) {
            for (target in visible.indices) {
                val reordered = visible.toMutableList().apply { add(target, removeAt(indexOf(moved))) }
                val move = resolveQueueMove(source, reordered, moved)
                val result =
                    if (move == null) {
                        source
                    } else {
                        source.toMutableList().apply { add(move.second, removeAt(move.first)) }
                    }
                assertEquals("move $moved to $target", reordered, result.filterNot { it in hidden })
            }
        }
    }

    @Test
    fun `unknown key produces no move`() {
        assertNull(resolveQueueMove(listOf("a", "b"), listOf("b", "a"), "z"))
    }

    private fun mediaItem(mediaId: String): MediaItem = MediaItem.Builder().setMediaId(mediaId).build()
}
