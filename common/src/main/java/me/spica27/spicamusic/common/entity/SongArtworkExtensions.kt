package me.spica27.spicamusic.common.entity

import android.net.Uri
import androidx.core.net.toUri

fun Song.getCoverUri(): Uri = "content://media/external/audio/albumart/$albumId".toUri()

fun Album.getCoverUri(): Uri? = "content://media/external/audio/albumart/$id".toUri()

fun Artist.getCoverUri(): Uri = "content://media/external/audio/albumart/$coverAlbumId".toUri()
