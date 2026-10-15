package me.spica27.spicamusic.ui.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.ArtistKind

/** 推断出的群星没有标签原文，曲库里存的是这个默认名，显示时换成本地化文案 */
private const val DEFAULT_VARIOUS_NAME = "Various Artists"

/** 艺人显示名：未知艺人与默认的群星名使用本地化文案 */
@Composable
fun Artist.displayName(): String =
    when {
        kind == ArtistKind.UNKNOWN || name.isBlank() -> stringResource(R.string.unknown_artist)
        kind == ArtistKind.VARIOUS && name == DEFAULT_VARIOUS_NAME -> stringResource(R.string.various_artists)
        else -> name
    }

/** 专辑艺人显示名：同 [Artist.displayName] */
@Composable
fun Album.artistDisplay(): String =
    when (artist) {
        "" -> stringResource(R.string.unknown_artist)
        DEFAULT_VARIOUS_NAME -> stringResource(R.string.various_artists)
        else -> artist
    }
