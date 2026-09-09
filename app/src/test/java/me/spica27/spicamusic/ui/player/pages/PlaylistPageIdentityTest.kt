package me.spica27.spicamusic.ui.player.pages

import androidx.media3.common.MediaItem
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistPageIdentityTest {
    @Test
    fun `duplicate media ids receive distinct occurrence keys`() {
        val items =
            listOf(
                mediaItem("10"),
                mediaItem("10"),
                mediaItem("20"),
                mediaItem("10"),
            )

        assertEquals(
            listOf("10#1", "10#2", "20#1", "10#3"),
            createPlaylistItemKeys(items),
        )
    }

    @Test
    fun `selection resolves the exact duplicate occurrences`() {
        val itemKeys = listOf("10#1", "10#2", "20#1", "10#3")

        assertEquals(
            listOf(1, 3),
            selectedPlaylistIndices(
                itemKeys = itemKeys,
                selectedItemKeys = setOf("10#2", "10#3"),
            ),
        )
    }

    private fun mediaItem(mediaId: String): MediaItem = MediaItem.Builder().setMediaId(mediaId).build()
}
