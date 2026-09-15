package me.spica27.spicamusic.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Playlist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {
    @Test
    fun `root route cannot be popped`() {
        val navigator = createNavigator()

        assertNull(navigator.removeLastOrNull())
        assertEquals(listOf(HomeRoute), navigator.entries)
    }

    @Test
    fun `dialogs pop before screens`() {
        val navigator = createNavigator()
        navigator.add(SettingsRoute)
        navigator.add(SleepTimerRoute)

        assertEquals(SleepTimerRoute, navigator.removeLastOrNull())
        assertEquals(SettingsRoute, navigator.removeLastOrNull())
        assertEquals(listOf(HomeRoute), navigator.entries)
    }

    @Test
    fun `screen navigation clears transient dialogs`() {
        val navigator = createNavigator()
        navigator.add(SettingsRoute)
        navigator.add(SleepTimerRoute)

        navigator.add(AboutRoute)

        assertEquals(listOf(HomeRoute, SettingsRoute, AboutRoute), navigator.entries)
    }

    @Test
    fun `same screen is single top`() {
        val navigator = createNavigator()

        assertTrue(navigator.add(SettingsRoute))
        assertFalse(navigator.add(SettingsRoute))
        assertEquals(listOf(HomeRoute, SettingsRoute), navigator.entries)
    }

    @Test
    fun `navigator recreation retains screens and drops transient dialogs`() {
        val screenBackStack = NavBackStack<NavKey>(HomeRoute)
        val navigator = AppNavigator(screenBackStack)
        navigator.add(SettingsRoute)
        navigator.add(SleepTimerRoute)

        val recreatedNavigator = AppNavigator(screenBackStack)

        assertEquals(listOf(HomeRoute, SettingsRoute), recreatedNavigator.entries)
    }

    @OptIn(InternalSerializationApi::class)
    @Test
    fun `every screen route exposes a runtime serializer`() {
        val routes =
            listOf<ScreenRoute>(
                HomeRoute,
                AlbumDetailRoute(Album(id = "1", title = "Album", artist = "Artist")),
                ArtistDetailRoute(Artist(name = "Artist", songCount = 1, coverAlbumId = 1L)),
                PlaylistDetailRoute(Playlist(playlistId = 1L, playlistName = "Playlist")),
                AllPlaylistsRoute,
                PlaylistCreatorRoute,
                SearchRoute,
                SettingsRoute,
                AboutRoute,
                AppLicenseRoute,
                OpenSourceLicensesRoute,
                PrivacyPolicyRoute,
                AudioEffectsRoute,
                FavoriteRoute,
                IgnoredSongsRoute,
                ScannerRoute,
                LyricRoute(heroArtworkUri = "content://artwork/1"),
            )

        routes.forEach { route ->
            assertNotNull(route::class.serializer())
        }
    }

    private fun createNavigator(): AppNavigator = AppNavigator(NavBackStack<NavKey>(HomeRoute))
}
