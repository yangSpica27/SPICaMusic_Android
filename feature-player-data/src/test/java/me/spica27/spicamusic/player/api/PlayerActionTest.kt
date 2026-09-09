package me.spica27.spicamusic.player.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerActionTest {
    @Test
    fun `removal indices are distinct valid and descending`() {
        assertEquals(
            listOf(4, 2, 0),
            normalizedRemovalIndices(
                indices = listOf(2, -1, 4, 2, 5, 0),
                itemCount = 5,
            ),
        )
    }

    @Test
    fun `empty timeline produces no removal indices`() {
        assertEquals(
            emptyList<Int>(),
            normalizedRemovalIndices(
                indices = listOf(0, 1),
                itemCount = 0,
            ),
        )
    }
}
