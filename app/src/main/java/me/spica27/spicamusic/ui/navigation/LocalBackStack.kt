package me.spica27.spicamusic.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateList

val LocalBackStack =
    compositionLocalOf<SnapshotStateList<Any>> {
        error("No BackStack provided. Wrap your content with NavDisplay.")
    }
